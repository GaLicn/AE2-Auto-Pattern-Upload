package com.example.ae2_auto_pattern_upload.wireless;

import com.example.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LabeledWirelessTransceiverRegistry {
    private static final long CHANNEL_START = 1_000_000L;
    private static final Map<Integer, Map<String, LabelNetwork>> NETWORKS = new HashMap<>();
    private static long nextChannel = CHANNEL_START;

    private LabeledWirelessTransceiverRegistry() {
    }

    public static synchronized void register(TileLabeledWirelessTransceiver tile) {
        World world = tile.getWorld();
        String label = normalizeLabel(tile.getLabelKey());
        if (world == null || world.isRemote || label.isEmpty()) {
            return;
        }

        Map<String, LabelNetwork> byLabel =
                NETWORKS.computeIfAbsent(world.provider.getDimension(), ignored -> new HashMap<>());
        LabelNetwork network = byLabel.get(label);
        if (network == null) {
            network = new LabelNetwork(nextChannel++);
            byLabel.put(label, network);
        }
        network.endpoints.add(tile);
    }

    public static synchronized void unregister(TileLabeledWirelessTransceiver tile) {
        unregister(tile, tile.getLabelKey());
    }

    public static synchronized void unregister(TileLabeledWirelessTransceiver tile, @Nullable String labelKey) {
        World world = tile.getWorld();
        String label = normalize(labelKey);
        if (world == null || label.isEmpty()) {
            return;
        }

        Map<String, LabelNetwork> byLabel = NETWORKS.get(world.provider.getDimension());
        if (byLabel == null) {
            return;
        }

        LabelNetwork network = byLabel.get(label);
        if (network == null) {
            return;
        }

        network.endpoints.removeIf(endpoint -> endpoint == tile || !isValid(endpoint, world, label));
        if (network.endpoints.isEmpty()) {
            byLabel.remove(label);
        }

        if (byLabel.isEmpty()) {
            NETWORKS.remove(world.provider.getDimension());
        }
    }

    @Nullable
    public static synchronized TileLabeledWirelessTransceiver getMaster(World world, @Nullable String labelKey) {
        String label = normalize(labelKey);
        if (label.isEmpty()) {
            return null;
        }

        Map<String, LabelNetwork> byLabel = NETWORKS.get(world.provider.getDimension());
        if (byLabel == null) {
            return null;
        }

        LabelNetwork network = byLabel.get(label);
        if (network == null) {
            return null;
        }

        TileLabeledWirelessTransceiver best = null;
        long bestPos = Long.MAX_VALUE;
        Iterator<TileLabeledWirelessTransceiver> iterator = network.endpoints.iterator();
        while (iterator.hasNext()) {
            TileLabeledWirelessTransceiver endpoint = iterator.next();
            if (!isValid(endpoint, world, label)) {
                iterator.remove();
                continue;
            }

            long pos = endpoint.getPos().toLong();
            if (best == null || pos < bestPos) {
                best = endpoint;
                bestPos = pos;
            }
        }

        if (network.endpoints.isEmpty()) {
            byLabel.remove(label);
            if (byLabel.isEmpty()) {
                NETWORKS.remove(world.provider.getDimension());
            }
        }

        return best;
    }

    public static synchronized int getOnlineCount(World world, @Nullable String labelKey) {
        String label = normalize(labelKey);
        if (label.isEmpty()) {
            return 0;
        }

        Map<String, LabelNetwork> byLabel = NETWORKS.get(world.provider.getDimension());
        if (byLabel == null) {
            return 0;
        }

        LabelNetwork network = byLabel.get(label);
        if (network == null) {
            return 0;
        }

        int count = 0;
        Iterator<TileLabeledWirelessTransceiver> iterator = network.endpoints.iterator();
        while (iterator.hasNext()) {
            TileLabeledWirelessTransceiver endpoint = iterator.next();
            if (!isValid(endpoint, world, label)) {
                iterator.remove();
                continue;
            }
            count++;
        }

        if (network.endpoints.isEmpty()) {
            byLabel.remove(label);
            if (byLabel.isEmpty()) {
                NETWORKS.remove(world.provider.getDimension());
            }
        }

        return count;
    }

    public static synchronized List<LabelNetworkSnapshot> listNetworks(World world) {
        Map<String, LabelNetwork> byLabel = NETWORKS.get(world.provider.getDimension());
        if (byLabel == null) {
            return Collections.emptyList();
        }

        List<LabelNetworkSnapshot> snapshots = new ArrayList<>();
        Iterator<Map.Entry<String, LabelNetwork>> iterator = byLabel.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LabelNetwork> entry = iterator.next();
            LabelNetwork network = entry.getValue();
            network.endpoints.removeIf(endpoint -> !isValid(endpoint, world, entry.getKey()));
            if (network.endpoints.isEmpty()) {
                iterator.remove();
                continue;
            }
            snapshots.add(new LabelNetworkSnapshot(entry.getKey(), network.channel));
        }

        snapshots.sort(Comparator.comparingLong(LabelNetworkSnapshot::getChannel));
        if (byLabel.isEmpty()) {
            NETWORKS.remove(world.provider.getDimension());
        }
        return snapshots;
    }

    public static synchronized void deleteNetwork(World world, @Nullable String labelKey) {
        String label = normalizeLabel(labelKey);
        if (label.isEmpty()) {
            return;
        }

        Map<String, LabelNetwork> byLabel = NETWORKS.get(world.provider.getDimension());
        if (byLabel == null) {
            return;
        }

        LabelNetwork network = byLabel.remove(label);
        if (network == null) {
            return;
        }

        List<TileLabeledWirelessTransceiver> endpoints = new ArrayList<>(network.endpoints);
        if (byLabel.isEmpty()) {
            NETWORKS.remove(world.provider.getDimension());
        }

        for (TileLabeledWirelessTransceiver endpoint : endpoints) {
            if (isValid(endpoint, world, label)) {
                endpoint.clearLabel();
            }
        }
    }

    public static synchronized boolean isMaster(TileLabeledWirelessTransceiver tile) {
        World world = tile.getWorld();
        if (world == null || world.isRemote) {
            return false;
        }

        return getMaster(world, tile.getLabelKey()) == tile;
    }

    private static boolean isValid(
            @Nullable TileLabeledWirelessTransceiver tile,
            World world,
            String label) {
        return tile != null
                && !tile.isInvalid()
                && tile.getWorld() == world
                && label.equals(tile.getLabelKey());
    }

    private static String normalize(@Nullable String labelKey) {
        return normalizeLabel(labelKey);
    }

    public static String normalizeLabel(@Nullable String rawLabel) {
        if (rawLabel == null) {
            return "";
        }

        String label = rawLabel.trim();
        if (label.length() > 64) {
            label = label.substring(0, 64);
        }
        return label;
    }

    public static final class LabelNetworkSnapshot {
        private final String label;
        private final long channel;

        public LabelNetworkSnapshot(String label, long channel) {
            this.label = label;
            this.channel = channel;
        }

        public String getLabel() {
            return this.label;
        }

        public long getChannel() {
            return this.channel;
        }
    }

    private static final class LabelNetwork {
        private final long channel;
        private final Set<TileLabeledWirelessTransceiver> endpoints = new LinkedHashSet<>();

        private LabelNetwork(long channel) {
            this.channel = channel;
        }
    }
}
