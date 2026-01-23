# 标签无线收发器 (LabeledWirelessTransceiver) 移植方案

## 1. 概述

本文档描述如何将 1.20 版本的 `LabeledWirelessTransceiver`（标签无线收发器）移植到 Minecraft 1.7.10 + AE2 GTNH 版本。

### 1.1 功能概述
标签无线收发器是一种通过"标签名称"而非数字频率来建立无线ME网络连接的设备：
- 使用字符串标签代替数字频率
- 同一标签的所有收发器共享同一个虚拟网络节点
- 支持玩家/队伍隔离
- 支持跨维度连接（可配置）

### 1.2 核心组件
| 1.20 组件 | 1.7.10 对应 | 说明 |
|-----------|-------------|------|
| `LabeledWirelessTransceiverBlock` | 继承 `AEBaseTileBlock` | 方块类 |
| `LabeledWirelessTransceiverBlockEntity` | 继承 `AENetworkTile` | TileEntity |
| `LabelNetworkRegistry` (SavedData) | 继承 `WorldSavedData` | 标签网络注册中心 |
| `LabelLink` | 保持不变 | 连接管理器 |
| `IWirelessEndpoint` | 保持不变 | 端点接口 |

---

## 2. API 差异对照

### 2.1 网格节点创建

**1.20 版本：**
```java
// 使用 GridHelper 和 IManagedGridNode
this.managedNode = GridHelper.createManagedNode(this, NodeListener.INSTANCE)
    .setFlags(GridFlags.DENSE_CAPACITY);
this.managedNode.setIdlePowerUsage(idlePower);
this.managedNode.setInWorldNode(true);
this.managedNode.create(level, pos);
```

**1.7.10 版本：**
```java
// 使用 AENetworkProxy（推荐）或直接使用 AEApi
// 方式1：继承 AENetworkTile，自动获得 AENetworkProxy
public class TileLabeledWirelessTransceiver extends AENetworkTile {
    @Override
    protected AENetworkProxy createProxy() {
        AENetworkProxy proxy = new AENetworkProxy(this, "proxy", getItemFromTile(this), true);
        proxy.setFlags(GridFlags.DENSE_CAPACITY);
        proxy.setIdlePowerUsage(idlePower);
        return proxy;
    }
}

// 方式2：直接创建节点
IGridNode node = AEApi.instance().createGridNode(gridBlock);
node.updateState();
```

### 2.2 网格连接创建

**1.20 版本：**
```java
GridHelper.createConnection(nodeA, nodeB);
```

**1.7.10 版本：**
```java
// 使用 GridConnection 构造函数
new GridConnection(nodeA, nodeB, ForgeDirection.UNKNOWN);
```

### 2.3 方块状态 (BlockState)

**1.20 版本：**
```java
public static final BooleanProperty STATE = BooleanProperty.create("state");
this.registerDefaultState(this.stateDefinition.any().setValue(STATE, false));
level.setBlock(pos, state.setValue(STATE, online), 3);
```

**1.7.10 版本：**
```java
// 使用 metadata 代替 BlockState
// metadata 0 = offline, 1 = online
world.setBlockMetadataWithNotify(x, y, z, online ? 1 : 0, 3);
```

### 2.4 数据持久化 (SavedData vs WorldSavedData)

**1.20 版本：**
```java
public class LabelNetworkRegistry extends SavedData {
    public static LabelNetworkRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            LabelNetworkRegistry::load, 
            LabelNetworkRegistry::new, 
            SAVE_ID
        );
    }
}
```

**1.7.10 版本：**
```java
public class LabelNetworkRegistry extends WorldSavedData {
    public static final String SAVE_ID = "eaep_label_networks";
    
    public LabelNetworkRegistry() {
        super(SAVE_ID);
    }
    
    public static LabelNetworkRegistry get(World world) {
        LabelNetworkRegistry data = (LabelNetworkRegistry) world.mapStorage
            .loadData(LabelNetworkRegistry.class, SAVE_ID);
        if (data == null) {
            data = new LabelNetworkRegistry();
            world.mapStorage.setData(SAVE_ID, data);
        }
        return data;
    }
}
```


