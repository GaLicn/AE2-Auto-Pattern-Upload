package com.gali.ae2_auto_pattern_upload.network;

import com.gali.ae2_auto_pattern_upload.AE2AutoPatternUpload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(AE2AutoPatternUpload.MODID);
        registrar.playToServer(RequestProvidersListC2SPacket.TYPE,
                RequestProvidersListC2SPacket.STREAM_CODEC, RequestProvidersListC2SPacket::handle);
        registrar.playToClient(ProvidersListS2CPacket.TYPE,
                ProvidersListS2CPacket.STREAM_CODEC, ProvidersListS2CPacket::handle);
        registrar.playToServer(UploadEncodedPatternC2SPacket.TYPE,
                UploadEncodedPatternC2SPacket.STREAM_CODEC, UploadEncodedPatternC2SPacket::handle);
    }
}
