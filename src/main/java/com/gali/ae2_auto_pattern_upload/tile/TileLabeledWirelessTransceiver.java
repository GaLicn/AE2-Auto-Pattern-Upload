package com.gali.ae2_auto_pattern_upload.tile;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gali.ae2_auto_pattern_upload.Config;
import com.gali.ae2_auto_pattern_upload.wireless.IWirelessEndpoint;
import com.gali.ae2_auto_pattern_upload.wireless.LabelLink;
import com.gali.ae2_auto_pattern_upload.wireless.LabelNetworkRegistry;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.util.AECableType;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkTile;

/**
 * 标签无线收发器 TileEntity
 */
public class TileLabeledWirelessTransceiver extends AENetworkTile implements IWirelessEndpoint {

    private long frequency = 0L;
    private String labelForDisplay = null;
    private boolean beingRemoved = false;
    private UUID placerId = null;
    private String placerName = null;

    private final LabelLink labelLink = new LabelLink(this);

    public TileLabeledWirelessTransceiver() {
        super();
    }

    @Override
    protected AENetworkProxy createProxy() {
        AENetworkProxy proxy = new AENetworkProxy(this, "proxy", getItemFromTile(this), true);
        proxy.setFlags(GridFlags.DENSE_CAPACITY);
        proxy.setIdlePowerUsage(Config.wirelessTransceiverIdlePower);
        proxy.setValidSides(EnumSet.allOf(ForgeDirection.class));
        return proxy;
    }

    @Override
    public void validate() {
        super.validate();
        if (worldObj != null && !worldObj.isRemote) {
            getProxy().onReady();
        }
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.SMART;
    }

    // ==================== IWirelessEndpoint ====================

    @Override
    public World getServerWorld() {
        return this.worldObj;
    }

    @Override
    public int getXCoord() {
        return this.xCoord;
    }

    @Override
    public int getYCoord() {
        return this.yCoord;
    }

    @Override
    public int getZCoord() {
        return this.zCoord;
    }

    @Override
    public IGridNode getGridNode() {
        return getProxy().getNode();
    }

    @Override
    public boolean isEndpointRemoved() {
        return this.isInvalid();
    }

    // ==================== 公共方法 ====================

    public void setPlacerId(UUID placerId, String placerName) {
        this.placerId = placerId;
        this.placerName = placerName;
        markDirty();
        markForUpdate();
    }

    public UUID getPlacerId() {
        return placerId;
    }

    public String getPlacerName() {
        return placerName;
    }

    public long getFrequency() {
        return frequency;
    }

    public String getLabelForDisplay() {
        return labelForDisplay;
    }

    /**
     * 应用标签
     */
    public void applyLabel(String rawLabel) {
        if (worldObj == null || worldObj.isRemote) return;

        String normalized = LabelNetworkRegistry.normalizeLabel(rawLabel);

        // 先注销旧网络引用
        LabelNetworkRegistry registry = LabelNetworkRegistry.get(worldObj);
        if (registry != null) {
            registry.unregister(this);
        }

        LabelNetworkRegistry.LabelNetwork network = registry != null
            ? registry.register(worldObj, normalized, placerId, this)
            : null;
        if (network == null) {
            clearLabel();
            return;
        }

        this.labelForDisplay = normalized;
        this.frequency = network.getChannel();
        this.labelLink.setTarget(network);
        updateBlockState();
        markDirty();
        markForUpdate();
    }

    /**
     * 清除标签
     */
    public void clearLabel() {
        if (worldObj != null && !worldObj.isRemote) {
            LabelNetworkRegistry registry = LabelNetworkRegistry.get(worldObj);
            if (registry != null) {
                registry.unregister(this);
            }
        }
        this.labelForDisplay = null;
        this.frequency = 0L;
        this.labelLink.clearTarget();
        updateBlockState();
        markDirty();
        markForUpdate();
    }

