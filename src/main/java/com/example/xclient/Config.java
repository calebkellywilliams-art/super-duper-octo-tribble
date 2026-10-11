package com.example.xclient;

import com.example.xclient.Module.Category;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Saves slider values (and render/client toggles) to .minecraft/config/xclient.properties. */
public final class Config {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("xclient.properties");

    private Config() {}

    // Only restore harmless toggles on startup (never things like Creative or Nuker).
    private static boolean restoreEnabled(Module m) {
        return (m.category == Category.RENDER || m.category == Category.CLIENT) && m != Modules.PANIC && m != Modules.FREECAM;
    }

    public static void load() {
        Properties pr = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            pr.load(in);
        } catch (IOException e) {
            return; // no config yet
        }
        for (Module m : Modules.ALL) {
            if (restoreEnabled(m)) {
                String v = pr.getProperty("module." + m.name);
                if (v != null) m.enabled = Boolean.parseBoolean(v);
            }
            for (Setting s : m.settings) {
                String v = pr.getProperty("setting." + m.name + "." + s.name);
                if (v != null) {
                    try { s.value = Math.max(s.min, Math.min(s.max, Double.parseDouble(v))); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    public static void save() {
        Properties pr = new Properties();
        for (Module m : Modules.ALL) {
            if (restoreEnabled(m)) pr.setProperty("module." + m.name, Boolean.toString(m.enabled));
            for (Setting s : m.settings) pr.setProperty("setting." + m.name + "." + s.name, Double.toString(s.value));
        }
        try (OutputStream out = Files.newOutputStream(FILE)) {
            pr.store(out, "Zenith Client settings");
        } catch (IOException ignored) {}
    }
}
