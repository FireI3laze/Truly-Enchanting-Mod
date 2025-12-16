package com.fireblaze.magic_overhaul.block;

import com.fireblaze.magic_overhaul.blockentity.MonolithBlockEntity;
import com.fireblaze.magic_overhaul.network.SyncMagicAccumulatorPacket;
import com.fireblaze.magic_overhaul.registry.ModItems;
import com.fireblaze.magic_overhaul.util.MagicCostCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MonolithBlock extends Block implements EntityBlock {
    // Ganz oben in deiner Block-Klasse
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);


    public static final net.minecraft.world.level.block.state.properties.EnumProperty<DoubleBlockHalf> HALF =
            BlockStateProperties.DOUBLE_BLOCK_HALF;

    public MonolithBlock(Properties props) {
        super(props);
        registerDefaultState(getStateDefinition().any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(LIGHT, 0));

    }

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, LIGHT);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();

        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).isAir()) {
            return defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER);
        } else {
            return null;
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Obere Hälfte setzen
        if (!level.isClientSide) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()) {
                level.setBlock(above, defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(LIGHT, state.getValue(LIGHT)), 3);
            }

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MonolithBlockEntity monolithBE) {
                monolithBE.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }


    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(HALF);
        BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        if (level.getBlockState(other).getBlock() == this) {
            level.destroyBlock(other, false);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack held = player.getItemInHand(hand);
        if(held.getItem() == ModItems.WAND.get()) {
            return InteractionResult.PASS;
        }

        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            // Place item directly
            if (!level.isClientSide) {
                MonolithBlockEntity be = getBE(level, pos);
                if (be != null) {
                    if (be.tryInsertOrExtractItem(player, hand)) {
                        return InteractionResult.SUCCESS;
                    }
                    // todo float magicPower = be.scanSurroundingBlocks(300, 25); do I need this here?
                    // MagicOverhaul.LOGGER.debug("Monolith scanned magic power: {}", magicPower);

                    if (player instanceof ServerPlayer serverPlayer) {
                        SyncMagicAccumulatorPacket.sendToClient(serverPlayer, be.getBlockPos(), be.getMagicAccumulator());
                        NetworkHooks.openScreen(serverPlayer, be, be.getBlockPos());
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Lower half: open GUI
        if (!level.isClientSide) {
            MonolithBlockEntity be = getBE(level, pos);
            assert be != null;

            int magicPower = be.scanSurroundingBlocks(3000, 25);
            //MagicOverhaul.LOGGER.debug("Monolith scanned magic power: {}", magicPower);

            // --- Alle Enchantments der Rune berechnen ---
            //if (runeDef == null) player.sendSystemMessage(Component.literal("§cNo Rune set."));

            if (player instanceof ServerPlayer serverPlayer) {
                SyncMagicAccumulatorPacket.sendToClient(serverPlayer, be.getBlockPos(), be.getMagicAccumulator());

                // Open GUI
                NetworkHooks.openScreen(serverPlayer, be, be.getBlockPos());
            }
        }

        return InteractionResult.SUCCESS;
    }

    private int getUnlockedLevel(Enchantment enchant, float magicPower) {
        int highest = 0;

        for (int lvl = 1; lvl <= enchant.getMaxLevel(); lvl++) {
            int required = MagicCostCalculator.calculateMagicRequirement(enchant, lvl);

            if (magicPower >= required) {
                highest = lvl;
            } else {
                break; // weitere Level werden nur teurer
            }
        }

        return highest;
    }


    private MonolithBlockEntity getBE(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) pos = pos.below();

        if (level.getBlockEntity(pos) instanceof MonolithBlockEntity be)
            return be;

        return null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MonolithBlockEntity(pPos, pState);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LIGHT);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MonolithBlockEntity monolithBE) {
                monolithBE.dropInventory(level, pos);
            }
        }
        super.onRemove(oldState, level, pos, newState, isMoving);
    }
}
