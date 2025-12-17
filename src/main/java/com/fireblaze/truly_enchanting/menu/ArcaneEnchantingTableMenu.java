package com.fireblaze.truly_enchanting.menu;

import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.truly_enchanting.item.MagicEssenceItem;
import com.fireblaze.truly_enchanting.registry.ModItems;
import com.fireblaze.truly_enchanting.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;

public class ArcaneEnchantingTableMenu extends AbstractContainerMenu {
    private Map<Enchantment, Integer> unlockedEnchantments = new HashMap<>();
    private Map<Enchantment, Integer> selectedEnchantments = new HashMap<>(); //todo move selected Enchantments from Entity to here
    private final BlockPos pos;
    private final ArcaneEnchantingTableBlockEntity arcane;

    public ArcaneEnchantingTableMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.ARCANE_ENCHANTING_TABLE_MENU.get(), id);
        this.pos = buf.readBlockPos();
        ArcaneEnchantingTableBlockEntity tempArcane = null;

        var be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof ArcaneEnchantingTableBlockEntity arcaneBE) {
            tempArcane = arcaneBE; // speichern
        }
        this.arcane = tempArcane;

        assert this.arcane != null;
        IItemHandler blockInv = this.arcane.getItemHandler();

        // Slot 0: Item, das verzaubert werden soll
        this.addSlot(new SlotItemHandler(blockInv, 0, 44, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return canBeEnchanted(stack);
            }
        });

        // Slot 1: Magic Essence
        this.addSlot(new SlotItemHandler(blockInv, 1, 116, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.MAGIC_ESSENCE.get() &&
                        stack.getOrCreateTag().getBoolean(MagicEssenceItem.NBT_CHARGED);
            }
        });

        // Spieler-Inventar
        int startX = 8;
        int startY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInv,
                        col + row * 9 + 9,
                        startX + col * 18,
                        startY + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInv, col, startX + col * 18, startY + 58));
        }
    }

    private boolean canBeEnchanted(ItemStack stack) {
        // 1) Normale Tools (damageable)
        if (stack.isDamageableItem()) return true;

        // 2) Unbreakable Tools (NBT: {Unbreakable:1b})
        if (stack.hasTag() && stack.getTag().getBoolean("Unbreakable")) return true;

        return false;
    }



    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            stack = stackInSlot.copy();
            int containerSlots = 2; // unsere beiden Slots im Arcane Table

            if (index < containerSlots) {
                // Vom Container in Spielerinventar
                if (!moveItemStackTo(stackInSlot, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Vom Spieler in Container
                boolean moved = false;

                // Slot 1: Magic Essence
                Slot essenceSlot = this.slots.get(1);
                if (stackInSlot.getItem() == ModItems.MAGIC_ESSENCE.get() &&
                        stackInSlot.getOrCreateTag().getBoolean(MagicEssenceItem.NBT_CHARGED) &&
                        essenceSlot.mayPlace(stackInSlot)) {

                    moved = moveItemStackTo(stackInSlot, 1, 2, false);
                }

                // Slot 0: zu verzauberndes Item
                Slot itemSlot = this.slots.get(0);
                if (!moved && canBeEnchanted(stackInSlot) && itemSlot.mayPlace(stackInSlot)) {
                    moved = moveItemStackTo(stackInSlot, 0, 1, false);
                }

                if (!moved) {
                    return ItemStack.EMPTY; // Item darf nicht verschoben werden
                }
            }

            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return stack;
    }
    public void setUnlockedEnchantments(Map<Enchantment, Integer> map) {
        this.unlockedEnchantments = map;
    }

    public Map<Enchantment, Integer> getUnlockedEnchantments() {
        return unlockedEnchantments;
    }
    public void setSelectedEnchantments(Map<Enchantment, Integer> map) {
        arcane.getSelected().clear();
        arcane.getSelected().putAll(map);
    }
    public Map<Enchantment, Integer> getSelectedEnchantments() {
        return arcane.getSelected();
    }
    public void requestRecalculationIfNeeded() {
        if (arcane.getLevel() instanceof ServerLevel sl) {
            arcane.getOrComputeMonolithMagic(sl);
        }
    }
    public ArcaneEnchantingTableBlockEntity getBlockEntity() {
        return arcane;
    }
    public ItemStack getItemInSlot0() {
        Slot slot = this.slots.get(0); // Slot 0 = zu verzauberndes Item
        if (slot != null && slot.hasItem()) {
            return slot.getItem();
        }
        return ItemStack.EMPTY;
    }

}
