package com.fireblaze.magic_overhaul.enchanting;

import com.fireblaze.magic_overhaul.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.fireblaze.magic_overhaul.blockentity.MonolithBlockEntity;
import com.fireblaze.magic_overhaul.runes.RuneDefinition;
import com.fireblaze.magic_overhaul.network.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;

public class LinkedMonolithManager {

    private final List<BlockPos> linkedMonoliths = new ArrayList<>();
    private Level level;

    // Neu: für Client-Seite
    private boolean initialized = false;

    public LinkedMonolithManager(Level level) {
        this.level = level;
    }

    // --- INITIALIZED GETTER/SETTER ---
    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean value) {
        this.initialized = value;
    }

    // --- LEVEL ---
    public void setLevel(Level level) {
        this.level = level;
    }

    // --- MONOLITHEN MANAGEMENT ---
    public boolean addMonolith(MonolithBlockEntity monolith) {
        if (monolith == null) {
            return false;
        }

        RuneDefinition rune = monolith.getCurrentRune();
        if (rune == null) {
            return false;
        }

        // Prüfen, ob Rune schon verknüpft
        for (BlockPos pos : linkedMonoliths) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MonolithBlockEntity existingMonolith) {
                RuneDefinition existingRune = existingMonolith.getCurrentRune();
                if (existingRune != null && existingRune == rune) {
                    return false; // Rune schon vorhanden
                }
            }
        }

        linkedMonoliths.add(monolith.getBlockPos());
        return true;
    }

    public void removeMonolith(BlockPos pos) {
        linkedMonoliths.remove(pos);
    }

    public void setLinkedMonoliths(List<BlockPos> list) {
        linkedMonoliths.clear();
        linkedMonoliths.addAll(list);
    }

    public void cleanupInvalidMonoliths(ArcaneEnchantingTableBlockEntity arcaneEnchantingTable) {
        if (level == null || level.isClientSide) return;

        boolean changed = false;

        Iterator<BlockPos> it = linkedMonoliths.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            BlockEntity be = level.getBlockEntity(pos);

            if (!(be instanceof MonolithBlockEntity monolith)) {
                it.remove();
                changed = true;
                continue;
            }

            if (monolith.getCurrentRune() == null) {
                it.remove();
                changed = true;
            }
        }

        if (changed) {
            arcaneEnchantingTable.setChanged();
        }

        Network.syncLinkedMonoliths(arcaneEnchantingTable);
    }


    public Map<Enchantment, Integer> getAllUnlockedEnchantments() {
        Map<Enchantment, Integer> map = new HashMap<>();
        for (BlockPos pos : linkedMonoliths) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MonolithBlockEntity monolith) {
                RuneDefinition rune = monolith.getCurrentRune();
                if (rune != null) {
                    for (Enchantment ench : rune.enchantments) {
                        int level = monolith.calculateCurrentMagicPower();
                        map.put(ench, Math.max(map.getOrDefault(ench, 0), level));
                    }
                }
            }
        }
        return map;
    }

    public List<BlockPos> getLinkedMonoliths() {
        return Collections.unmodifiableList(linkedMonoliths);
    }

    public void addMonolithPosition(BlockPos pos) {
        if (!linkedMonoliths.contains(pos)) {
            linkedMonoliths.add(pos);
        }
    }

    public BlockPos getMonolithForEnchantment(Enchantment enchantment) {
        if (level == null) return null;

        for (BlockPos pos : linkedMonoliths) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MonolithBlockEntity monolith) {
                RuneDefinition rune = monolith.getCurrentRune();
                if (rune != null) {
                    for (Enchantment e : rune.enchantments) {
                        if (e == enchantment) {
                            return pos;
                        }
                    }
                }
            }
        }

        return null;
    }

    public List<BlockPos> getAllMonoliths() {
        return getLinkedMonoliths();
    }

    // --- SERIALIZATION ---
    public ListTag serializeToTag() {
        ListTag listTag = new ListTag();
        for (BlockPos pos : linkedMonoliths) {
            listTag.add(LongTag.valueOf(pos.asLong()));
        }
        return listTag;
    }

    public void deserializeFromTag(ListTag tag) {
        linkedMonoliths.clear();
        for (int i = 0; i < tag.size(); i++) {
            if(tag.get(i) instanceof LongTag longTag) {
                linkedMonoliths.add(BlockPos.of(longTag.getAsLong()));
            }
        }
    }
}
