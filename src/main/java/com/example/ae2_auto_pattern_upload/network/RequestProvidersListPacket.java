package com.example.ae2_auto_pattern_upload.network;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.container.implementations.ContainerPatternEncoder;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * C2S: 请求可用供应器列表
 * 当玩家点击编码终端上的上传按钮时发送此包
 */
public class RequestProvidersListPacket implements IMessage {
    
    @Override
    public void fromBytes(ByteBuf buf) {
        // 无数据
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        // 无数据
    }
    
    public static class Handler implements IMessageHandler<RequestProvidersListPacket, IMessage> {
        @Override
        public IMessage onMessage(RequestProvidersListPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }
            
            // 检查玩家是否打开编码终端
            if (!(player.openContainer instanceof ContainerPatternEncoder)) {
                return null;
            }
            
            ContainerPatternEncoder container = (ContainerPatternEncoder) player.openContainer;
            
            try {
                // 获取网络中的供应器列表
                java.util.List<Long> ids = new java.util.ArrayList<>();
                java.util.List<String> names = new java.util.ArrayList<>();
                java.util.List<Integer> slots = new java.util.ArrayList<>();
                
                // 获取网络节点
                // 对于普通终端，通过 getPart() 获取；对于无线终端，通过 iGuiItemObject 获取
                IGridNode node = null;
                
                // 尝试通过 getPart() 获取（普通样板终端）
                try {
                    java.lang.reflect.Method getPartMethod = container.getClass().getMethod("getPart");
                    Object part = getPartMethod.invoke(container);
                    if (part != null) {
                        java.lang.reflect.Method getGridNodeMethod = part.getClass().getMethod("getGridNode", 
                            appeng.api.util.AEPartLocation.class);
                        node = (IGridNode) getGridNodeMethod.invoke(part, 
                            appeng.api.util.AEPartLocation.INTERNAL);
                    }
                } catch (Exception e) {
                    //null
                }
                
                // 如果 getPart() 方式失败，尝试通过无线终端字段获取（无线样板终端）
                if (node == null) {
                    try {
                        // 首先尝试无线终端专用字段 wirelessTerminalGUIObject
                        java.lang.reflect.Field wirelessTerminalField = null;
                        try {
                            wirelessTerminalField = container.getClass().getDeclaredField("wirelessTerminalGUIObject");
                        } catch (NoSuchFieldException e) {
                            // 如果找不到 wirelessTerminalGUIObject，尝试父类的 iGuiItemObject
                            try {
                                wirelessTerminalField = container.getClass().getSuperclass().getDeclaredField("iGuiItemObject");
                            } catch (NoSuchFieldException e2) {
                                // 继续尝试直接在当前类查找 iGuiItemObject
                                wirelessTerminalField = container.getClass().getDeclaredField("iGuiItemObject");
                            }
                        }
                        
                        if (wirelessTerminalField != null) {
                            wirelessTerminalField.setAccessible(true);
                            Object wirelessTerminal = wirelessTerminalField.get(container);
                            
                            if (wirelessTerminal != null && wirelessTerminal instanceof appeng.api.networking.security.IActionHost) {
                                appeng.api.networking.security.IActionHost actionHost = (appeng.api.networking.security.IActionHost) wirelessTerminal;
                                node = actionHost.getActionableNode();
                            }
                        }
                    } catch (Exception e) {
                        // 忽略异常，继续执行
                    }
                }
                
                if (node == null) {
                    return null;
                }
                
                IGrid grid = node.getGrid();
                if (grid == null) {
                    return null;
                }
                
                // 遍历网络中的所有供应器（ICraftingProvider）
                for (Class<? extends IGridHost> hostClass : grid.getMachinesClasses()) {
                    if (!ICraftingProvider.class.isAssignableFrom(hostClass)) continue;
                    
                    for (IGridNode machineNode : grid.getMachines(hostClass)) {
                        if (machineNode == null) continue;
                        
                        Object machine = machineNode.getMachine();
                        if (!(machine instanceof ICraftingProvider)) continue;
                        
                        ICraftingProvider provider = (ICraftingProvider) machine;
                        
                        // 获取供应器 ID（使用 hashCode 作为唯一标识）和名称
                        long providerId = (long) machineNode.hashCode();
                        String providerName = "Crafting Provider";  // 默认名称
                        
                        // 尝试获取更好的名称
                        if (machine instanceof net.minecraft.tileentity.TileEntity) {
                            net.minecraft.tileentity.TileEntity te = (net.minecraft.tileentity.TileEntity) machine;
                            if (te.getDisplayName() != null) {
                                providerName = te.getDisplayName().getUnformattedText();
                            }
                        }
                        
                        ids.add(providerId);
                        names.add(providerName);
                        slots.add(1);  // 暂时设为 1，后续可以改进
                    }
                }
                
                // 发送响应包到客户端
                ProvidersListS2CPacket response = new ProvidersListS2CPacket(ids, names, slots);
                ModNetwork.CHANNEL.sendTo(response, player);
                
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
            
            return null;
        }
    }
}
