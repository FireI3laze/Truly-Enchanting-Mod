package com.fireblaze.magic_overhaul;

import com.fireblaze.magic_overhaul.client.renderer.ArcaneEnchantingTableRenderer;
import com.fireblaze.magic_overhaul.command.RuneCommand;
import com.fireblaze.magic_overhaul.network.Network;
import com.fireblaze.magic_overhaul.registry.*;
import com.fireblaze.magic_overhaul.client.screen.MonolithScreen;
import com.fireblaze.magic_overhaul.client.screen.ArcaneEnchantingTableScreen;
import com.fireblaze.magic_overhaul.client.renderer.MonolithRenderer;
import com.fireblaze.magic_overhaul.runes.RuneLoader;
import com.fireblaze.magic_overhaul.util.Registration;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MagicOverhaul.MODID)
public class MagicOverhaul
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "magic_overhaul";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public MagicOverhaul(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModCreativeModTabs.register(modEventBus);

        // WICHTIG: Zuerst alle RegistryObject-Klassen laden
        ModBlocks.register();
        ModItems.register();
        ModBlockItems.register();
        ModBlockEntities.register();
        ModMenus.register();

        // --- Sound Registry ---
        ModSounds.register(modEventBus);

        // Dann die DeferredRegister registrieren
        Registration.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
        ModRunes.ITEMS.register(modEventBus);

        // Lifecycle-Events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        // MinecraftForge EventBus
        MinecraftForge.EVENT_BUS.register(this);
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        Network.register();

        event.enqueueWork(() -> {
            File runesDir = new File(
                    FMLPaths.CONFIGDIR.get().toFile(),
                    "truly_enchanting/runes"
            );

            // 1) Default-Runen kopieren (falls nötig)
            RuneLoader.ensureDefaultRunes(runesDir, MODID);

            // 2) Runen aus Config laden
            RuneLoader.loadRunes(runesDir, MODID);
        });
    }


    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

    }

    @Mod.EventBusSubscriber(modid = MagicOverhaul.MODID)
    public class CommandEvents {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            event.getDispatcher().register(
                    RuneCommand.build() // build liefert "rune" + "give" + argument
            );
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Lade Runen beim Serverstart
        // ModRunes.loadAllRunes(event.getServer()); todo datapack driven
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MagicOverhaul.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            MenuScreens.register(ModMenus.MONOLITH_MENU.get(), MonolithScreen::new);
            MenuScreens.register(ModMenus.ARCANE_ENCHANTING_TABLE_MENU.get(), ArcaneEnchantingTableScreen::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.MONOLITH_BLOCK_ENTITY.get(), MonolithRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_ENCHANTING_TABLE.get(), ArcaneEnchantingTableRenderer::new);
        }
    }
}
