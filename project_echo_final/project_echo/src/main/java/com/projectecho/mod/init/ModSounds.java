package com.projectecho.mod.init;

import com.projectecho.mod.ProjectEcho;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ProjectEcho.MOD_ID);

    public static final RegistryObject<SoundEvent> NULL_AMBIENT  = reg("entity.null_pointer.ambient");
    public static final RegistryObject<SoundEvent> NULL_HURT     = reg("entity.null_pointer.hurt");
    public static final RegistryObject<SoundEvent> NULL_WARNING  = reg("entity.null_pointer.warning");
    public static final RegistryObject<SoundEvent> NULL_BLINK    = reg("entity.null_pointer.blink");
    public static final RegistryObject<SoundEvent> NULL_REPAIR   = reg("entity.null_pointer.repair");
    public static final RegistryObject<SoundEvent> NULL_SYNC     = reg("entity.null_pointer.sync");
    public static final RegistryObject<SoundEvent> NULL_COLLAPSE = reg("entity.null_pointer.collapse");
    public static final RegistryObject<SoundEvent> NULL_ASCEND   = reg("entity.null_pointer.ascend");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(ProjectEcho.MOD_ID, name)));
    }
}
