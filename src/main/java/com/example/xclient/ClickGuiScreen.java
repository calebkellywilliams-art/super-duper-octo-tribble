package com.example.xclient;

import com.example.xclient.Module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Click GUI. Input is polled from GLFW in render() so it doesn't depend on Screen input method signatures. */
public class ClickGuiScreen extends Screen {
    private static final int W = 110, HEAD = 16, ROW = 14, SROW = 13;
    private static final int ACCENT = 0xFF8B5CF6;
    private static final List<Panel> PANELS = new ArrayList<>(); // static so positions persist between opens

    private static class Panel {
        final Category cat; final List<Module> mods = new ArrayList<>();
        int x, y; boolean collapsed;
        Panel(Category cat, int x, int y) { this.cat = cat; this.x = x; this.y = y; }
    }

    private Panel dragging; private int dragDx, dragDy;
    private Setting slider; private int sliderX;
    private boolean prevL, prevR, prevKey;

    public ClickGuiScreen() {
        super(Text.literal("Zenith Client"));
        if (PANELS.isEmpty()) {
            int sw = MinecraftClient.getInstance().getWindow().getScaledWidth();
            int x = 10, y = 24;
            for (Category c : Category.values()) {
                if (x + W > sw - 5) { x = 10; y += 175; } // wrap to the next row on narrow screens
                Panel p = new Panel(c, x, y);
                for (Module m : Modules.ALL) if (m.category == c) p.mods.add(m);
                PANELS.add(p);
                x += W + 8;
            }
        }
        long h = handle();
        prevL = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        prevR = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        prevKey = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_F10) == GLFW.GLFW_PRESS; // still held from opening
    }

    private static long handle() { return MinecraftClient.getInstance().getWindow().getHandle(); }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void removed() { Config.save(); }

    private int height(Panel p) {
        int h = HEAD;
        if (p.collapsed) return h;
        for (Module m : p.mods) {
            h += ROW;
            if (m.expanded) h += m.settings.size() * SROW;
        }
        return h;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        long h = handle();
        boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean key = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_F10) == GLFW.GLFW_PRESS;

        if (key && !prevKey) { // F10 again closes (Esc also works)
            this.client.setScreen(null);
            return;
        }
        boolean lEdge = l && !prevL, rEdge = r && !prevR;
        prevL = l; prevR = r; prevKey = key;

        handleInput(mx, my, l, lEdge, rEdge);
        for (Panel p : PANELS) draw(ctx, p, mx, my);
    }

    private void handleInput(int mx, int my, boolean l, boolean lEdge, boolean rEdge) {
        if (!l) { dragging = null; slider = null; }
        if (dragging != null) { dragging.x = mx - dragDx; dragging.y = my - dragDy; }
        if (slider != null) setFromMouse(slider, sliderX, mx);
        if (!lEdge && !rEdge) return;

        Panel hit = null;
        for (int i = PANELS.size() - 1; i >= 0; i--) {
            Panel p = PANELS.get(i);
            if (mx >= p.x && mx < p.x + W && my >= p.y && my < p.y + height(p)) { hit = p; break; }
        }
        if (hit == null) return;
        PANELS.remove(hit); PANELS.add(hit); // bring to front

        if (my - hit.y < HEAD) {
            if (lEdge) { dragging = hit; dragDx = mx - hit.x; dragDy = my - hit.y; }
            else hit.collapsed = !hit.collapsed;
            return;
        }
        if (hit.collapsed) return;

        int y = hit.y + HEAD;
        for (Module m : hit.mods) {
            if (my >= y && my < y + ROW) {
                if (lEdge) m.toggle();
                else if (!m.settings.isEmpty()) m.expanded = !m.expanded;
                return;
            }
            y += ROW;
            if (m.expanded) {
                for (Setting s : m.settings) {
                    if (lEdge && my >= y && my < y + SROW) {
                        slider = s; sliderX = hit.x;
                        setFromMouse(s, hit.x, mx);
                        return;
                    }
                    y += SROW;
                }
            }
        }
    }

    private void setFromMouse(Setting s, int panelX, int mx) {
        double t = Math.max(0, Math.min(1, (mx - (panelX + 4)) / (double) (W - 8)));
        double v = s.min + t * (s.max - s.min);
        v = Math.round(v / s.step) * s.step;
        s.value = Math.max(s.min, Math.min(s.max, v));
    }

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawTextWithShadow(this.textRenderer, s, x, y, color);
    }

    private void draw(DrawContext ctx, Panel p, int mx, int my) {
        ctx.fill(p.x, p.y, p.x + W, p.y + height(p), 0xE61A1026);
        ctx.fill(p.x, p.y, p.x + W, p.y + HEAD, 0xFF24163A);
        ctx.fill(p.x, p.y + HEAD - 1, p.x + W, p.y + HEAD, ACCENT);
        text(ctx, p.cat.label, p.x + 5, p.y + 4, 0xFFFFFFFF);
        text(ctx, p.collapsed ? "+" : "-", p.x + W - 10, p.y + 4, 0xFFAAAAAA);
        if (p.collapsed) return;

        int y = p.y + HEAD;
        for (Module m : p.mods) {
            boolean hover = mx >= p.x && mx < p.x + W && my >= y && my < y + ROW;
            if (m.enabled) ctx.fill(p.x + 1, y, p.x + W - 1, y + ROW, ACCENT);
            else if (hover) ctx.fill(p.x + 1, y, p.x + W - 1, y + ROW, 0x30FFFFFF);
            text(ctx, m.name, p.x + 6, y + 3, m.enabled ? 0xFFFFFFFF : 0xFFB4B4BC);
            if (!m.settings.isEmpty()) text(ctx, m.expanded ? "v" : ">", p.x + W - 10, y + 3, 0xFFDDDDDD);
            y += ROW;

            if (m.expanded) {
                for (Setting s : m.settings) {
                    ctx.fill(p.x + 1, y, p.x + W - 1, y + SROW, 0xFF18181E);
                    text(ctx, s.name + ": " + String.format("%.1f", s.value), p.x + 6, y + 1, 0xFFDDDDDD);
                    double frac = (s.value - s.min) / (s.max - s.min);
                    ctx.fill(p.x + 4, y + SROW - 3, p.x + W - 4, y + SROW - 1, 0xFF2A2A33);
                    ctx.fill(p.x + 4, y + SROW - 3, p.x + 4 + (int) ((W - 8) * frac), y + SROW - 1, ACCENT);
                    y += SROW;
                }
            }
        }
    }
}
