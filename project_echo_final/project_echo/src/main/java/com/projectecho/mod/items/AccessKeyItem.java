package com.projectecho.mod.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class AccessKeyItem extends Item {

    public AccessKeyItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            // Ключ Доступа — способности хранителя
            // 1. Останавливает время (ночной цикл)
            if (world instanceof ServerWorld) {
                ServerWorld sw = (ServerWorld) world;

                // Даём игроку сверхспособности Хранителя
                player.addEffect(new EffectInstance(Effects.NIGHT_VISION, 600, 0, false, false));
                player.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 600, 4, false, false));
                player.addEffect(new EffectInstance(Effects.SPEED, 600, 2, false, false));
                player.addEffect(new EffectInstance(Effects.SATURATION, 600, 5, false, false));

                // Приближает рассвет
                long time = sw.getDayTime() % 24000;
                if (time > 12000) {
                    sw.setDayTime(0); // Рассвет
                }

                // Восстанавливает мир вокруг игрока
                BlockPos pos = player.blockPosition();
                int restored = 0;
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockPos checkPos = pos.offset(x, 0, z);
                        if (sw.getBlockState(checkPos).is(net.minecraft.block.Blocks.DIRT)) {
                            sw.setBlock(checkPos, net.minecraft.block.Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                            restored++;
                        }
                    }
                }

                if (player instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntity) player).displayClientMessage(
                            new StringTextComponent(TextFormatting.GREEN +
                                    "[ACCESS KEY] Watcher abilities activated. " + restored + " blocks restored."), false);
                    ((ServerPlayerEntity) player).displayClientMessage(
                            new StringTextComponent(TextFormatting.AQUA +
                                    "You are now the Guardian of this world."), false);
                }

                // Частицы
                sw.sendParticles(net.minecraft.particles.ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1, player.getZ(),
                        50, 2, 1, 2, 0.1);
            }

            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }

        return ActionResult.sidedSuccess(stack, world.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Светится как зачарованный предмет
    }
}
