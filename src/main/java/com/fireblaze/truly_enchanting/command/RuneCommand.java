package com.fireblaze.truly_enchanting.command;

import com.fireblaze.truly_enchanting.TrulyEnchanting;
import com.fireblaze.truly_enchanting.network.Network;
import com.fireblaze.truly_enchanting.network.SyncRuneDefinitionsPacket;
import com.fireblaze.truly_enchanting.runes.RuneDefinition;
import com.fireblaze.truly_enchanting.runes.RuneLoader;
import com.fireblaze.truly_enchanting.registry.ModRunes;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RuneCommand {

    /* ===================== Rune Suggestions ===================== */

    private static SuggestionProvider<CommandSourceStack> SUGGEST_RUNES =
            (ctx, builder) -> {
                for (String id : RuneLoader.RUNE_DEFINITIONS.keySet()) {
                    if (id.startsWith(TrulyEnchanting.MODID + ":")) {
                        builder.suggest(id.substring(TrulyEnchanting.MODID.length() + 1));
                    }
                }
                return builder.buildFuture();
            };

    /* ===================== Root ===================== */

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("runes")

                /* ---------- /rune give ---------- */
                .then(Commands.literal("give")
                        .then(Commands.argument("rune", StringArgumentType.word())
                                .suggests(SUGGEST_RUNES)
                                .executes(ctx -> giveRune(
                                        ctx,
                                        TrulyEnchanting.MODID + ":" +
                                                StringArgumentType.getString(ctx, "rune")
                                ))))

                /* ---------- /rune remove ---------- */
                .then(Commands.literal("remove")
                        .then(Commands.argument("rune", StringArgumentType.word())
                                .suggests(SUGGEST_RUNES)
                                .executes(RuneCommand::executeRemoveRune)))

                /* ---------- /rune reload ---------- */
                .then(Commands.literal("reload")
                        .executes(RuneCommand::executeReloadRunes))

                /* ---------- /rune edit ---------- */
                .then(buildEditCommand());
    }


    /* ===================== /rune edit ===================== */

    private static LiteralArgumentBuilder<CommandSourceStack> buildEditCommand() {
        return Commands.literal("edit")
                .then(Commands.argument("rune", StringArgumentType.word())
                        .suggests(SUGGEST_RUNES)

                        /* -------- BLOCKS -------- */
                        .then(blockSection("blocks"))

                        /* -------- BLOCK TAGS -------- */
                        .then(blockSection("block_tags"))

                        /* -------- ENCHANTMENTS -------- */
                        .then(Commands.literal("enchantments")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("entry", StringArgumentType.string())
                                                .suggests((c, b) -> {
                                                    ForgeRegistries.ENCHANTMENTS.getValues().forEach(e -> {
                                                        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(e);
                                                        if (id != null) b.suggest("\"" + id + "\"");
                                                    });
                                                    return b.buildFuture();
                                                })
                                                .executes(ctx -> executeAdd(ctx, "enchantments"))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("entry", StringArgumentType.string())
                                                .suggests((ctx, builder) -> {
                                                    RuneDefinition rune = getRune(ctx);
                                                    if (rune == null) return builder.buildFuture();

                                                    rune.enchantments.forEach(ench -> {
                                                        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(ench);
                                                        if (id != null) {
                                                            builder.suggest("\"" + id + "\"");
                                                        }
                                                    });

                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> executeRemove(ctx, "enchantments"))))

                        )
                );
    }

    /* ===================== Shared block / tag section ===================== */

    private static LiteralArgumentBuilder<CommandSourceStack> blockSection(String field) {
        return Commands.literal(field)

                /* ---------- ADD ---------- */
                .then(Commands.literal("add")
                        .then(Commands.argument("entry", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    RuneDefinition rune = getRune(ctx);
                                    if (rune == null) return builder.buildFuture();

                                    if (field.equals("blocks")) {
                                        ForgeRegistries.BLOCKS.getValues().forEach(b -> {
                                            if (!rune.blockMap.containsKey(b)) {
                                                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
                                                if (id != null) builder.suggest("\"" + id + "\"");
                                            }
                                        });
                                    }

                                    if (field.equals("block_tags")) {
                                        ForgeRegistries.BLOCKS.tags().getTagNames().forEach(tag -> {
                                            TagKey<Block> key = TagKey.create(
                                                    ForgeRegistries.BLOCKS.getRegistryKey(),
                                                    tag.location()
                                            );
                                            if (!rune.blockTagsMap.containsKey(key)) {
                                                builder.suggest("\"#" + tag.location() + "\"");
                                            }
                                        });
                                    }

                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("min", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("max", IntegerArgumentType.integer(0))
                                                .executes(ctx -> executeAdd(ctx, field))
                                        )
                                )
                        )
                )

                /* ---------- REMOVE ---------- */
                .then(Commands.literal("remove")
                        .then(Commands.argument("entry", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    RuneDefinition rune = getRune(ctx);
                                    if (rune == null) return builder.buildFuture();

                                    if (field.equals("blocks")) {
                                        rune.blocks.forEach(b -> {
                                            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b.block);
                                            if (id != null) builder.suggest("\"" + id + "\"");
                                        });
                                    }

                                    if (field.equals("block_tags")) {
                                        rune.blockTags.forEach(t ->
                                                builder.suggest("\"#" + t.tag.location() + "\""));
                                    }

                                    return builder.buildFuture();
                                })
                                .executes(ctx -> executeRemove(ctx, field))
                        )
                )

                /* ---------- EDIT ---------- */
                .then(Commands.literal("edit")
                        .then(Commands.argument("entry", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    RuneDefinition rune = getRune(ctx);
                                    if (rune == null) return builder.buildFuture();

                                    if (field.equals("blocks")) {
                                        rune.blocks.forEach(b -> {
                                            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b.block);
                                            if (id != null) builder.suggest("\"" + id + "\"");
                                        });
                                    }

                                    if (field.equals("block_tags")) {
                                        rune.blockTags.forEach(t ->
                                                builder.suggest("\"#" + t.tag.location() + "\""));
                                    }

                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("min", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("max", IntegerArgumentType.integer(0))
                                                .executes(ctx -> executeEdit(ctx, field))
                                        )
                                )
                        )
                );
    }


    /* ===================== EXECUTE ===================== */

    private static int executeAdd(CommandContext<CommandSourceStack> ctx, String field) {
        RuneDefinition rune = getRune(ctx);
        if (rune == null) return 0;

        String entry = StringArgumentType.getString(ctx, "entry");
        ResourceLocation runeId = rune.id;

        int min = 0;
        int max = 0;

        try {
            min = IntegerArgumentType.getInteger(ctx, "min");
            max = IntegerArgumentType.getInteger(ctx, "max");
        } catch (IllegalArgumentException ignored) {
            // enchantments haben kein min/max
        }

        boolean success = false;

        switch (field) {
            case "blocks" -> {
                Block block = resolveBlock(entry);
                if (block != null && rune.addBlock(block, min, max)) {
                    updateRuneJson(runeId, "blocks", entry, false, 0, 0);
                    success = updateRuneJson(runeId, "blocks", entry, true, min, max);
                }
            }

            case "block_tags" -> {
                TagKey<Block> tag = resolveBlockTag(entry);
                String tagId = entry.startsWith("#") ? entry.substring(1) : entry;

                if (tag != null && rune.addBlockTag(tag, min, max)) {
                    updateRuneJson(runeId, "block_tags", tagId, false, 0, 0);
                    success = updateRuneJson(runeId, "block_tags", tagId, true, min, max);
                }
            }

            case "enchantments" -> {
                Enchantment ench = resolveEnchantment(entry);
                if (ench != null && rune.addEnchantment(ench)) {
                    success = updateRuneJson(runeId, "enchantments", entry, true, 0, 0);
                }
            }
        }

        ctx.getSource().sendSystemMessage(
                Component.literal(success ? "§aEntry added." : "§cFailed to add entry.")
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx, String field) {
        RuneDefinition rune = getRune(ctx);
        if (rune == null) return 0;

        String entry = StringArgumentType.getString(ctx, "entry");
        ResourceLocation runeId = rune.id;

        boolean success = false;

        switch (field) {
            case "blocks" -> {
                Block block = resolveBlock(entry);
                if (block != null && rune.removeBlock(block)) {
                    success = updateRuneJson(runeId, "blocks", entry, false, 0, 0);
                }
            }

            case "block_tags" -> {
                TagKey<Block> tag = resolveBlockTag(entry);
                String tagId = entry.startsWith("#") ? entry.substring(1) : entry;

                if (tag != null && rune.removeBlockTag(tag)) {
                    success = updateRuneJson(runeId, "block_tags", tagId, false, 0, 0);
                }
            }

            case "enchantments" -> {
                Enchantment ench = resolveEnchantment(entry);
                if (ench != null && rune.removeEnchantment(ench)) {
                    success = updateRuneJson(runeId, "enchantments", entry, false, 0, 0);
                }
            }
        }

        ctx.getSource().sendSystemMessage(
                Component.literal(success ? "§aEntry removed" : "§cFailed removing entry")
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int executeEdit(CommandContext<CommandSourceStack> ctx, String field) {
        RuneDefinition rune = getRune(ctx);
        if (rune == null) return 0;

        String entry = StringArgumentType.getString(ctx, "entry");
        int min = IntegerArgumentType.getInteger(ctx, "min");
        int max = IntegerArgumentType.getInteger(ctx, "max");

        ResourceLocation runeId = rune.id;

        boolean success = false;

        switch (field) {
            case "blocks" -> {
                Block block = resolveBlock(entry);
                if (block != null && rune.editBlock(block, min, max)) {
                    updateRuneJson(runeId, "blocks", entry, false, 0, 0);
                    success = updateRuneJson(runeId, "blocks", entry, true, min, max);
                }
            }

            case "block_tags" -> {
                TagKey<Block> tag = resolveBlockTag(entry);
                String tagId = entry.startsWith("#") ? entry.substring(1) : entry;

                if (tag != null && rune.editBlockTag(tag, min, max)) {
                    updateRuneJson(runeId, "block_tags", tagId, false, 0, 0);
                    success = updateRuneJson(runeId, "block_tags", tagId, true, min, max);
                }
            }
        }

        ctx.getSource().sendSystemMessage(
                Component.literal(success ? "§aEdited." : "§cFailed.")
        );

        return Command.SINGLE_SUCCESS;
    }

    /* ===================== Helpers ===================== */

    private static RuneDefinition getRune(CommandContext<CommandSourceStack> ctx) {
        return RuneLoader.getRuneDefinition(
                TrulyEnchanting.MODID + ":" + StringArgumentType.getString(ctx, "rune"));
    }

    private static Block resolveBlock(String s) {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(s));
    }

    private static TagKey<Block> resolveBlockTag(String s) {
        if (!s.startsWith("#")) return null;
        return TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(),
                ResourceLocation.tryParse(s.substring(1)));
    }

    private static Enchantment resolveEnchantment(String s) {
        return ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.tryParse(s));
    }

    private static int giveRune(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(ModRunes.RUNE.get());
        stack.getOrCreateTag().putString("rune_id", id);
        p.getInventory().add(stack);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeRemoveRune(CommandContext<CommandSourceStack> ctx) {
        String runeName = StringArgumentType.getString(ctx, "rune");
        ResourceLocation runeId = ResourceLocation.fromNamespaceAndPath(TrulyEnchanting.MODID, runeName);

        RuneDefinition rune = RuneLoader.RUNE_DEFINITIONS.get(runeId.toString());
        if (rune == null) {
            ctx.getSource().sendFailure(
                    Component.literal("§cRune not found.")
            );
            return 0;
        }

        /* ---------- JSON löschen ---------- */
        Path jsonPath = getRuneJsonPath(runeId);
        boolean jsonDeleted = false;

        try {
            if (Files.exists(jsonPath)) {
                Files.delete(jsonPath);
                jsonDeleted = true;
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(
                    Component.literal("§cFailed to delete rune JSON.")
            );
            e.printStackTrace();
            return 0;
        }

        /* ---------- Rune aus Loader entfernen ---------- */
        RuneLoader.RUNE_DEFINITIONS.remove(runeId.toString());

        ctx.getSource().sendSystemMessage(
                Component.literal(
                        "§aRune '" + runeName + "' removed."
                                + (jsonDeleted ? " JSON deleted." : " JSON not found.")
                )
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int executeReloadRunes(CommandContext<CommandSourceStack> ctx) {

        File runesDir = new File(
                FMLPaths.CONFIGDIR.get().toFile(),
                "truly_enchanting/runes"
        );

        if (!runesDir.exists()) {
            ctx.getSource().sendFailure(
                    Component.literal("§cRune directory does not exist.")
            );
            return 0;
        }

        try {
            RuneLoader.reloadRunes(runesDir, TrulyEnchanting.MODID);

            ctx.getSource().sendSystemMessage(
                    Component.literal("§aRunes reloaded successfully.")
            );
            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            ctx.getSource().sendFailure(
                    Component.literal("§cFailed to reload runes. See log.")
            );
            e.printStackTrace();
            return 0;
        }
    }

    private static Path getRuneJsonPath(ResourceLocation runeId) {
        // RuneId hat Form "modid:name"
        String path = runeId.getPath();           // z.B. "ancient_city"

        // Config-Ordner: configs/truly_enchanting/runes/<path>.json
        Path configDir = FMLPaths.CONFIGDIR.get(); // Forge Config-Pfad
        return configDir.resolve("truly_enchanting").resolve("runes").resolve(path + ".json");
    }


    private static boolean updateRuneJson(
            ResourceLocation runeId,
            String field,
            String value,
            boolean add,
            int min,
            int max
    ) {
        if (value == null) return false; // <-- null-Einträge direkt ignorieren

        Path jsonPath = getRuneJsonPath(runeId);
        if (!Files.exists(jsonPath)) return false;

        boolean changed = false;

        try (Reader reader = Files.newBufferedReader(jsonPath)) {
            com.google.gson.JsonObject root =
                    com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

            com.google.gson.JsonArray array = root.getAsJsonArray(field);
            if (array == null) return false;

            /* ---------- ADD ---------- */
            if (add) {

                // ===== ENCHANTMENTS =====
                if (field.equals("enchantments")) {
                    array.add(value);
                }

                // ===== BLOCKS / TAGS =====
                else {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();

                    if (field.equals("blocks")) obj.addProperty("block", value);
                    else obj.addProperty("tag", value);

                    obj.addProperty("min", min);
                    obj.addProperty("max", max);

                    array.add(obj);
                }
                changed = true;
            }

            /* ---------- REMOVE ---------- */
            else {
                for (int i = 0; i < array.size(); i++) {

                    // ===== ENCHANTMENTS =====
                    if (field.equals("enchantments")) {
                        if (!array.get(i).isJsonNull() && array.get(i).getAsString().equals(value)) {
                            array.remove(i);
                            changed = true;
                            break;
                        }
                    }

                    // ===== BLOCKS / TAGS =====
                    else {
                        com.google.gson.JsonObject obj = array.get(i).getAsJsonObject();
                        String key = field.equals("blocks") ? "block" : "tag";

                        if (obj.has(key) && !obj.get(key).isJsonNull() && obj.get(key).getAsString().equals(value)) {
                            array.remove(i);
                            changed = true;
                            break;
                        }
                    }
                }
            }

            try (Writer writer = Files.newBufferedWriter(jsonPath)) {
                new com.google.gson.GsonBuilder()
                        .serializeNulls() // <- nulls werden nicht gelöscht
                        .setPrettyPrinting()
                        .create()
                        .toJson(root, writer);
            }

            if (changed) {
                // RuneLoader Map updaten
                RuneDefinition rune = RuneLoader.getRuneDefinition(runeId.toString());
                if (rune != null) RuneLoader.RUNE_DEFINITIONS.put(runeId.toString(), rune);

                // Packet an den Spieler senden
                assert rune != null;
                SyncRuneDefinitionsPacket packet =
                        new SyncRuneDefinitionsPacket(Map.of(runeId.toString(), rune));

                boolean isSinglePlayer = Minecraft.getInstance().isLocalServer();
                if (!isSinglePlayer) Network.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
