package com.fireblaze.truly_enchanting;

import com.fireblaze.truly_enchanting.client.renderer.ArcaneEnchantingTableRenderer;
import com.fireblaze.truly_enchanting.command.RuneCommand;
import com.fireblaze.truly_enchanting.network.Network;
import com.fireblaze.truly_enchanting.registry.*;
import com.fireblaze.truly_enchanting.client.screen.MonolithScreen;
import com.fireblaze.truly_enchanting.client.screen.ArcaneEnchantingTableScreen;
import com.fireblaze.truly_enchanting.client.renderer.MonolithRenderer;
import com.fireblaze.truly_enchanting.runes.RuneLoader;
import com.fireblaze.truly_enchanting.util.Registration;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
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

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TrulyEnchanting.MODID)
public class TrulyEnchanting
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "truly_enchanting";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrulyEnchanting(FMLJavaModLoadingContext context) {
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

    @Mod.EventBusSubscriber(modid = TrulyEnchanting.MODID)
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
    @Mod.EventBusSubscriber(modid = TrulyEnchanting.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
