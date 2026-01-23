package com.gali.network;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.menu.me.items.PatternEncodingTermMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * C2S: 请求可用供应器列表
 */
public class RequestProvidersListPacket {
    
    public RequestProvidersListPacket() {
    }
    
    public RequestProvidersListPacket(FriendlyByteBuf buf) {
        // 无数据需要读取
    }
    
    public void encode(FriendlyByteBuf buf) {
        // 无数据需要写入
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof PatternEncodingTermMenu menu)) {
                return;
            }
            
            try {
                List<Long> ids = new ArrayList<>();
                List<String> names = new ArrayList<>();
                List<Integer> emptySlots = new ArrayList<>();
                
                // 获取网络节点
                IGridNode node = menu.getNetworkNode();
                if (node == null) return;
                
                IGrid grid = node.getGrid();
                if (grid == null) return;
                
                // 遍历网络中的所有样板容器
                for (var machineClass : grid.getMachineClasses()) {
                    if (PatternContainer.class.isAssignableFrom(machineClass)) {
                        @SuppressWarnings("unchecked")
                        Class<? extends PatternContainer> containerClass = (Class<? extends PatternContainer>) machineClass;
                        
                        for (var container : grid.getActiveMachines(containerClass)) {
                            if (container == null || !container.isVisibleInTerminal()) continue;
                            
                            var inv = container.getTerminalPatternInventory();
                            if (inv == null || inv.size() <= 0) continue;
                            
                            // 计算空槽位数量
                            int empty = 0;
                            for (int i = 0; i < inv.size(); i++) {
                                if (inv.getStackInSlot(i).isEmpty()) {
                                    empty++;
                                }
                            }
                            
                            if (empty > 0) {
                                // 使用容器的hashCode作为ID
                                long id = (long) container.hashCode();
                                
                                // 获取显示名称（优先使用组名）
                                String name = "样板供应器";
                                try {
                                    var group = container.getTerminalGroup();
                                    if (group != null) {
                                        name = Component.Serializer.toJson(group.name());
                                    }
                                } catch (Exception ignored) {
                                }
                                
                                ids.add(id);
                                names.add(name);
                                emptySlots.add(empty);
                            }
                        }
                    }
                }
                
                // 发送响应包到客户端
                ProvidersListS2CPacket response = new ProvidersListS2CPacket(ids, names, emptySlots);
                ModNetwork.CHANNEL.sendTo(response, player.connection.connection, 
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
