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
            if (player == null) return null;
            
            // 检查玩家是否打开编码终端
            if (!(player.openContainer instanceof ContainerPatternEncoder)) {
                return null;
            }
            
            ContainerPatternEncoder container = (ContainerPatternEncoder) player.openContainer;
            
            try {
                // 获取已编码槽位的样板
                net.minecraftforge.items.IItemHandler patternInventory = container.getPart().getInventoryByName("pattern");
                if (patternInventory == null) return null;
                
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
                IGridNode node = container.getPart().getGridNode();
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
                    System.err.println("[上传样板] 找不到目标供应器");
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
                            
                            System.out.println("[上传样板] 找到样板库存，共 " + patternsInv.getSlots() + " 个槽位");
                            
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
                                        System.out.println("[上传样板] 样板已通过 getInventoryByName() 上传到供应器槽位 " + i);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[上传样板] getInventoryByName() 方法调用失败: " + e.getMessage());
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
                                System.out.println("[上传样板] 样板已通过 IItemHandlerModifiable 上传到供应器");
                                break;
                            }
                        }
                    }
                }
                
                if (uploadSuccess) {
                    // 清空编码终端的输出槽位
                    patternInventory.extractItem(1, 1, false);
                    System.out.println("[上传样板] 样板上传成功，编码终端已清空");
                    return null;
                }
                
                System.err.println("[上传样板] 无法上传样板：找不到合适的库存接口或库存已满");
                
            } catch (Throwable t) {
                System.err.println("[上传样板] 上传失败: " + t.getMessage());
                t.printStackTrace();
                return null;
            }
            
            return null;
        }
    }
}
