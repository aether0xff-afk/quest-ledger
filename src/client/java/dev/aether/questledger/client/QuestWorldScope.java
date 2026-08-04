package dev.aether.questledger.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

public record QuestWorldScope(
        String key,
        String displayName,
        Kind kind,
        String directoryName
) {
    public enum Kind {
        SINGLEPLAYER,
        MULTIPLAYER,
        CONNECTION
    }

    public static Optional<QuestWorldScope> resolve(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return Optional.empty();
        }

        if (minecraft.hasSingleplayerServer()) {
            var server = minecraft.getSingleplayerServer();
            if (server != null) {
                Path root = server.getWorldPath(LevelResource.ROOT)
                        .toAbsolutePath()
                        .normalize();
                String displayName = server.getWorldData().getLevelName();
                return Optional.of(create(
                        "singleplayer:" + root,
                        displayName,
                        Kind.SINGLEPLAYER
                ));
            }
        }

        ServerData serverData = minecraft.getCurrentServer();
        if (serverData != null) {
            String address = serverData.ip == null ? "unknown" : serverData.ip.strip();
            String displayName = serverData.name == null || serverData.name.isBlank()
                    ? address
                    : serverData.name.strip();
            return Optional.of(create(
                    "multiplayer:" + address.toLowerCase(Locale.ROOT),
                    displayName,
                    Kind.MULTIPLAYER
            ));
        }

        var listener = minecraft.getConnection();
        if (listener != null) {
            String remoteAddress = String.valueOf(
                    listener.getConnection().getRemoteAddress()
            );
            return Optional.of(create(
                    "connection:" + remoteAddress,
                    remoteAddress,
                    Kind.CONNECTION
            ));
        }

        return Optional.empty();
    }

    private static QuestWorldScope create(String key, String displayName, Kind kind) {
        String safeName = displayName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (safeName.isBlank()) {
            safeName = kind.name().toLowerCase(Locale.ROOT);
        }
        if (safeName.length() > 40) {
            safeName = safeName.substring(0, 40);
        }
        return new QuestWorldScope(
                key,
                displayName,
                kind,
                safeName + "-" + shortHash(key)
        );
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
