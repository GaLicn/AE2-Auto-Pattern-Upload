package com.example.ae2_auto_pattern_upload.block;

import com.example.ae2_auto_pattern_upload.Tags;
import com.example.ae2_auto_pattern_upload.tile.TileWirelessTransceiver;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class BlockWirelessTransceiver extends Block implements ITileEntityProvider {
    public static final PropertyInteger STATE = PropertyInteger.create("state", 0, 5);

    public BlockWirelessTransceiver() {
        super(Material.IRON);
        this.setRegistryName(Tags.MOD_ID, "wireless_transceiver");
        this.setTranslationKey(Tags.MOD_ID + ".wireless_transceiver");
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setHardness(3.0F);
        this.setResistance(10.0F);
        this.setSoundType(SoundType.METAL);
        this.setDefaultState(this.blockState.getBaseState().withProperty(STATE, 5));
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileWirelessTransceiver();
    }

    @Override
    public boolean onBlockActivated(
            World worldIn,
            BlockPos pos,
            IBlockState state,
            EntityPlayer playerIn,
            EnumHand hand,
            EnumFacing facing,
            float hitX,
            float hitY,
            float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }

        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (!(tileEntity instanceof TileWirelessTransceiver)) {
            return false;
        }

        TileWirelessTransceiver transceiver = (TileWirelessTransceiver) tileEntity;
        if (playerIn.isSneaking()) {
            long step = getFrequencyStep(playerIn.getHeldItem(hand));
            transceiver.setFrequency(transceiver.getFrequency() + step);
            notifyFrequency(playerIn, transceiver);
            return true;
        }

        transceiver.setMasterMode(!transceiver.isMasterMode());
        notifyMode(playerIn, transceiver);
        return true;
    }

    @Override
    public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn) {
        if (worldIn.isRemote || !playerIn.isSneaking()) {
            return;
        }

        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (!(tileEntity instanceof TileWirelessTransceiver)) {
            return;
        }

        TileWirelessTransceiver transceiver = (TileWirelessTransceiver) tileEntity;
        long step = getFrequencyStep(playerIn.getHeldItemMainhand());
        transceiver.setFrequency(Math.max(0L, transceiver.getFrequency() - step));
        notifyFrequency(playerIn, transceiver);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty<?>[]{STATE});
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        int clamped = Math.max(0, Math.min(5, meta));
        return this.getDefaultState().withProperty(STATE, clamped);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STATE);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.SOLID;
    }

    private long getFrequencyStep(ItemStack stack) {
        if (!stack.isEmpty() && (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.REDSTONE_TORCH)
                || stack.getItem() == net.minecraft.init.Items.STICK)) {
            return 10L;
        }

        return 1L;
    }

    private void notifyFrequency(EntityPlayer player, TileWirelessTransceiver transceiver) {
        player.sendStatusMessage(
                new TextComponentTranslation(
                        "message.ae2_auto_pattern_upload.wireless_transceiver.frequency",
                        transceiver.getFrequency()),
                true);
    }

    private void notifyMode(EntityPlayer player, TileWirelessTransceiver transceiver) {
        String modeKey = transceiver.isMasterMode()
                ? "message.ae2_auto_pattern_upload.wireless_transceiver.mode.master"
                : "message.ae2_auto_pattern_upload.wireless_transceiver.mode.slave";
        player.sendStatusMessage(
                new TextComponentTranslation(
                        "message.ae2_auto_pattern_upload.wireless_transceiver.mode",
                        new TextComponentTranslation(modeKey)),
                true);
    }
}
