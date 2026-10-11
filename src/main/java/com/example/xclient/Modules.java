package com.example.xclient;

import com.example.xclient.Module.Category;
import net.minecraft.client.MinecraftClient;

import java.util.List;

public final class Modules {
    // ---- Movement ----
    public static final Module FLY        = new Module("Fly", Category.MOVEMENT);
    public static final Module SPEED      = new Module("Speed", Category.MOVEMENT);
    public static final Module SPRINT     = new Module("Sprint", Category.MOVEMENT);
    public static final Module STEP       = new Module("Step", Category.MOVEMENT);
    public static final Module HIGHJUMP   = new Module("HighJump", Category.MOVEMENT);
    public static final Module AUTOJUMP   = new Module("AutoJump", Category.MOVEMENT);
    public static final Module AIRJUMP    = new Module("AirJump", Category.MOVEMENT);
    public static final Module LOWGRAVITY = new Module("LowGravity", Category.MOVEMENT);
    public static final Module DOLPHIN    = new Module("Dolphin", Category.MOVEMENT);
    // ---- Exploit ----
    public static final Module SPIDER     = new Module("Spider", Category.EXPLOIT);
    public static final Module GLIDE      = new Module("Glide", Category.EXPLOIT);
    public static final Module NOFALL     = new Module("NoFall", Category.EXPLOIT);
    public static final Module FASTFALL   = new Module("FastFall", Category.EXPLOIT);
    public static final Module FASTLADDER = new Module("FastLadder", Category.EXPLOIT);
    public static final Module JESUS      = new Module("Jesus", Category.EXPLOIT);
    public static final Module SAFEWALK   = new Module("SafeWalk", Category.EXPLOIT);
    public static final Module NOCLIP     = new Module("NoClip", Category.EXPLOIT);
    // ---- Combat ----
    public static final Module REACH       = new Module("Reach", Category.COMBAT);
    public static final Module KILLAURA    = new Module("KillAura", Category.COMBAT);
    public static final Module TRIGGERBOT  = new Module("TriggerBot", Category.COMBAT);
    public static final Module AIMBOT      = new Module("Aimbot", Category.COMBAT);
    public static final Module NOKNOCKBACK = new Module("NoKnockback", Category.COMBAT);
    public static final Module PURGE       = new Module("Purge", Category.COMBAT); // button
    // ---- Player ----
    public static final Module GODMODE     = new Module("Godmode", Category.PLAYER);
    public static final Module NOHUNGER    = new Module("NoHunger", Category.PLAYER);
    public static final Module NODROWN     = new Module("NoDrown", Category.PLAYER);
    public static final Module REGEN       = new Module("Regen", Category.PLAYER);
    public static final Module NOFIRE      = new Module("NoFire", Category.PLAYER);
    public static final Module CREATIVE    = new Module("Creative", Category.PLAYER);
    public static final Module AUTORESPAWN = new Module("AutoRespawn", Category.PLAYER);
    public static final Module XPBOOST     = new Module("XPBoost", Category.PLAYER);
    public static final Module HEAL        = new Module("Heal", Category.PLAYER); // button
    // ---- Auto ----
    public static final Module AUTOTOTEM   = new Module("AutoTotem", Category.AUTO);
    public static final Module AUTOARMOR   = new Module("AutoArmor", Category.AUTO);
    public static final Module AUTOEAT     = new Module("AutoEat", Category.AUTO);
    public static final Module REFILL      = new Module("Refill", Category.AUTO);
    public static final Module UNBREAKING  = new Module("Unbreaking", Category.AUTO);
    public static final Module MAGNET      = new Module("Magnet", Category.AUTO);
    // ---- World ----
    public static final Module NUKER       = new Module("Nuker", Category.WORLD);
    public static final Module INSTANTMINE = new Module("InstantMine", Category.WORLD);
    public static final Module AUTOMINE    = new Module("AutoMine", Category.WORLD);
    public static final Module SCAFFOLD    = new Module("Scaffold", Category.WORLD);
    public static final Module FASTPLACE   = new Module("FastPlace", Category.WORLD);
    // ---- Finders ----
    public static final Module BASEFINDER      = new Module("BaseFinder", Category.FINDER);
    public static final Module STASHFINDER     = new Module("StashFinder", Category.FINDER);
    public static final Module STRUCTUREFINDER = new Module("StructureFinder", Category.FINDER);
    public static final Module PORTALFINDER    = new Module("PortalFinder", Category.FINDER);
    // ---- Render ----
    public static final Module ESP        = new Module("ESP", Category.RENDER);
    public static final Module ITEMESP    = new Module("ItemESP", Category.RENDER);
    public static final Module CHESTESP   = new Module("ChestESP", Category.RENDER);
    public static final Module OREESP     = new Module("OreESP", Category.RENDER);
    public static final Module TRACERS    = new Module("Tracers", Category.RENDER);
    public static final Module XRAY       = new Module("X-Ray", Category.RENDER);
    public static final Module FULLBRIGHT = new Module("Fullbright", Category.RENDER);
    public static final Module ZOOM       = new Module("Zoom", Category.RENDER);
    public static final Module FREECAM    = new Module("Freecam", Category.RENDER);
    // ---- Client ----
    public static final Module HUD     = new Module("HUD", Category.CLIENT);
    public static final Module RAINBOW = new Module("Rainbow", Category.CLIENT);
    public static final Module PANIC   = new Module("Panic", Category.CLIENT); // button

