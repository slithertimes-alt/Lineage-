package com.lineage;

import com.lineage.ability.Abilities;
import com.lineage.network.Network;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Lineage.MODID)
public class Lineage {
    public static final String MODID = "lineage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Lineage() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::setup);
        Abilities.registerBuiltins();
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(Network::register);
    }
}