---

## 3. 详细实现方案

### 3.1 文件结构

```
src/main/java/com/yourmod/
├── block/
│   └── BlockLabeledWirelessTransceiver.java
├── tile/
│   └── TileLabeledWirelessTransceiver.java
├── wireless/
│   ├── IWirelessEndpoint.java
│   ├── LabelLink.java
│   └── LabelNetworkRegistry.java
├── container/
│   └── ContainerLabeledWirelessTransceiver.java
├── gui/
│   └── GuiLabeledWirelessTransceiver.java
└── init/
    └── ModBlocks.java
```

### 3.2 IWirelessEndpoint 接口

```java
package com.yourmod.wireless;

import appeng.api.networking.IGridNode;
import net.minecraft.world.World;

/**
 * 无线端点接口 - 1.7.10 版本
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
    
    /** 是否已移除 */
    boolean isEndpointRemoved();
}
```

### 3.3 方块类实现

```java
package com.yourmod.block;

import appeng.block.AEBaseTileBlock;
import com.yourmod.tile.TileLabeledWirelessTransceiver;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockLabeledWirelessTransceiver extends AEBaseTileBlock {

    public BlockLabeledWirelessTransceiver() {
        super(Material.iron);
        setTileEntity(TileLabeledWirelessTransceiver.class);
        setHardness(2.0F);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, 
            EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (!world.isRemote && placer instanceof EntityPlayer) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileLabeledWirelessTransceiver tile) {
                EntityPlayer player = (EntityPlayer) placer;
                tile.setPlacerId(player.getUniqueID(), player.getCommandSenderName());
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z,
            EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileLabeledWirelessTransceiver) {
                // 打开GUI
                player.openGui(YourMod.instance, GuiIds.LABELED_WIRELESS_TRANSCEIVER, 
                    world, x, y, z);
                return true;
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, 
            net.minecraft.block.Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileLabeledWirelessTransceiver tile) {
            tile.onRemoved();
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
```


### 3.4 TileEntity 实现

