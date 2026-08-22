package com.gali.ae2_auto_pattern_upload.network;

import com.gali.ae2_auto_pattern_upload.AE2AutoPatternUpload;
import appeng.menu.me.items.PatternEncodingTermMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UploadEncodedPatternC2SPacket(long providerId, boolean showStatusMessage, String providerName)
        implements CustomPacketPayload {
    public static final Type<UploadEncodedPatternC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AE2AutoPatternUpload.MODID, "upload_encoded_pattern"));
    public static final StreamCodec<FriendlyByteBuf, UploadEncodedPatternC2SPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> {
                        buf.writeLong(packet.providerId);
                        buf.writeBoolean(packet.showStatusMessage);
                        buf.writeUtf(packet.providerName == null ? "" : packet.providerName, 256);
                    },
                    buf -> new UploadEncodedPatternC2SPacket(buf.readLong(), buf.readBoolean(), buf.readUtf(256)));

    public UploadEncodedPatternC2SPacket(long providerId) {
        this(providerId, false, "");
    }

    public static void handle(UploadEncodedPatternC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof PatternEncodingTermMenu menu)) {
                return;
            }
            int index = (int) (-1L - packet.providerId);
            boolean uploaded = PatternUploadUtil.uploadByIndex(player, menu, index);
            if (packet.showStatusMessage) {
                player.sendSystemMessage(Component.translatable(
                        uploaded
                                ? "ae2_auto_pattern_upload.screen.upload.auto_success"
                                : "ae2_auto_pattern_upload.screen.upload.auto_failed",
                        packet.providerName == null || packet.providerName.isBlank()
                                ? "#" + packet.providerId : packet.providerName));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
