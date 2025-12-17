package com.fireblaze.truly_enchanting.item;

import com.fireblaze.truly_enchanting.util.XPUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class MagicEssenceItem extends Item {

    public static final String NBT_CHARGED = "Charged";
    public static final int XP_COST = 5; // später anpassbar

    public MagicEssenceItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(NBT_CHARGED);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        // Nur Server
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        boolean charged = stack.getOrCreateTag().getBoolean(NBT_CHARGED);
        if (charged) {
            // Schon geladen -> momentan nichts machen
            return InteractionResultHolder.success(stack);
        }

        int totalXp = XPUtil.getTotalExperience(sp);
        int count = stack.getCount();

        if (totalXp < XP_COST) {
            sp.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Not enough experience"),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // Wie viele können geladen werden?
        int maxByXp = totalXp / XP_COST;
        int toConvert = Math.min(count, maxByXp);

        if (toConvert <= 0)
            return InteractionResultHolder.fail(stack);

        // XP abziehen
        XPUtil.setPlayerTotalExperience(sp, totalXp - toConvert * XP_COST);

        // Charged Stack erzeugen
        ItemStack chargedStack = new ItemStack(this, toConvert);
        chargedStack.getOrCreateTag().putBoolean(NBT_CHARGED, true);

        // Uncharged Stack reduzieren
        stack.shrink(toConvert);

        // Inventar/Drop
        if (!sp.getInventory().add(chargedStack)) {
            sp.drop(chargedStack, false);
        }

        // Feedback
        ServerLevel sLevel = (ServerLevel) level;
        sLevel.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1f, 1f);

        for (int i = 0; i < Math.min(20, toConvert * 2); i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.5;
            double oy = level.random.nextDouble() * 0.5 + 1;
            double oz = (level.random.nextDouble() - 0.5) * 0.5;
            sLevel.sendParticles(ParticleTypes.ENCHANT,
                    sp.getX() + ox, sp.getY() + oy, sp.getZ() + oz,
                    1, 0, 0.05, 0, 0);
        }

        sp.displayClientMessage(
                net.minecraft.network.chat.Component.literal("Charged: " + toConvert + " Magic Essence"),
                true
        );

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        boolean charged = tag != null && tag.getBoolean(NBT_CHARGED);

        if (!charged) tooltip.add(net.minecraft.network.chat.Component.literal("Right-click to charge").withStyle(ChatFormatting.GREEN));
        else tooltip.add(net.minecraft.network.chat.Component.literal("Charged").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

}