```java
package com.yourmod.tile;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkTile;
import com.yourmod.wireless.IWirelessEndpoint;
import com.yourmod.wireless.LabelLink;
import com.yourmod.wireless.LabelNetworkRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.UUID;

public class TileLabeledWirelessTransceiver extends AENetworkTile 
        implements IWirelessEndpoint {

    private long frequency = 0L;
    private String labelForDisplay = null;
    private boolean beingRemoved = false;
    private UUID placerId = null;
    private String placerName = null;
    
    private final LabelLink labelLink = new LabelLink(this);

    @Override
    protected AENetworkProxy createProxy() {
        AENetworkProxy proxy = new AENetworkProxy(this, "proxy", getItemFromTile(this), true);
        proxy.setFlags(GridFlags.DENSE_CAPACITY);
        proxy.setIdlePowerUsage(1.0); // 可配置
        proxy.setValidSides(java.util.EnumSet.allOf(ForgeDirection.class));
        return proxy;
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.GLASS;
    }

    // ==================== IWirelessEndpoint ====================
    
    @Override
    public World getServerWorld() {
        return this.worldObj;
    }

    @Override
    public int getXCoord() { return this.xCoord; }

    @Override
    public int getYCoord() { return this.yCoord; }

    @Override
    public int getZCoord() { return this.zCoord; }

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
    }

    public UUID getPlacerId() { return placerId; }
    public String getPlacerName() { return placerName; }
    public long getFrequency() { return frequency; }
    public String getLabelForDisplay() { return labelForDisplay; }

    public void applyLabel(String rawLabel) {
        if (worldObj == null || worldObj.isRemote) return;

        // 先注销旧网络引用
        LabelNetworkRegistry.get(worldObj).unregister(this);

        var network = LabelNetworkRegistry.get(worldObj)
            .register(worldObj, rawLabel, placerId, this);
        if (network == null) {
            clearLabel();
            return;
        }

        this.labelForDisplay = rawLabel;
        this.frequency = network.getChannel();
        this.labelLink.setTarget(network);
        updateState();
        markDirty();
    }

    public void clearLabel() {
        if (worldObj != null && !worldObj.isRemote) {
            LabelNetworkRegistry.get(worldObj).unregister(this);
        }
        this.labelForDisplay = null;
        this.frequency = 0L;
        this.labelLink.clearTarget();
        updateState();
        markDirty();
    }

    public void onRemoved() {
        this.beingRemoved = true;
        labelLink.onUnloadOrRemove();
        if (worldObj != null && !worldObj.isRemote) {
            LabelNetworkRegistry.get(worldObj).unregister(this);
        }
    }

    // ==================== Tick ====================

    @TileEvent(TileEventType.TICK)
    public void onTick() {
        if (worldObj.isRemote) return;
        labelLink.updateStatus();
        updateState();
    }

    private void updateState() {
        if (worldObj == null || worldObj.isRemote) return;
        if (beingRemoved || isInvalid()) return;

        IGridNode node = getGridNode();
        boolean online = false;
        if (node != null && node.isActive()) {
            try {
                var grid = node.getGrid();
                var energy = grid.getCache(appeng.api.networking.energy.IEnergyGrid.class);
                online = energy != null && energy.isNetworkPowered();
            } catch (Exception ignored) {}
        }

        int currentMeta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        int newMeta = online ? 1 : 0;
        if (currentMeta != newMeta) {
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, newMeta, 3);
        }
    }

    // ==================== NBT ====================

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeToNBT_Custom(NBTTagCompound tag) {
        tag.setLong("frequency", frequency);
        if (labelForDisplay != null) {
            tag.setString("label", labelForDisplay);
        }
        if (placerId != null) {
            tag.setString("placerIdMost", Long.toString(placerId.getMostSignificantBits()));
            tag.setString("placerIdLeast", Long.toString(placerId.getLeastSignificantBits()));
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
            long most = Long.parseLong(tag.getString("placerIdMost"));
            long least = Long.parseLong(tag.getString("placerIdLeast"));
            this.placerId = new UUID(most, least);
        }
        if (tag.hasKey("placerName")) {
            this.placerName = tag.getString("placerName");
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        if (worldObj != null && !worldObj.isRemote && labelForDisplay != null) {
            refreshLabel(true);
        }
    }

    public void refreshLabel(boolean ensureRegister) {
        if (worldObj == null || worldObj.isRemote) return;
        if (labelForDisplay == null || labelForDisplay.isEmpty()) {
            this.frequency = 0L;
            this.labelLink.clearTarget();
            updateState();
            return;
        }
        var registry = LabelNetworkRegistry.get(worldObj);
        var network = registry.getNetwork(worldObj, labelForDisplay, placerId);
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
        updateState();
        markDirty();
    }
}
```


### 3.5 LabelLink 连接管理器

```java
package com.yourmod.wireless;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.me.GridConnection;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * 标签无线收发器的连接器 - 1.7.10 版本
 * 将 BE 的 in-world 节点连接到标签网络的虚拟节点
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
        if (targetDim != Integer.MIN_VALUE && targetDim != hostWorld.provider.dimensionId) {
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
```

### 3.6 LabelNetworkRegistry 标签网络注册中心

