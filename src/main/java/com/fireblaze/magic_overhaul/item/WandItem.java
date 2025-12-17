package com.fireblaze.magic_overhaul.item;

import com.fireblaze.magic_overhaul.MagicOverhaul;
import com.fireblaze.magic_overhaul.block.MonolithBlock;
import com.fireblaze.magic_overhaul.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.magic_overhaul.blockentity.MonolithBlockEntity;
import com.fireblaze.magic_overhaul.network.Network;
import com.fireblaze.magic_overhaul.registry.ModSounds;
import com.fireblaze.magic_overhaul.util.BindingManager;
import com.fireblaze.magic_overhaul.util.BoundTable;
import com.fireblaze.magic_overhaul.util.ClientBindingState;
import com.fireblaze.magic_overhaul.util.MagicCostCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import org.joml.Matrix4f;


import java.util.*;

@Mod.EventBusSubscriber(modid = MagicOverhaul.MODID)
public class WandItem extends Item {
    private static final Map<ParticleBeam, Integer> activeBeams = new HashMap<>();
    public enum WandMode {
        LINK(0, "Link Mode"),
        ENCHANT(1, "Enchant Mode"),
        BIND(2, "Bind Mode");

        public final int id;
        public final String display;

        WandMode(int id, String display) {
            this.id = id;
            this.display = display;
        }

        public static WandMode fromId(int id) {
            for(WandMode m : values()) if(m.id == id) return m;
            return LINK;
        }
    }

    public WandItem(Properties properties) {
        super(properties);
    }

    public static WandMode getMode(ItemStack stack) {
        int id = stack.getOrCreateTag().getInt("wandMode");
        return WandMode.fromId(id);
    }

    public static void setMode(ItemStack stack, WandMode mode) {
        stack.getOrCreateTag().putInt("wandMode", mode.id);
    }

