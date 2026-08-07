package com.captainratax.simplenobeambeacon;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class HiddenBeaconService implements Listener {
    private static final long REFRESH_PERIOD_TICKS = 80L;
    private static final int BEAM_COLUMN_HAS_TINTED_GLASS = 1;
    private static final int BEAM_COLUMN_IS_BLOCKED = 1 << 1;
    private static final Method PAPER_GET_EFFECT_RANGE = findPaperEffectRangeMethod();

    private final Plugin plugin;
    private final SchedulerFacade scheduler;
    private final ConcurrentHashMap<BeaconKey, Object> tracked = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BeaconKey, HiddenBeacon> active = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> scheduledPlayers = new ConcurrentHashMap<>();

    HiddenBeaconService(Plugin plugin, SchedulerFacade scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                scheduleChunkScan(chunk);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            schedulePlayer(player);
        }
    }

    void stop() {
        tracked.clear();
        active.clear();
        scheduledPlayers.clear();
    }

    int trackedCount() {
        return tracked.size();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        inspectChangedBlock(event.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        forgetChangedBlock(broken);
        if (broken.getType() == Material.TINTED_GLASS) {
            deactivateColumnBelow(broken);
            Location location = broken.getLocation();
            scheduler.runLater(location, 1L, () -> scanBeaconsBelow(location.getBlock()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block changed = event.getBlock();
        Material type = changed.getType();
        if (type == Material.BEACON || type == Material.TINTED_GLASS) {
            scheduler.runLater(changed.getLocation(), 1L, () -> inspectChangedBlock(changed));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        schedulePistonRescan(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        schedulePistonRescan(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        forgetDestroyedBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        forgetDestroyedBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked != null && clicked.getType() == Material.BEACON) {
            trackIfHidden(clicked);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        schedulePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        scheduledPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        scanChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        forgetChunk(event.getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        UUID worldId = event.getWorld().getUID();
        tracked.keySet().removeIf(key -> key.worldId().equals(worldId));
        active.keySet().removeIf(key -> key.worldId().equals(worldId));
    }

    private void inspectChangedBlock(Block block) {
        if (block.getType() == Material.BEACON) {
            trackIfHidden(block);
        }
        if (block.getType() == Material.TINTED_GLASS) {
            scanBeaconsBelow(block);
        }
    }

    private void forgetChangedBlock(Block block) {
        if (block.getType() == Material.BEACON) {
            forget(BeaconKey.from(block.getLocation()));
        }
    }

    private void trackIfHidden(Block candidate) {
        if (candidate.getType() != Material.BEACON
                || (scanBeamColumn(candidate) & BEAM_COLUMN_HAS_TINTED_GLASS) == 0) {
            return;
        }

        BeaconKey key = BeaconKey.from(candidate.getLocation());
        Object generation = new Object();
        Object existingGeneration = tracked.putIfAbsent(key, generation);
        if (existingGeneration != null) {
            refresh(key, existingGeneration);
            return;
        }

        refresh(key, generation);
        Location location = candidate.getLocation();
        scheduler.runAtLocation(location, REFRESH_PERIOD_TICKS, REFRESH_PERIOD_TICKS, () -> {
            if (tracked.get(key) != generation) {
                return false;
            }
            return refresh(key, generation);
        });
    }

    private boolean refresh(BeaconKey key, Object generation) {
        if (tracked.get(key) != generation) {
            return false;
        }

        World world = Bukkit.getWorld(key.worldId());
        if (world == null) {
            forget(key, generation);
            return false;
        }

        Block beaconBlock = world.getBlockAt(key.x(), key.y(), key.z());
        if (beaconBlock.getType() != Material.BEACON) {
            forget(key, generation);
            return false;
        }

        int beamColumn = scanBeamColumn(beaconBlock);
        if ((beamColumn & BEAM_COLUMN_HAS_TINTED_GLASS) == 0) {
            forget(key, generation);
            return false;
        }

        int tier = calculateTier(beaconBlock);
        if (tier == 0 || (beamColumn & BEAM_COLUMN_IS_BLOCKED) != 0) {
            return deactivateIfCurrent(key, generation);
        }

        BlockState state = beaconBlock.getState();
        if (!(state instanceof Beacon)) {
            return deactivateIfCurrent(key, generation);
        }

        Beacon beacon = (Beacon) state;
        PotionEffect primaryEffect = beacon.getPrimaryEffect();
        if (primaryEffect == null) {
            return deactivateIfCurrent(key, generation);
        }

        PotionEffect secondaryEffect = beacon.getSecondaryEffect();
        if (tracked.get(key) != generation) {
            return false;
        }
        HiddenBeacon snapshot = new HiddenBeacon(
                key.worldId(),
                key.x(),
                key.y(),
                key.z(),
                tier,
                resolveEffectRange(beacon, tier),
                primaryEffect.getType(),
                secondaryEffect == null ? null : secondaryEffect.getType(),
                primaryEffect.getAmplifier() > 0,
                generation
        );
        active.put(key, snapshot);
        if (tracked.get(key) != generation) {
            active.remove(key, snapshot);
            return false;
        }
        return true;
    }

    private int calculateTier(Block beacon) {
        World world = beacon.getWorld();
        int completed = 0;
        for (int level = 1; level <= 4; level++) {
            int y = beacon.getY() - level;
            if (y < world.getMinHeight()) {
                break;
            }

            for (int x = beacon.getX() - level; x <= beacon.getX() + level; x++) {
                for (int z = beacon.getZ() - level; z <= beacon.getZ() + level; z++) {
                    if (!Tag.BEACON_BASE_BLOCKS.isTagged(world.getBlockAt(x, y, z).getType())) {
                        return completed;
                    }
                }
            }
            completed = level;
        }
        return completed;
    }

    private int scanBeamColumn(Block beacon) {
        World world = beacon.getWorld();
        int result = 0;
        for (int y = beacon.getY() + 1; y < world.getMaxHeight(); y++) {
            Material type = world.getBlockAt(beacon.getX(), y, beacon.getZ()).getType();
            if (type == Material.TINTED_GLASS) {
                result |= BEAM_COLUMN_HAS_TINTED_GLASS;
            } else if (type != Material.BEDROCK && type.isOccluding()) {
                result |= BEAM_COLUMN_IS_BLOCKED;
            }
        }
        return result;
    }

    private void schedulePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        Object generation = new Object();
        if (scheduledPlayers.putIfAbsent(playerId, generation) != null) {
            return;
        }

        scheduler.runForPlayer(
                player,
                1L,
                REFRESH_PERIOD_TICKS,
                () -> {
                    if (scheduledPlayers.get(playerId) != generation || !player.isOnline()) {
                        return false;
                    }
                    applyEffects(player, active.values());
                    return true;
                },
                () -> scheduledPlayers.remove(playerId, generation)
        );
    }

    private void applyEffects(Player player, Collection<HiddenBeacon> beacons) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        UUID worldId = world.getUID();
        BoundingBox bounds = player.getBoundingBox();
        for (HiddenBeacon beacon : beacons) {
            if (!beacon.worldId().equals(worldId)) {
                continue;
            }
            if (!BeaconMath.intersects(
                    beacon.x(),
                    beacon.y(),
                    beacon.z(),
                    beacon.range(),
                    world.getMaxHeight() - world.getMinHeight(),
                    bounds.getMinX(),
                    bounds.getMinY(),
                    bounds.getMinZ(),
                    bounds.getMaxX(),
                    bounds.getMaxY(),
                    bounds.getMaxZ()
            )) {
                continue;
            }
            applyBeaconEffects(player, beacon);
        }
    }

    private void applyBeaconEffects(Player player, HiddenBeacon beacon) {
        PotionEffectType primary = beacon.primary();
        PotionEffectType secondary = beacon.secondary();
        boolean upgradedPrimary = beacon.tier() == 4 && beacon.upgradedPrimary();
        int duration = BeaconMath.effectDurationTicks(beacon.tier());

        new PotionEffect(primary, duration, upgradedPrimary ? 1 : 0, true, true).apply(player);
        if (beacon.tier() == 4 && secondary != null && !primary.equals(secondary)) {
            new PotionEffect(secondary, duration, 0, true, true).apply(player);
        }
    }

    private void scanChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Beacon) {
                trackIfHidden(state.getBlock());
            }
        }
    }

    private void scheduleChunkScan(Chunk chunk) {
        World world = chunk.getWorld();
        Location anchor = new Location(world, (chunk.getX() << 4) + 8, world.getMinHeight(), (chunk.getZ() << 4) + 8);
        scheduler.runLater(anchor, 1L, () -> {
            if (world.isChunkLoaded(chunk.getX(), chunk.getZ())) {
                scanChunk(world.getChunkAt(chunk.getX(), chunk.getZ()));
            }
        });
    }

    private void schedulePistonRescan(Collection<Block> movedBlocks, BlockFace direction) {
        for (Block source : movedBlocks) {
            forgetChangedBlock(source);
            if (source.getType() == Material.TINTED_GLASS) {
                deactivateColumnBelow(source);
            }

            Location sourceLocation = source.getLocation();
            Location forward = sourceLocation.clone().add(
                    direction.getModX(),
                    direction.getModY(),
                    direction.getModZ()
            );
            Location backward = sourceLocation.clone().subtract(
                    direction.getModX(),
                    direction.getModY(),
                    direction.getModZ()
            );
            scheduleChangedAreaScan(sourceLocation);
            scheduleChangedAreaScan(forward);
            scheduleChangedAreaScan(backward);
        }
    }

    private void scheduleChangedAreaScan(Location location) {
        scheduler.runLater(location, 1L, () -> {
            Block changed = location.getBlock();
            inspectChangedBlock(changed);
            inspectChangedBlock(changed.getRelative(BlockFace.DOWN));
            inspectChangedBlock(changed.getRelative(BlockFace.UP));
        });
    }

    private void forgetDestroyedBlocks(Collection<Block> blocks) {
        for (Block block : blocks) {
            forgetChangedBlock(block);
            if (block.getType() == Material.TINTED_GLASS) {
                deactivateColumnBelow(block);
            }
        }
    }

    private void scanBeaconsBelow(Block top) {
        World world = top.getWorld();
        for (int y = top.getY() - 1; y >= world.getMinHeight(); y--) {
            Block candidate = world.getBlockAt(top.getX(), y, top.getZ());
            if (candidate.getType() == Material.BEACON) {
                trackIfHidden(candidate);
            }
        }
    }

    private void deactivateColumnBelow(Block top) {
        UUID worldId = top.getWorld().getUID();
        int x = top.getX();
        int y = top.getY();
        int z = top.getZ();
        active.keySet().removeIf(key -> key.worldId().equals(worldId)
                && key.x() == x
                && key.y() < y
                && key.z() == z);
    }

    private void forgetChunk(UUID worldId, int chunkX, int chunkZ) {
        tracked.keySet().removeIf(key -> key.worldId().equals(worldId)
                && key.chunkX() == chunkX
                && key.chunkZ() == chunkZ);
        active.keySet().removeIf(key -> key.worldId().equals(worldId)
                && key.chunkX() == chunkX
                && key.chunkZ() == chunkZ);
    }

    private void forget(BeaconKey key) {
        tracked.remove(key);
        active.remove(key);
    }

    private void forget(BeaconKey key, Object generation) {
        if (tracked.remove(key, generation)) {
            removeActiveGeneration(key, generation);
        }
    }

    private boolean deactivateIfCurrent(BeaconKey key, Object generation) {
        if (tracked.get(key) != generation) {
            return false;
        }
        removeActiveGeneration(key, generation);
        return true;
    }

    private void removeActiveGeneration(BeaconKey key, Object generation) {
        active.computeIfPresent(key, (ignored, snapshot) ->
                snapshot.generation() == generation ? null : snapshot
        );
    }

    private static double resolveEffectRange(Beacon beacon, int calculatedTier) {
        double vanillaRange = BeaconMath.rangeForTier(calculatedTier);
        if (PAPER_GET_EFFECT_RANGE == null) {
            return vanillaRange;
        }

        try {
            double paperRange = ((Number) PAPER_GET_EFFECT_RANGE.invoke(beacon)).doubleValue();
            double staleDefault = BeaconMath.rangeForTier(beacon.getTier());
            if (!Double.isFinite(paperRange) || paperRange < 0.0D
                    || Math.abs(paperRange - staleDefault) < 0.000_001D) {
                return vanillaRange;
            }
            return paperRange;
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException ignored) {
            return vanillaRange;
        }
    }

    private static Method findPaperEffectRangeMethod() {
        try {
            return Beacon.class.getMethod("getEffectRange");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

}
