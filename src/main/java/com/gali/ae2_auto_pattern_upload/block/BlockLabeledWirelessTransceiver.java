package com.gali.ae2_auto_pattern_upload.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
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

    public BlockLabeledWirelessTransceiver() {
        super(Material.iron);
        setBlockName("labeledWirelessTransceiver");
        setHardness(2.0F);
        setResistance(10.0F);
        setTileEntity(TileLabeledWirelessTransceiver.class);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        // 暂时使用铁块纹理作为占位
        blockIcon = Blocks.iron_block.getIcon(0, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return blockIcon;
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
}
