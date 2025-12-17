package com.fireblaze.truly_enchanting.registry;

import com.fireblaze.truly_enchanting.TrulyEnchanting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TrulyEnchanting.MODID);

    public static final RegistryObject<SoundEvent> BEAM_LOOP = SOUNDS.register("beam_loop",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TrulyEnchanting.MODID, "beam_loop")));

    public static final RegistryObject<SoundEvent> MAGIC_EXPLOSION = SOUNDS.register("magic_explosion",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TrulyEnchanting.MODID, "magic_explosion")));

    public static final RegistryObject<SoundEvent> BAD_OMEN = SOUNDS.register("bad_omen",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TrulyEnchanting.MODID, "bad_omen")));


    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
