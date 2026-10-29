package com.gali.ae2_auto_pattern_upload.network.crafting;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.gali.ae2_auto_pattern_upload.crafting.CraftingItemsCache;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 同步正在合成的物品到客户端
 */
public class PacketCraftingItemsUpdate implements IMessage {

    private Set<IAEItemStack> craftingItems = new HashSet<>();

    public PacketCraftingItemsUpdate() {}

    public PacketCraftingItemsUpdate(Set<IAEItemStack> items) {
        this.craftingItems = items;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        craftingItems = new HashSet<>();
        for (int i = 0; i < count; i++) {
            try {
                IAEItemStack stack = AEItemStack.loadItemStackFromPacket(buf);
                if (stack != null) {
                    craftingItems.add(stack);
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(craftingItems.size());
        for (IAEItemStack stack : craftingItems) {
            try {
                stack.writeToPacket(buf);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public Set<IAEItemStack> getCraftingItems() {
        return craftingItems;
    }

    /**
     * 客户端处理器
     */
    public static class ClientHandler implements IMessageHandler<PacketCraftingItemsUpdate, IMessage> {

        @Override
        public IMessage onMessage(PacketCraftingItemsUpdate message, MessageContext ctx) {
            CraftingItemsCache.updateFromServer(message.getCraftingItems());
            refreshCurrentTerminalRepo();
            return null;
        }

        private void refreshCurrentTerminalRepo() {
            GuiScreen screen = Minecraft.getMinecraft().currentScreen;
            if (screen == null) {
                return;
            }

            // 强制刷新当前打开的终端
            IDisplayRepo repo = findRepo(screen);
            if (repo != null) {
                repo.updateView();
            }
        }

        private IDisplayRepo findRepo(GuiScreen screen) {
            Class<?> currentClass = screen.getClass();
            while (currentClass != null) {
                try {
                    Field repoField = currentClass.getDeclaredField("repo");
                    repoField.setAccessible(true);
                    Object repo = repoField.get(screen);
                    return repo instanceof IDisplayRepo ? (IDisplayRepo) repo : null;
                } catch (NoSuchFieldException ignored) {
                    currentClass = currentClass.getSuperclass();
                } catch (Throwable ignored) {
                    return null;
                }
            }
            return null;
        }
    }
}
