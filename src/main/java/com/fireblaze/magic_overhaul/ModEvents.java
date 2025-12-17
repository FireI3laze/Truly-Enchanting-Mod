package com.fireblaze.magic_overhaul;

import com.fireblaze.magic_overhaul.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.magic_overhaul.network.Network;
import com.fireblaze.magic_overhaul.network.SyncBindingPacket;
import com.fireblaze.magic_overhaul.network.SyncRuneDefinitionsPacket;
import com.fireblaze.magic_overhaul.registry.ModEnchantments;
import com.fireblaze.magic_overhaul.runes.RuneLoader;
import com.fireblaze.magic_overhaul.util.BindingManager;
import com.fireblaze.magic_overhaul.util.BoundTable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MagicOverhaul.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BoundTable boundTable = BindingManager.getBoundTable(event.getEntity());
        if (boundTable == null || boundTable.pos() == null || boundTable.dimension() == null) return;

        Network.sendToClient(player, new SyncBindingPacket(boundTable.pos(), boundTable.dimension()));
        SyncRuneDefinitionsPacket packet =
                new SyncRuneDefinitionsPacket(RuneLoader.getRuneDefinitions());

        Network.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                packet
        );
    }

    /*
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        // Prüfen, ob das Item in der Hand die Verzauberung "Veil of Eternity" hat
        ItemStack stack = player.getMainHandItem(); // Haupt-Hand
        if (stack.isEmpty()) return;

        // Prüfe, ob das Item verzaubert ist
        if (stack.getEnchantmentLevel(ModEnchantments.VEIL_OF_ETERNITY.get()) > 0) {
            BlockPos pos = BindingManager.getBoundTable(player);
            if (pos == null) {
                stack.getOrCreateTag().putBoolean("Unbreakable", false);
                return;
            }
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof ArcaneEnchantingTableBlockEntity tableBE) {
                float magicPower = tableBE.getMagicAccumulator().getCurrentMagicPower();

                if (magicPower > 0) {
                    // Magie verbrauchen
                    int unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack);
                    boolean consumeMagic = true;

                    if (unbreakingLevel > 0) {
                        // Chance, dass Magie nicht verbraucht wird
                        float chance = 1f / (unbreakingLevel + 1);
                        if (player.getRandom().nextFloat() >= chance) {
                            consumeMagic = false; // Magie wird nicht verbraucht
                        }
                    }

                    if (consumeMagic) {
                        tableBE.getMagicAccumulator().setAccumulatedMagicPower(magicPower - 1);
                    }
                    stack.getOrCreateTag().putBoolean("Unbreakable", consumeMagic);
                }
                else stack.getOrCreateTag().putBoolean("Unbreakable", false);
            }
        }
    }
    */


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;

        BoundTable boundTable = BindingManager.getBoundTable(player);
        if (boundTable == null || boundTable.pos() == null || boundTable.dimension() == null) return;

        // Dimension des Tables abrufen
        ServerLevel tableLevel = player.getServer().getLevel(boundTable.dimension());
        if (tableLevel == null) return;

        BlockEntity be = tableLevel.getBlockEntity(boundTable.pos());
        if (!(be instanceof ArcaneEnchantingTableBlockEntity tableBE)) return;

        float magicPower = tableBE.getMagicAccumulator().getCurrentMagicPower();
        if (magicPower <= 1) return;

        // Prüfen alle Slots: MainHand, OffHand, Rüstung
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(player.getMainHandItem());
        stacks.add(player.getOffhandItem());
        stacks.add(player.getItemBySlot(EquipmentSlot.HEAD));
        stacks.add(player.getItemBySlot(EquipmentSlot.CHEST));
        stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
        stacks.add(player.getItemBySlot(EquipmentSlot.FEET));

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            if (stack.getEnchantmentLevel(ModEnchantments.VEIL_OF_ETERNITY.get()) <= 0) continue;

            int damage = stack.getDamageValue();
            if (damage > 0) {
                // Magie verbrauchen
                tableBE.getMagicAccumulator().setAccumulatedMagicPower(magicPower - 1);
                stack.setDamageValue(damage - 1);

                // Magische Kraft neu abrufen
                magicPower = tableBE.getMagicAccumulator().getCurrentMagicPower();
                if (magicPower <= 0) break; // keine Magie mehr übrig
            }
        }
    }


    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Vanilla Enchanting Table?
        if (state.getBlock() instanceof EnchantmentTableBlock) {

            // Nur Server-seitig handeln
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.literal("Enchantment Table disabled. Use The Arcane Enchantment Table instead."),
                        true
                );
            }

            // Verhindert, dass sich das GUI öffnet
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

}

