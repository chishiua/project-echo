package com.projectecho.mod;

import com.projectecho.mod.init.ModEntityTypes;
import com.projectecho.mod.init.ModSounds;
import com.projectecho.mod.init.ModItems;
import com.projectecho.mod.events.NullPointerEvents;
import com.projectecho.mod.events.WorldCorruptionEvents;
import com.projectecho.mod.ClientSetup;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ProjectEcho.MOD_ID)
public class ProjectEcho {

    public static final String MOD_ID = "projectecho";
    public static final Logger LOGGER = LogManager.getLogger();

    public ProjectEcho() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(ClientSetup::onClientSetup);

        MinecraftForge.EVENT_BUS.register(new NullPointerEvents());
        MinecraftForge.EVENT_BUS.register(new WorldCorruptionEvents());
    }
}
