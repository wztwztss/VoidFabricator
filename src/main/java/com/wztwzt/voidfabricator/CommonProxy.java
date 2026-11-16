package com.wztwzt.voidfabricator;

import com.wztwzt.voidfabricator.block.BlockVoidFabricator;
import com.wztwzt.voidfabricator.item.ItemBlockVoidFabricator;
import com.wztwzt.voidfabricator.item.ItemMachineRecipePoolPattern;
import com.wztwzt.voidfabricator.tile.TileVoidFabricator;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/** Server-side and common lifecycle hooks. Keep this class free of client-only imports. */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        VoidFabricator.voidFabricatorBlock = new BlockVoidFabricator();
        GameRegistry
            .registerBlock(VoidFabricator.voidFabricatorBlock, ItemBlockVoidFabricator.class, "void_fabricator");

        VoidFabricator.machineRecipePoolPattern = new ItemMachineRecipePoolPattern();
        GameRegistry.registerItem(VoidFabricator.machineRecipePoolPattern, "machine_recipe_pool_pattern");

        GameRegistry.registerTileEntity(TileVoidFabricator.class, "voidfabricator:void_fabricator");
    }

    public void init(FMLInitializationEvent event) {
        // Recipe registration is intentionally deferred until the execution path is runtime-tested.
    }

    public void postInit(FMLPostInitializationEvent event) {
        // Dynamic machine adapters are added after the generic RecipeMap adapter.
    }
}
