package com.example.ae2_auto_pattern_upload.wireless;

import com.example.ae2_auto_pattern_upload.tile.TileWirelessTransceiver;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class WirelessTransceiverRegistry {
    private static final Map<Integer, Map<Long, TileWirelessTransceiver>> MASTERS = new HashMap<>();

    private WirelessTransceiverRegistry() {
    }

    public static synchronized void register(TileWirelessTransceiver tile) {
        World world = tile.getWorld();
        if (world == null || world.isRemote || !tile.isMasterMode() || tile.getFrequency() <= 0) {
            return;
        }

        Map<Long, TileWirelessTransceiver> byFrequency =
                MASTERS.computeIfAbsent(world.provider.getDimension(), ignored -> new HashMap<>());
        TileWirelessTransceiver current = byFrequency.get(tile.getFrequency());
        if (current == tile || !isValid(current, world, tile.getFrequency())) {
            byFrequency.put(tile.getFrequency(), tile);
        }
    }

    public static synchronized void unregister(TileWirelessTransceiver tile) {
        World world = tile.getWorld();
        if (world == null) {
            return;
        }

        Map<Long, TileWirelessTransceiver> byFrequency = MASTERS.get(world.provider.getDimension());
        if (byFrequency == null) {
            return;
        }

        TileWirelessTransceiver current = byFrequency.get(tile.getFrequency());
        if (current == tile) {
            byFrequency.remove(tile.getFrequency());
        }

        if (byFrequency.isEmpty()) {
            MASTERS.remove(world.provider.getDimension());
        }
    }

    @Nullable
    public static synchronized TileWirelessTransceiver getMaster(World world, long frequency) {
        Map<Long, TileWirelessTransceiver> byFrequency = MASTERS.get(world.provider.getDimension());
        if (byFrequency == null) {
            return null;
        }

        TileWirelessTransceiver tile = byFrequency.get(frequency);
        if (!isValid(tile, world, frequency)) {
            byFrequency.remove(frequency);
            if (byFrequency.isEmpty()) {
                MASTERS.remove(world.provider.getDimension());
            }
            return null;
        }

        return tile;
    }

    private static boolean isValid(@Nullable TileWirelessTransceiver tile, World world, long frequency) {
        return tile != null
                && !tile.isInvalid()
                && tile.getWorld() == world
                && tile.isMasterMode()
                && tile.getFrequency() == frequency;
    }
}
