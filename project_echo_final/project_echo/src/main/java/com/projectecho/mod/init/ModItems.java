package com.projectecho.mod.init;

import com.projectecho.mod.ProjectEcho;
import com.projectecho.mod.items.AccessKeyItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ProjectEcho.MOD_ID);

    public static final RegistryObject<Item> ACCESS_KEY =
            ITEMS.register("access_key",
                    () -> new AccessKeyItem(new Item.Properties()
                            .tab(ItemGroup.TAB_MISC)
                            .stacksTo(1)));
}
