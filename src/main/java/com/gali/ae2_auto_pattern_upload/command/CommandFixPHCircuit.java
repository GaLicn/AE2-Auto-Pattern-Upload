package com.gali.ae2_auto_pattern_upload.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.gali.ae2_auto_pattern_upload.util.AEUtil;
import com.gali.ae2_auto_pattern_upload.util.CircuitUtils;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.util.IInterfaceViewable;
import appeng.helpers.IInterfaceHost;
import appeng.items.misc.ItemEncodedPattern;

public class CommandFixPHCircuit extends CommandBase {

    @Override
    public String getCommandName() {
        return "fixphcircuit";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fixphcircuit [debug]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP player)) {
            throw new CommandException("只有玩家可以使用此命令");
        }

        boolean debug = args.length > 0 && args[0].equalsIgnoreCase("debug");

        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null) {
            throw new CommandException("请手持无线终端");
        }

        if (!AEUtil.isWirelessTerminal(heldItem)) {
            throw new CommandException("请手持有效的无线终端");
        }

        IGrid grid = AEUtil.getGridFromWirelessTerminal(player);
        if (grid == null) {
            throw new CommandException("无法获取AE网络，请确保无线终端已链接且在网络范围内");
        }

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "开始处理网络中的样板..."));
        if (debug) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[调试模式已开启]"));
        }

        int totalPatterns = 0;
        int fixedPatterns = 0;
        int totalCircuits = 0;

        for (Class<? extends IGridHost> hostClass : grid.getMachinesClasses()) {
            if (!ICraftingProvider.class.isAssignableFrom(hostClass)) {
                continue;
            }

            IMachineSet machines = grid.getMachines(hostClass);
            if (machines == null) {
                continue;
            }

            for (IGridNode machineNode : machines) {
                if (machineNode == null) {
                    continue;
                }

                Object machine = machineNode.getMachine();
                if (!(machine instanceof ICraftingProvider provider)) {
                    continue;
                }

                List<PatternInfo> patterns = getPatternsFromProvider(provider);

                for (PatternInfo patternInfo : patterns) {
                    if (patternInfo == null || patternInfo.stack == null || patternInfo.stack.stackSize <= 0) {
                        continue;
                    }

                    totalPatterns++;

                    if (!isEncodedPattern(patternInfo.stack)) {
                        continue;
                    }

                    int fixed = fixPatternCircuit(patternInfo, sender, debug);
                    if (fixed > 0) {
                        fixedPatterns++;
                        totalCircuits += fixed;
                    }
                }
            }
        }

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "处理完成！"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "总样板数: " + totalPatterns));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "修改的样板数: " + fixedPatterns));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "修改的虚拟电路数: " + totalCircuits));
    }

    private static class PatternInfo {

        ItemStack stack;
        IInventory inventory;
        int slot;

        PatternInfo(ItemStack stack, IInventory inventory, int slot) {
            this.stack = stack;
            this.inventory = inventory;
            this.slot = slot;
        }
    }

    private List<PatternInfo> getPatternsFromProvider(ICraftingProvider provider) {
        List<PatternInfo> patterns = new ArrayList<>();

        if (provider instanceof IInterfaceHost host) {
            IInventory patternInventory = host.getPatterns();
            if (patternInventory != null) {
                int availableSlots = host.rows() * host.rowSize();
                int limit = Math.min(availableSlots, patternInventory.getSizeInventory());
                for (int i = 0; i < limit; i++) {
                    ItemStack stack = patternInventory.getStackInSlot(i);
                    if (stack != null && stack.stackSize > 0) {
                        patterns.add(new PatternInfo(stack, patternInventory, i));
                    }
                }
            }
            return patterns;
        }

        if (provider instanceof IInterfaceViewable viewable) {
            IInventory patternInventory = viewable.getPatterns();
            if (patternInventory != null) {
                int availableSlots = viewable.rows() * viewable.rowSize();
                int limit = Math.min(availableSlots, patternInventory.getSizeInventory());
                for (int i = 0; i < limit; i++) {
                    ItemStack stack = patternInventory.getStackInSlot(i);
                    if (stack != null && stack.stackSize > 0) {
                        patterns.add(new PatternInfo(stack, patternInventory, i));
                    }
                }
            }
            return patterns;
        }

        if (provider instanceof IInventory inv) {
            addPatternsFromInventory(inv, patterns);
        }

        return patterns;
    }

    private void addPatternsFromInventory(IInventory inv, List<PatternInfo> patterns) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.stackSize > 0 && isEncodedPattern(stack)) {
                patterns.add(new PatternInfo(stack, inv, i));
            }
        }
    }

    private boolean isEncodedPattern(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (stack.getItem() instanceof ItemEncodedPattern) {
            return true;
        }
        String itemClassName = stack.getItem()
            .getClass()
            .getName();
        if (itemClassName.contains("ItemFluidEncodedPattern") || itemClassName.contains("FluidEncodedPattern")) {
            return true;
        }
        return false;
    }

    private int fixPatternCircuit(PatternInfo patternInfo, ICommandSender sender, boolean debug) {
        int fixedCount = 0;

        ItemStack patternStack = patternInfo.stack;
        NBTTagCompound tag = patternStack.getTagCompound();
        if (tag == null) {
            return 0;
        }

        boolean modified = false;

        // 处理输入物品
        if (tag.hasKey("in", 9)) {
            NBTTagList inList = tag.getTagList("in", 10);
            for (int i = 0; i < inList.tagCount(); i++) {
                NBTTagCompound itemTag = inList.getCompoundTagAt(i);
                if (fixItemStackTag(itemTag, sender, debug, "in[" + i + "]")) {
                    fixedCount++;
                    modified = true;
                }
            }
        }

        // 处理输出物品
        if (tag.hasKey("out", 9)) {
            NBTTagList outList = tag.getTagList("out", 10);
            for (int i = 0; i < outList.tagCount(); i++) {
                NBTTagCompound itemTag = outList.getCompoundTagAt(i);
                if (fixItemStackTag(itemTag, sender, debug, "out[" + i + "]")) {
                    fixedCount++;
                    modified = true;
                }
            }
        }

        // 如果修改了，更新物品栏
        if (modified && patternInfo.inventory != null) {
            patternInfo.inventory.setInventorySlotContents(patternInfo.slot, patternStack);
            patternInfo.inventory.markDirty();
        }

        return fixedCount;
    }

    private boolean fixItemStackTag(NBTTagCompound itemTag, ICommandSender sender, boolean debug, String location) {
        if (itemTag == null || itemTag.hasNoTags()) {
            return false;
        }

        String itemId = itemTag.getString("id");
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }

        // 获取数量 - AE2FC 使用 "Cnt" 字段存储数量，"Count" 字段通常为0
        long count = getItemCount(itemTag);

        // 调试输出
        if (debug) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GRAY + "[调试] 位置:" + location + " ID:" + itemId + " 数量:" + count));
        }

        // 检查是否是编程电路
        if (!CircuitUtils.isProgrammingCircuitFromNBT(itemTag)) {
            return false;
        }

        if (debug) {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.YELLOW + "[调试] 发现编程电路! 位置:" + location + " 当前数量:" + count));
        }

        if (count <= 1) {
            return false;
        }

        // 设置数量为1
        // 对于 AE2FC 样板，需要设置 "Cnt" 字段
        if (itemTag.hasKey("Cnt", 4)) { // 4 = long
            itemTag.setLong("Cnt", 1L);
        } else {
            itemTag.setByte("Count", (byte) 1);
        }

        if (debug) {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.GREEN + "[调试] 已修改! 位置:" + location + " 新数量:1"));
        }

        return true;
    }

    /**
     * 获取物品数量
     * AE2FC 使用 "Cnt" 字段 (long)，普通物品使用 "Count" 字段 (byte)
     */
    private long getItemCount(NBTTagCompound itemTag) {
        // 优先检查 AE2FC 的 "Cnt" 字段
        if (itemTag.hasKey("Cnt", 4)) { // 4 = long
            return itemTag.getLong("Cnt");
        }
        // 检查普通 "Count" 字段
        if (itemTag.hasKey("Count", 1)) { // 1 = byte
            return itemTag.getByte("Count");
        }
        if (itemTag.hasKey("Count", 3)) { // 3 = int
            return itemTag.getInteger("Count");
        }
        return 0;
    }
}
