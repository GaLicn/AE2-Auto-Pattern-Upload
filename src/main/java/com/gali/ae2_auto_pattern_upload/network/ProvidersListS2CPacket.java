package com.gali.ae2_auto_pattern_upload.network;

import com.gali.ae2_auto_pattern_upload.client.gui.GuiProviderSelect;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class ProvidersListS2CPacket implements IMessage {

    private List<Long> ids;
    private List<String> names;
    private List<Integer> emptySlots;

    public ProvidersListS2CPacket() {
        this.ids = new ArrayList<Long>();
        this.names = new ArrayList<String>();
        this.emptySlots = new ArrayList<Integer>();
    }

    public ProvidersListS2CPacket(List<Long> ids, List<String> names, List<Integer> emptySlots) {
        this.ids = ids;
        this.names = names;
        this.emptySlots = emptySlots;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        ids = new ArrayList<Long>(size);
        names = new ArrayList<String>(size);
        emptySlots = new ArrayList<Integer>(size);

        for (int i = 0; i < size; i++) {
            ids.add(buf.readLong());
            int len = buf.readInt();
            byte[] nameBytes = new byte[len];
            buf.readBytes(nameBytes);
            names.add(new String(nameBytes, java.nio.charset.StandardCharsets.UTF_8));
            emptySlots.add(buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            buf.writeLong(ids.get(i));
            byte[] nameBytes = names.get(i).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            buf.writeInt(nameBytes.length);
            buf.writeBytes(nameBytes);
            buf.writeInt(emptySlots.get(i));
        }
    }

    public static class Handler implements IMessageHandler<ProvidersListS2CPacket, IMessage> {

        @Override
        public IMessage onMessage(final ProvidersListS2CPacket message, MessageContext ctx) {
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.func_152344_a(new Runnable() {
                @Override
                public void run() {
                    GuiScreen current = Minecraft.getMinecraft().currentScreen;
                    Minecraft.getMinecraft()
                            .displayGuiScreen(new GuiProviderSelect(current, message.ids, message.names, message.emptySlots));
                }
            });
            return null;
        }
    }
}
