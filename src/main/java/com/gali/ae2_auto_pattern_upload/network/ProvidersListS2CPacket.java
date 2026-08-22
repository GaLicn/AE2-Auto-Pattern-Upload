package com.gali.ae2_auto_pattern_upload.network;

import com.gali.ae2_auto_pattern_upload.AE2AutoPatternUpload;
import com.gali.ae2_auto_pattern_upload.client.ProviderSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record ProvidersListS2CPacket(List<Long> ids, List<Component> names, List<Integer> emptySlots)
        implements CustomPacketPayload {
    public static final Type<ProvidersListS2CPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AE2AutoPatternUpload.MODID, "providers_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProvidersListS2CPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.ids.size());
                for (int i = 0; i < packet.ids.size(); i++) {
                    buf.writeLong(packet.ids.get(i));
                    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.names.get(i));
                    buf.writeVarInt(packet.emptySlots.get(i));
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<Long> ids = new ArrayList<>(size);
                List<Component> names = new ArrayList<>(size);
                List<Integer> slots = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ids.add(buf.readLong());
                    names.add(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf));
                    slots.add(buf.readVarInt());
                }
                return new ProvidersListS2CPacket(ids, names, slots);
            });

    public static void handle(ProvidersListS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new ProviderSelectScreen(minecraft.screen,
                    packet.ids, packet.names, packet.emptySlots));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
