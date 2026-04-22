package com.example.ae2_auto_pattern_upload.network;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.helpers.IInterfaceHost;
import appeng.parts.misc.PartInterface;
import appeng.tile.misc.TileInterface;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.items.IItemHandler;

import java.util.HashSet;
import java.util.Set;

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
            com.example.ae2_auto_pattern_upload.ExampleMod.LOGGER.info("[APU] Received RequestProvidersListPacket from client");
            
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                com.example.ae2_auto_pattern_upload.ExampleMod.LOGGER.warn("[APU] Player is null in RequestProvidersListPacket handler");
                return null;
            }

            com.example.ae2_auto_pattern_upload.ExampleMod.LOGGER.info("[APU] Processing request from player: {}", player.getName());

            if (player.openContainer == null) {
                com.example.ae2_auto_pattern_upload.ExampleMod.LOGGER.warn("[APU] Player {} has no open container", player.getName());
                return null;
            }

            Object container = player.openContainer;
            com.example.ae2_auto_pattern_upload.ExampleMod.LOGGER.info("[APU] Container type: {}", container.getClass().getName());
            
            try {
                // 获取网络中的供应器列表
                java.util.List<Long> ids = new java.util.ArrayList<>();
                java.util.List<String> names = new java.util.ArrayList<>();
                java.util.List<Integer> slots = new java.util.ArrayList<>();
                
                // 获取网络节点
                // 对于普通终端，通过 getPart() 获取；对于无线终端，通过 iGuiItemObject 获取
                IGridNode node = null;
                
                // 尝试通过 getPart() 获取（普通样板终端 / 流体样板终端等基于 Part 的实现）
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
                    // getPart 不存在或失败，继续尝试无线终端方式
                }
                
                // 如果 getPart() 方式失败，尝试通过无线终端字段获取（无线样板终端及其子类，比如无线流体样板终端）
                if (node == null) {
                    try {
                        // 在类继承链中查找无线终端相关字段：优先 wirelessTerminalGUIObject，其次 iGuiItemObject
                        Class<?> cls = container.getClass();
                        java.lang.reflect.Field wirelessTerminalField = null;
                        while (cls != null && wirelessTerminalField == null) {
                            try {
                                wirelessTerminalField = cls.getDeclaredField("wirelessTerminalGUIObject");
                            } catch (NoSuchFieldException e) {
                                try {
                                    wirelessTerminalField = cls.getDeclaredField("iGuiItemObject");
                                } catch (NoSuchFieldException ignoredInner) {
                                    cls = cls.getSuperclass();
                                }
                            }
                        }

                        if (wirelessTerminalField != null) {
                            wirelessTerminalField.setAccessible(true);
                            Object wirelessTerminal = wirelessTerminalField.get(container);

                            if (wirelessTerminal != null && wirelessTerminal instanceof appeng.api.networking.security.IActionHost) {
                                appeng.api.networking.security.IActionHost actionHost =
                                        (appeng.api.networking.security.IActionHost) wirelessTerminal;
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
                
                Set<Long> seenProviderIds = new HashSet<>();

                // 先按 AE2 原生接口终端的方式，显式收集 block / part 形态的接口。
                // 这样可以稳定拿到 part 接口，并使用 duality 中的终端名与真实样板空槽数。
                collectInterfaceHosts(grid.getMachines(TileInterface.class), ids, names, slots, seenProviderIds);
                collectInterfaceHosts(grid.getMachines(PartInterface.class), ids, names, slots, seenProviderIds);

                // 再回退遍历网络中的其他供应器（ICraftingProvider）
                for (Class<? extends IGridHost> hostClass : grid.getMachinesClasses()) {
                    if (!ICraftingProvider.class.isAssignableFrom(hostClass)) continue;
                    
                    for (IGridNode machineNode : grid.getMachines(hostClass)) {
                        if (machineNode == null) continue;
                        
                        Object machine = machineNode.getMachine();
                        if (!(machine instanceof ICraftingProvider)) continue;
                        
                        long providerId = (long) machineNode.hashCode();
                        if (!seenProviderIds.add(providerId)) {
                            continue;
                        }

                        String providerName = "Crafting Provider";  // 默认名称
                        
                        // 尝试获取更好的名称
                        if (machine instanceof net.minecraft.tileentity.TileEntity) {
                            net.minecraft.tileentity.TileEntity te = (net.minecraft.tileentity.TileEntity) machine;
                            if (te.getDisplayName() != null) {
                                providerName = te.getDisplayName().getUnformattedText();
                            }
                        }
                        
                        ids.add(providerId);
                        names.add(ProvidersListS2CPacket.normalizeProviderName(providerName));
                        slots.add(getEmptyPatternSlots(machine));
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

        private static void collectInterfaceHosts(Iterable<IGridNode> machineNodes,
                                                  java.util.List<Long> ids,
                                                  java.util.List<String> names,
                                                  java.util.List<Integer> slots,
                                                  Set<Long> seenProviderIds) {
            if (machineNodes == null) {
                return;
            }

            for (IGridNode machineNode : machineNodes) {
                if (machineNode == null || !machineNode.isActive()) {
                    continue;
                }

                Object machine = machineNode.getMachine();
                if (!(machine instanceof IInterfaceHost)) {
                    continue;
                }

                IInterfaceHost interfaceHost = (IInterfaceHost) machine;
                long providerId = (long) machineNode.hashCode();
                if (!seenProviderIds.add(providerId)) {
                    continue;
                }

                String providerName = interfaceHost.getInterfaceDuality().getTermName();
                ids.add(providerId);
                names.add(ProvidersListS2CPacket.normalizeProviderName(providerName));
                slots.add(getEmptyPatternSlots(interfaceHost));
            }
        }

        private static int getEmptyPatternSlots(Object provider) {
            try {
                java.lang.reflect.Method getInventoryByNameMethod = provider.getClass().getMethod("getInventoryByName", String.class);
                Object patternsObj = getInventoryByNameMethod.invoke(provider, "patterns");
                if (patternsObj instanceof IItemHandler) {
                    IItemHandler patterns = (IItemHandler) patternsObj;
                    int emptySlots = 0;
                    for (int i = 0; i < patterns.getSlots(); i++) {
                        if (patterns.getStackInSlot(i).isEmpty()) {
                            emptySlots++;
                        }
                    }
                    return emptySlots;
                }
            } catch (Exception ignored) {
            }

            return 0;
        }
    }
}