    /**
     * 刷新标签
     */
    public void refreshLabel(boolean ensureRegister) {
        if (worldObj == null || worldObj.isRemote) return;
        if (labelForDisplay == null || labelForDisplay.isEmpty()) {
            this.frequency = 0L;
            this.labelLink.clearTarget();
            updateBlockState();
            return;
        }
        LabelNetworkRegistry registry = LabelNetworkRegistry.get(worldObj);
        if (registry == null) return;

        LabelNetworkRegistry.LabelNetwork network = registry.getNetwork(worldObj, labelForDisplay, placerId);
        if (network == null && ensureRegister) {
            network = registry.register(worldObj, labelForDisplay, placerId, this);
        }
        if (network == null) {
            this.frequency = 0L;
            this.labelLink.clearTarget();
        } else {
            network.ensureVirtualNode(worldObj);
            this.frequency = network.getChannel();
            this.labelLink.setTarget(network);
        }
        updateBlockState();
        markDirty();
        markForUpdate();
    }

    /**
     * 方块被移除时调用
     */
    public void onRemoved() {
        this.beingRemoved = true;
        labelLink.onUnloadOrRemove();
        if (worldObj != null && !worldObj.isRemote) {
            LabelNetworkRegistry registry = LabelNetworkRegistry.get(worldObj);
            if (registry != null) {
                registry.unregister(this);
            }
        }
    }

    // ==================== Tick ====================

    @TileEvent(TileEventType.TICK)
    public void onTick() {
        if (worldObj == null || worldObj.isRemote) return;
        labelLink.updateStatus();
        updateBlockState();
    }

    /**
     * 更新方块状态（metadata）
     */
    private void updateBlockState() {
        if (worldObj == null || worldObj.isRemote) return;
        if (beingRemoved || isInvalid()) return;

        IGridNode node = getGridNode();
        boolean online = false;
        if (node != null && node.isActive()) {
            try {
                IEnergyGrid energy = node.getGrid()
                    .getCache(IEnergyGrid.class);
                online = energy != null && energy.isNetworkPowered();
            } catch (Exception ignored) {}
        }

        int currentMeta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        int newMeta = online ? 1 : 0;
        if (currentMeta != newMeta) {
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, newMeta, 3);
        }
    }

    // ==================== 生命周期 ====================

    @Override
    public void onReady() {
        super.onReady();
        if (worldObj != null && !worldObj.isRemote && labelForDisplay != null) {
            refreshLabel(true);
        }
    }

    @Override
    public void onChunkUnload() {
        labelLink.onUnloadOrRemove();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        labelLink.onUnloadOrRemove();
        super.invalidate();
    }

    // ==================== NBT ====================

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeToNBT_Custom(NBTTagCompound tag) {
        tag.setLong("frequency", frequency);
        if (labelForDisplay != null) {
            tag.setString("label", labelForDisplay);
        }
        if (placerId != null) {
            tag.setLong("placerIdMost", placerId.getMostSignificantBits());
            tag.setLong("placerIdLeast", placerId.getLeastSignificantBits());
        }
        if (placerName != null) {
            tag.setString("placerName", placerName);
        }
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNBT_Custom(NBTTagCompound tag) {
        this.frequency = tag.getLong("frequency");
        this.labelForDisplay = tag.hasKey("label") ? tag.getString("label") : null;
        if (tag.hasKey("placerIdMost") && tag.hasKey("placerIdLeast")) {
            long most = tag.getLong("placerIdMost");
            long least = tag.getLong("placerIdLeast");
            this.placerId = new UUID(most, least);
        }
        if (tag.hasKey("placerName")) {
            this.placerName = tag.getString("placerName");
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("frequency", frequency);
        if (labelForDisplay != null) {
            tag.setString("label", labelForDisplay);
        }
        if (placerId != null) {
            tag.setLong("placerIdMost", placerId.getMostSignificantBits());
            tag.setLong("placerIdLeast", placerId.getLeastSignificantBits());
        }
        if (placerName != null) {
            tag.setString("placerName", placerName);
        }
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.func_148857_g();
        if (tag == null) {
            return;
        }
        readFromNBT_Custom(tag);
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }
}
