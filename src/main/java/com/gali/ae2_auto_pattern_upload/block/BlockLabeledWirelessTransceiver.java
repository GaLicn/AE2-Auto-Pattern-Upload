package com.gali.ae2_auto_pattern_upload.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gali.ae2_auto_pattern_upload.MyMod;
import com.gali.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;

import appeng.block.AEBaseTileBlock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 标签无线收发器方块
 */
public class BlockLabeledWirelessTransceiver extends AEBaseTileBlock {

    @SideOnly(Side.CLIENT)
    private IIcon iconOff;

    @SideOnly(Side.CLIENT)
    private IIcon iconOn;

    public BlockLabeledWirelessTransceiver() {
        super(Material.iron);
        setBlockName("labeledWirelessTransceiver");
        setHardness(1.5F); // 与石头一致的硬度
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 0); // 需要镐子挖掘，木镐即可
        setTileEntity(TileLabeledWirelessTransceiver.class);
    }

    @Override
    public int getRenderType() {
        return 0; // 使用标准方块渲染
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        // 注册开关两种状态的纹理
        iconOff = reg.registerIcon(MyMod.MODID + ":labeled_wireless_transceiver_off");
        iconOn = reg.registerIcon(MyMod.MODID + ":labeled_wireless_transceiver_on");
        blockIcon = iconOff;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        // meta 0 = 离线/关闭, meta 1 = 在线/开启
        return meta == 1 ? iconOn : iconOff;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (!world.isRemote && placer instanceof EntityPlayer) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileLabeledWirelessTransceiver) {
                TileLabeledWirelessTransceiver tile = (TileLabeledWirelessTransceiver) te;
                EntityPlayer player = (EntityPlayer) placer;
                tile.setPlacerId(player.getUniqueID(), player.getCommandSenderName());
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)) {
            return true;
        }

        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileLabeledWirelessTransceiver) {
                // 打开GUI
                player.openGui(MyMod.instance, GuiIds.LABELED_WIRELESS_TRANSCEIVER, world, x, y, z);
                return true;
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileLabeledWirelessTransceiver) {
            TileLabeledWirelessTransceiver tile = (TileLabeledWirelessTransceiver) te;
            tile.onRemoved();
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        return meta == 1 ? 8 : 0;
    }

    @Override
    public int quantityDropped(int meta, int fortune, java.util.Random random) {
        return 1; // 掉落1个物品
    }
}
