package com.fireblaze.truly_enchanting.blockentity;

import com.fireblaze.truly_enchanting.block.MonolithBlock;
import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.MagicAccumulator;
import com.fireblaze.truly_enchanting.registry.ModBlockEntities;
import com.fireblaze.truly_enchanting.menu.MonolithMenu;
import com.fireblaze.truly_enchanting.registry.ModRunes;
import com.fireblaze.truly_enchanting.runes.RuneDefinition;
import com.fireblaze.truly_enchanting.runes.RuneItem;
import com.fireblaze.truly_enchanting.runes.RuneLoader;
import com.fireblaze.truly_enchanting.util.MagicSourceBlocks;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;

public class MonolithBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
            //return stack.is(ModTags.Items.RUNES); //todo  fix that
        }
    };

    public ItemStack getItemInSlot(int slot) {
        return items.getStackInSlot(slot);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> items);

    public MonolithBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MONOLITH_BLOCK_ENTITY.get(), pos, state);
    }

    // Capability anbieten
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }


    @Override
    public void setRemoved() {
        super.setRemoved();
        handler.invalidate();
    }

    // ############## Inventory ##############

    public boolean tryInsertOrExtractItem(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack slotItem = items.getStackInSlot(0);

        boolean changed = false;

        // 1. Hand leer → Item rausnehmen
        // 1. Hand leer → Item rausnehmen
        if (held.isEmpty() && !slotItem.isEmpty()) {
            player.setItemInHand(hand, slotItem.copy());
            items.setStackInSlot(0, ItemStack.EMPTY);
            changed = true;
        }

        // 2. Hand voll & Slot leer → Item einsetzen
        else if (!held.isEmpty() && slotItem.isEmpty()) {
            if (items.isItemValid(0, held)) {
                items.setStackInSlot(0, held.split(1));
                changed = true;
            }
        }

        // 3. Hand voll & Slot voll → Items tauschen, falls Hand-Item valid ist
        else if (!held.isEmpty() && !slotItem.isEmpty()) {
            if (items.isItemValid(0, held)) {
                // Hand-Item und Slot-Item tauschen
                ItemStack temp = slotItem.copy();
                items.setStackInSlot(0, held.split(1));  // 1. Hand-Item in Slot
                player.setItemInHand(hand, temp);        // ursprüngliches Slot-Item in Hand
                changed = true;
            }
        }

        if (changed) {
            setChanged();
            updateLight();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); // BlockUpdate für Clients
            }
        }

        return changed;
    }

    private void updateLight() {
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            int light = items.getStackInSlot(0).isEmpty() ? 0 : 15;
            level.setBlock(worldPosition, state.setValue(MonolithBlock.LIGHT, light), 3);
        }
    }

    // ############## Interface ##############

    @Override
    public Component getDisplayName() {
        return Component.literal("Monolith");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(this.worldPosition);

        return new MonolithMenu(id, inv, buf);
    }

    // ############## Saves ##############

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            setChanged(); // markiert BE
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); // Client rendert direkt
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        handleUpdateTag(pkt.getTag());
    }


    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Items", items.serializeNBT());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        items.deserializeNBT(tag.getCompound("Items"));
    }

    public void dropInventory(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.copy());
            }
        }
    }

    // ############## Runes ##############

    public RuneDefinition getCurrentRune() {
        ItemStack stack = items.getStackInSlot(0);

        if (stack.isEmpty()) return null;
        if (!(stack.getItem() instanceof RuneItem)) return null;
        if (!stack.hasTag() || !stack.getTag().contains("rune_id")) return null;

        String runeId = stack.getTag().getString("rune_id");
        return RuneLoader.getRuneDefinition(runeId); // Map<String, RuneDefinition> in ModRunes
    }



    // ############## Pathfinding/Flood-Fill-Scan ##############

    // Map um für jeden Blocktyp zu tracken, wie viel Magie schon von diesem Block gezählt wurde
    private Map<Block, Integer> blockCurrentPower = new HashMap<>();
    private Map<TagKey<Block>, Integer> tagCurrentPower = new HashMap<>();
    private int currentMagicPower = 0;
    private int maxMagicPower = 0;

    /**
     * Scan die Umgebung nach RuneBlöcken
     *
     * @param scanCap    maximale Anzahl Blöcke, die gezählt werden
     * @param radiusCap  maximale Entfernung vom Monolithen
     * @return Summe der MagiePower der gefundenen Blöcke
     */
    private final MagicAccumulator accumulator = new MagicAccumulator();
    public MagicAccumulator getMagicAccumulator () {
        return accumulator;
    }
    public int scanSurroundingBlocks(int scanCap, int radiusCap) {
        if (this.getCurrentRune() == null) {
            return 0;
        }
        accumulator.scan(level, this.worldPosition, this.getCurrentRune().blockMap, this.getCurrentRune().blockTagsMap, scanCap, radiusCap);
        setChanged();

        return accumulator.getCurrentMagicPowerIncreaseRate();
    }

    public int calculateCurrentMagicPower() {
        currentMagicPower = scanSurroundingBlocks(3000, 25);
        return currentMagicPower;
    }
    public int getCurrentMagicPower() {
        return currentMagicPower;
    }

    public int getMaxMagicPower() {
        return maxMagicPower;
    }

    public int getCurrentPowerForBlock(Block block) {
        return blockCurrentPower.getOrDefault(block, 0);
    }
    public int getCurrentPowerForTag(TagKey<Block> block) {
        return tagCurrentPower.getOrDefault(block, 0);
    }

    public int getMaxPowerForBlock(Block block) {
        RuneDefinition rune = getCurrentRune();
        if (rune == null) return 0;
        MagicSourceBlocks rb = rune.blockMap.get(block);
        return rb != null ? rb.magicCap : 0;
    }

    public void insertRune(RuneDefinition rune) {
        if (rune == null) return;

        // Rune-Item finden
        Item runeItem = ModRunes.RUNE.get();

        ItemStack stack = new ItemStack(runeItem);
        stack.setCount(1);
    }

}