```java
package com.yourmod.wireless;

import appeng.api.AEApi;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.GridNotification;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.*;

public class LabelNetworkRegistry extends WorldSavedData {
    public static final String SAVE_ID = "eaep_label_networks";
    private static final long CHANNEL_START = 1_000_000L;

    private final Map<Key, LabelNetwork> networks = new HashMap<>();
    private long nextChannel = CHANNEL_START;

    public LabelNetworkRegistry() {
        super(SAVE_ID);
    }

    public LabelNetworkRegistry(String name) {
        super(name);
    }

    public static LabelNetworkRegistry get(World world) {
        // 使用主世界存储
        World overworld = world.isRemote ? world : 
            world.getMinecraftServer().worldServerForDimension(0);
        
        LabelNetworkRegistry data = (LabelNetworkRegistry) overworld.mapStorage
            .loadData(LabelNetworkRegistry.class, SAVE_ID);
        if (data == null) {
            data = new LabelNetworkRegistry();
            overworld.mapStorage.setData(SAVE_ID, data);
        }
        return data;
    }

    /**
     * 规范化标签
     */
    public static String normalizeLabel(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        if (t.length() > 64) t = t.substring(0, 64);
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-')) {
                return null;
            }
        }
        return t;
    }

    /**
     * 注册标签网络
     */
    public synchronized LabelNetwork register(World world, String rawLabel, 
            UUID placerId, IWirelessEndpoint endpoint) {
        String label = normalizeLabel(rawLabel);
        if (label == null) return null;

        UUID owner = placerId != null ? placerId : PUBLIC_NETWORK_UUID;
        int dimId = world.provider.dimensionId; // 或 Integer.MIN_VALUE 表示跨维
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

        network.addEndpoint(endpoint);
        markDirty();
        return network;
    }

    public synchronized void unregister(IWirelessEndpoint endpoint) {
        World world = endpoint.getServerWorld();
        if (world == null) return;
        int dimId = world.provider.dimensionId;
        int x = endpoint.getXCoord();
        int y = endpoint.getYCoord();
        int z = endpoint.getZCoord();
        
        for (LabelNetwork net : networks.values()) {
            net.removeEndpoint(dimId, x, y, z);
        }
        markDirty();
    }

    public synchronized LabelNetwork getNetwork(World world, String rawLabel, UUID placerId) {
        String label = normalizeLabel(rawLabel);
        if (label == null) return null;
        UUID owner = placerId != null ? placerId : PUBLIC_NETWORK_UUID;
        int dimId = world.provider.dimensionId;
        Key key = new Key(dimId, label, owner);
        return networks.get(key);
    }

    private long allocateChannel() {
        if (nextChannel < CHANNEL_START) nextChannel = CHANNEL_START;
        return nextChannel++;
    }

    public static final UUID PUBLIC_NETWORK_UUID = new UUID(0, 0);

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
            UUID owner = new UUID(
                nbt.getLong("ownerMost"), 
                nbt.getLong("ownerLeast")
            );
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

    public record Key(int dim, String label, UUID owner) {}

    public static class LabelNetwork {
        private final int dimensionId;
        private final String label;
        private final UUID owner;
        private final long channel;
        private final Set<EndpointRef> endpoints = new HashSet<>();
        private IGridNode virtualNode = null;

        public LabelNetwork(int dim, String label, UUID owner, long channel) {
            this.dimensionId = dim;
            this.label = label;
            this.owner = owner;
            this.channel = channel;
        }

        public long getChannel() { return channel; }
        public int getDimensionId() { return dimensionId; }
        public IGridNode getNode() { return virtualNode; }

        public boolean ensureVirtualNode(World world) {
            if (virtualNode != null) return true;
            
            VirtualLabelGridBlock gridBlock = new VirtualLabelGridBlock(world, label);
            virtualNode = AEApi.instance().createGridNode(gridBlock);
            virtualNode.updateState();
            return virtualNode != null;
        }

        public void destroyVirtualNode() {
            if (virtualNode != null) {
                virtualNode.destroy();
                virtualNode = null;
            }
        }

        public void addEndpoint(IWirelessEndpoint ep) {
            endpoints.add(new EndpointRef(
                ep.getServerWorld().provider.dimensionId,
                ep.getXCoord(), ep.getYCoord(), ep.getZCoord()
            ));
        }

        public void removeEndpoint(int dim, int x, int y, int z) {
            endpoints.removeIf(ref -> ref.matches(dim, x, y, z));
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

    public record EndpointRef(int dim, int x, int y, int z) {
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
                tag.getInteger("z")
            );
        }
    }
}
```


### 3.7 虚拟节点 GridBlock 实现

