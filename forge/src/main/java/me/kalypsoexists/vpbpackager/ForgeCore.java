package me.kalypsoexists.vpbpackager;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(Common.MOD_ID)
public class ForgeCore {
    
    public ForgeCore() {
        Common.LOG.info("Loading VPB Packager (Forge)");
        Common.init(FMLPaths.GAMEDIR.get());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onLoadComplete);
    }

    @SubscribeEvent
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        Common.loadComplete();
    }


}