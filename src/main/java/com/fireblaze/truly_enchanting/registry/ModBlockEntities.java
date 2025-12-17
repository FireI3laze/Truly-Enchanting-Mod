package com.fireblaze.truly_enchanting.registry;

import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.truly_enchanting.blockentity.MonolithBlockEntity;
import com.fireblaze.truly_enchanting.util.Registration;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final RegistryObject<BlockEntityType<MonolithBlockEntity>> MONOLITH_BLOCK_ENTITY =
            Registration.BLOCK_ENTITIES.register("monolith",
                    () -> BlockEntityType.Builder.of(
                            MonolithBlockEntity::new,
                            ModBlocks.MONOLITH.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<ArcaneEnchantingTableBlockEntity>> ARCANE_ENCHANTING_TABLE =
            Registration.BLOCK_ENTITIES.register("arcane_enchanting_table.json",
                    () -> BlockEntityType.Builder.of(
                            ArcaneEnchantingTableBlockEntity::new,
                            ModBlocks.ARCANE_ENCHANTING_TABLE.get() // <- Hier referenzierst du den Block
                    ).build(null)
            );

    public static void register() {}
}
