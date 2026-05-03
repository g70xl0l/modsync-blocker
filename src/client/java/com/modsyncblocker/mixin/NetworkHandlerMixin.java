package com.modsyncblocker.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

@Mixin(value = {ClientCommonNetworkHandler.class, ClientPlayNetworkHandler.class, ClientConfigurationNetworkHandler.class}, priority = 500)
public class NetworkHandlerMixin {

    private String extractData(CustomPayload payload) {
        if (payload instanceof com.modsyncblocker.ModSyncBlocker.ConnectionPayload cp) return cp.data();
        if (payload instanceof com.modsyncblocker.ModSyncBlocker.UploadPayload up) return up.data();
        try {
            return (String) payload.getClass().getMethod("data").invoke(payload);
        } catch (Exception e) {
            return null;
        }
    }

    @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        String id = packet.payload().getId().id().toString();
        if (id.startsWith("modsync:")) {
            ci.cancel();

            String data = extractData(packet.payload());
            if (data == null) return;

            MinecraftClient client = MinecraftClient.getInstance();

            if (id.equals("modsync:connection")) {
                if (data.startsWith("HANDSHAKE_START|")) {
                    String challenge = data.substring("HANDSHAKE_START|".length());
                    com.modsyncblocker.FakePayloadSender.sendSpoofedHandshake(challenge);
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§a[ModSyncBlocker] Handshake spoofed (Mods hidden)"), false);
                        }
                    });
                }
            } else if (id.equals("modsync:upload")) {
                if (data.startsWith("REQUEST|SCREENSHOT|")) {
                    com.modsyncblocker.FakePayloadSender.sendSpoofedPreUpload();
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§a[ModSyncBlocker] Screenshot request spoofed"), false);
                        }
                    });
                } else if (data.startsWith("PROCEED|")) {
                    com.modsyncblocker.FakePayloadSender.sendSpoofedUploadData();
                } else if (data.startsWith("REQUEST|MOD|") || data.startsWith("REQUEST|PACK|")) {
                    com.modsyncblocker.FakePayloadSender.sendSpoofedError();
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§a[ModSyncBlocker] Blocked file dump request"), false);
                        }
                    });
                }
            }
        }
    }

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true, require = 0)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof CustomPayloadC2SPacket customPacket) {
            String id = customPacket.payload().getId().id().toString();
            if (id.startsWith("modsync:")) {
                if (com.modsyncblocker.FakePayloadSender.IS_SPOOFING) {
                    return;
                }
                
                ci.cancel();
            }
        }
    }
}
