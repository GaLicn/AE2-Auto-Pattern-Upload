package com.gali.ae2_auto_pattern_upload.wireless;

import net.minecraft.world.World;

import appeng.api.networking.IGridNode;

/**
 * 无线端点接口 - 1.7.10 版本
 * 无线收发器方块实体需实现该接口，以便无线逻辑能够获取世界、位置与 AE2 节点。
 */
public interface IWirelessEndpoint {

    /** 返回服务端世界 */
    World getServerWorld();

    /** 返回方块坐标 X */
    int getXCoord();

    /** 返回方块坐标 Y */
    int getYCoord();

    /** 返回方块坐标 Z */
    int getZCoord();

    /** 返回 AE2 网格节点 */
    IGridNode getGridNode();

    /** 是否已移除/销毁 */
    boolean isEndpointRemoved();
}
