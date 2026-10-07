package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class Simpledash implements ClientModInitializer {
    // Регистрируем кнопку. По умолчанию Правый Shift
    public static KeyBinding dashKey = new KeyBinding("key.simpledash.activate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.simpledash");
    public static boolean autoDash = true;
    public static boolean maceHelper = true;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity player = client.player;
            if (player == null) return;

            // Переключение режимов на Правый Shift
            if (dashKey.wasPressed() && client.currentScreen == null) {
                autoDash = !autoDash;
                maceHelper = !maceHelper;
                player.sendMessage(net.minecraft.text.Text.of("§a[SimpleDash] Функции переключены!"), true);
            }

            // Авто-рывок копьем при зажатии ЛКМ (клике по воздуху)
            if (autoDash && client.options.attackKey.isPressed()) {
                executeTridentDash(player);
            }

            // Функции для булавы (Mace)
            if (maceHelper && player.getMainHandStack().isOf(Items.MACE)) {
                PlayerEntity target = getClosestTarget(client, player, 6.0);
                if (target != null) {
                    
                    // 1. Авто-наводка в прыжке/падении сверху
                    if (player.fallDistance > 0 && !player.isOnGround()) {
                        aimAtTarget(player, target);
                    }

                    // 2. Если враг поднял щит — моментально бьем топором за 0 тиков
                    if (target.isUsingItem() && (target.getOffHandStack().isOf(Items.SHIELD) || target.getMainHandStack().isOf(Items.SHIELD))) {
                        executeShieldBreak(client, player);
                    } 
                    // 3. Автоматический крит булавой (Triggerbot) точно на дистанции удара
                    else if (player.fallDistance > 0.1 && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                        if (player.isInAttackRange(target)) {
                            client.interactionManager.attackEntity(player, target);
                            player.swingHand(Hand.MAIN_HAND);
                        }
                    }
                }
            }
        });
    }

    private static void executeTridentDash(PlayerEntity player) {
        if (player.getMainHandStack().isOf(Items.TRIDENT)) return;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.TRIDENT)) {
                int oldSlot = player.getInventory().selectedSlot;
                player.getInventory().selectedSlot = i;
                player.setCurrentHand(Hand.MAIN_HAND);
                player.getInventory().selectedSlot = oldSlot;
                break;
            }
        }
    }

    private static void aimAtTarget(PlayerEntity player, PlayerEntity target) {
        Vec3d targetPos = target.getEyePos(); Vec3d playerPos = player.getEyePos();
        double diffX = targetPos.x - playerPos.x; double diffY = targetPos.y - playerPos.y; double diffZ = targetPos.z - playerPos.z;
        double diffXZ = MathHelper.sqrt((float) (diffX * diffX + diffZ * diffZ));
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX))) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        player.setYaw(player.getYaw() + MathHelper.wrapDegrees(yaw - player.getYaw()) * 0.4F);
        player.setPitch(player.getPitch() + MathHelper.wrapDegrees(pitch - player.getPitch()) * 0.4F);
    }

    private static void executeShieldBreak(MinecraftClient client, PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem().toString().contains("axe") && !stack.getItem().toString().contains("pickaxe")) {
                int oldSlot = player.getInventory().selectedSlot;
                player.getInventory().selectedSlot = i;
                if (client.crosshairTarget instanceof EntityHitResult) {
                    client.interactionManager.attackEntity(player, ((EntityHitResult) client.crosshairTarget).getEntity());
                    player.swingHand(Hand.MAIN_HAND);
                }
                player.getInventory().selectedSlot = oldSlot;
                break;
            }
        }
    }

    private static PlayerEntity getClosestTarget(MinecraftClient client, PlayerEntity player, double range) {
        List<PlayerEntity> players = client.world.getPlayers();
        PlayerEntity closest = null; double closestDist = range;
        for (PlayerEntity p : players) {
            if (p != player && !p.isDead() && !p.isInvisible()) {
                double dist = player.distanceTo(p);
                if (dist < closestDist) { closestDist = dist; closest = p; }
            }
        }
        return closest;
    }
}
