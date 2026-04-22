package com.example.ae2_auto_pattern_upload.wireless;

import com.example.ae2_auto_pattern_upload.tile.TileWirelessTransceiver;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.HashMap;

public final class WirelessTransceiverRegistry {
    private static final Map<Long, TileWirelessTransceiver> MASTERS = new HashMap<>();

    private WirelessTransceiverRegistry() {
    }

    public static synchronized void register(TileWirelessTransceiver tile) {
        World world = tile.getWorld();
        if (world == null || world.isRemote || !tile.isMasterMode() || tile.getFrequency() <= 0) {
            return;
        }

        TileWirelessTransceiver current = MASTERS.get(tile.getFrequency());
        if (current == tile || !isValid(current, tile.getFrequency())) {
            MASTERS.put(tile.getFrequency(), tile);
        }
    }

    public static synchronized void unregister(TileWirelessTransceiver tile) {
        World world = tile.getWorld();
        if (world == null) {
            return;
        }

        TileWirelessTransceiver current = MASTERS.get(tile.getFrequency());
        if (current == tile) {
            MASTERS.remove(tile.getFrequency());
        }
    }

    @Nullable
    public static synchronized TileWirelessTransceiver getMaster(World world, long frequency) {
        TileWirelessTransceiver tile = MASTERS.get(frequency);
        if (!isValid(tile, frequency)) {
            MASTERS.remove(frequency);
            return null;
        }

        return tile;
    }

    private static boolean isValid(@Nullable TileWirelessTransceiver tile, long frequency) {
        return tile != null
                && !tile.isInvalid()
                && tile.getWorld() != null
                && tile.isMasterMode()
                && tile.getFrequency() == frequency;
    }
}