```java
package com.yourmod.wireless;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridNotification;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.EnumSet;

/**
 * 虚拟标签网络节点的 GridBlock 实现
 */
public class VirtualLabelGridBlock implements IGridBlock, IGridHost {
    private final World world;
    private final String label;

    public VirtualLabelGridBlock(World world, String label) {
        this.world = world;
        this.label = label;
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
        return null; // 或返回收发器物品
    }

    // ==================== IGridHost ====================

    @Override
    public appeng.api.networking.IGridNode getGridNode(ForgeDirection dir) {
        return null;
    }

    @Override
    public appeng.api.util.AECableType getCableConnectionType(ForgeDirection dir) {
        return appeng.api.util.AECableType.NONE;
    }

    @Override
    public void securityBreak() {
        // 不处理
    }
}
```

---

## 4. GUI 和 Container 实现

### 4.1 Container 实现

```java
package com.yourmod.container;

import com.yourmod.tile.TileLabeledWirelessTransceiver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ContainerLabeledWirelessTransceiver extends Container {
    private final TileLabeledWirelessTransceiver tile;

    public ContainerLabeledWirelessTransceiver(InventoryPlayer playerInv, 
            TileLabeledWirelessTransceiver tile) {
        this.tile = tile;
        // 添加玩家物品栏槽位（如果需要）
    }

    public TileLabeledWirelessTransceiver getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile != null && !tile.isInvalid() &&
            player.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, 
                tile.zCoord + 0.5) <= 64;
    }
}
```

### 4.2 GUI 实现

```java
package com.yourmod.gui;

import com.yourmod.container.ContainerLabeledWirelessTransceiver;
import com.yourmod.tile.TileLabeledWirelessTransceiver;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiLabeledWirelessTransceiver extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "yourmod", "textures/gui/labeled_wireless_transceiver.png");
    
    private final TileLabeledWirelessTransceiver tile;
    private GuiTextField labelField;
    private GuiButton applyButton;
    private GuiButton clearButton;

    public GuiLabeledWirelessTransceiver(InventoryPlayer playerInv, 
            TileLabeledWirelessTransceiver tile) {
        super(new ContainerLabeledWirelessTransceiver(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        // 标签输入框
        labelField = new GuiTextField(fontRendererObj, x + 10, y + 20, 100, 16);
        labelField.setMaxStringLength(64);
        labelField.setText(tile.getLabelForDisplay() != null ? 
            tile.getLabelForDisplay() : "");

        // 应用按钮
        applyButton = new GuiButton(0, x + 115, y + 18, 50, 20, "应用");
        buttonList.add(applyButton);

        // 清除按钮
        clearButton = new GuiButton(1, x + 115, y + 42, 50, 20, "清除");
        buttonList.add(clearButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            // 应用标签 - 发送网络包到服务端
            String label = labelField.getText();
            // PacketHandler.sendToServer(new PacketApplyLabel(tile, label));
        } else if (button.id == 1) {
            // 清除标签
            labelField.setText("");
            // PacketHandler.sendToServer(new PacketClearLabel(tile));
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (labelField.isFocused()) {
            labelField.textboxKeyTyped(c, keyCode);
        } else {
            super.keyTyped(c, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        labelField.mouseClicked(x, y, button);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, 
            int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString("标签无线收发器", 8, 6, 0x404040);
        
        // 显示当前状态
        String status = tile.getFrequency() > 0 ? 
            "频道: " + tile.getFrequency() : "未连接";
        fontRendererObj.drawString(status, 8, 70, 0x404040);
        
        labelField.drawTextBox();
    }
}
```


---

## 5. 网络包实现

### 5.1 应用标签网络包

```java
package com.yourmod.network;

import com.yourmod.tile.TileLabeledWirelessTransceiver;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class PacketApplyLabel implements IMessage {
    private int x, y, z;
    private String label;

    public PacketApplyLabel() {}

    public PacketApplyLabel(TileLabeledWirelessTransceiver tile, String label) {
        this.x = tile.xCoord;
        this.y = tile.yCoord;
        this.z = tile.zCoord;
        this.label = label;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        label = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        ByteBufUtils.writeUTF8String(buf, label);
    }

    public static class Handler implements IMessageHandler<PacketApplyLabel, IMessage> {
        @Override
        public IMessage onMessage(PacketApplyLabel msg, MessageContext ctx) {
            World world = ctx.getServerHandler().playerEntity.worldObj;
            TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
            if (te instanceof TileLabeledWirelessTransceiver tile) {
                tile.applyLabel(msg.label);
            }
            return null;
        }
    }
}
```

