package com.projectecho.mod.events;

import com.projectecho.mod.ProjectEcho;
import com.projectecho.mod.entity.custom.NullPointerEntity;
import com.projectecho.mod.init.ModEntityTypes;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = ProjectEcho.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NullPointerEvents {

    // Игрок ставит блок — проверяем, помогает ли он NULL_POINTER
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        if (event.getWorld().isClientSide()) return;

        PlayerEntity player = (PlayerEntity) event.getEntity();
        World world = player.level;
        BlockPos placedPos = event.getPos();

        // Ищем NULL_POINTER рядом
        List<NullPointerEntity> nullPointers = world.getEntitiesOfClass(
                NullPointerEntity.class,
                player.getBoundingBox().inflate(20.0));

        for (NullPointerEntity entity : nullPointers) {
            BlockPos repairTarget = entity.getRepairTarget();
            if (repairTarget != null && repairTarget.distSqr(placedPos) < 9) {
                // Игрок поставил блок рядом с целью починки!
                entity.onPlayerHelped(player);
                if (player instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntity) player).displayClientMessage(
                            new StringTextComponent(TextFormatting.AQUA +
                                    "[SYNC] You assisted the Watcher. Synchronization increasing..."), false);
                }
            }
        }
    }

    // Игрок ломает блок — NULL_POINTER "замечает" это
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isClientSide()) return;

        PlayerEntity player = event.getPlayer();
        if (player == null) return;

        World world = player.level;

        List<NullPointerEntity> nullPointers = world.getEntitiesOfClass(
                NullPointerEntity.class,
                player.getBoundingBox().inflate(30.0));

        for (NullPointerEntity entity : nullPointers) {
            // Если сущность спокойна и видит, что игрок ломает — "наблюдает"
            if (entity.getStage() == NullPointerEntity.STAGE_IDLE) {
                if (player instanceof ServerPlayerEntity) {
                    // Изредка — предупреждение
                    if (world.random.nextInt(10) == 0) {
                        ((ServerPlayerEntity) player).displayClientMessage(
                                new StringTextComponent(TextFormatting.DARK_GRAY +
                                        "[NOTICE] The Watcher observes your actions."), true);
                    }
                }
            }
        }
    }
}
