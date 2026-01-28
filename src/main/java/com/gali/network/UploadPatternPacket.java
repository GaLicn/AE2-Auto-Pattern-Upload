package com.gali.network;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.gali.mixin.PatternEncodingTermMenuAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: 上传编码样板到指定供应器
 */
public class UploadPatternPacket {
    private final long providerId;
    
    public UploadPatternPacket(long providerId) {
        this.providerId = providerId;
    }
    
    public static UploadPatternPacket decode(FriendlyByteBuf buf) {
        return new UploadPatternPacket(buf.readLong());
    }
    
    public static void encode(UploadPatternPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.providerId);
    }
    
    public static void handle(UploadPatternPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof PatternEncodingTermMenu menu)) {
                return;
            }
            
            long providerId = msg.providerId;
            
            try {
                // 通过Mixin访问器获取编码终端的样板输出槽
                PatternEncodingTermMenuAccessor accessor = (PatternEncodingTermMenuAccessor) menu;
                var encodedSlot = accessor.getEncodedPatternSlot();
                if (encodedSlot == null) {
                    return;
                }
                
                // 获取输出槽位的样板
                ItemStack encodedPattern = encodedSlot.getItem();
                if (encodedPattern.isEmpty()) {
                    return;
                }
                
                // 检查是否是有效的编码样板
                if (!isValidPattern(encodedPattern)) {
                    return;
                }
                
                // 获取网络节点
                IGridNode node = menu.getNetworkNode();
                if (node == null) return;
                
                IGrid grid = node.getGrid();
                if (grid == null) return;
                
                // 查找目标供应器
                PatternContainer targetProvider = null;
                for (var machineClass : grid.getMachineClasses()) {
                    if (PatternContainer.class.isAssignableFrom(machineClass)) {
                        @SuppressWarnings("unchecked")
                        Class<? extends PatternContainer> containerClass = (Class<? extends PatternContainer>) machineClass;
                        
                        for (var container : grid.getActiveMachines(containerClass)) {
                            if (container != null && container.hashCode() == (int) providerId) {
                                targetProvider = container;
                                break;
                            }
                        }
                        if (targetProvider != null) break;
                    }
                }
                
                if (targetProvider == null) {
                    return;
                }
                
                // 获取目标供应器的样板库存
                InternalInventory providerInv = targetProvider.getTerminalPatternInventory();
                if (providerInv == null) {
                    return;
                }
                
                // 尝试将样板插入到第一个空槽位
                boolean uploaded = false;
                for (int i = 0; i < providerInv.size(); i++) {
                    if (providerInv.getStackInSlot(i).isEmpty()) {
                        ItemStack copy = encodedPattern.copy();
                        copy.setCount(1);
                        providerInv.setItemDirect(i, copy);
                        uploaded = true;
                        break;
                    }
                }
                
                if (uploaded) {
                    // 清空编码终端的输出槽位
                    encodedSlot.set(ItemStack.EMPTY);
                }
                
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static boolean isValidPattern(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // 检查是否是AE2的编码样板
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return false;
        }
        
        // 简单检查：样板物品通常包含NBT数据
        return stack.hasTag();
    }
}
