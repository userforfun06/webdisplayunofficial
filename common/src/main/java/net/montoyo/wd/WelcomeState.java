package net.montoyo.wd;

import net.minecraft.server.MinecraftServer;
import net.montoyo.wd.utilities.Log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WelcomeState {

    private static final String FILE_NAME = "wd_welcome.dat";
    private final Set<UUID> welcomedPlayers = new HashSet<>();
    private final File dataFile;

    public WelcomeState(MinecraftServer server) {
        dataFile = net.montoyo.wd.client.WebDisplaysDirs.getCacheFile(FILE_NAME);
        load();
    }

    private void load() {
        welcomedPlayers.clear();
        if (!dataFile.exists()) return;
        try {
            for (String line : Files.readAllLines(dataFile.toPath(), StandardCharsets.UTF_8)) {
                line = line.trim();
                if (!line.isEmpty()) {
                    welcomedPlayers.add(UUID.fromString(line));
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            Log.warning("Could not load welcome state from %s: %s", dataFile.getAbsolutePath(), e.getMessage());
        }
    }

    private void save() {
        try {
            BufferedWriter writer = Files.newBufferedWriter(dataFile.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try (writer) {
                for (UUID uuid : welcomedPlayers) {
                    writer.write(uuid.toString());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            Log.error("Could not save welcome state to %s: %s", dataFile.getAbsolutePath(), e.getMessage());
        }
    }

    public boolean hasBeenWelcomed(UUID uuid) {
        return welcomedPlayers.contains(uuid);
    }

    public void markWelcomed(UUID uuid) {
        welcomedPlayers.add(uuid);
        save();
    }

}