---

## 6. 注册

### 6.1 方块和TileEntity注册

```java
package com.yourmod.init;

import com.yourmod.block.BlockLabeledWirelessTransceiver;
import com.yourmod.tile.TileLabeledWirelessTransceiver;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

public class ModBlocks {
    public static Block labeledWirelessTransceiver;

    public static void init() {
        labeledWirelessTransceiver = new BlockLabeledWirelessTransceiver()
            .setBlockName("labeledWirelessTransceiver")
            .setCreativeTab(YourMod.creativeTab);
        
        GameRegistry.registerBlock(labeledWirelessTransceiver, 
            "labeledWirelessTransceiver");
        GameRegistry.registerTileEntity(TileLabeledWirelessTransceiver.class, 
            "yourmod:labeledWirelessTransceiver");
    }
}
```

### 6.2 GUI Handler

```java
package com.yourmod;

import com.yourmod.container.ContainerLabeledWirelessTransceiver;
import com.yourmod.gui.GuiLabeledWirelessTransceiver;
import com.yourmod.tile.TileLabeledWirelessTransceiver;
import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class GuiHandler implements IGuiHandler {
    public static final int LABELED_WIRELESS_TRANSCEIVER = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, 
            World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (id == LABELED_WIRELESS_TRANSCEIVER && 
                te instanceof TileLabeledWirelessTransceiver tile) {
            return new ContainerLabeledWirelessTransceiver(player.inventory, tile);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, 
            World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (id == LABELED_WIRELESS_TRANSCEIVER && 
                te instanceof TileLabeledWirelessTransceiver tile) {
            return new GuiLabeledWirelessTransceiver(player.inventory, tile);
        }
        return null;
    }
}
```

---

## 7. 关键差异总结

| 特性 | 1.20 版本 | 1.7.10 版本 |
|------|-----------|-------------|
| 方块基类 | `Block` + `EntityBlock` | `AEBaseTileBlock` |
| TileEntity基类 | `AEBaseBlockEntity` | `AENetworkTile` |
| 网格节点 | `IManagedGridNode` | `AENetworkProxy` / `IGridNode` |
| 连接创建 | `GridHelper.createConnection()` | `new GridConnection()` |
| 数据持久化 | `SavedData` | `WorldSavedData` |
| 方块状态 | `BlockState` + `BooleanProperty` | `metadata` |
| GUI系统 | `AbstractContainerMenu` + `Screen` | `Container` + `GuiContainer` |
| 网络包 | Forge网络系统 | `SimpleNetworkWrapper` |
| 坐标 | `BlockPos` | `int x, y, z` |
| 方向 | `Direction` | `ForgeDirection` |
| NBT UUID | `tag.putUUID()` / `tag.getUUID()` | 手动存储 Most/Least |

---

## 8. 注意事项

1. **线程安全**: `LabelNetworkRegistry` 的方法需要 `synchronized` 保护
2. **节点生命周期**: 确保在 `onChunkUnload` 和 `invalidate` 时正确销毁节点
3. **虚拟节点**: 虚拟节点不应该在世界中可见，`isWorldAccessible()` 返回 `false`
4. **NBT兼容**: 1.7.10 没有原生 UUID NBT 支持，需要手动存储
5. **Tick事件**: 使用 `@TileEvent(TileEventType.TICK)` 注解
6. **GUI同步**: 需要实现网络包来同步客户端和服务端状态

---

## 9. 测试清单

- [ ] 放置方块后正确记录放置者UUID
- [ ] 输入标签后正确创建/加入标签网络
- [ ] 同标签的多个收发器能够共享网络
- [ ] 清除标签后正确断开连接
- [ ] 方块破坏后正确清理注册信息
- [ ] 世界重载后标签网络正确恢复
- [ ] 跨维度功能（如果启用）正常工作
- [ ] GUI正确显示当前状态
