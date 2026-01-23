package com.gali.ae2_auto_pattern_upload.wireless;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.me.GridConnection;

/**
 * 标签无线收发器的连接器 - 1.7.10 版本
 * 将 TileEntity 的 in-world 节点连接到标签网络的虚拟节点
 */
public class LabelLink {

    private final IWirelessEndpoint host;
    private IGridConnection connection = null;
    private LabelNetworkRegistry.LabelNetwork target = null;

    public LabelLink(IWirelessEndpoint host) {
        this.host = host;
    }

    public void setTarget(LabelNetworkRegistry.LabelNetwork target) {
        this.target = target;
        updateStatus();
    }

    public void clearTarget() {
        setTarget(null);
    }

    public boolean isConnected() {
        return connection != null;
    }

    /**
     * 在 serverTick 或标签变化时调用
     */
    public void updateStatus() {
        if (host.isEndpointRemoved()) {
            destroyConnection();
            return;
        }
        if (target == null) {
            destroyConnection();
            return;
        }

        World hostWorld = host.getServerWorld();
        if (hostWorld == null || hostWorld.isRemote) {
            destroyConnection();
            return;
        }

        // 维度校验（如果不支持跨维度）
        int targetDim = target.getDimensionId();
        if (!LabelNetworkRegistry.isCrossDimEnabled() && targetDim != Integer.MIN_VALUE
            && targetDim != hostWorld.provider.dimensionId) {
            destroyConnection();
            return;
        }

        IGridNode hostNode = host.getGridNode();
        IGridNode targetNode = target.getNode();
        if (hostNode == null || targetNode == null) {
            destroyConnection();
            return;
        }

        try {
            if (connection != null) {
                IGridNode a = connection.a();
                IGridNode b = connection.b();
                if ((a == hostNode || b == hostNode) && (a == targetNode || b == targetNode)) {
                    return; // 已经正确连接
                }
                connection.destroy();
                connection = null;
            }
            // 创建新连接
            connection = new GridConnection(hostNode, targetNode, ForgeDirection.UNKNOWN);
        } catch (Exception ignore) {
            destroyConnection();
        }
    }

    public void onUnloadOrRemove() {
        destroyConnection();
    }

    private void destroyConnection() {
        if (connection != null) {
            try {
                connection.destroy();
            } catch (Exception ignored) {}
            connection = null;
        }
    }
}
