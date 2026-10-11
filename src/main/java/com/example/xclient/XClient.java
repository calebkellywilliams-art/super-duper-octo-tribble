package com.example.xclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class XClient implements ClientModInitializer {
    private static final double DEF_BLOCK = 4.5, DEF_ENTITY = 3.0;

    public static final Set<Block> XRAY_BLOCKS = Set.of(
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
        Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
        Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
        Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS,
        Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.SPAWNER
    );

    private KeyBinding kGui;
    private final Map<KeyBinding, Module> hotkeys = new LinkedHashMap<>();
    private int tickCounter = 0;
    private boolean nvAdded = false;

    @Override
    public void onInitializeClient() {
        Config.load();
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Config.save());

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("xclient", "main"));
        kGui = reg("key.xclient.gui", GLFW.GLFW_KEY_F10, cat);
        hotkeys.put(reg("key.xclient.esp",   GLFW.GLFW_KEY_G, cat), Modules.ESP);
        hotkeys.put(reg("key.xclient.xray",  GLFW.GLFW_KEY_X, cat), Modules.XRAY);
        hotkeys.put(reg("key.xclient.reach", GLFW.GLFW_KEY_V, cat), Modules.REACH);
        hotkeys.put(reg("key.xclient.hud",   GLFW.GLFW_KEY_H, cat), Modules.HUD);
        hotkeys.put(reg("key.xclient.fullbright", GLFW.GLFW_KEY_UNKNOWN, cat), Modules.FULLBRIGHT);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
            Identifier.of("xclient", "hud"), this::renderHud);
    }

    private static KeyBinding reg(String name, int key, KeyBinding.Category cat) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, key, cat));
    }

    private void onTick(MinecraftClient mc) {
        while (kGui.wasPressed()) {
            if (mc.player != null) mc.setScreen(new ClickGuiScreen());
        }
        hotkeys.forEach((key, module) -> { while (key.wasPressed()) module.toggle(); });

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Fullbright = client-side night vision
        boolean wantNv = Modules.XRAY.enabled || Modules.FULLBRIGHT.enabled;
        if (wantNv && !player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1_000_000, 0, false, false));
            nvAdded = true;
        } else if (!wantNv && nvAdded) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            nvAdded = false;
        }

        if (++tickCounter % 5 == 0) applyReach(mc, player);
        Features.tick(mc, player, tickCounter);
    }

    private void applyReach(MinecraftClient mc, ClientPlayerEntity player) {
        double block  = Modules.REACH.enabled ? Modules.REACH_BLOCK.value  : DEF_BLOCK;
        double entity = Modules.REACH.enabled ? Modules.REACH_ENTITY.value : DEF_ENTITY;
        setRange(player, block, entity);

        // Singleplayer: the integrated server validates interactions, so set it there too.
        IntegratedServer server = mc.getServer();
        if (server != null) {
            var uuid = player.getUuid();
            server.execute(() -> {
                var sp = server.getPlayerManager().getPlayer(uuid);
                if (sp != null) setRange(sp, block, entity);
            });
        }
    }

    private static void setRange(PlayerEntity p, double block, double entity) {
        EntityAttributeInstance b = p.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
        if (b != null && b.getBaseValue() != block) b.setBaseValue(block);
        EntityAttributeInstance e = p.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
        if (e != null && e.getBaseValue() != entity) e.setBaseValue(entity);
    }

    private void renderHud(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        Overlay.render(ctx, mc);
        if (!Modules.HUD.enabled) return;
        var tr = mc.textRenderer;

        int x = 4, y = 4;
        ctx.drawTextWithShadow(tr, "Zenith Client", x, y, Modules.RAINBOW.enabled ? rainbow(0) : 0xFFA855F7);
        y += 12;

        boolean any = false;
        for (Module m : Modules.ALL) {
            if (!m.enabled || m == Modules.HUD || m == Modules.RAINBOW || Modules.isButton(m)) continue;
            String label = m == Modules.REACH ? String.format("Reach %.1f", Modules.REACH_BLOCK.value) : m.name;
            ctx.drawTextWithShadow(tr, label, x, y, Modules.RAINBOW.enabled ? rainbow(y * 8) : 0xFFC084FC);
            y += 10; any = true;
        }
        if (!any) { ctx.drawTextWithShadow(tr, "(no modules on)", x, y, 0xFFAAAAAA); y += 10; }

        y += 4;
        ctx.drawTextWithShadow(tr, String.format("XYZ %.1f / %.1f / %.1f",
            mc.player.getX(), mc.player.getY(), mc.player.getZ()), x, y, 0xFFFFFFFF);
        y += 10;
        ctx.drawTextWithShadow(tr, mc.getCurrentFps() + " FPS", x, y, 0xFFFFFFFF);
        y += 10;
        ctx.drawTextWithShadow(tr, String.format("%.1f b/s", mc.player.getVelocity().horizontalLength() * 20), x, y, 0xFFFFFFFF);
    }

    private static int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() + offset * 10L) % 3000L) / 3000f;
        return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.7f, 1f);
    }
}
