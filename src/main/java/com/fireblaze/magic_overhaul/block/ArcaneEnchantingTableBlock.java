package com.fireblaze.magic_overhaul.block;

import com.fireblaze.magic_overhaul.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.magic_overhaul.enchanting.CachedMagicResult;
import com.fireblaze.magic_overhaul.menu.ArcaneEnchantingTableMenu;
import com.fireblaze.magic_overhaul.network.Network;
import com.fireblaze.magic_overhaul.network.SyncMagicAccumulatorPacket;
import com.fireblaze.magic_overhaul.network.SyncSelectionPacket;
import com.fireblaze.magic_overhaul.network.SyncUnlockedEnchantmentsPacket;
import com.fireblaze.magic_overhaul.registry.ModBlockEntities;
import com.fireblaze.magic_overhaul.registry.ModItems;
import com.fireblaze.magic_overhaul.util.BindingManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ArcaneEnchantingTableBlock extends Block implements EntityBlock {

    public ArcaneEnchantingTableBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == ModItems.WAND.get()) {
            return InteractionResult.PASS; // Wand ignoriert GUI
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof ArcaneEnchantingTableBlockEntity tableBE)) return InteractionResult.PASS;

            // --- 1. Berechnung der Magic-Werte ---
            Map<BlockPos, CachedMagicResult> data = tableBE.getOrComputeMonolithMagic((ServerLevel) level);

            // --- 2. Zusammenführen aller freigeschalteten Enchantments ---
            Map<Enchantment, Integer> unlocked = new HashMap<>();
            for (var result : data.values()) {
                unlocked.putAll(result.unlockedLevels);
            }

            tableBE.scanSurroundingBlocks(3000, 25);
            tableBE.getMagicAccumulator().updateMaxMagic(tableBE);

            SyncMagicAccumulatorPacket.sendToClient(serverPlayer, tableBE.getBlockPos(), tableBE.getMagicAccumulator()); // Packet for the accumulated magic


            // --- 3. GUI öffnen und Sync an Client ---
            // --- GUI öffnen ---
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Arcane Enchanting Table");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeBlockPos(pos);
                    return new ArcaneEnchantingTableMenu(id, inv, buf);
                }
            }, pos);


            // --- Direkt danach: aktuellen Status senden ---
            Network.sendToClient(serverPlayer, new SyncUnlockedEnchantmentsPacket(unlocked)); // Packet for all enchantments that are unlocked from the monoliths
            Network.sendToClient(serverPlayer, new SyncSelectionPacket(tableBE.getSelected())); // Packet for the current selection on the enchanter to display live changes between players
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ArcaneEnchantingTableBlockEntity tableBE) {
                tableBE.dropInventory(level, pos);
            }
            assert be != null;
            BindingManager.removeBindingsForTable(level, pos);

            level.destroyBlock(pos, true);
        }
        super.onRemove(oldState, level, pos, newState, isMoving);
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneEnchantingTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        /*
        if (level.isClientSide) {
            return null; // Der Tisch tickt nur serverseitig todo Muss aber auch client seitig ticken?
        }
        */

        return type == ModBlockEntities.ARCANE_ENCHANTING_TABLE.get()
                ? (lvl, pos, st, be) -> ArcaneEnchantingTableBlockEntity.tick(lvl, pos, st, (ArcaneEnchantingTableBlockEntity) be)
                : null;
    }
}
