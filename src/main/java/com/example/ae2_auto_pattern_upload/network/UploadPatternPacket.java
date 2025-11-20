package com.example.ae2_auto_pattern_upload.network;

import appeng.api.AEApi;
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
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * C2S: 上传编码样板到指定供应器
 */
public class UploadPatternPacket implements IMessage {
    
    private long providerId;
    
    public UploadPatternPacket() {
    }
    
    public UploadPatternPacket(long providerId) {
        this.providerId = providerId;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        this.providerId = buf.readLong();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.providerId);
    }
    
    public static class Handler implements IMessageHandler<UploadPatternPacket, IMessage> {
        @Override
        public IMessage onMessage(UploadPatternPacket message, MessageContext ctx) {
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
                // 获取已编码槽位的样板
                // 对于普通终端，通过 getPart() 获取；对于无线终端，通过 iGuiItemObject 获取
                net.minecraftforge.items.IItemHandler patternInventory = null;
                
                // 尝试通过 getPart() 获取（普通样板终端）
                try {
                    java.lang.reflect.Method getPartMethod = container.getClass().getMethod("getPart");
                    Object part = getPartMethod.invoke(container);
                    if (part != null) {
                        java.lang.reflect.Method getInventoryByNameMethod = part.getClass().getMethod("getInventoryByName", String.class);
                        Object inv = getInventoryByNameMethod.invoke(part, "pattern");
                        if (inv instanceof net.minecraftforge.items.IItemHandler) {
                            patternInventory = (net.minecraftforge.items.IItemHandler) inv;
                        }
                    }
                } catch (Exception e) {
                    // null
                }
                
                // 如果 getPart() 方式失败，尝试通过无线终端字段获取（无线样板终端）
                if (patternInventory == null) {
                    try {
                        // 对于无线终端，pattern 是容器自己的字段
                        java.lang.reflect.Field patternField = container.getClass().getDeclaredField("pattern");
                        patternField.setAccessible(true);
                        Object patternObj = patternField.get(container);
                        if (patternObj instanceof net.minecraftforge.items.IItemHandler) {
                            patternInventory = (net.minecraftforge.items.IItemHandler) patternObj;
                        }
                    } catch (NoSuchFieldException e) {
                        // 如果找不到 pattern 字段，尝试通过无线终端对象的 getInventoryByName 获取
                        try {
                            java.lang.reflect.Field wirelessTerminalField = null;
                            try {
                                wirelessTerminalField = container.getClass().getDeclaredField("wirelessTerminalGUIObject");
                            } catch (NoSuchFieldException e2) {
                                try {
                                    wirelessTerminalField = container.getClass().getSuperclass().getDeclaredField("iGuiItemObject");
                                } catch (NoSuchFieldException e3) {
                                    wirelessTerminalField = container.getClass().getDeclaredField("iGuiItemObject");
                                }
                            }
                            
                            if (wirelessTerminalField != null) {
                                wirelessTerminalField.setAccessible(true);
                                Object wirelessTerminal = wirelessTerminalField.get(container);
                                if (wirelessTerminal != null) {
                                    java.lang.reflect.Method getInventoryByNameMethod = wirelessTerminal.getClass().getMethod("getInventoryByName", String.class);
                                    Object inv = getInventoryByNameMethod.invoke(wirelessTerminal, "pattern");
                                    if (inv instanceof net.minecraftforge.items.IItemHandler) {
                                        patternInventory = (net.minecraftforge.items.IItemHandler) inv;
                                    }
                                }
                            }
                        } catch (Exception e2) {
                            // 忽略异常，继续执行
                        }
                    } catch (Exception e) {
                        // 忽略异常，继续执行
                    }
                }
                
                if (patternInventory == null) {
                    return null;
                }
                
                // 获取输出槽位的样板（已编码的样板）
                ItemStack encodedPattern = patternInventory.getStackInSlot(1);
                if (encodedPattern == null || encodedPattern.isEmpty()) {
                    return null;
                }
                
                // 检查是否是有效的编码样板
                if (!AEApi.instance().definitions().items().encodedPattern().isSameAs(encodedPattern)) {
                    return null;
                }
                
                // 获取网络节点和网络
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
                    // 如果 getPart() 返回 null 或方法不存在，继续尝试无线终端方式
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
                        // 如果无线终端方式也失败，返回 null
                    }
                }
                
                if (node == null) return null;
                
                IGrid grid = node.getGrid();
                if (grid == null) return null;
                
                // 查找目标供应器
                ICraftingProvider targetProvider = null;
                for (Class<? extends IGridHost> hostClass : grid.getMachinesClasses()) {
                    if (!ICraftingProvider.class.isAssignableFrom(hostClass)) continue;
                    
                    for (IGridNode machineNode : grid.getMachines(hostClass)) {
                        if (machineNode == null) continue;
                        
                        Object machine = machineNode.getMachine();
                        if (!(machine instanceof ICraftingProvider)) continue;
                        
                        // 比较 hashCode 来找到目标供应器
                        if ((long) machineNode.hashCode() == message.providerId) {
                            targetProvider = (ICraftingProvider) machine;
                            break;
                        }
                    }
                    
                    if (targetProvider != null) break;
                }
                
                if (targetProvider == null) {
                    return null;
                }
                
                // 尝试将样板插入到供应器
                boolean uploadSuccess = false;
                
                // 首先尝试通过 getInventoryByName("patterns") 获取样板库存
                try {
                    // 尝试调用 getInventoryByName("patterns") 方法
                    java.lang.reflect.Method getInventoryByNameMethod = null;
                    
                    Class<?> clazz = targetProvider.getClass();
                    while (clazz != null && getInventoryByNameMethod == null) {
                        try {
                            getInventoryByNameMethod = clazz.getDeclaredMethod("getInventoryByName", String.class);
                            getInventoryByNameMethod.setAccessible(true);
                            break;
                        } catch (NoSuchMethodException e) {
                            clazz = clazz.getSuperclass();
                        }
                    }
                    
                    if (getInventoryByNameMethod != null) {
                        Object patternsObj = getInventoryByNameMethod.invoke(targetProvider, "patterns");
                        
                        if (patternsObj instanceof net.minecraftforge.items.IItemHandler) {
                            net.minecraftforge.items.IItemHandler patternsInv = 
                                (net.minecraftforge.items.IItemHandler) patternsObj;
                            
                            // 尝试将样板插入到供应器的第一个空槽位
                            for (int i = 0; i < patternsInv.getSlots(); i++) {
                                ItemStack slot = patternsInv.getStackInSlot(i);
                                if (slot == null || slot.isEmpty()) {
                                    // 复制样板并插入
                                    ItemStack copy = encodedPattern.copy();
                                    copy.setCount(1);
                                    
                                    // 使用 insertItem 方法插入样板
                                    ItemStack remainder = patternsInv.insertItem(i, copy, false);
                                    
                                    if (remainder == null || remainder.isEmpty()) {
                                        uploadSuccess = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略异常，继续尝试其他方法
                }
                
                // 备选方案：尝试 IItemHandler 接口（Forge 的现代库存系统）
                if (!uploadSuccess && targetProvider instanceof net.minecraft.tileentity.TileEntity) {
                    net.minecraft.tileentity.TileEntity te = (net.minecraft.tileentity.TileEntity) targetProvider;
                    
                    if (te instanceof net.minecraftforge.items.IItemHandlerModifiable) {
                        net.minecraftforge.items.IItemHandlerModifiable providerInv = 
                            (net.minecraftforge.items.IItemHandlerModifiable) te;
                        
                        for (int i = 0; i < providerInv.getSlots(); i++) {
                            ItemStack slot = providerInv.getStackInSlot(i);
                            if (slot == null || slot.isEmpty()) {
                                ItemStack copy = encodedPattern.copy();
                                copy.setCount(1);
                                providerInv.setStackInSlot(i, copy);
                                uploadSuccess = true;
                                break;
                            }
                        }
                    }
                }
                
                if (uploadSuccess) {
                    // 清空编码终端的输出槽位
                    patternInventory.extractItem(1, 1, false);
                    return null;
                }
                
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
            
            return null;
        }
    }
}

