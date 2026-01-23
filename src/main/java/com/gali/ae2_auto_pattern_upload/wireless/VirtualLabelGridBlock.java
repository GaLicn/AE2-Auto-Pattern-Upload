package com.gali.ae2_auto_pattern_upload.wireless;

import java.util.EnumSet;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridNotification;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;

/**
 * 虚拟标签网络节点的 GridBlock 实现
 * 用于创建不在世界中的虚拟AE2节点
 */
public class VirtualLabelGridBlock implements IGridBlock, IGridHost {

    private final World world;
    private final String label;
    private IGridNode node;

    public VirtualLabelGridBlock(World world, String label) {
        this.world = world;
        this.label = label;
    }

    public void setNode(IGridNode node) {
        this.node = node;
    }

    @Override
    public double getIdlePowerUsage() {
        return 0.0; // 虚拟节点不消耗能量
    }

    @Override
    public EnumSet<GridFlags> getFlags() {
        return EnumSet.of(GridFlags.DENSE_CAPACITY);
    }

    @Override
    public boolean isWorldAccessible() {
        return false; // 虚拟节点不在世界中
    }

    @Override
    public DimensionalCoord getLocation() {
        // 返回一个虚拟位置
        return new DimensionalCoord(world, 0, -1, 0);
    }

    @Override
    public AEColor getGridColor() {
        return AEColor.Transparent;
    }

    @Override
    public void onGridNotification(GridNotification notification) {
        // 不处理
    }

    @Override
    public void setNetworkStatus(IGrid grid, int channelsInUse) {
        // 不处理
    }

    @Override
    public EnumSet<ForgeDirection> getConnectableSides() {
        return EnumSet.noneOf(ForgeDirection.class);
    }

    @Override
    public IGridHost getMachine() {
        return this;
    }

    @Override
    public void gridChanged() {
        // 不处理
    }

    @Override
    public ItemStack getMachineRepresentation() {
        return null;
    }

    // ==================== IGridHost ====================

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return node;
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.NONE;
    }

    @Override
    public void securityBreak() {
        // 不处理
    }
}