    public static final Setting FLY_SPEED     = new Setting("Speed", 0.5, 5.0, 0.5, 1.0);
    public static final Setting SPEED_MULT    = new Setting("Multiplier", 1.0, 5.0, 0.5, 2.0);
    public static final Setting STEP_HEIGHT   = new Setting("Height", 1.0, 5.0, 0.5, 2.0);
    public static final Setting JUMP_STRENGTH = new Setting("Strength", 0.5, 3.0, 0.1, 1.0);
    public static final Setting GRAVITY       = new Setting("Gravity", 0.01, 0.07, 0.01, 0.02);
    public static final Setting REACH_BLOCK   = new Setting("Block", 3.0, 8.0, 0.5, 6.0);
    public static final Setting REACH_ENTITY  = new Setting("Entity", 3.0, 8.0, 0.5, 6.0);
    public static final Setting NUKER_RADIUS  = new Setting("Radius", 1.0, 5.0, 1.0, 3.0);
    public static final Setting ZOOM_FOV      = new Setting("FOV", 10.0, 90.0, 5.0, 30.0);

    public static final List<Module> ALL = List.of(
        FLY, SPEED, SPRINT, STEP, HIGHJUMP, AUTOJUMP, AIRJUMP, LOWGRAVITY, DOLPHIN,
        SPIDER, GLIDE, NOFALL, FASTFALL, FASTLADDER, JESUS, SAFEWALK, NOCLIP,
        REACH, KILLAURA, TRIGGERBOT, AIMBOT, NOKNOCKBACK, PURGE,
        GODMODE, NOHUNGER, NODROWN, REGEN, NOFIRE, CREATIVE, AUTORESPAWN, XPBOOST, HEAL,
        AUTOTOTEM, AUTOARMOR, AUTOEAT, REFILL, UNBREAKING, MAGNET,
        NUKER, INSTANTMINE, AUTOMINE, SCAFFOLD, FASTPLACE,
        BASEFINDER, STASHFINDER, STRUCTUREFINDER, PORTALFINDER,
        ESP, ITEMESP, CHESTESP, OREESP, TRACERS, XRAY, FULLBRIGHT, ZOOM, FREECAM,
        HUD, RAINBOW, PANIC);

    static {
        HUD.enabled = true;
        FLY.settings.add(FLY_SPEED);
        SPEED.settings.add(SPEED_MULT);
        STEP.settings.add(STEP_HEIGHT);
        HIGHJUMP.settings.add(JUMP_STRENGTH);
        LOWGRAVITY.settings.add(GRAVITY);
        REACH.settings.add(REACH_BLOCK);
        REACH.settings.add(REACH_ENTITY);
        NUKER.settings.add(NUKER_RADIUS);
        ZOOM.settings.add(ZOOM_FOV);
        XRAY.onChange = on -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.worldRenderer != null) mc.worldRenderer.reload();
        };
        PANIC.onChange = on -> {
            if (!on) return;
            for (Module m : ALL) if (m != PANIC && m != HUD && m != RAINBOW && m.enabled) m.setEnabled(false);
            PANIC.enabled = false;
        };
    }

    public static boolean isButton(Module m) { return m == PANIC || m == HEAL || m == PURGE; }

    private Modules() {}
}
