package com.example.xclient;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import com.example.xclient.mixin.MinecraftClientAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Per-tick logic for the movement / combat / player cheats. Singleplayer. */
public final class Features {
    private static boolean wasGod, wasCreative, wasAutoMine, zoomed;
    private static int savedFov = 70;
    private static boolean wasSpectator, returnOnExit;
    private static GameMode savedMode = GameMode.SURVIVAL;
    private static Vec3d savedPos = Vec3d.ZERO;

    private Features() {}

    public static void tick(MinecraftClient mc, ClientPlayerEntity p, int tick) {
        // ---- Movement ----
        if (Modules.FLY.enabled) fly(mc, p);
        else if (Modules.SPEED.enabled) speed(mc, p);

        boolean moving = !moveDir(mc, p).equals(Vec3d.ZERO);

        if (Modules.SPRINT.enabled && mc.options.forwardKey.isPressed() && !p.isSneaking()
                && !p.horizontalCollision && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
        if (Modules.SPIDER.enabled && p.horizontalCollision) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.2, v.z);
        }
        if (Modules.FASTLADDER.enabled && p.isClimbing()
                && (mc.options.forwardKey.isPressed() || mc.options.jumpKey.isPressed())) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.4, v.z);
        }
        if (Modules.GLIDE.enabled && !p.isOnGround()) {
            Vec3d v = p.getVelocity();
            if (v.y < -0.1) p.setVelocity(v.x, -0.1, v.z);
        }
        if (Modules.FASTFALL.enabled && !p.isOnGround()) {
            Vec3d v = p.getVelocity();
            if (v.y < -0.2) p.setVelocity(v.x, Math.max(v.y * 1.15, -2.5), v.z);
        }
        if (Modules.DOLPHIN.enabled && p.isTouchingWater() && moving) {
            Vec3d d = moveDir(mc, p);
            p.setVelocity(d.x * 0.35, p.getVelocity().y, d.z * 0.35);
        }
        if (Modules.JESUS.enabled && !p.isSneaking() && mc.world != null) {
            Vec3d v = p.getVelocity();
            if (p.isTouchingWater() || p.isInLava()) {
                p.setVelocity(v.x, 0.12, v.z);
            } else if (!mc.world.getFluidState(p.getBlockPos().down()).isEmpty() && v.y < 0) {
                p.setVelocity(v.x, 0, v.z);
            }
        }
        if (Modules.AIRJUMP.enabled && !Modules.FLY.enabled && mc.options.jumpKey.isPressed() && !p.isOnGround()) {
            Vec3d v = p.getVelocity();
            if (v.y < 0.3) p.setVelocity(v.x, 0.3, v.z);
        }
        if (Modules.AUTOJUMP.enabled && p.isOnGround() && moving) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.42, v.z);
        }
        if (Modules.NOFALL.enabled || Modules.FLY.enabled || Modules.GLIDE.enabled || Modules.JESUS.enabled) {
            p.fallDistance = 0;
            server(mc, p, sp -> sp.fallDistance = 0);
        }

        // ---- Combat ----
        if (Modules.KILLAURA.enabled) aura(mc, p);
        if (Modules.TRIGGERBOT.enabled) trigger(mc, p);
        if (Modules.AIMBOT.enabled) aim(mc, p);
        if (Modules.PURGE.enabled) { purge(mc, p); Modules.PURGE.enabled = false; }

        // ---- Player ----
        if (Modules.AUTORESPAWN.enabled && p.getHealth() <= 0) p.requestRespawn();
        if (Modules.NUKER.enabled && tick % 2 == 0) nuker(mc, p);
        if (Modules.INSTANTMINE.enabled) instantMine(mc, p);
        if (Modules.MAGNET.enabled && tick % 2 == 0) magnet(mc, p);
        if (Modules.XPBOOST.enabled && tick % 10 == 0) server(mc, p, sp -> sp.addExperienceLevels(1));

        if (Modules.AUTOMINE.enabled && mc.currentScreen == null) {
            mc.options.attackKey.setPressed(true);
            wasAutoMine = true;
        } else if (wasAutoMine) {
            mc.options.attackKey.setPressed(false);
            wasAutoMine = false;
        }

        if (Modules.HEAL.enabled) {
            server(mc, p, sp -> {
                sp.setHealth(sp.getMaxHealth());
                sp.getHungerManager().setFoodLevel(20);
                sp.getHungerManager().setSaturationLevel(20f);
            });
            Modules.HEAL.enabled = false;
        }

        boolean god = Modules.GODMODE.enabled, hunger = Modules.NOHUNGER.enabled, drown = Modules.NODROWN.enabled;
        boolean regen = Modules.REGEN.enabled, fire = Modules.NOFIRE.enabled;
        boolean unbreak = Modules.UNBREAKING.enabled, refill = Modules.REFILL.enabled;
        boolean prevGod = wasGod;
        if (god || hunger || drown || regen || fire || unbreak || refill || prevGod) {
            server(mc, p, sp -> {
                if (god) {
                    sp.setInvulnerable(true);
                    if (sp.getHealth() < sp.getMaxHealth()) sp.setHealth(sp.getMaxHealth());
                } else if (prevGod) {
                    sp.setInvulnerable(false);
                }
                if (hunger) {
                    sp.getHungerManager().setFoodLevel(20);
                    sp.getHungerManager().setSaturationLevel(20f);
                }
                if (drown) sp.setAir(sp.getMaxAir());
                if (regen && sp.getHealth() < sp.getMaxHealth()) sp.heal(1.0f);
                if (fire) { sp.extinguish(); sp.setFireTicks(0); }
                ItemStack held = sp.getMainHandStack();
                if (!held.isEmpty()) {
                    if (unbreak && held.isDamaged()) held.setDamage(0);
                    if (refill && held.getCount() < held.getMaxCount()) held.setCount(held.getMaxCount());
                }
            });
        }
        wasGod = god;

        boolean creative = Modules.CREATIVE.enabled;
        if (creative != wasCreative) {
            server(mc, p, sp -> sp.changeGameMode(creative ? GameMode.CREATIVE : GameMode.SURVIVAL));
            wasCreative = creative;
        }

        // ---- Auto / building / spectator-based ----
        boolean totem = Modules.AUTOTOTEM.enabled, armor = Modules.AUTOARMOR.enabled, eat = Modules.AUTOEAT.enabled;
        if (totem || armor || eat) server(mc, p, sp -> inventoryAutomation(sp, totem, armor, eat));
        if (Modules.SCAFFOLD.enabled) scaffold(mc, p);
        if (Modules.FASTPLACE.enabled && mc instanceof MinecraftClientAccessor acc) acc.xclient$setItemUseCooldown(0);

        // NoClip and Freecam both use spectator mode on the integrated server (real no-clip, no rubber-banding).
        boolean spectator = Modules.NOCLIP.enabled || Modules.FREECAM.enabled;
        if (spectator != wasSpectator) {
            boolean freecam = Modules.FREECAM.enabled;
            if (spectator) {
                server(mc, p, sp -> {
                    savedMode = sp.interactionManager.getGameMode();
                    savedPos = new Vec3d(sp.getX(), sp.getY(), sp.getZ());
                    returnOnExit = freecam;
                    sp.changeGameMode(GameMode.SPECTATOR);
                });
            } else {
                server(mc, p, sp -> {
                    sp.changeGameMode(savedMode);
                    if (returnOnExit) sp.requestTeleport(savedPos.x, savedPos.y, savedPos.z);
                });
            }
            wasSpectator = spectator;
        }

        Overlay.scan(mc, p, tick);
        Finder.tickAll(mc, p);

        // ---- Render ----
        if (Modules.ZOOM.enabled) {
            if (!zoomed) { savedFov = mc.options.getFov().getValue(); zoomed = true; }
            mc.options.getFov().setValue((int) Modules.ZOOM_FOV.value);
        } else if (zoomed) {
            mc.options.getFov().setValue(savedFov);
            zoomed = false;
        }

        if (tick % 5 == 0) attributes(mc, p);
    }

    // ---- helpers ----
    private static void inventoryAutomation(ServerPlayerEntity sp, boolean totem, boolean armor, boolean eat) {
        var inv = sp.getInventory();
        if (totem && sp.getOffHandStack().isEmpty()) {
            for (int i = 0; i < 36; i++) {
                ItemStack st = inv.getStack(i);
                if (st.isOf(Items.TOTEM_OF_UNDYING)) {
                    sp.equipStack(EquipmentSlot.OFFHAND, st.copyWithCount(1));
                    st.decrement(1);
                    break;
                }
            }
        }
        if (armor) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (!sp.getEquippedStack(slot).isEmpty()) continue;
                for (int i = 0; i < 36; i++) {
                    ItemStack st = inv.getStack(i);
                    if (!st.isEmpty() && sp.getPreferredEquipmentSlot(st) == slot) {
                        sp.equipStack(slot, st.copyWithCount(1));
                        st.decrement(1);
                        break;
                    }
                }
            }
        }
        if (eat && sp.getHungerManager().getFoodLevel() <= 14) {
            for (int i = 0; i < 36; i++) {
                ItemStack st = inv.getStack(i);
                FoodComponent food = st.get(DataComponentTypes.FOOD);
                if (food != null && !st.isOf(Items.GOLDEN_APPLE) && !st.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
                    sp.getHungerManager().add(food.nutrition(), food.saturation());
                    st.decrement(1);
                    break;
                }
            }
        }
    }

    private static void scaffold(MinecraftClient mc, ClientPlayerEntity p) {
        IntegratedServer srv = mc.getServer();
        if (srv == null) return;
        server(mc, p, sp -> {
            for (ServerWorld w : srv.getWorlds()) {
                if (w.getEntity(sp.getUuid()) != sp) continue; // find the world the player is in
                BlockPos below = sp.getBlockPos().down();
                if (!w.getBlockState(below).isReplaceable()) return;
                for (int i = 0; i < 36; i++) {
                    ItemStack st = sp.getInventory().getStack(i);
                    if (!st.isEmpty() && st.getItem() instanceof BlockItem bi) {
                        w.setBlockState(below, bi.getBlock().getDefaultState());
                        if (!sp.isCreative()) st.decrement(1);
                        return;
                    }
                }
                return;
            }
        });
    }

    private static Vec3d moveDir(MinecraftClient mc, ClientPlayerEntity p) {
        double f = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        double s = (mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0);
        double len = Math.sqrt(f * f + s * s);
        if (len == 0) return Vec3d.ZERO;
        f /= len; s /= len;
        double yaw = Math.toRadians(p.getYaw());
        return new Vec3d(-Math.sin(yaw) * f + Math.cos(yaw) * s, 0, Math.cos(yaw) * f + Math.sin(yaw) * s);
    }

    private static void fly(MinecraftClient mc, ClientPlayerEntity p) {
        double sp = Modules.FLY_SPEED.value;
        Vec3d d = moveDir(mc, p);
        double vy = 0;
        if (mc.options.jumpKey.isPressed()) vy += sp;
        if (mc.options.sneakKey.isPressed()) vy -= sp;
        p.setVelocity(d.x * sp, vy, d.z * sp);
    }

    private static void speed(MinecraftClient mc, ClientPlayerEntity p) {
        Vec3d d = moveDir(mc, p);
        if (d.equals(Vec3d.ZERO)) return;
        double v = 0.22 * Modules.SPEED_MULT.value;
        p.setVelocity(d.x * v, p.getVelocity().y, d.z * v);
    }

    private static double attackRange() {
        return Modules.REACH.enabled ? Modules.REACH_ENTITY.value : 3.0;
    }

    private static LivingEntity nearestHostile(MinecraftClient mc, ClientPlayerEntity p, double range) {
        if (mc.world == null) return null;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof Monster && e instanceof LivingEntity le && le.isAlive()) { // hostile mobs only
                double d = p.distanceTo(le);
                if (d <= range && d < bestDist) { best = le; bestDist = d; }
            }
        }
        return best;
    }

    private static void aura(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.interactionManager == null) return;
        if (p.getAttackCooldownProgress(0.5f) < 1.0f) return;
        LivingEntity target = nearestHostile(mc, p, attackRange());
        if (target != null) {
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    private static void trigger(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.interactionManager == null) return;
        if (p.getAttackCooldownProgress(0.5f) < 1.0f) return;
        if (mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof Monster
                && hit.getEntity() instanceof LivingEntity le && le.isAlive()) {
            mc.interactionManager.attackEntity(p, le);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    private static void aim(MinecraftClient mc, ClientPlayerEntity p) {
        LivingEntity t = nearestHostile(mc, p, 8.0);
        if (t == null) return;
        Vec3d to = new Vec3d(t.getX(), t.getY() + t.getHeight() * 0.5, t.getZ()).subtract(p.getEyePos());
        double yaw = Math.toDegrees(Math.atan2(-to.x, to.z));
        double pitch = -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        float step = 25f; // max degrees per tick, keeps it smooth
        float dy = MathHelper.clamp(MathHelper.wrapDegrees((float) yaw - p.getYaw()), -step, step);
        float dp = MathHelper.clamp((float) pitch - p.getPitch(), -step, step);
        p.setYaw(p.getYaw() + dy);
        p.setPitch(MathHelper.clamp(p.getPitch() + dp, -90f, 90f));
    }

    private static void purge(MinecraftClient mc, ClientPlayerEntity p) {
        IntegratedServer srv = mc.getServer();
        if (mc.world == null || srv == null) return;
        List<UUID> ids = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof Monster && p.distanceTo(e) < 64) ids.add(e.getUuid());
        }
        if (ids.isEmpty()) return;
        server(mc, p, sp -> {
            for (ServerWorld w : srv.getWorlds()) {
                for (UUID id : ids) {
                    Entity e = w.getEntity(id);
                    if (e != null) e.discard();
                }
            }
        });
    }

    private static void magnet(MinecraftClient mc, ClientPlayerEntity p) {
        IntegratedServer srv = mc.getServer();
        if (mc.world == null || srv == null) return;
        List<UUID> ids = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof ItemEntity && p.distanceTo(e) < 10) ids.add(e.getUuid());
        }
        if (ids.isEmpty()) return;
        server(mc, p, sp -> {
            for (ServerWorld w : srv.getWorlds()) {
                for (UUID id : ids) {
                    Entity e = w.getEntity(id);
                    if (e != null) e.setPosition(sp.getX(), sp.getY(), sp.getZ());
                }
            }
        });
    }

    private static void instantMine(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.world == null || mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
        if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            BlockState st = mc.world.getBlockState(pos);
            if (!st.isAir() && st.getHardness(mc.world, pos) >= 0) {
                server(mc, p, sp -> sp.interactionManager.tryBreakBlock(pos));
            }
        }
    }

    private static void nuker(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.world == null) return;
        int r = (int) Modules.NUKER_RADIUS.value;
        BlockPos c = p.getBlockPos();
        List<BlockPos> targets = new ArrayList<>();
        outer:
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = 0; dy <= r; dy++) { // never digs the floor out from under you
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = c.add(dx, dy, dz);
                    BlockState st = mc.world.getBlockState(pos);
                    if (st.isAir() || st.getHardness(mc.world, pos) < 0) continue; // skip air + bedrock
                    targets.add(pos);
                    if (targets.size() >= 15) break outer;
                }
            }
        }
        if (!targets.isEmpty()) {
            server(mc, p, sp -> { for (BlockPos pos : targets) sp.interactionManager.tryBreakBlock(pos); });
        }
    }

    private static void attributes(MinecraftClient mc, ClientPlayerEntity p) {
        double step = Modules.STEP.enabled ? Modules.STEP_HEIGHT.value : 0.6;
        double jump = Modules.HIGHJUMP.enabled ? Modules.JUMP_STRENGTH.value : 0.42;
        double grav = Modules.LOWGRAVITY.enabled ? Modules.GRAVITY.value : 0.08;
        double kb = Modules.NOKNOCKBACK.enabled ? 1.0 : 0.0;
        set(p, EntityAttributes.STEP_HEIGHT, step);
        set(p, EntityAttributes.JUMP_STRENGTH, jump);
        set(p, EntityAttributes.GRAVITY, grav);
        set(p, EntityAttributes.KNOCKBACK_RESISTANCE, kb);
        server(mc, p, sp -> {
            set(sp, EntityAttributes.STEP_HEIGHT, step);
            set(sp, EntityAttributes.KNOCKBACK_RESISTANCE, kb);
        });
    }

    private static void set(PlayerEntity p, RegistryEntry<EntityAttribute> attr, double value) {
        EntityAttributeInstance a = p.getAttributeInstance(attr);
        if (a != null && a.getBaseValue() != value) a.setBaseValue(value);
    }

    /** Run something on the integrated server's copy of the player (singleplayer only). */
    private static void server(MinecraftClient mc, ClientPlayerEntity p, Consumer<ServerPlayerEntity> action) {
        IntegratedServer s = mc.getServer();
        if (s == null) return;
        UUID id = p.getUuid();
        s.execute(() -> {
            ServerPlayerEntity sp = s.getPlayerManager().getPlayer(id);
            if (sp != null) action.accept(sp);
        });
    }
}
