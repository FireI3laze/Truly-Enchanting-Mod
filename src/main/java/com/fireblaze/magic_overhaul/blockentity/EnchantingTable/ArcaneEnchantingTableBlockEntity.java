package com.fireblaze.magic_overhaul.blockentity.EnchantingTable;

import com.fireblaze.magic_overhaul.blockentity.MonolithBlockEntity;
import com.fireblaze.magic_overhaul.enchanting.CachedMagicResult;
import com.fireblaze.magic_overhaul.enchanting.LinkedMonolithManager;
import com.fireblaze.magic_overhaul.menu.ArcaneEnchantingTableMenu;
import com.fireblaze.magic_overhaul.network.SyncMagicAccumulatorPacket;
import com.fireblaze.magic_overhaul.network.SyncSelectionPacket;
import com.fireblaze.magic_overhaul.util.MagicCostCalculator;
import com.fireblaze.magic_overhaul.util.MagicScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.fireblaze.magic_overhaul.registry.ModBlockEntities;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArcaneEnchantingTableBlockEntity extends BlockEntity {

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0 && enchantingInProgress) {
                return ItemStack.EMPTY; // Blockiere das Herausziehen während Animation
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 0 && enchantingInProgress) {
                return stack; // Blockiere Einfügen während Animation (optional)
            }
            return super.insertItem(slot, stack, simulate);
        }
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            markForRenderUpdate();

            if (slot == 0 && getStackInSlot(0).isEmpty()) {
                clearSelection();
            }
        }
    };

    private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> inventory);
    private LinkedMonolithManager monolithManager;
    private final MagicAccumulator accumulator = new MagicAccumulator();

    public ArcaneEnchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_ENCHANTING_TABLE.get(), pos, state);
        this.monolithManager = new LinkedMonolithManager(null); // Level später setzen
        accumulator.initPalette("/data/magic_overhaul/config/arcane_table.json");
    }

    public IItemHandler getItemHandler() {
        return inventory;
    }
    public ItemStack getItemInSlot0() {
        return inventory.getStackInSlot(0);
    }

    public ItemStack getItemInSlot1() {
        return inventory.getStackInSlot(1);
    }


    // NBT speichern
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));

        if (tag.contains("LinkedMonoliths")) {
            CompoundTag linkedTag = tag.getCompound("LinkedMonoliths");
            for (String key : linkedTag.getAllKeys()) {
                long l = linkedTag.getLong(key);
                BlockPos pos = BlockPos.of(l);
                monolithManager.addMonolithPosition(pos);
            }
        }

        // load enchantment selection
        selected.clear();
        ListTag list = tag.getList("SelectedEnchantments", Tag.TAG_COMPOUND);

        for (Tag t : list) {
            CompoundTag c = (CompoundTag) t;
            String fullId = c.getString("id");  // z.B. "minecraft:sharpness"
            String[] parts = fullId.split(":", 2);  // teilt in ["minecraft", "sharpness"]
            if (parts.length == 2) {
                Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]));
                if (ench != null) {
                    selected.put(ench, c.getInt("level"));
                }
            } else {
                // Fallback, falls kein Namespace vorhanden ist
                Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(ResourceLocation.fromNamespaceAndPath("minecraft", parts[0]));
                if (ench != null) {
                    selected.put(ench, c.getInt("level"));
                }
            }

        }
        if (tag.contains("AccumulatedMagicPower")) {
            accumulator.setAccumulatedMagicPower(tag.getFloat("AccumulatedMagicPower"));
        }
    }


    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put("Inventory", inventory.serializeNBT());

        // Monolith-Positionsliste speichern
        CompoundTag linkedTag = new CompoundTag();
        int index = 0;
        for (BlockPos pos : monolithManager.getLinkedMonoliths()) {
            linkedTag.putLong("monolith_" + index, pos.asLong());
            index++;
        }
        tag.put("LinkedMonoliths", linkedTag);

        // Enchantment Selektion speichern
        ListTag list = new ListTag();
        for (var e : selected.entrySet()) {
            CompoundTag t = new CompoundTag();
            //t.putString("id", Enchantment.getKey(e.getKey()).toString());
            t.putInt("level", e.getValue());
            list.add(t);
        }
        tag.put("SelectedEnchantments", list);
        tag.putFloat("AccumulatedMagicPower", accumulator.getAccumulatedMagicPower());
    }

    /** Fügt einen Monolithen hinzu */
    public boolean linkMonolith(MonolithBlockEntity monolith) {
        boolean added = monolithManager.addMonolith(monolith);
        if(added) setChanged();
        return added;
    }

    public LinkedMonolithManager getLinkedMonolithManager() {
        // Client-seitig sicherstellen, dass die Liste initialisiert ist
        if (level != null && level.isClientSide && !monolithManager.isInitialized()) {
            CompoundTag tag = getUpdateTag();
            if (tag.contains("linkedMonoliths")) {
                monolithManager.deserializeFromTag(tag.getList("linkedMonoliths", Tag.TAG_LONG));
            }
            monolithManager.setLevel(level);
            monolithManager.setInitialized(true);
        }
        return monolithManager;
    }


    private Map<BlockPos, CachedMagicResult> lastCalculation = null;
    private long lastCalcTimestamp = 0;
    private static final long TABLE_COOLDOWN_MS = 2000; // 2 Sekunden
    public Map<BlockPos, CachedMagicResult> getOrComputeMonolithMagic(ServerLevel level) {

        this.monolithManager.cleanupInvalidMonoliths(this);
        long now = level.getGameTime() * 50;

        if (lastCalculation != null && (now - lastCalcTimestamp) < TABLE_COOLDOWN_MS) {
            return lastCalculation;
        }

        // Neu berechnen
        Map<BlockPos, CachedMagicResult> result = new HashMap<>();

        for (BlockPos mPos : this.getLinkedMonolithManager().getLinkedMonoliths()) {
            BlockEntity monBE = level.getBlockEntity(mPos);
            if (!(monBE instanceof MonolithBlockEntity monolith)) continue;

            float magicPower = monolith.calculateCurrentMagicPower();
            var rune = monolith.getCurrentRune();

            Map<Enchantment, Integer> unlocked =
                    (rune != null)
                            ? MagicCostCalculator.getUnlockedLevels(rune, magicPower)
                            : Map.of();

            result.put(mPos, new CachedMagicResult(now, magicPower, unlocked));
        }


        lastCalculation = result;
        lastCalcTimestamp = now;
        return result;
    }

    // ========= Magic Accumulation ===========
    public MagicAccumulator getMagicAccumulator () {
        return accumulator;
    }
    private void ensurePaletteLoaded() { // todo needed?
        if (accumulator.getBlockPalette().isEmpty()) {
            accumulator.loadFromJson("/data/magic_overhaul/config/arcane_table.json");
            System.out.println("[ArcaneEnchantingTable] Loaded palette!");
        }
    }

    public int scanSurroundingBlocks(int scanCap, int radiusCap) {
        MagicScanner.scanMagicBlocks(
                level, this.worldPosition,
                accumulator.getBlockPalette(), accumulator.getTagPalette(),
                scanCap, radiusCap
        );

        accumulator.scan(level, this.worldPosition, 3000, 25);
        setChanged();

        return accumulator.getCurrentMagicPowerIncreaseRate();
    }

    private static final int SYNC_INTERVAL = 5;
    private int tickCounter = 0;

    public static void tick(Level level, BlockPos pos, BlockState state, ArcaneEnchantingTableBlockEntity be) {
        be.getMagicAccumulator().tickMagicGain(level);
        if (level.isClientSide) return;

        be.tickCounter++;
        // Nur alle X Ticks syncen
        if (be.tickCounter % SYNC_INTERVAL == 0) {
            for (ServerPlayer player : ((ServerLevel) level).players()) {
                if (player.containerMenu instanceof ArcaneEnchantingTableMenu menu &&
                        menu.getBlockEntity() == be) {

                    SyncMagicAccumulatorPacket.sendToClient(player, pos, be.getMagicAccumulator());
                }
            }
        }
    }

    private final Map<Enchantment, Integer> selected = new HashMap<>();

    public Map<Enchantment, Integer> getSelected() {
        return selected;
    }

    public void setEnchantmentLevel(Enchantment ench, int lvl) {
        if (lvl <= 0) selected.remove(ench);
        else selected.put(ench, lvl);

        setChanged();

        if (!level.isClientSide) {
            // An alle offenen Clients senden
            SyncSelectionPacket.sendToClients(this);
        }
    }

    public int getSelectedLevel(Enchantment ench) {
        return selected.getOrDefault(ench, 0);
    }

    // Reset bei Item-Entfernung
    public void clearSelection() {
        selected.clear();
        setChanged();
    }

    public void markForRenderUpdate() {
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            setChanged();
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

            // Alle Spieler im Chunk senden BE-Paket
            ChunkPos chunkPos = new ChunkPos(worldPosition);
            serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)
                    .forEach(player -> {
                        player.connection.send(ClientboundBlockEntityDataPacket.create(this));
                    });
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Inventory", inventory.serializeNBT());
        tag.put("linkedMonoliths", monolithManager.serializeToTag());
        return tag;
    }

    // handleUpdateTag()
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("linkedMonoliths")) {
            monolithManager.deserializeFromTag(tag.getList("linkedMonoliths", Tag.TAG_LONG));
        }
    }

    private boolean enchantingInProgress = false;

    public boolean isEnchantingInProgress() {
        return enchantingInProgress;
    }

    public void setEnchantingInProgress(boolean value) {
        enchantingInProgress = value;
        markForRenderUpdate();
    }
    private final Map<UUID, Player> boundPlayers = new HashMap<>();

    public void addBoundPlayer(Player player) {
        boundPlayers.put(player.getUUID(), player);
    }

    public void removeBoundPlayer(Player player) {
        boundPlayers.remove(player.getUUID());
    }

    public boolean isPlayerBound(Player player) {
        return boundPlayers.containsKey(player.getUUID());
    }

    public Map<UUID, Player> getBoundPlayers() {
        return boundPlayers;
    }

    public void dropInventory(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.copy());
            }
        }
    }

    public static final TicketType<BlockPos> ARCANE_TABLE_TICKET =
            TicketType.create("ARCANE_TABLE_TICKET", Comparator.comparingLong(BlockPos::asLong));

    @Override
    public void onLoad() {
        super.onLoad();
        if(level != null) {
            monolithManager.setLevel(level); // Level nachtragen, Manager bleibt bestehen
        }
        if (!this.level.isClientSide) {
            ((ServerLevel) level).getChunkSource().addRegionTicket(
                    ARCANE_TABLE_TICKET,
                    new ChunkPos(worldPosition),
                    1,
                    worldPosition
            );

            this.getOrComputeMonolithMagic((ServerLevel) level);
            this.getMagicAccumulator().updateMaxMagic(this);
            scanSurroundingBlocks(3000, 25);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!this.level.isClientSide) {
            ((ServerLevel) level).getChunkSource().removeRegionTicket(
                    ARCANE_TABLE_TICKET,
                    new ChunkPos(worldPosition),
                    1,
                    worldPosition
            );
        }
    }
}
