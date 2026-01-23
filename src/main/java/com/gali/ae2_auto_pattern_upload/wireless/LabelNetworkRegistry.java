package com.gali.ae2_auto_pattern_upload.wireless;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import com.gali.ae2_auto_pattern_upload.Config;
import com.gali.ae2_auto_pattern_upload.MyMod;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;

/**
 * 标签无线网络注册中心（WorldSavedData）
 * - 负责标签到频道（频率）的分配与复用
 * - 创建/销毁虚拟节点，所有收发器连接到同一虚拟节点
 * - 记录在线端点集合
 */
public class LabelNetworkRegistry extends WorldSavedData {

    public static final String SAVE_ID = MyMod.MODID + "_label_networks";
    private static final long CHANNEL_START = 1_000_000L;

    /** 公共收发器UUID（用于没有设置所有者的收发器） */
    public static final UUID PUBLIC_NETWORK_UUID = new UUID(0, 0);

    private final Map<Key, LabelNetwork> networks = new HashMap<>();
    private long nextChannel = CHANNEL_START;

    public LabelNetworkRegistry() {
        super(SAVE_ID);
    }

    public LabelNetworkRegistry(String name) {
        super(name);
    }

    /**
     * 获取注册中心实例
     */
    public static LabelNetworkRegistry get(World world) {
        if (world.isRemote) return null;

        // 使用主世界存储
        World overworld = MinecraftServer.getServer()
            .worldServerForDimension(0);

        LabelNetworkRegistry data = (LabelNetworkRegistry) overworld.mapStorage
            .loadData(LabelNetworkRegistry.class, SAVE_ID);
        if (data == null) {
            data = new LabelNetworkRegistry();
            overworld.mapStorage.setData(SAVE_ID, data);
        }
        return data;
    }

    /**
     * 是否启用跨维度
     */
    public static boolean isCrossDimEnabled() {
        return Config.wirelessCrossDimEnable;
    }

