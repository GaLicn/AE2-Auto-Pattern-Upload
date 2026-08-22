package com.gali.ae2_auto_pattern_upload.network;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.gali.ae2_auto_pattern_upload.AE2AutoPatternUpload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;

public record RequestProvidersListC2SPacket() implements CustomPacketPayload {
    public static final Type<RequestProvidersListC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AE2AutoPatternUpload.MODID, "request_providers_list"));
    public static final RequestProvidersListC2SPacket INSTANCE = new RequestProvidersListC2SPacket();
    public static final StreamCodec<FriendlyByteBuf, RequestProvidersListC2SPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    public static void handle(RequestProvidersListC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof PatternEncodingTermMenu menu)) {
                return;
            }
            var providers = PatternUploadUtil.listAvailableProviders(menu);
            var ids = new ArrayList<Long>(providers.size());
            var names = new ArrayList<net.minecraft.network.chat.Component>(providers.size());
            var slots = new ArrayList<Integer>(providers.size());
            for (int index = 0; index < providers.size(); index++) {
                ids.add(-1L - index);
                names.add(providers.get(index).name());
                slots.add(providers.get(index).emptySlots());
            }
            player.connection.send(new ProvidersListS2CPacket(ids, names, slots));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
