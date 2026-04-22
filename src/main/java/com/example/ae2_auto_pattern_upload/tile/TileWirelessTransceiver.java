package com.example.ae2_auto_pattern_upload.tile;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.tile.grid.AENetworkTile;
import com.example.ae2_auto_pattern_upload.block.BlockWirelessTransceiver;
import com.example.ae2_auto_pattern_upload.init.ModItems;
import com.example.ae2_auto_pattern_upload.wireless.WirelessTransceiverRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;

public class TileWirelessTransceiver extends AENetworkTile implements ITickable {
    private static final double IDLE_POWER = 100.0D;

    private long frequency = 1L;
    private boolean masterMode = false;
    private IGridConnection slaveConnection;
    private int tickCounter;

    public TileWirelessTransceiver() {
        this.getProxy().setFlags(GridFlags.DENSE_CAPACITY);
        this.getProxy().setIdlePowerUsage(IDLE_POWER);
        this.getProxy().setVisualRepresentation(new ItemStack(ModItems.WIRELESS_TRANSCEIVER));
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
        this.frequency = Math.max(0L, data.getLong("frequency"));
        this.masterMode = data.getBoolean("masterMode");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setLong("frequency", this.frequency);
        data.setBoolean("masterMode", this.masterMode);
        return data;
    }

    public long getFrequency() {
        return this.frequency;
    }

    public void setFrequency(long frequency) {
        long newFrequency = Math.max(0L, frequency);
        if (this.frequency == newFrequency) {
            return;
        }

        if (this.world != null && !this.world.isRemote && this.masterMode) {
            WirelessTransceiverRegistry.unregister(this);
        }

        this.frequency = newFrequency;
        refreshWirelessState();
        saveChanges();
    }

    public boolean isMasterMode() {
        return this.masterMode;
    }

    public void setMasterMode(boolean masterMode) {
        if (this.masterMode == masterMode) {
            return;
        }

        if (this.world != null && !this.world.isRemote && this.masterMode) {
            WirelessTransceiverRegistry.unregister(this);
        }

        this.masterMode = masterMode;
        refreshWirelessState();
        saveChanges();
    }

    private void refreshWirelessState() {
        if (this.world == null || this.world.isRemote) {
            return;
        }

        if (this.masterMode) {
            destroySlaveConnection();
            if (this.frequency > 0) {
                WirelessTransceiverRegistry.register(this);
            }
        } else {
            WirelessTransceiverRegistry.unregister(this);
            updateSlaveConnection();
        }

        updateVisualState();
        markForUpdate();
    }

    private void updateSlaveConnection() {
        if (this.frequency <= 0) {
            destroySlaveConnection();
            return;
        }

        TileWirelessTransceiver master = WirelessTransceiverRegistry.getMaster(this.world, this.frequency);
        if (master == null || master == this || master.isInvalid()) {
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
        if (!(currentState.getBlock() instanceof BlockWirelessTransceiver)) {
            return;
        }

        IGridNode node = this.getActionableNode();
        int newState = 5;

        if (node != null && node.isActive()) {
            boolean hasConnection = false;
            int usedChannels = 0;

            for (IGridConnection connection : node.getConnections()) {
                hasConnection = true;
                usedChannels = Math.max(usedChannels, connection.getUsedChannels());
            }

            if (hasConnection) {
                if (usedChannels >= 32) {
                    newState = 4;
                } else if (usedChannels >= 24) {
                    newState = 3;
                } else if (usedChannels >= 16) {
                    newState = 2;
                } else if (usedChannels >= 8) {
                    newState = 1;
                } else {
                    newState = 0;
                }
            }
        }

        if (currentState.getValue(BlockWirelessTransceiver.STATE) != newState) {
            this.world.setBlockState(this.pos, currentState.withProperty(BlockWirelessTransceiver.STATE, newState), 3);
        }
    }

    private void cleanupWirelessState() {
        if (this.world != null && !this.world.isRemote) {
            WirelessTransceiverRegistry.unregister(this);
        }
        destroySlaveConnection();
    }

    private void destroySlaveConnection() {
        if (this.slaveConnection != null) {
            this.slaveConnection.destroy();
            this.slaveConnection = null;
        }
    }
}
