package com.projectecho.mod.entity.custom;

import com.projectecho.mod.ProjectEcho;
import com.projectecho.mod.init.ModSounds;
import com.projectecho.mod.init.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.List;

public class NullPointerEntity extends CreatureEntity {

    // Стадии конфликта
    public static final int STAGE_IDLE = 0;
    public static final int STAGE_WARNING = 1;    // 1-5 ударов
    public static final int STAGE_LEAK = 2;        // 6-15 ударов
    public static final int STAGE_COLLAPSE = 3;    // смерть

    private static final DataParameter<Integer> HIT_COUNT =
            EntityDataManager.defineId(NullPointerEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> SYNC_LEVEL =
            EntityDataManager.defineId(NullPointerEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> STAGE =
            EntityDataManager.defineId(NullPointerEntity.class, DataSerializers.INT);

    // Позиция последней "починки" — для механики синхронизации
    private BlockPos repairTarget = null;
    private int repairCooldown = 0;
    private int teleportCooldown = 0;
    private int ambientTimer = 0;

    public NullPointerEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        this.setCustomName(new StringTextComponent("NULL_POINTER"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.xpReward = 0; // Не даёт опыт — он не враг
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HIT_COUNT, 0);
        this.entityData.define(SYNC_LEVEL, 0);
        this.entityData.define(STAGE, STAGE_IDLE);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return CreatureEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D) // не ходит обычно
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D); // нельзя отбросить
    }

    @Override
    public void tick() {
        super.tick();

        int stage = getStage();
        int hits = getHitCount();

        // Обновляем стадию по ударам
        if (!level.isClientSide) {
            if (hits >= 1 && hits <= 5 && stage < STAGE_WARNING) {
                setStage(STAGE_WARNING);
                broadcastWarning("[WARNING] Core entity integrity compromised.");
            } else if (hits >= 6 && hits <= 15 && stage < STAGE_LEAK) {
                setStage(STAGE_LEAK);
                broadcastWarning("[ERROR] Data leak detected. World corruption initiated...");
                startLeakEffects();
            }
        }

        // Телепортация рывками (только когда игрок не смотрит)
        teleportCooldown++;
        if (!level.isClientSide && teleportCooldown >= 60) {
            teleportCooldown = 0;
            blinkMovement();
        }

        // Логика "починки" мира
        repairCooldown++;
        if (!level.isClientSide && repairCooldown >= 100) {
            repairCooldown = 0;
            findAndRepairBlock();
        }

        // Эффекты частиц
        if (level.isClientSide) {
            spawnEntityParticles(stage);
        }

        // Атмосферные звуки
        ambientTimer++;
        if (!level.isClientSide && ambientTimer >= 200) {
            ambientTimer = 0;
            level.playSound(null, blockPosition(), ModSounds.NULL_AMBIENT.get(),
                    SoundCategory.HOSTILE, 1.0F, 1.0F);
        }
    }

    // Рывок когда игрок не смотрит
    private void blinkMovement() {
        List<PlayerEntity> players = level.getEntitiesOfClass(PlayerEntity.class,
                getBoundingBox().inflate(30.0));

        boolean anyoneWatching = false;
        for (PlayerEntity player : players) {
            // Проверяем смотрит ли игрок в нашу сторону
            double dx = getX() - player.getX();
            double dz = getZ() - player.getZ();
            double angle = Math.atan2(dz, dx);
            double playerAngle = Math.toRadians(player.yRot + 90);
            double diff = Math.abs(angle - playerAngle) % (Math.PI * 2);
            if (diff < 0.7) { // ~40 градусов
                anyoneWatching = true;
                break;
            }
        }

        if (!anyoneWatching && getStage() < STAGE_COLLAPSE) {
            // Рывок к случайной позиции рядом
            double newX = getX() + (random.nextDouble() - 0.5) * 20;
            double newZ = getZ() + (random.nextDouble() - 0.5) * 20;
            BlockPos target = new BlockPos(newX, getY(), newZ);
            // Убедимся что блок существует
            if (level.getBlockState(target.below()).isSolidRender(level, target.below())) {
                teleportTo(newX, getY(), newZ);
                level.playSound(null, blockPosition(), ModSounds.NULL_BLINK.get(),
                        SoundCategory.HOSTILE, 0.5F, 1.0F);
            }
        }
    }