    public static void cycleMode(ItemStack stack) {
        int next = (getMode(stack).id + 1) % WandMode.values().length;
        setMode(stack, WandMode.fromId(next));
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public class WandScrollHandler {

        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent event) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof WandItem)) return;

            // Muss Strg gehalten werden
            if (!Screen.hasControlDown()) return;

            double delta = event.getScrollDelta();
            if (delta == 0) return;

            event.setCanceled(true); // verhindert Hotbar-Scrollen

            // Berechne neuen Modus
            WandItem.WandMode current = WandItem.getMode(stack);
            int nextId;

            if (delta > 0) { // hoch scrollen
                nextId = (current.id + 1) % WandItem.WandMode.values().length;
            } else { // runter scrollen
                nextId = (current.id - 1 + WandItem.WandMode.values().length) % WandItem.WandMode.values().length;
            }

            WandItem.WandMode nextMode = WandItem.WandMode.fromId(nextId);
            WandItem.setMode(stack, nextMode);

            // Server informieren
            Network.sendWandModeToServer(nextMode.id);

            // Clientseitige Anzeige
            player.displayClientMessage(Component.literal(nextMode.display), true);
        }
    }


    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class WandSelectHandler {

        private static ItemStack lastMainHand = ItemStack.EMPTY;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.ClientTickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ItemStack current = mc.player.getMainHandItem();

            // Wenn sich der Held der Hand NICHT ändert → nichts tun
            if (ItemStack.matches(current, lastMainHand)) return;

            lastMainHand = current.copy();

            // Falls kein Wand → ignorieren
            if (!(current.getItem() instanceof WandItem wand)) return;

            ChatFormatting color;
            switch (getMode(current)) {
                case LINK -> color = ChatFormatting.GREEN;
                case ENCHANT -> color = ChatFormatting.LIGHT_PURPLE; // Lila
                case BIND -> color = ChatFormatting.RED;
                default -> color = ChatFormatting.WHITE;
            }
            // Modus anzeigen
            WandItem.WandMode mode = WandItem.getMode(current);
            mc.player.displayClientMessage(Component.literal(getMode(current).display).withStyle(color), true);
        }
    }




    // Rechtsklick auf Block
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack stack = context.getItemInHand();
        assert player != null;

        if (getMode(stack) == WandMode.ENCHANT) {
            if (be instanceof ArcaneEnchantingTableBlockEntity tableBE) {
                ItemStackHandler inv = (ItemStackHandler) tableBE.getItemHandler();
                ItemStack target = inv.getStackInSlot(0); // Item, das verzaubert werden soll
                ItemStack essence = inv.getStackInSlot(1); // Charged Magic Essence

                if(!checkBinding(player, tableBE, level)) return InteractionResult.SUCCESS;

                if (target.isEmpty() || essence.isEmpty()) {
                    if(!level.isClientSide)
                        player.displayClientMessage(Component.literal("No item or Magic Essence in table!"), true);
                    return InteractionResult.SUCCESS;
                }

                Map<Enchantment, Integer> selected = new HashMap<>(tableBE.getSelected());
                if (selected.isEmpty()) {
                    if(!level.isClientSide)
                        player.displayClientMessage(Component.literal("No enchantments selected!"), true);
                    return InteractionResult.SUCCESS;
                }

                int totalCost = 0;
                for (Map.Entry<Enchantment, Integer> entry : selected.entrySet()) {
                    Enchantment ench = entry.getKey();
                    int levelForEnch = entry.getValue();

                    totalCost += MagicCostCalculator.calculateMagicRequirement(ench, levelForEnch);
                }
                float accumulated = tableBE.getMagicAccumulator().getAccumulatedMagicPower();
                if (accumulated < totalCost) {
                    if(!level.isClientSide)
                        player.displayClientMessage(Component.literal("Not enough Magical Power accumulated! (" + accumulated + "/" + totalCost +  ")"), true);
                    return InteractionResult.SUCCESS;
                }

                boolean anyChange = selected.entrySet().stream().anyMatch(e -> {
                    int oldLevel = target.getEnchantmentLevel(e.getKey());
                    int newLevel = e.getValue();
                    return newLevel > oldLevel;
                });

                if (!anyChange) {
                    if(!level.isClientSide)
                        player.displayClientMessage(Component.literal("All selected enchantments are already applied!"), true);
                    return InteractionResult.SUCCESS;
                }

                if (essence.getCount() < selected.size()) {
                    if(!level.isClientSide)
                        player.displayClientMessage(Component.literal("Not enough Magic Essence!"), true);
                    return InteractionResult.SUCCESS;
                }

                if (level instanceof ServerLevel serverLevel) {
                    // Prüfen, ob aktuell schon eine Animation läuft
                    if (!activeBeams.isEmpty()) {
                        if(!level.isClientSide)
                            player.displayClientMessage(Component.literal("Enchantment in progress!"), true);
                        return InteractionResult.SUCCESS;
                    }

                    // Partikelanimation starten
                    tableBE.setEnchantingInProgress(true);
                    int finalTotalCost = totalCost;
                    startParticleBeamAnimation(serverLevel, tableBE, () -> {
                        int usedEssence = 0;
                        for (var entry : selected.entrySet()) {
                            Enchantment ench = entry.getKey();
                            int newLevel = entry.getValue();
                            int oldLevel = target.getEnchantmentLevel(ench);

                            if (newLevel > oldLevel) {
                                usedEssence += applyEnchantmentWithBadOmenChance(
                                        serverLevel, player, target, ench, newLevel, tableBE.getBlockPos()
                                );
                            }
                        }

                        // Essenz reduzieren
                        essence.shrink(usedEssence);
                        tableBE.getMagicAccumulator().setAccumulatedMagicPower(tableBE.getMagicAccumulator().getCurrentMagicPower() - finalTotalCost);
                        tableBE.setChanged();

                        // Partikel-Explosion
                        spawnEnchantExplosion(serverLevel, tableBE.getBlockPos());

                        // Item droppen
                        ItemStack enchantedItem = target.copy();
                        tableBE.setEnchantingInProgress(false);

                        inv.setStackInSlot(0, ItemStack.EMPTY);
                        tableBE.markForRenderUpdate();

                        double dropX = tableBE.getBlockPos().getX() + 0.5;
                        double dropY = tableBE.getBlockPos().getY() + 1.0;
                        double dropZ = tableBE.getBlockPos().getZ() + 0.5;

                        ItemEntity entity = new ItemEntity(serverLevel, dropX, dropY, dropZ, enchantedItem);
                        entity.setDeltaMovement(0, 0, 0);
                        entity.setPickUpDelay(40);
                        entity.setNoGravity(true);
                        serverLevel.addFreshEntity(entity);
                    });
                }

                return InteractionResult.SUCCESS;
            }
        }

        if (getMode(stack) == WandMode.LINK) {
            if(be instanceof MonolithBlockEntity monolith) {

                if (monolith.getBlockState().getValue(MonolithBlock.HALF) == DoubleBlockHalf.UPPER) {
                    pos = pos.below();
                }

                if(stack.hasTag() && stack.getTag().contains("linkedMonolith")) {
                    long linked = stack.getTag().getLong("linkedMonolith");
                    BlockPos linkedPos = BlockPos.of(linked);

                    if(linkedPos.equals(pos)) {
                        // Same Monolith -> clear selection
                        stack.removeTagKey("linkedMonolith");
                        if(!level.isClientSide) {
                            player.displayClientMessage(Component.literal("Monolith selection cleared!"), true);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }

                // New Monolith -> store selection
                stack.getOrCreateTag().putLong("linkedMonolith", pos.asLong());
                if(!level.isClientSide) {
                    player.displayClientMessage(Component.literal("Monolith selected!"), true);
                }
                return InteractionResult.SUCCESS;
            }

            if (be instanceof ArcaneEnchantingTableBlockEntity table) {

                if(!checkBinding(player, table, Objects.requireNonNull(table.getLevel()))) return InteractionResult.SUCCESS;

                if (stack.hasTag() && stack.getTag().contains("linkedMonolith")) {
                    long l = stack.getTag().getLong("linkedMonolith");
                    BlockPos linkedPos = BlockPos.of(l);

                    // Prüfen, ob Monolith bereits verlinkt ist
                    if (table.getLinkedMonolithManager().getLinkedMonoliths().contains(linkedPos)) {

                        // Entfernen
                        table.getLinkedMonolithManager().removeMonolith(linkedPos);
                        table.setChanged();

                        if (!level.isClientSide) {
                            player.displayClientMessage(
                                    Component.literal("Monolith unlinked!"),
                                    true
                            );
                            table.getLinkedMonolithManager().cleanupInvalidMonoliths(table);
                        }
                        return InteractionResult.SUCCESS;
                    }

                    // Sonst neuer Link
                    BlockEntity linkedBE = level.getBlockEntity(linkedPos);
                    if (linkedBE instanceof MonolithBlockEntity linkedMonolith) {

                        if (linkedMonolith.getCurrentRune() == null) { //todo nicht null sondern rune tag filter
                            player.displayClientMessage(
                                    Component.literal("Monolith has no rune"),
                                    true
                            );

                            return InteractionResult.SUCCESS;
                        }

                        boolean added = table.linkMonolith(linkedMonolith);

                        if (!level.isClientSide) {
                            if (added) {
                                player.displayClientMessage(
                                        Component.literal("Monolith linked!"),
                                        true
                                );
                                table.getLinkedMonolithManager().cleanupInvalidMonoliths(table);
                            } else {
                                player.displayClientMessage(
                                        Component.literal("Rune already linked! Monolith at: " +
                                                linkedPos.getX() + ", " +
                                                linkedPos.getY() + ", " +
                                                linkedPos.getZ()),
                                        true
                                );
                            }

                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
        }

        if(getMode(stack) == WandMode.BIND) {
            if(level.isClientSide) return InteractionResult.SUCCESS;
            if(!(be instanceof ArcaneEnchantingTableBlockEntity table)) return InteractionResult.PASS;

            BoundTable currentBinding = BindingManager.getBoundTable(player);
            BoundTable boundTable = new BoundTable(table.getBlockPos(), Objects.requireNonNull(table.getLevel()).dimension());

            //if (currentBinding != null) System.out.println(currentBinding + " | " + currentBinding.equals(boundTable));
            //else System.out.println("binding is null");

            if(currentBinding != null && !currentBinding.equals(boundTable)) {
                    player.displayClientMessage(Component.literal("You are already bound to another table!"), true);
                return InteractionResult.SUCCESS;
            }

            if(currentBinding != null && currentBinding.equals(boundTable)) {
                // trennen
                BindingManager.unbind(player);
                    player.displayClientMessage(Component.literal("Unbound from this table."), true);
                return InteractionResult.SUCCESS;
            }

            // verbinden
            boolean success = BindingManager.bind(player, table);
                player.displayClientMessage(Component.literal("Bound to this Arcane Enchanting Table."), true);
            return InteractionResult.SUCCESS;
        }


        return InteractionResult.PASS;
    }

    private boolean checkBinding(Player player, ArcaneEnchantingTableBlockEntity tableBE, Level level) {
        if (level.isClientSide) return false;
        BoundTable boundTable = new BoundTable(tableBE.getBlockPos(), level.dimension());

        if (BindingManager.getBoundTable(player) == null) {
            player.displayClientMessage(Component.literal("Not bound to any table. Switch Wand Mode to toggle Binding"), true);
        }
        else if (!BindingManager.getBoundTable(player).equals(boundTable)) {
            player.displayClientMessage(Component.literal("Not bound to this table. Switch Wand Mode to toggle Binding"), true);
            return false;
        }

        return true;
    }

    private record ParticleBeam(ServerLevel level, BlockPos start, Vec3 end, Runnable onComplete, double distance) {
    public static boolean completed = false; // Ob dieser Strahl fertig ist
    }

    private static void startParticleBeamAnimation(ServerLevel level, ArcaneEnchantingTableBlockEntity tableBE, Runnable onComplete) {
        BlockPos tablePos = tableBE.getBlockPos();
        Map<BlockPos, Enchantment> usedMonoliths = new HashMap<>();
        Map<Enchantment, Integer> selected = tableBE.getSelected();

        // Bestimme die Monolithen für die ausgewählten Enchantments
        for (Enchantment ench : selected.keySet()) {
            BlockPos monolith = tableBE.getLinkedMonolithManager().getMonolithForEnchantment(ench);
            if (monolith != null) {
                usedMonoliths.put(monolith, ench);
            }
        }

        int delayTicks = 0;

        int i = 0;
        int size = usedMonoliths.size();

        for (BlockPos monolithPos : usedMonoliths.keySet()) {

            double distance = monolithPos.distToCenterSqr(tablePos.getX() + 0.5, tablePos.getY() + 1.625, tablePos.getZ() + 0.5);
            distance = Math.sqrt(distance); // echte Distanz

            // ---- SOUND: einmal pro Monolith beim Beam-Start ----
            level.playSound(
                    null,
                    monolithPos,
                    ModSounds.BEAM_LOOP.get(),
                    SoundSource.BLOCKS,
                    5.0f,
                    0.9f
            );
            if (distance > 75) {
                level.playSound(
                        null,
                        tablePos,
                        ModSounds.BEAM_LOOP.get(),
                        SoundSource.BLOCKS,
                        5.0f,
                        0.9f
                );
            }

            // Zielpunkt am Tisch
            Vec3 particleTargetPos = new Vec3(
                    tablePos.getX() + 0.5,
                    tablePos.getY() + 1.625,
                    tablePos.getZ() + 0.5
            );


            // Nur der letzte Beam bekommt onComplete, alle anderen null
            boolean isLast = (i == size - 1);
            Runnable complete = isLast ? onComplete : null;

            // ParticleBeam anlegen
            ParticleBeam beam = new ParticleBeam(level, monolithPos, particleTargetPos, complete, distance);

            // Startverzögerung, falls du wieder welche willst
            activeBeams.put(beam, -i * delayTicks);
            i++;
        }
    }

    private static void spawnEnchantExplosion(ServerLevel level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                ModSounds.MAGIC_EXPLOSION.get(),
                SoundSource.BLOCKS,
                5.0f,
                1.0f
        );

        int particleCount = 300;  // Anzahl der Partikel
        double spread = 1;      // Streuung der Partikel
        ParticleOptions[] particleTypes = new ParticleOptions[]{
                ParticleTypes.ENCHANT,
                ParticleTypes.END_ROD,
                ParticleTypes.WITCH
        };

        for (int i = 0; i < particleCount; i++) {
            // Zufällige Position um den Tisch herum
            double offsetX = (level.random.nextDouble() - 0.5) * spread;
            double offsetY = (level.random.nextDouble() - 0.5) * spread;
            double offsetZ = (level.random.nextDouble() - 0.5) * spread;

            // Zufällige Geschwindigkeit
            double velocityX = (level.random.nextDouble() - 0.5) * 0.5;
            double velocityY = level.random.nextDouble() * 0.5;
            double velocityZ = (level.random.nextDouble() - 0.5) * 0.5;

            // Zufälliger Partikeltyp
            ParticleOptions type = particleTypes[level.random.nextInt(particleTypes.length)];

            level.sendParticles(
                    type,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + 1.0 + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    1,          // count pro Aufruf
                    velocityX,  // dx
                    velocityY,  // dy
                    velocityZ,  // dz
                    0.5         // extra Geschwindigkeit/Größe
            );
        }
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;

        var iterator = activeBeams.entrySet().iterator();
        boolean anyRunning = false; // prüfen, ob noch ein Strahl läuft

        while (iterator.hasNext()) {
            var entry = iterator.next();
            ParticleBeam beam = entry.getKey();
            int tick = entry.getValue();
            int durationTicks = 75; // Beam-Laufzeit
            int delayAfterBeam = 10;    // 1 Sekunde Verzögerung

            int particlesPerBlock = 10;


            if (tick < 0) { // Noch in Startverzögerung
                activeBeams.put(beam, tick + 1);
                anyRunning = true;
                continue;
            }

            if (tick < durationTicks) { // Beam läuft
                anyRunning = true;

                // Partikellogik
                double startX = beam.start.getX() + 0.5;
                double startY = beam.start.getY() + 2.2;
                double startZ = beam.start.getZ() + 0.5;
                double endX = beam.end.x;
                double endY = beam.end.y - 0.3;
                double endZ = beam.end.z;

                // Map von Partikeltyp -> Anzahl pro Tick
                double distance = beam.distance();

                int dynamicCount = Math.max(5, (int) (distance * particlesPerBlock));
                if (dynamicCount > 2000) dynamicCount = 2000;

                Map<ParticleOptions, Integer> particleMap = new HashMap<>();
                particleMap.put(ParticleTypes.END_ROD, dynamicCount);
                particleMap.put(ParticleTypes.ENCHANT, dynamicCount / 2);
                particleMap.put(ParticleTypes.ELECTRIC_SPARK, dynamicCount / 2);




                for (var entry2 : particleMap.entrySet()) {
                    ParticleOptions type = entry2.getKey();
                    int count = entry2.getValue();

                    for (int i = 0; i < count; i++) {
                        double factor = i / (double) count;
                        double x = startX + (endX - startX) * factor;
                        double y = startY + (endY - startY) * factor;
                        double z = startZ + (endZ - startZ) * factor;

                        double offsetX = (beam.level.random.nextDouble() - 0.5) * 0.02;
                        double offsetY = (beam.level.random.nextDouble() - 0.5) * 0.02;
                        double offsetZ = (beam.level.random.nextDouble() - 0.5) * 0.02;

                        beam.level.sendParticles(type, x + offsetX, y + offsetY, z + offsetZ, 1, 0, 0, 0, 0);
                    }
                }


                activeBeams.put(beam, tick + 1);
                continue;
            }

            if (tick < durationTicks + delayAfterBeam) {
                // Verzögerung läuft
                activeBeams.put(beam, tick + 1);
                anyRunning = true;
                continue;
            }

            // Verzögerung vorbei, onComplete ausführen
            if (beam.onComplete != null) {
                beam.onComplete.run();
            }
            iterator.remove();
        }

        // --- Enchant anwenden nur, wenn kein Strahl mehr aktiv ---
        if (!anyRunning && !activeBeams.isEmpty()) {
            // Es gibt nur eine onComplete-Runnable pro Animation, die wir einmal ausführen
            ParticleBeam anyBeam = activeBeams.keySet().iterator().next();
            if (anyBeam.onComplete != null) {
                anyBeam.onComplete.run();
            }
            activeBeams.clear(); // alle Strahlen entfernt, Item ist enchanted
        }
    }

    // Neue Methode in ModEvents
    private static int applyEnchantmentWithBadOmenChance(ServerLevel level, Player player, ItemStack target, Enchantment ench, int newLevel, BlockPos tablePos) {
        Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(target);

        // Prüfe, ob ench einen bestehenden Enchant ersetzt
        boolean replacing = current.containsKey(ench);

        // Entferne inkompatible Enchants (außer ench selbst)
        current.entrySet().removeIf(e -> e.getKey() != ench && !ench.isCompatibleWith(e.getKey()));

        boolean applyCurse = false;
        var badOmen = player.getEffect(MobEffects.BAD_OMEN);
        if (badOmen != null) {
            int amplifier = badOmen.getAmplifier() + 1;
            int chance = amplifier * 5;
            if (level.random.nextInt(100) < chance) {
                applyCurse = true;
            }
        }

        if (applyCurse) {
            Enchantment randomCurse = level.random.nextBoolean()
                    ? Enchantments.BINDING_CURSE
                    : Enchantments.VANISHING_CURSE;

            current.put(randomCurse, 1);

            // Curse-Sound abspielen
            level.playSound(null, tablePos, ModSounds.BAD_OMEN.get(), SoundSource.BLOCKS, 5.0f, 1.0f);

            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Bad Omen! A curse has been applied!"), true);
            }

            // Curse-Beam-Partikel
            spawnCurseBeamParticles(level, tablePos);
        } else {
            current.put(ench, newLevel);

            // Partikel, wenn ein bestehender Enchant ersetzt wird
            if (replacing) {
                spawnReplacingBeamParticles(level, tablePos);
            }
        }

        EnchantmentHelper.setEnchantments(current, target);
        return 1;
    }

    // Neue Methode: Curse-Particle-Strahl
    private static void spawnCurseBeamParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                100, 0.02, 0.02, 0.02, 0.01);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                100, 0.01, 0.01, 0.01, 0.01);
    }

    private static void spawnReplacingBeamParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                80, 0.02, 0.02, 0.02, 0.02);
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                50, 0.01, 0.01, 0.01, 0.01);
    }

    // Rechtsklick in der Luft (mit Info)
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (getMode(stack) != WandMode.LINK) {
            return InteractionResultHolder.pass(stack);
        }

        if(stack.hasTag() && stack.getTag().contains("linkedMonolith")) {
            BlockPos linkedPos = BlockPos.of(stack.getTag().getLong("linkedMonolith"));
            if(!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("Monolith currently selected at: " +
                                linkedPos.getX() + ", " +
                                linkedPos.getY() + ", " +
                                linkedPos.getZ()),
                        true
                );
            }
        } else {
            if(!level.isClientSide) {
                player.displayClientMessage(Component.literal("No Monolith selected."), true);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class WandHighlightHandler {

        @SubscribeEvent
        public static void onRenderWorldLast(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ItemStack stack = mc.player.getMainHandItem();
            if (!(stack.getItem() instanceof WandItem)) return;
            if (WandItem.getMode(stack) != WandItem.WandMode.LINK) return;


            Camera camera = event.getCamera();
            PoseStack poseStack = event.getPoseStack();

            // Buffer hier deklarieren
            MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

            // OpenGL Setup: Linien sollen durch Blöcke sichtbar sein
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(2f);

            BlockPos pos = null;

            if (stack.getTag() != null) {
                pos = BlockPos.of(stack.getTag().getLong("linkedMonolith"));
                renderBoxAroundBlock(poseStack, camera, pos, buffer, 1, 0, 0);
            }

            BoundTable boundTable = ClientBindingState.getBoundTable();
            if (boundTable == null || boundTable.pos() == null || boundTable.dimension() == null || boundTable.dimension() != mc.player.level().dimension()) return;

            Level level = mc.player.level();
            BlockEntity be = level.getBlockEntity(boundTable.pos());
            if (!(be instanceof ArcaneEnchantingTableBlockEntity tableBE)) return;

            List<BlockPos> temp = new ArrayList<>(tableBE.getLinkedMonolithManager().getLinkedMonoliths());
            if (pos != null) temp.remove(pos);


            for (BlockPos monolithPos : temp) {
                renderBoxAroundBlock(poseStack, camera,monolithPos, buffer, 0, 1, 0);
            }


            // ===========================
            //if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

            // Batch abschließen
            buffer.endBatch(RenderType.lines());

            // OpenGL zurücksetzen
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }
    private static void renderBoxAroundBlock(PoseStack poseStack, Camera camera, BlockPos pos, MultiBufferSource.BufferSource buffer, float red, float green, float blue) {
            poseStack.pushPose();
                poseStack.translate(
                        pos.getX() - camera.getPosition().x,
                        pos.getY() - camera.getPosition().y,
                        pos.getZ() - camera.getPosition().z
                );

    VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
                LevelRenderer.renderLineBox(poseStack, consumer, 0, 0, 0, 1, 3, 1, red, green, blue, 0.5f); // 3 Blöcke hoch

                poseStack.popPose();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("Ctrl + Scroll to toggle Mode").withStyle(ChatFormatting.BLUE));
    }
}
