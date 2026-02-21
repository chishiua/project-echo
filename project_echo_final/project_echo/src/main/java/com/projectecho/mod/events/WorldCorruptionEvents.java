package com.projectecho.mod.events;

import com.projectecho.mod.ProjectEcho;
import com.projectecho.mod.entity.custom.NullPointerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Mod.EventBusSubscriber(modid = ProjectEcho.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldCorruptionEvents {

    private static boolean isCorrupted = false;
    private static int corruptionTimer = 0;
    private static final Random RANDOM = new Random();
    private static final Set<BlockPos> corruptedZones = new HashSet<>();

    public static void startCorruption(ServerWorld world) {
        isCorrupted = true;
        corruptionTimer = 0;
        corruptedZones.clear();
    }

    public static void freezeAnimals(ServerWorld world, BlockPos center, int radius) {
        List<AnimalEntity> animals = world.getEntitiesOfClass(AnimalEntity.class,
                new net.minecraft.util.math.AxisAlignedBB(
                        center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                        center.getX() + radius, center.getY() + radius, center.getZ() + radius));
        for (AnimalEntity animal : animals) {
            // Замораживаем животное — делаем невидимым и неподвижным
            animal.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 200, 255, false, false));
            animal.addEffect(new EffectInstance(Effects.BLINDNESS, 200, 0, false, false));
            // Делаем "статуей" — белый цвет
            animal.setGlowingTag(true);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!(event.world instanceof ServerWorld)) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (!isCorrupted) return;

        ServerWorld world = (ServerWorld) event.world;
        corruptionTimer++;

        // Гравитация ломается: блоки песка и гравия летят вверх
        if (corruptionTimer % 40 == 0) {
            for (PlayerEntity player : world.players()) {
                BlockPos pos = player.blockPosition();
                for (int i = 0; i < 5; i++) {
                    int x = pos.getX() + RANDOM.nextInt(30) - 15;
                    int z = pos.getZ() + RANDOM.nextInt(30) - 15;
                    for (int y = 60; y < 90; y++) {
                        BlockPos checkPos = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(checkPos);
                        if (state.is(Blocks.SAND) || state.is(Blocks.GRAVEL)) {
                            // Блок "улетает" вверх
                            world.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                            world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                    x + 0.5, y + 0.5, z + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
                            break;
                        }
                    }
                }
            }
        }

        // Заражение: "битые чанки" расширяются
        if (corruptionTimer % 200 == 0) {
            for (PlayerEntity player : world.players()) {
                BlockPos corruptPos = player.blockPosition()
                        .offset(RANDOM.nextInt(40) - 20, 0, RANDOM.nextInt(40) - 20);
                corruptedZones.add(corruptPos);

                // Сообщение
                if (player instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntity) player).displayClientMessage(
                            new StringTextComponent(TextFormatting.DARK_RED +
                                    "[CORRUPTION] Sector " + corruptPos.getX() + "," + corruptPos.getZ() +
                                    " marked for deletion."), false);
                }
            }
        }

        // Урон в заражённых зонах
        if (corruptionTimer % 60 == 0) {
            for (PlayerEntity player : world.players()) {
                for (BlockPos zone : corruptedZones) {
                    double dist = player.blockPosition().distSqr(zone);
                    if (dist < 100) { // 10 блоков
                        player.hurt(net.minecraft.util.DamageSource.MAGIC, 1.0F);
                        player.addEffect(new EffectInstance(Effects.BLINDNESS, 40, 0, false, false));
                        if (player instanceof ServerPlayerEntity) {
                            ((ServerPlayerEntity) player).displayClientMessage(
                                    new StringTextComponent(TextFormatting.RED +
                                            "[DAMAGE] Corrupted zone active."), true);
                        }
                        break;
                    }
                }
            }
        }

        // Туман — постоянная слепота лёгкая
        if (corruptionTimer % 100 == 0) {
            for (PlayerEntity player : world.players()) {
                player.addEffect(new EffectInstance(Effects.BLINDNESS, 120, 0, false, false));
            }
        }
    }

    public static boolean isWorldCorrupted() { return isCorrupted; }
}
