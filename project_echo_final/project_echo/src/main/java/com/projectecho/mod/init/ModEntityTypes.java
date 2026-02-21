package com.projectecho.mod.init;

import com.projectecho.mod.ProjectEcho;
import com.projectecho.mod.entity.custom.NullPointerEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, ProjectEcho.MOD_ID);

    public static final RegistryObject<EntityType<NullPointerEntity>> NULL_POINTER =
            ENTITY_TYPES.register("null_pointer",
                    () -> EntityType.Builder.<NullPointerEntity>of(
                                    NullPointerEntity::new, EntityClassification.MISC)
                            .sized(0.7f, 2.0f)
                            .build("null_pointer"));
}
