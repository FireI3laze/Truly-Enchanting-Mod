package com.fireblaze.truly_enchanting.blockentity.EnchantingTable;

import com.fireblaze.truly_enchanting.blockentity.MonolithBlockEntity;
import com.fireblaze.truly_enchanting.runes.RuneLoader;
import com.fireblaze.truly_enchanting.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class MagicAccumulator {

    private ArcaneEnchantingTableBlockEntity tableBE;
    private final Map<Block, MagicSourceBlocks> blockPalette = new HashMap<>();
    private final Map<TagKey<Block>, MagicSourceBlockTags> tagPalette = new HashMap<>();
    private Map<Block, Integer> blockCurrentPower = new HashMap<>(); // gefüllt beim Server, leer beim Client
    private Map<TagKey<Block>, Integer> tagCurrentPower = new HashMap<>();
    private Map<MonolithBlockEntity, Float> monolithsCurrentPower = new HashMap<>();

    private final int magicPowerPerEssence = 100;
    private int currentMagicPowerIncreaseRate = 0;
    private float accumulatedMagicPower = 0;
    private final float magicPowerCapPerPlayerHard = 100000.0f;
    private float magicPowerCapPerPlayerSoft;
    private float catchedMagicPowerCapPerPlayerSoft;

    public void addBlock(MagicSourceBlocks block) {
        blockPalette.put(block.block, block);
    }

    public void addTag(MagicSourceBlockTags tag) {
        tagPalette.put(tag.tag, tag);
    }

    public void loadFromJson(String path) {
        ArcaneTableConfigLoader.loadIntoAccumulator(this, path);
    }

    public void updateMaxMagic(ArcaneEnchantingTableBlockEntity tableBE) {
        this.tableBE = tableBE;
        int runeCount = RuneLoader.RUNE_DEFINITIONS.size();
        float capIncreasePerRune = magicPowerCapPerPlayerHard / runeCount;
        float percentagePower;

        magicPowerCapPerPlayerSoft = 0;
        monolithsCurrentPower.clear();

        List<BlockPos> linkedMonoliths = tableBE.getLinkedMonolithManager().getLinkedMonoliths();
        for (BlockPos mPos : linkedMonoliths) {
            BlockEntity monBE = Objects.requireNonNull(tableBE.getLevel()).getBlockEntity(mPos);
            if (!(monBE instanceof MonolithBlockEntity monolith)) continue;
            int monolithMagicPower = monolith.getMagicAccumulator().getCurrentMagicPowerIncreaseRate();
            int maxRuneRequirement = MagicCostCalculator.getMaxMagicRequirement(monolith.getCurrentRune());

            if (maxRuneRequirement > monolithMagicPower) percentagePower = (float) monolithMagicPower / maxRuneRequirement;
            else percentagePower = 1.0f;

            monolithsCurrentPower.put(monolith, capIncreasePerRune * percentagePower);
            magicPowerCapPerPlayerSoft += capIncreasePerRune * percentagePower;
        }
    }

    public void tickMagicGain(Level level) {
        if (level.isClientSide) return;

        // Tisch muss vorhanden sein
        if (tableBE == null || tableBE.getLevel() == null) return;

        // Soft Cap erreicht → kein weiteres Aufladen
        if (accumulatedMagicPower >= magicPowerCapPerPlayerSoft) return;

        // Anzahl der Spieler prüfen
        List<UUID> boundPlayers = BindingManager.getBoundPlayer(tableBE.getBlockPos());
        if (boundPlayers.isEmpty()) return;

        int onlineBoundPlayers = 0;
        if (level instanceof ServerLevel serverLevel) {
            var allPlayers = serverLevel.getServer().getPlayerList().getPlayers();
            for (var player : allPlayers) {
                if (boundPlayers.contains(player.getUUID())) {
                    onlineBoundPlayers++;
                }
            }
        }
        // Magiezuwachs für diesen Tick berechnen
        float gainThisTick = (float) currentMagicPowerIncreaseRate / (60 * 60 * 20) * onlineBoundPlayers;

        // Überladung verhindern
        accumulatedMagicPower = Math.min(
                accumulatedMagicPower + gainThisTick,
                magicPowerCapPerPlayerSoft
        );
    }

    public boolean magicEssenceToMagicPower(int amount) {
        if (magicPowerCapPerPlayerSoft < magicPowerPerEssence * amount) return false;
        accumulatedMagicPower += magicPowerPerEssence * amount;
        return true;
    }

    public void scan(Level level, BlockPos pos, int scanCap, int radiusCap) {
        MagicScanner.ScanResult result = MagicScanner.scanMagicBlocks(level, pos, blockPalette, tagPalette, scanCap, radiusCap);
        this.blockCurrentPower = result.blockPowerMap;
        this.tagCurrentPower = result.tagPowerMap;
        this.currentMagicPowerIncreaseRate = result.totalMagic;
    }

    public void scan(Level level, BlockPos pos, Map<Block, MagicSourceBlocks> blockPalette, Map<TagKey<Block>, MagicSourceBlockTags> tagPalette, int scanCap, int radiusCap) {
        MagicScanner.ScanResult result = MagicScanner.scanMagicBlocks(level, pos, blockPalette, tagPalette, scanCap, radiusCap);
        this.blockCurrentPower = result.blockPowerMap;
        this.tagCurrentPower = result.tagPowerMap;
        this.currentMagicPowerIncreaseRate = result.totalMagic;
    }

    public void initPalette(String jsonPath) {
        blockPalette.clear();
        tagPalette.clear();

        ArcaneTableConfigLoader.loadIntoAccumulator(this, jsonPath);
    }

    public Map<Block, MagicSourceBlocks> getBlockPalette() { return blockPalette; }
    public Map<TagKey<Block>, MagicSourceBlockTags> getTagPalette() { return tagPalette; }
    public float getCurrentMagicPower() { return accumulatedMagicPower; }
    public void setCurrentMagicPower(float accumulatedMagicPower) { this.accumulatedMagicPower = accumulatedMagicPower; }
    public int getCurrentMagicPowerIncreaseRate() { return currentMagicPowerIncreaseRate; }
    public void setCurrentMagicPowerIncreaseRate(int currentMagicPowerIncreaseRate) { this.currentMagicPowerIncreaseRate = currentMagicPowerIncreaseRate; }
    public float getMagicPowerCapPerPlayerSoft() { return magicPowerCapPerPlayerSoft; }
    public void setMagicPowerCapPerPlayerSoft(float magicCap) { this.magicPowerCapPerPlayerSoft = magicCap; }
    public float getAccumulatedMagicPower() { return accumulatedMagicPower; }
    public void setAccumulatedMagicPower(float magicPower) { this.accumulatedMagicPower = magicPower; }
    public ArcaneEnchantingTableBlockEntity getArcaneEnchantingTableBE() { return tableBE; }
    public void setArcaneEnchantingTableBE(ArcaneEnchantingTableBlockEntity tableBE) { this.tableBE = tableBE; }

    public void clearMonolithsCurrentPower() {
        monolithsCurrentPower.clear();
    }


    public int getCurrentPowerForBlock(Block block) {
        return blockCurrentPower.getOrDefault(block, 0);
    }
    public Map<Block, Integer> getBlockCurrentPowerMap() {
        return blockCurrentPower;
    }
    public void addToBlockCurrentPowerMap(Block block, int power) {
        blockCurrentPower.put(block, power);
    }
    public void clearCurrentPowerForBlock() {
        blockCurrentPower.clear();
    }

    public Map<TagKey<Block>, Integer> getBlockTagCurrentPowerMap() {
        return tagCurrentPower;
    }
    public int getCurrentPowerForTag(TagKey<Block> tag) {
        return tagCurrentPower.getOrDefault(tag, 0);
    }
    public void addToTagCurrentPowerMap(TagKey<Block> tag, int power) {
        tagCurrentPower.put(tag, power);
    }
    public void clearCurrentPowerForTag() {
        tagCurrentPower.clear();
    }
}