    // Починка блоков рядом
    private void findAndRepairBlock() {
        // Ищем "повреждённые" блоки (воздух там, где должна быть трава, земля)
        BlockPos pos = blockPosition();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                BlockPos above = checkPos.above();
                // Если сверху воздух, а снизу земля без травы — "чиним"
                if (level.getBlockState(checkPos).is(Blocks.DIRT) &&
                        level.getBlockState(above).isAir()) {
                    repairTarget = checkPos;
                    // Двигаемся к цели
                    teleportTo(checkPos.getX() + 0.5, checkPos.getY() + 1, checkPos.getZ() + 0.5);
                    ((ServerWorld)level).setBlock(checkPos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    level.playSound(null, checkPos, ModSounds.NULL_REPAIR.get(),
                            SoundCategory.BLOCKS, 0.8F, 1.0F);
                    return;
                }
            }
        }
    }

    // Игрок помог починить — увеличиваем синхронизацию
    public void onPlayerHelped(PlayerEntity player) {
        int sync = getSyncLevel();
        sync = Math.min(100, sync + 10);
        setSyncLevel(sync);

        // Сообщение игроку
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity sp = (ServerPlayerEntity) player;
            sp.displayClientMessage(
                    new StringTextComponent(TextFormatting.AQUA + "[SYNC] Synchronization: " + sync + "%"),
                    true);

            // Финал при 100%
            if (sync >= 100) {
                triggerGoodEnding(sp);
            }
        }
        level.playSound(null, blockPosition(), ModSounds.NULL_SYNC.get(),
                SoundCategory.HOSTILE, 1.0F, 1.0F);
    }

    // Хороший финал — Ключ Доступа
    private void triggerGoodEnding(ServerPlayerEntity player) {
        player.displayClientMessage(
                new StringTextComponent(TextFormatting.GREEN +
                        "[SYSTEM] Synchronization complete. Access Key granted."), false);
        player.displayClientMessage(
                new StringTextComponent(TextFormatting.GREEN +
                        "The Watcher thanks you. The world is now yours to protect."), false);

        // Выдаём Ключ Доступа
        ItemStack key = new ItemStack(ModItems.ACCESS_KEY.get());
        player.inventory.add(key);

        // Световой взрыв
        if (level instanceof ServerWorld) {
            ((ServerWorld) level).sendParticles(ParticleTypes.END_ROD,
                    getX(), getY() + 1, getZ(), 200, 1, 1, 1, 0.3);
        }
        level.playSound(null, blockPosition(), ModSounds.NULL_ASCEND.get(),
                SoundCategory.HOSTILE, 2.0F, 1.0F);

        // Сущность исчезает красиво
        remove();
    }

    // Плохой финал — коллапс
    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        // Ничего не дропает
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!level.isClientSide) {
            triggerBadEnding();
        }
    }

    private void triggerBadEnding() {
        // Белый экран через сообщение всем игрокам
        List<PlayerEntity> players = level.getEntitiesOfClass(PlayerEntity.class,
                getBoundingBox().inflate(100));
        for (PlayerEntity p : players) {
            if (p instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity) p).displayClientMessage(
                        new StringTextComponent(TextFormatting.WHITE +
                                "                                                  "), false);
            }
        }

        // Запускаем коррупцию мира
        level.getServer().getWorldData().getGameRuleValue(GameRules.RULE_DOMOBSPAWNING);
        WorldCorruptionEvents.startCorruption((ServerWorld) level);

        // Финальное сообщение
        for (PlayerEntity p : players) {
            if (p instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity) p).displayClientMessage(
                        new StringTextComponent(TextFormatting.DARK_RED +
                                "[FATAL] Core process terminated. Initiating world deletion..."), false);
            }
        }
        level.playSound(null, blockPosition(), ModSounds.NULL_COLLAPSE.get(),
                SoundCategory.HOSTILE, 2.0F, 1.0F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(source.getEntity() instanceof PlayerEntity)) return false;

        boolean result = super.hurt(source, amount);
        if (result) {
            int hits = getHitCount() + 1;
            setHitCount(hits);

            PlayerEntity player = (PlayerEntity) source.getEntity();

            // Реакции по стадиям
            if (hits == 1) {
                // Останавливается и смотрит
                this.getLookControl().setLookAt(player, 180, 180);
                if (player instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntity) player).displayClientMessage(
                            new StringTextComponent(TextFormatting.GRAY +
                                    "[NOTICE] Anomaly detected in sector."), true);
                }
            } else if (hits == 5) {
                if (player instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntity) player).displayClientMessage(
                            new StringTextComponent(TextFormatting.YELLOW +
                                    "[WARNING] Core entity integrity compromised."), false);
                }
                level.playSound(null, blockPosition(), ModSounds.NULL_WARNING.get(),
                        SoundCategory.HOSTILE, 1.5F, 1.0F);
            } else if (hits == 10) {
                if (player instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntity) player).displayClientMessage(
                            new StringTextComponent(TextFormatting.RED +
                                    "[ERROR] Data leak detected. Structural collapse imminent."), false);
                }
                // Накладываем эффекты на игрока
                player.addEffect(new EffectInstance(Effects.BLINDNESS, 60, 0));
                player.addEffect(new EffectInstance(Effects.CONFUSION, 200, 1));
            } else if (hits == 15) {
                if (player instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntity) player).displayClientMessage(
                            new StringTextComponent(TextFormatting.DARK_RED +
                                    "[CRITICAL] Cannot stop cascade. Final warning."), false);
                }
            }
        }
        return result;
    }

    private void startLeakEffects() {
        // Замораживаем животных рядом — через WorldCorruptionEvents
        if (level instanceof ServerWorld) {
            WorldCorruptionEvents.freezeAnimals((ServerWorld) level, blockPosition(), 24);
        }
    }

    private void spawnEntityParticles(int stage) {
        // Всегда: белые статические частицы
        for (int i = 0; i < 5; i++) {
            double x = getX() + (random.nextDouble() - 0.5) * 1.2;
            double y = getY() + random.nextDouble() * 2.0;
            double z = getZ() + (random.nextDouble() - 0.5) * 1.2;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.02, 0);
        }

        // При утечке: чёрные искры разлетаются
        if (stage >= STAGE_LEAK) {
            for (int i = 0; i < 8; i++) {
                double x = getX() + (random.nextDouble() - 0.5) * 3;
                double y = getY() + random.nextDouble() * 2.0;
                double z = getZ() + (random.nextDouble() - 0.5) * 3;
                double vx = (random.nextDouble() - 0.5) * 0.3;
                double vz = (random.nextDouble() - 0.5) * 0.3;
                level.addParticle(ParticleTypes.SMOKE, x, y, z, vx, 0.1, vz);
            }
        }
    }

    private void broadcastWarning(String message) {
        if (level instanceof ServerWorld) {
            ServerWorld sw = (ServerWorld) level;
            sw.players().forEach(p ->
                    p.displayClientMessage(
                            new StringTextComponent(TextFormatting.GRAY + message), false));
        }
    }

    // Getters/Setters
    public int getHitCount() { return this.entityData.get(HIT_COUNT); }
    public void setHitCount(int count) { this.entityData.set(HIT_COUNT, count); }
    public int getSyncLevel() { return this.entityData.get(SYNC_LEVEL); }
    public void setSyncLevel(int level) { this.entityData.set(SYNC_LEVEL, level); }
    public int getStage() { return this.entityData.get(STAGE); }
    public void setStage(int stage) { this.entityData.set(STAGE, stage); }
    public BlockPos getRepairTarget() { return repairTarget; }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        setHitCount(nbt.getInt("HitCount"));
        setSyncLevel(nbt.getInt("SyncLevel"));
        setStage(nbt.getInt("Stage"));
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("HitCount", getHitCount());
        nbt.putInt("SyncLevel", getSyncLevel());
        nbt.putInt("Stage", getStage());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.NULL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.NULL_COLLAPSE.get();
    }

    @Override
    protected float getSoundVolume() { return 2.0F; }

    @Override
    public boolean canBeLeashed(PlayerEntity player) { return false; }
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.FALL || source == DamageSource.DROWN
                || source.isFire() || source == DamageSource.IN_WALL;
    }
}
