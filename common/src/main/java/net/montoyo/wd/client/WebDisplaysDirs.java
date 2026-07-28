package net.montoyo.wd.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WebDisplaysDirs {

    private WebDisplaysDirs() {}

    private static Path getBasePath() {
        return FabricLoader.getInstance().getGameDir().resolve("mods/webdisplays");
    }

    public static File getCacheFile(String name) {
        Path dir = getBasePath();
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {}
        return dir.resolve(name).toFile();
    }

    public static File getSubDir(String sub) {
        Path dir = getBasePath().resolve(sub);
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {}
        return dir.toFile();
    }

    public static Path getBaseDir() {
        return getBasePath();
    }
}
