package com.example.xclient;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Generic "find clusters of interesting blocks" scanner used by BaseFinder, StashFinder, StructureFinder
 * and PortalFinder. Each finder scans one slice of the world per tick (so there is no lag), counts matching
 * blocks per 16x16 chunk, and flags chunks that reach its threshold. Only sees chunks that are loaded.
 */
public final class Finder {
    private static final int R = 48;   // horizontal scan radius (blocks)
    private static final int VY = 32;  // vertical scan range above/below you

    private static final Set<Block> BASE_BLOCKS = Set.of(
        Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
        Blocks.ENCHANTING_TABLE, Blocks.BREWING_STAND, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
        Blocks.BEACON, Blocks.JUKEBOX, Blocks.NOTE_BLOCK, Blocks.HOPPER, Blocks.DISPENSER, Blocks.DROPPER,
        Blocks.OBSERVER, Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.REPEATER, Blocks.COMPARATOR,
        Blocks.ENDER_CHEST, Blocks.TNT);
    private static final Set<Block> STASH_BLOCKS = Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL);
    private static final Set<Block> STRUCTURE_BLOCKS = Set.of(
        Blocks.SPAWNER, Blocks.TRIAL_SPAWNER, Blocks.VAULT, Blocks.REINFORCED_DEEPSLATE, Blocks.SCULK_SHRIEKER);
    private static final Set<Block> PORTAL_BLOCKS = Set.of(
        Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME);

    public static final Finder BASE = new Finder(Modules.BASEFINDER,
        st -> BASE_BLOCKS.contains(st.getBlock()) || st.isIn(BlockTags.BEDS), 8, "BASE ", "base", 0xFFFF55FF);
    public static final Finder STASH = new Finder(Modules.STASHFINDER,
        st -> STASH_BLOCKS.contains(st.getBlock()), 6, "STASH ", "stash", 0xFFFFAA00);
    public static final Finder STRUCTURE = new Finder(Modules.STRUCTUREFINDER,
        st -> STRUCTURE_BLOCKS.contains(st.getBlock()), 1, "STRUCTURE ", "structure", 0xFF55FFFF);
    public static final Finder PORTAL = new Finder(Modules.PORTALFINDER,
        st -> PORTAL_BLOCKS.contains(st.getBlock()), 1, "PORTAL ", "portal", 0xFF5555FF);

    public static final List<Finder> ALL = List.of(BASE, STASH, STRUCTURE, PORTAL);

    // ---- per-finder state ----
    public final String label;
    public final int color;
    /** Flagged chunk centres, read by the overlay. Replaced (not edited) after each full pass. */
    public volatile List<BlockPos> hits = List.of();

    private final Module module;
    private final Predicate<BlockState> match;
    private final int threshold;
    private final String noun;
    private final Set<Long> reported = new HashSet<>();
    private Map<Long, long[]> current = new HashMap<>(); // chunk -> {count, sumX, sumY, sumZ}
    private int slab = -R, cx, cy, cz;
    private boolean active;

    private Finder(Module module, Predicate<BlockState> match, int threshold, String label, String noun, int color) {
        this.module = module; this.match = match; this.threshold = threshold;
        this.label = label; this.noun = noun; this.color = color;
    }

    public static void tickAll(MinecraftClient mc, ClientPlayerEntity p) {
        for (Finder f : ALL) f.tick(mc, p);
    }

    public static boolean anyHits() {
        for (Finder f : ALL) if (!f.hits.isEmpty()) return true;
        return false;
    }

    private void tick(MinecraftClient mc, ClientPlayerEntity p) {
        if (!module.enabled || mc.world == null) {
            if (active) reset();
            return;
        }
        active = true;

        if (slab == -R) { // start of a new pass: remember where we are
            BlockPos c = p.getBlockPos();
            cx = c.getX(); cy = c.getY(); cz = c.getZ();
            current = new HashMap<>();
        }

        int x = cx + slab;
        int minY = Math.max(mc.world.getBottomY(), cy - VY);
        int maxY = Math.min(mc.world.getBottomY() + mc.world.getHeight() - 1, cy + VY);
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int z = cz - R; z <= cz + R; z++) {
            for (int y = minY; y <= maxY; y++) {
                BlockState st = mc.world.getBlockState(m.set(x, y, z));
                if (st.isAir() || !match.test(st)) continue;
                long key = ChunkPos.toLong(x >> 4, z >> 4);
                long[] e = current.computeIfAbsent(key, k -> new long[4]);
                e[0]++; e[1] += x; e[2] += y; e[3] += z;
            }
        }

        if (++slab > R) finishPass(p);
    }

    private void finishPass(ClientPlayerEntity p) {
        List<BlockPos> found = new ArrayList<>();
        int announced = 0;
        for (Map.Entry<Long, long[]> en : current.entrySet()) {
            long[] e = en.getValue();
            if (e[0] < threshold) continue;
            BlockPos centre = new BlockPos((int) (e[1] / e[0]), (int) (e[2] / e[0]), (int) (e[3] / e[0]));
            found.add(centre);
            if (reported.add(en.getKey()) && announced < 3) { // tell you once per chunk, max 3 per pass
                announced++;
                p.sendMessage(Text.literal("\u00a7d[Zenith] \u00a7fPossible " + noun + " near \u00a7e" + centre.getX() + ", "
                    + centre.getY() + ", " + centre.getZ() + " \u00a77(" + e[0] + " blocks)"), false);
            }
        }
        hits = found;
        slab = -R;
    }

    private void reset() {
        hits = List.of();
        reported.clear();
        current = new HashMap<>();
        slab = -R;
        active = false;
    }
}
