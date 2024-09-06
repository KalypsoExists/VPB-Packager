package me.kalypsoexists.vpbpackager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;


public class FabricCore implements ModInitializer {
    
    @Override
    public void onInitialize() {

        Common.LOG.info("Loading VPB Packager (Fabric)");
        Common.init(FabricLoader.getInstance().getGameDir());

    }
}
