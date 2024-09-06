package me.kalypsoexists.vpbpackager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(Common.MOD_ID)
public class ForgeCore {
    
    public ForgeCore() {

        Common.LOG.info("Loading VPB Packager (Forge)");
        Common.init(FMLPaths.GAMEDIR.get());

    }


}