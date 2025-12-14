package com.fireblaze.magic_overhaul;

import com.fireblaze.magic_overhaul.MagicOverhaul;
import com.fireblaze.magic_overhaul.registry.ModBlocks;
import com.fireblaze.magic_overhaul.registry.ModItems;
import com.fireblaze.magic_overhaul.registry.ModRunes;
import com.fireblaze.magic_overhaul.runes.RuneItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MOD_TABS =DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicOverhaul.MODID);

    public static final RegistryObject<CreativeModeTab> MAGIC_OVERHAUL_TAB = CREATIVE_MOD_TABS.register("magic_overhaul_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.WAND.get()))
                    .title(Component.translatable("creativetab.magic_overhaul_tab"))
                    .displayItems(((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.WAND.get());
                        pOutput.accept(ModItems.MAGIC_ESSENCE.get());
                        pOutput.accept(ModBlocks.MONOLITH.get());
                        pOutput.accept(ModBlocks.ARCANE_ENCHANTING_TABLE.get());
                        pOutput.accept(ModRunes.RUNE.get());
                    }))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MOD_TABS.register(eventBus);
    }
}
