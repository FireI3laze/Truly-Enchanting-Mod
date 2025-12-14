package com.fireblaze.magic_overhaul.menu;

import com.fireblaze.magic_overhaul.blockentity.MonolithBlockEntity;
import com.fireblaze.magic_overhaul.registry.ModMenus;
import com.fireblaze.magic_overhaul.runes.RuneDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class MonolithMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    public final MonolithBlockEntity monolith;
    public RuneDefinition currentRune;


    public MonolithMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.MONOLITH_MENU.get(), containerId);

        // BlockPos aus dem ByteBuf
        this.pos = buf.readBlockPos();

        MonolithBlockEntity tempMonolith = null;

        var be = playerInv.player.level().getBlockEntity(pos);
        if (be != null) {
            be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                // Nur ein Slot für den Monolith
                this.addSlot(new SlotItemHandler(handler, 0, 80, 20) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false; // verhindert Herausnehmen per Drag
                    }
                });
            });

            if (be instanceof MonolithBlockEntity monolithBE) {
                this.currentRune = monolithBE.getCurrentRune();
                tempMonolith = monolithBE; // speichern
            }
        }

        this.monolith = tempMonolith; // das Feld initialisieren


        // Spielerinventar wird NICHT hinzugefügt
    }


    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
