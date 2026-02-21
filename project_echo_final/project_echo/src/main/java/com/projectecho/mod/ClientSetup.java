package com.projectecho.mod;

import com.projectecho.mod.entity.NullPointerRenderer;
import com.projectecho.mod.init.ModEntityTypes;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientSetup {

    public static void onClientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntityTypes.NULL_POINTER.get(),
                NullPointerRenderer::new);
    }
}
