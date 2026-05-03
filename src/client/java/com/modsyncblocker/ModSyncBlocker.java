package com.modsyncblocker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ModSyncBlocker implements ClientModInitializer {

    public record ConnectionPayload(String data) implements CustomPayload {
        public static final Id<ConnectionPayload> ID = new Id<>(Identifier.of("modsync", "connection"));
        public static final PacketCodec<PacketByteBuf, ConnectionPayload> CODEC = PacketCodecs.STRING.xmap(ConnectionPayload::new, ConnectionPayload::data).cast();
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record UploadPayload(String data) implements CustomPayload {
        public static final Id<UploadPayload> ID = new Id<>(Identifier.of("modsync", "upload"));
        public static final PacketCodec<PacketByteBuf, UploadPayload> CODEC = PacketCodecs.STRING.xmap(UploadPayload::new, UploadPayload::data).cast();
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    @Override
    public void onInitializeClient() {
        try {
            PayloadTypeRegistry.playC2S().register(ConnectionPayload.ID, ConnectionPayload.CODEC);
        } catch (Exception e) {
            // Already registered by ModSync, ignore
        }
        try {
            PayloadTypeRegistry.playC2S().register(UploadPayload.ID, UploadPayload.CODEC);
        } catch (Exception e) {
            // Already registered by ModSync, ignore
        }

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sendToChat("§6[ModSyncBlocker] Загружен — подмена пакетов ModSync активна");
        });
    }

    private void sendToChat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }
}