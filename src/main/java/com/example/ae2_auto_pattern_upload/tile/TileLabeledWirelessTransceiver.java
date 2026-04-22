package com.example.ae2_auto_pattern_upload.tile;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.tile.grid.AENetworkTile;
import com.example.ae2_auto_pattern_upload.block.BlockLabeledWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.init.ModItems;
import com.example.ae2_auto_pattern_upload.wireless.LabeledWirelessTransceiverRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class TileLabeledWirelessTransceiver extends AENetworkTile implements ITickable {
    private static final double MAX_RANGE = 256.0D;
    private static final double IDLE_POWER = 100.0D;

    private String label = "";
    @Nullable
    private UUID ownerId;
    private String ownerName = "";
    private IGridConnection slaveConnection;
    private int tickCounter;

    public TileLabeledWirelessTransceiver() {
        this.getProxy().setFlags(GridFlags.DENSE_CAPACITY);
        this.getProxy().setIdlePowerUsage(IDLE_POWER);
        this.getProxy().setVisualRepresentation(new ItemStack(ModItems.LABELED_WIRELESS_TRANSCEIVER));
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public void onReady() {
        super.onReady();
        refreshWirelessState();
    }

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }

        this.tickCounter++;
        if (this.tickCounter >= 10) {
            this.tickCounter = 0;
            refreshWirelessState();
        }
    }

    @Override
    public void gridChanged() {
        updateVisualState();
    }

    @Override
    public void onChunkUnload() {
        cleanupWirelessState();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        cleanupWirelessState();
        super.invalidate();
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.label = sanitizeLabel(data.getString("label"));
        if (data.hasUniqueId("ownerId")) {
            this.ownerId = data.getUniqueId("ownerId");
        } else {
            this.ownerId = null;
        }
        this.ownerName = data.getString("ownerName");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setString("label", this.label);
        if (this.ownerId != null) {
            data.setUniqueId("ownerId", this.ownerId);
        }
        if (!this.ownerName.isEmpty()) {
            data.setString("ownerName", this.ownerName);
        }
        return data;
    }

    public String getLabel() {
        return this.label;
    }

    public String getLabelKey() {
        return this.label;
    }

    public boolean hasLabel() {
        return !this.label.isEmpty();
    }

    public void setLabel(@Nullable String label) {
        String newLabel = LabeledWirelessTransceiverRegistry.normalizeLabel(label);
        if (Objects.equals(this.label, newLabel)) {
            return;
        }

        String oldLabel = this.label;
        if (this.world != null && !this.world.isRemote && !oldLabel.isEmpty()) {
            LabeledWirelessTransceiverRegistry.unregister(this, oldLabel);
        }

        this.label = newLabel;
        refreshWirelessState();
        saveChanges();
    }

    public void clearLabel() {
        setLabel("");
    }

    public void setOwner(@Nullable UUID ownerId, @Nullable String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName == null ? "" : ownerName;
        saveChanges();
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public int getLinkedCount() {
        if (this.world == null || this.world.isRemote || this.label.isEmpty()) {
            return 0;
        }
        return LabeledWirelessTransceiverRegistry.getOnlineCount(this.world, this.label);
    }

    public int getUsedChannels() {
        IGridNode node = this.getActionableNode();
        if (node == null || !node.isActive()) {
            return 0;
        }

        int usedChannels = 0;
        for (IGridConnection connection : node.getConnections()) {
            usedChannels = Math.max(usedChannels, connection.getUsedChannels());
        }
        return usedChannels;
    }

    public int getMaxChannels() {
        return 32;
    }

    private void refreshWirelessState() {
        if (this.world == null || this.world.isRemote) {
            return;
        }

        if (this.label.isEmpty()) {
            LabeledWirelessTransceiverRegistry.unregister(this);
            destroySlaveConnection();
        } else {
            LabeledWirelessTransceiverRegistry.register(this);
            updateSlaveConnection();
        }

        updateVisualState();
        markForUpdate();
    }

    private void updateSlaveConnection() {
        TileLabeledWirelessTransceiver master =
                LabeledWirelessTransceiverRegistry.getMaster(this.world, this.label);
        if (master == null || master == this || master.isInvalid()) {
            destroySlaveConnection();
            return;
        }

        double distanceSq = master.getPos().distanceSq(this.pos);
        if (distanceSq > MAX_RANGE * MAX_RANGE) {
            destroySlaveConnection();
            return;
        }

        IGridNode selfNode = this.getActionableNode();
        IGridNode masterNode = master.getActionableNode();
        if (selfNode == null || masterNode == null) {
            destroySlaveConnection();
            return;
        }

        if (this.slaveConnection != null) {
            boolean sameEndpoints = (this.slaveConnection.a() == selfNode || this.slaveConnection.b() == selfNode)
                    && (this.slaveConnection.a() == masterNode || this.slaveConnection.b() == masterNode);
            if (sameEndpoints) {
                return;
            }

            destroySlaveConnection();
        }

        try {
            this.slaveConnection = AEApi.instance().grid().createGridConnection(selfNode, masterNode);
        } catch (FailedConnectionException ignored) {
            destroySlaveConnection();
        }
    }

    private void updateVisualState() {
        if (this.world == null || this.world.isRemote) {
            return;
        }

        IBlockState currentState = this.world.getBlockState(this.pos);
        if (!(currentState.getBlock() instanceof BlockLabeledWirelessTransceiver)) {
            return;
        }

        IGridNode node = this.getActionableNode();
        boolean online = false;
        if (!this.label.isEmpty() && node != null && node.isActive()) {
            online = LabeledWirelessTransceiverRegistry.isMaster(this) || this.slaveConnection != null;
        }

        if (currentState.getValue(BlockLabeledWirelessTransceiver.STATE) != online) {
            this.world.setBlockState(
                    this.pos,
                    currentState.withProperty(BlockLabeledWirelessTransceiver.STATE, online),
                    3);
        }
    }

    private void cleanupWirelessState() {
        if (this.world != null && !this.world.isRemote) {
            LabeledWirelessTransceiverRegistry.unregister(this);
        }
        destroySlaveConnection();
    }

    private void destroySlaveConnection() {
        if (this.slaveConnection != null) {
            this.slaveConnection.destroy();
            this.slaveConnection = null;
        }
    }

    private String sanitizeLabel(@Nullable String rawLabel) {
        return LabeledWirelessTransceiverRegistry.normalizeLabel(rawLabel);
    }
}
