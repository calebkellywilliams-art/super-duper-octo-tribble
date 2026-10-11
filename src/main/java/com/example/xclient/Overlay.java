package com.example.xclient;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tracers / ChestESP / OreESP. Instead of hooking the 3D renderer (which changes a lot between versions),
 * this projects world positions onto the screen and draws 2D markers on the HUD layer.
 */
public final class Overlay {
    private static final Set<Block> CHEST_BLOCKS =
        Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL, Blocks.SPAWNER);
    private static final List<BlockPos> chests = new ArrayList<>();
    private static final List<BlockPos> ores = new ArrayList<>();

    private record P(double x, double y, boolean front) {}

    private Overlay() {}

    /** Called every tick; rescans nearby blocks once per second. */
    public static void scan(MinecraftClient mc, ClientPlayerEntity p, int tick) {
        if (tick % 20 != 0) return;
        chests.clear();
        ores.clear();
        if (mc.world == null || (!Modules.CHESTESP.enabled && !Modules.OREESP.enabled)) return;
        int r = 20;
        BlockPos c = p.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    Block b = mc.world.getBlockState(m).getBlock();
                    if (Modules.CHESTESP.enabled && CHEST_BLOCKS.contains(b)) chests.add(m.toImmutable());
                    else if (Modules.OREESP.enabled && XClient.XRAY_BLOCKS.contains(b)) ores.add(m.toImmutable());
                }
            }
        }
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        boolean tracers = Modules.TRACERS.enabled;
        if (!tracers && chests.isEmpty() && ores.isEmpty() && !Finder.anyHits()) return;

        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        double fov = mc.options.getFov().getValue();
        Vec3d eye = p.getEyePos();
        double yaw = p.getYaw(), pitch = p.getPitch();

        if (tracers) {
            int n = 0;
            for (Entity e : mc.world.getEntities()) {
                if (e == p || !(e instanceof LivingEntity le) || !le.isAlive()) continue;
                if (p.distanceTo(e) > 64) continue;
                if (++n > 40) break;
                int color = e instanceof Monster ? 0xFFFF5555 : e instanceof PlayerEntity ? 0xFFFFFFFF : 0xFF55FF55;
                P pr = project(eye, yaw, pitch, new Vec3d(e.getX(), e.getY() + e.getHeight() / 2, e.getZ()), w, h, fov);
                line(ctx, w / 2.0, h - 2.0, pr.x(), pr.y(), w, h, color);
            }
        }
        int count = 0;
        for (BlockPos pos : chests) {
            if (++count > 200) break;
            marker(ctx, mc, p, eye, yaw, pitch, pos, w, h, fov, "", 0xFFFFAA00);
        }
        count = 0;
        for (BlockPos pos : ores) {
            if (++count > 200) break;
            marker(ctx, mc, p, eye, yaw, pitch, pos, w, h, fov, "", oreColor(mc.world.getBlockState(pos).getBlock()));
        }
        for (Finder f : Finder.ALL) {
            for (BlockPos pos : f.hits) marker(ctx, mc, p, eye, yaw, pitch, pos, w, h, fov, f.label, f.color);
        }
    }

    private static void marker(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p, Vec3d eye, double yaw,
                               double pitch, BlockPos pos, int w, int h, double fov, String label, int color) {
        Vec3d center = Vec3d.ofCenter(pos);
        P pr = project(eye, yaw, pitch, center, w, h, fov);
        if (!pr.front() || pr.x() < 0 || pr.x() > w || pr.y() < 0 || pr.y() > h) return;
        int x = (int) pr.x(), y = (int) pr.y();
        ctx.fill(x - 3, y - 3, x + 4, y - 2, color);
        ctx.fill(x - 3, y + 3, x + 4, y + 4, color);
        ctx.fill(x - 3, y - 3, x - 2, y + 4, color);
        ctx.fill(x + 3, y - 3, x + 4, y + 4, color);
        ctx.drawTextWithShadow(mc.textRenderer, label + (int) new Vec3d(p.getX(), p.getY(), p.getZ()).distanceTo(center) + "m", x + 6, y - 4, color);
    }

    private static int oreColor(Block b) {
        String n = Registries.BLOCK.getId(b).getPath();
        if (n.contains("diamond")) return 0xFF55FFFF;
        if (n.contains("emerald")) return 0xFF55FF55;
        if (n.contains("gold")) return 0xFFFFD84A;
        if (n.contains("iron")) return 0xFFD8AF93;
        if (n.contains("redstone")) return 0xFFFF5555;
        if (n.contains("lapis")) return 0xFF5577FF;
        if (n.contains("coal")) return 0xFFAAAAAA;
        if (n.contains("copper")) return 0xFFE07A3F;
        if (n.contains("ancient")) return 0xFFC0804F;
        return 0xFFFFFFFF;
    }

    /** Projects a world position to GUI-scaled screen coordinates using the player's view. */
    private static P project(Vec3d eye, double yawDeg, double pitchDeg, Vec3d t, int w, int h, double fovDeg) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
        double vx = t.x - eye.x, vy = t.y - eye.y, vz = t.z - eye.z;
        double df = vx * fx + vy * fy + vz * fz;
        double dr = vx * rx + vy * ry + vz * rz;
        double du = vx * ux + vy * uy + vz * uz;
        double tan = Math.tan(Math.toRadians(fovDeg) / 2.0), aspect = (double) w / h;
        if (df > 0.05) {
            return new P(w / 2.0 + (dr / df) / (tan * aspect) * (w / 2.0),
                         h / 2.0 - (du / df) / tan * (h / 2.0), true);
        }
        double len = Math.hypot(dr, du);
        if (len < 1e-6) return new P(w / 2.0, h + 5000, false);
        return new P(w / 2.0 + dr / len * 10000, h / 2.0 - du / len * 10000, false); // behind: push to the edge
    }

    private static void line(DrawContext ctx, double ox, double oy, double ex, double ey, int w, int h, int color) {
        double t = 1;
        if (ex < 0) t = Math.min(t, (0 - ox) / (ex - ox)); else if (ex > w) t = Math.min(t, (w - ox) / (ex - ox));
        if (ey < 0) t = Math.min(t, (0 - oy) / (ey - oy)); else if (ey > h) t = Math.min(t, (h - oy) / (ey - oy));
        double x1 = ox + (ex - ox) * t, y1 = oy + (ey - oy) * t;
        int steps = (int) Math.min(Math.max(Math.abs(x1 - ox), Math.abs(y1 - oy)) / 2, 600);
        for (int i = 0; i <= steps; i++) {
            int x = (int) (ox + (x1 - ox) * i / Math.max(steps, 1));
            int y = (int) (oy + (y1 - oy) * i / Math.max(steps, 1));
            ctx.fill(x, y, x + 2, y + 2, color);
        }
    }
}
