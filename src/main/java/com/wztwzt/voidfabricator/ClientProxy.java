package com.wztwzt.voidfabricator;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;

/**
 * Client-side lifecycle hooks.
 *
 * <p>
 * GUI, TESR and client-side registration will be added here later. This class
 * must remain isolated from server-only classes.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        if (Loader.isModLoaded("NotEnoughItems")) {
            FMLCommonHandler.instance()
                .bus()
                .register(new com.wztwzt.voidfabricator.client.ClientKeyHandler());
            com.wztwzt.voidfabricator.client.VoidPatternViewHandler.register();
        }
    }
}
