package com.modsyncblocker;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.CustomPayload;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.UUID;

public class FakePayloadSender {
    public static boolean IS_SPOOFING = false;
    private static final String FAKE_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
    private static final long FAKE_PNG_SIZE = 68;

    public static String getClientInstanceId() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("modsyncblocker");
        File file = configDir.resolve("instance.dat").toFile();
        if (file.exists()) {
            try {
                return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        String newId = UUID.randomUUID().toString();
        try {
            if (!configDir.toFile().exists()) {
                configDir.toFile().mkdirs();
            }
            Files.write(file.toPath(), newId.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newId;
    }

    public static String calculateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(255 & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void sendSpoofedHandshake(String challenge) {
        String clientId = getClientInstanceId();
        String rawData = challenge + "&9aQ#Z" + "" + "" + clientId;
        String hash = calculateHash(rawData);
        String payloadData = "HANDSHAKE_RESPONSE|||" + clientId + "|" + hash + "|2.0";
        sendPayload("connection", payloadData);
    }

    public static void sendSpoofedPreUpload() {
        sendPayload("upload", "PRE_UPLOAD|screenshot.png|" + FAKE_PNG_SIZE);
    }

    public static void sendSpoofedUploadData() {
        Thread thread = new Thread(() -> {
            try {
                sendPayload("upload", "START|screenshot.png|" + FAKE_PNG_SIZE);
                Thread.sleep(50);
                sendPayload("upload", "DATA|0|" + FAKE_PNG_BASE64);
                Thread.sleep(50);
                sendPayload("upload", "END");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }
    
    public static void sendSpoofedError() {
        sendPayload("upload", "ERROR|Mod not found");
    }

    private static void sendPayload(String channel, String data) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        
        IS_SPOOFING = true;
        try {
            try {
                Class<?> payloadClass = null;
                if (channel.equals("connection")) {
                    payloadClass = Class.forName("com.ModSync.networking.ModSyncPayload");
                } else if (channel.equals("upload")) {
                    payloadClass = Class.forName("com.ModSync.networking.UploadPayload");
                }
                
                if (payloadClass != null) {
                    Object payload = payloadClass.getConstructor(String.class).newInstance(data);
                    ClientPlayNetworking.send((CustomPayload) payload);
                    return;
                }
            } catch (Throwable e) {
                // ModSync not installed, fallback
            }
            
            if (channel.equals("connection")) {
                ClientPlayNetworking.send(new ModSyncBlocker.ConnectionPayload(data));
            } else if (channel.equals("upload")) {
                ClientPlayNetworking.send(new ModSyncBlocker.UploadPayload(data));
            }
        } finally {
            IS_SPOOFING = false;
        }
    }
}
