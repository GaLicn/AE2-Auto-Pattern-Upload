package com.example.ae2_auto_pattern_upload.block;

import com.example.ae2_auto_pattern_upload.ExampleMod;
import com.example.ae2_auto_pattern_upload.Tags;
import com.example.ae2_auto_pattern_upload.gui.ModGuiHandler;
import com.example.ae2_auto_pattern_upload.tile.TileLabeledWirelessTransceiver;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockLabeledWirelessTransceiver extends Block implements ITileEntityProvider {
    public static final PropertyBool STATE = PropertyBool.create("state");

    public BlockLabeledWirelessTransceiver() {
        super(Material.IRON);
        this.setRegistryName(Tags.MOD_ID, "labeled_wireless_transceiver");
        this.setTranslationKey(Tags.MOD_ID + ".labeled_wireless_transceiver");
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setHardness(3.0F);
        this.setResistance(10.0F);
        this.setSoundType(SoundType.METAL);
        this.setDefaultState(this.blockState.getBaseState().withProperty(STATE, false));
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileLabeledWirelessTransceiver();
    }

    @Override
    public void onBlockPlacedBy(
            World worldIn,
            BlockPos pos,
            IBlockState state,
            EntityLivingBase placer,
            ItemStack stack) {
        if (worldIn.isRemote) {
            return;
        }

        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (!(tileEntity instanceof TileLabeledWirelessTransceiver)) {
            return;
        }

        TileLabeledWirelessTransceiver transceiver = (TileLabeledWirelessTransceiver) tileEntity;
        if (placer != null) {
            transceiver.setOwner(placer.getUniqueID(), placer.getName());
        }
        if (stack.hasDisplayName()) {
            transceiver.setLabel(stack.getDisplayName());
        }
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
        if (!(tileEntity instanceof TileLabeledWirelessTransceiver)) {
            return false;
        }

        playerIn.openGui(
                ExampleMod.INSTANCE,
                ModGuiHandler.GUI_LABELED_WIRELESS_TRANSCEIVER,
                worldIn,
                pos.getX(),
                pos.getY(),
                pos.getZ());
        return true;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty<?>[]{STATE});
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(STATE, meta > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STATE) ? 1 : 0;
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
}
