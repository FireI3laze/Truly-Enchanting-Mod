package com.fireblaze.truly_enchanting.registry;

import com.fireblaze.truly_enchanting.TrulyEnchanting;
import com.fireblaze.truly_enchanting.runes.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class ModRunes {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TrulyEnchanting.MODID);

    public static final RegistryObject<Item> RUNE = ITEMS.register("rune", RuneItem::new);

    public static final Map<String, RuneDefinition> RUNE_DEFINITIONS = new HashMap<>();

    /*
    public static RuneDefinition getRuneDefinition(String id) {
        return RUNE_DEFINITIONS.get(id);
    }


    public static final EnumMap<RuneType, RegistryObject<Item>> RUNES =
            new EnumMap<>(RuneType.class);

    static {
        for (RuneType type : RuneType.values()) {
            RUNES.put(type, ITEMS.register(
                    type.id + "_rune",
                    () -> new RuneItem(type)
            ));
        }
    }

    public static Item getItemFromType(RuneType type) {
        RegistryObject<Item> ro = RUNES.get(type);
        return ro != null ? ro.get() : null;
    }

    public static void loadAllRunes(MinecraftServer server) {
        RUNE_DEFINITIONS.clear();

        ResourceManager manager = server.getResourceManager();

        // listResources liefert Map<ResourceLocation, Resource>
        Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resources =
                manager.listResources("runes", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> entry : resources.entrySet()) {
            ResourceLocation rl = entry.getKey();
            net.minecraft.server.packs.resources.Resource resource = entry.getValue();

            try (InputStream stream = resource.open()) { // InputStream ist AutoCloseable
                JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

                // Rune-ID ohne Ordner + Endung
                String path = rl.getPath();
                path = path.substring("runes/".length(), path.length() - ".json".length());
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), path);

                RuneDefinition rune = RuneJsonParser.parse(id, json);
                RUNE_DEFINITIONS.put(id.toString(), rune);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    */
}
