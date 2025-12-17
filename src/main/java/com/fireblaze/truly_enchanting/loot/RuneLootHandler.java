package com.fireblaze.truly_enchanting.loot;

import com.fireblaze.truly_enchanting.TrulyEnchanting;
import com.fireblaze.truly_enchanting.registry.ModRunes;
import com.fireblaze.truly_enchanting.runes.RuneDefinition;
import com.fireblaze.truly_enchanting.runes.RuneLoot;
import com.fireblaze.truly_enchanting.runes.RuneLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrulyEnchanting.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RuneLootHandler {

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        LootTable table = event.getTable();
        if (table == null) return;

        ResourceLocation id = event.getName();
        if (id == null) return;

        for (RuneDefinition rune : RuneLoader.RUNE_DEFINITIONS.values()) {

            if (rune.loot == null) continue;
            RuneLoot loot = rune.loot;

            if (loot.chestLootTables == null) continue;

            for (ResourceLocation target : loot.chestLootTables) {

                if (!id.equals(target)) continue;

                CompoundTag nbt = new CompoundTag();
                nbt.putString("rune_id", rune.id.toString());

                LootPool pool = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(
                                LootItem.lootTableItem(ModRunes.RUNE.get())
                                        .apply(SetNbtFunction.setTag(nbt))
                                        .when(LootItemRandomChanceCondition.randomChance(loot.chance))
                        )
                        .build();


                table.addPool(pool);

                TrulyEnchanting.LOGGER.info(
                        "[Runes] → Rune '{}' in LootTable '{}' injected with chance {}",
                        rune.id, target, loot.chance
                );
            }
        }
    }
}

