package com.wztwzt.voidfabricator;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Void Fabricator standalone mod entry point.
 *
 * <p>
 * This project is intentionally independent from AE2-QoL (modId {@code ae2_qof}).
 * It must not reference or embed AE2-QoL classes. Runtime integration follows
 * the AE-native Provider/Medium design in {@code docs/DESIGN_void_fabricator.md}.
 */
@Mod(
    modid = VoidFabricator.MODID,
    version = Tags.VERSION,
    name = "Void Fabricator",
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:appliedenergistics2;required-after:gregtech;required-after:modularui2;required-after:modularui")
public class VoidFabricator {

    public static final String MODID = "voidfabricator";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static Block voidFabricatorBlock;
    public static Item machineRecipePoolPattern;

    @SidedProxy(
        clientSide = "com.wztwzt.voidfabricator.ClientProxy",
        serverSide = "com.wztwzt.voidfabricator.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        LOG.info("[VoidFabricator] preInit completed; block, pool pattern and AE tile integration registered");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
