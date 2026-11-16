package com.wztwzt.voidfabricator.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import com.wztwzt.voidfabricator.tile.TileVoidFabricator;

public class BlockVoidFabricator extends BlockContainer {

    public BlockVoidFabricator() {
        super(Material.iron);
        setBlockName("voidfabricator.void_fabricator");
        setBlockTextureName("voidfabricator:void_fabricator");
        setCreativeTab(CreativeTabs.tabMisc);
        setHardness(3.5F);
        setResistance(100.0F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileVoidFabricator();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote && placer instanceof EntityPlayer player) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileVoidFabricator fabricator) {
                fabricator.bindOwner(player.getUniqueID());
            }
        }
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileVoidFabricator fabricator) {
            return fabricator.onUse(player);
        }
        return false;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileVoidFabricator fabricator && fabricator.hasPendingOutputs()) {
            if (!world.isRemote && player != null) {
                player.addChatMessage(new ChatComponentTranslation("voidfabricator.chat.buffered_break"));
            }
            return false;
        }
        return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileVoidFabricator fabricator && fabricator.hasPendingOutputs()) {
            return;
        }
        super.onBlockExploded(world, x, y, z, explosion);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileVoidFabricator fabricator) {
            fabricator.dropContents();
        }
        super.breakBlock(world, x, y, z, block, metadata);
    }
}