    /**
     * 规范化标签：trim，保持大小写敏感
     */
    public static String normalizeLabel(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        if (t.length() > 64) t = t.substring(0, 64);
        // 过滤非法字符
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-')) {
                return null;
            }
        }
        return t;
    }

    /**
     * 注册/切换标签
     */
    public synchronized LabelNetwork register(World world, String rawLabel, UUID placerId, IWirelessEndpoint endpoint) {
        String label = normalizeLabel(rawLabel);
        if (label == null) return null;

        UUID owner = placerId != null ? placerId : PUBLIC_NETWORK_UUID;
        int dimId = isCrossDimEnabled() ? Integer.MIN_VALUE : world.provider.dimensionId;
        Key key = new Key(dimId, label, owner);

        LabelNetwork network = networks.get(key);
        if (network == null) {
            long channel = allocateChannel();
            network = new LabelNetwork(dimId, label, owner, channel);
            if (!network.ensureVirtualNode(world)) {
                return null;
            }
            networks.put(key, network);
            markDirty();
        } else {
            network.ensureVirtualNode(world);
        }

        int epDim = endpoint.getServerWorld().provider.dimensionId;
        int epX = endpoint.getXCoord();
        int epY = endpoint.getYCoord();
        int epZ = endpoint.getZCoord();
        if (!isCrossDimEnabled() && Config.wirelessMaxRange > 0.0D && network.endpointCount() > 0) {
            if (!network.isInRange(epDim, epX, epY, epZ, Config.wirelessMaxRange)) {
                return null;
            }
        }

        network.addEndpoint(endpoint);
        markDirty();
        return network;
    }

    /**
     * 注销端点
     */
    public synchronized void unregister(IWirelessEndpoint endpoint) {
        World world = endpoint.getServerWorld();
        if (world == null) return;
        int dimId = world.provider.dimensionId;
        int x = endpoint.getXCoord();
        int y = endpoint.getYCoord();
        int z = endpoint.getZCoord();

        Iterator<Map.Entry<Key, LabelNetwork>> it = networks.entrySet()
            .iterator();
        while (it.hasNext()) {
            Map.Entry<Key, LabelNetwork> entry = it.next();
            LabelNetwork net = entry.getValue();
            net.removeEndpoint(dimId, x, y, z);
            if (net.endpointCount() <= 0) {
                net.destroyVirtualNode();
                it.remove();
            }
        }
        markDirty();
    }

    /**
     * 获取网络
     */
    public synchronized LabelNetwork getNetwork(World world, String rawLabel, UUID placerId) {
        String label = normalizeLabel(rawLabel);
        if (label == null) return null;
        UUID owner = placerId != null ? placerId : PUBLIC_NETWORK_UUID;
        int dimId = isCrossDimEnabled() ? Integer.MIN_VALUE : world.provider.dimensionId;
        Key key = new Key(dimId, label, owner);
        return networks.get(key);
    }

    /**
     * 删除网络
     */
    public synchronized boolean removeNetwork(World world, String rawLabel, UUID placerId) {
        String label = normalizeLabel(rawLabel);
        if (label == null) return false;
        UUID owner = placerId != null ? placerId : PUBLIC_NETWORK_UUID;
        int dimId = isCrossDimEnabled() ? Integer.MIN_VALUE : world.provider.dimensionId;
        Key key = new Key(dimId, label, owner);
        LabelNetwork net = networks.remove(key);
        if (net != null) {
            net.destroyVirtualNode();
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * 获取玩家所属网络列表
     */
    public synchronized List<LabelNetworkSnapshot> listNetworks(World world, UUID placerId) {
        UUID owner = placerId != null ? placerId : PUBLIC_NETWORK_UUID;
        int dimId = isCrossDimEnabled() ? Integer.MIN_VALUE : world.provider.dimensionId;
        List<LabelNetworkSnapshot> list = new ArrayList<>();
        for (Map.Entry<Key, LabelNetwork> entry : networks.entrySet()) {
            Key key = entry.getKey();
            if (!Objects.equals(key.owner, owner)) continue;
            if (key.dim != Integer.MIN_VALUE && key.dim != dimId) continue;
            list.add(
                new LabelNetworkSnapshot(
                    key.label,
                    entry.getValue()
                        .getChannel()));
        }
        list.sort(Comparator.comparingLong(s -> s.channel));
        return list;
    }

    private long allocateChannel() {
        if (nextChannel < CHANNEL_START) nextChannel = CHANNEL_START;
        return nextChannel++;
    }

    // ==================== NBT ====================

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        nextChannel = tag.getLong("nextChannel");
        networks.clear();
        NBTTagList list = tag.getTagList("networks", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound nbt = list.getCompoundTagAt(i);
            String label = nbt.getString("label");
            int dim = nbt.getInteger("dim");
            UUID owner = new UUID(nbt.getLong("ownerMost"), nbt.getLong("ownerLeast"));
            long channel = nbt.getLong("channel");
            LabelNetwork net = new LabelNetwork(dim, label, owner, channel);
            net.loadEndpoints(nbt.getTagList("endpoints", 10));
            networks.put(new Key(dim, label, owner), net);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setLong("nextChannel", nextChannel);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Key, LabelNetwork> entry : networks.entrySet()) {
            Key k = entry.getKey();
            LabelNetwork v = entry.getValue();
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setString("label", k.label);
            nbt.setInteger("dim", k.dim);
            nbt.setLong("ownerMost", k.owner.getMostSignificantBits());
            nbt.setLong("ownerLeast", k.owner.getLeastSignificantBits());
            nbt.setLong("channel", v.channel);
            nbt.setTag("endpoints", v.saveEndpoints());
            list.appendTag(nbt);
        }
        tag.setTag("networks", list);
    }

    // ==================== 内部类 ====================

    /** 快照类，用于列表显示 */
    public static class LabelNetworkSnapshot {

        public final String label;
        public final long channel;

        public LabelNetworkSnapshot(String label, long channel) {
            this.label = label;
            this.channel = channel;
        }
    }

    /** 网络Key */
    public static class Key {

        public final int dim;
        public final String label;
        public final UUID owner;

        public Key(int dim, String label, UUID owner) {
            this.dim = dim;
            this.label = label;
            this.owner = owner;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return dim == key.dim && Objects.equals(label, key.label) && Objects.equals(owner, key.owner);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dim, label, owner);
        }
    }

    /** 标签网络 */
    public static class LabelNetwork {

        private final int dimensionId;
        private final String label;
        private final UUID owner;
        final long channel;
        private final Set<EndpointRef> endpoints = new HashSet<>();
        private IGridNode virtualNode = null;
        private VirtualLabelGridBlock gridBlock = null;

        public LabelNetwork(int dim, String label, UUID owner, long channel) {
            this.dimensionId = dim;
            this.label = label;
            this.owner = owner;
            this.channel = channel;
        }

        public long getChannel() {
            return channel;
        }

        public int getDimensionId() {
            return dimensionId;
        }

        public IGridNode getNode() {
            return virtualNode;
        }

        public boolean ensureVirtualNode(World world) {
            if (virtualNode != null) return true;

            try {
                gridBlock = new VirtualLabelGridBlock(world, label);
                virtualNode = AEApi.instance()
                    .createGridNode(gridBlock);
                gridBlock.setNode(virtualNode);
                virtualNode.updateState();
                return virtualNode != null;
            } catch (Exception e) {
                MyMod.LOG.error("Failed to create virtual node for label: " + label, e);
                return false;
            }
        }

        public void destroyVirtualNode() {
            if (virtualNode != null) {
                try {
                    virtualNode.destroy();
                } catch (Exception ignored) {}
                virtualNode = null;
                gridBlock = null;
            }
        }

        public void addEndpoint(IWirelessEndpoint ep) {
            endpoints.add(
                new EndpointRef(
                    ep.getServerWorld().provider.dimensionId,
                    ep.getXCoord(),
                    ep.getYCoord(),
                    ep.getZCoord()));
        }

        public void removeEndpoint(int dim, int x, int y, int z) {
            endpoints.removeIf(ref -> ref.matches(dim, x, y, z));
        }

        public int endpointCount() {
            return endpoints.size();
        }

        public boolean isInRange(int dim, int x, int y, int z, double maxRange) {
            if (maxRange <= 0.0D) {
                return true;
            }
            double maxRangeSq = maxRange * maxRange;
            for (EndpointRef ref : endpoints) {
                if (ref.dim != dim) {
                    continue;
                }
                long dx = (long) ref.x - (long) x;
                long dy = (long) ref.y - (long) y;
                long dz = (long) ref.z - (long) z;
                double distSq = (double) (dx * dx + dy * dy + dz * dz);
                if (distSq <= maxRangeSq) {
                    return true;
                }
            }
            return false;
        }

        public NBTTagList saveEndpoints() {
            NBTTagList list = new NBTTagList();
            for (EndpointRef ref : endpoints) {
                list.appendTag(ref.save());
            }
            return list;
        }

        public void loadEndpoints(NBTTagList list) {
            endpoints.clear();
            for (int i = 0; i < list.tagCount(); i++) {
                endpoints.add(EndpointRef.load(list.getCompoundTagAt(i)));
            }
        }
    }

    /** 端点引用 */
    public static class EndpointRef {

        public final int dim;
        public final int x, y, z;

        public EndpointRef(int dim, int x, int y, int z) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public boolean matches(int d, int px, int py, int pz) {
            return dim == d && x == px && y == py && z == pz;
        }

        public NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dim", dim);
            tag.setInteger("x", x);
            tag.setInteger("y", y);
            tag.setInteger("z", z);
            return tag;
        }

        public static EndpointRef load(NBTTagCompound tag) {
            return new EndpointRef(
                tag.getInteger("dim"),
                tag.getInteger("x"),
                tag.getInteger("y"),
                tag.getInteger("z"));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EndpointRef that = (EndpointRef) o;
            return dim == that.dim && x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dim, x, y, z);
        }
    }
}
