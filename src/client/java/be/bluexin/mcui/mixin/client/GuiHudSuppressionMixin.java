/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.mixin.client;

import be.bluexin.mcui.fabric.client.hud.VanillaHudPolicy;
import be.bluexin.mcui.themes.HudPartType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fine-grained gates for vanilla elements that Fabric groups in HOTBAR_AND_BARS. */
@Mixin(Gui.class)
public abstract class GuiHudSuppressionMixin {
    @Shadow
    private static void renderArmor(
        GuiGraphics graphics,
        Player player,
        int y,
        int heartRows,
        int rowHeight,
        int x
    ) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void renderHearts(
        GuiGraphics graphics,
        Player player,
        int x,
        int y,
        int rowHeight,
        int regenerationHeart,
        float maxHealth,
        int health,
        int displayHealth,
        int absorption,
        boolean blinking
    );

    @Shadow
    protected abstract void renderFood(GuiGraphics graphics, Player player, int y, int x);

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void mcui$suppressCrosshair(GuiGraphics graphics, DeltaTracker ticks, CallbackInfo ci) {
        if (VanillaHudPolicy.suppresses(HudPartType.CROSS_HAIR)) ci.cancel();
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void mcui$suppressEffects(GuiGraphics graphics, DeltaTracker ticks, CallbackInfo ci) {
        if (VanillaHudPolicy.suppresses(HudPartType.EFFECTS)) ci.cancel();
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void mcui$suppressExperienceLevel(GuiGraphics graphics, DeltaTracker ticks, CallbackInfo ci) {
        if (VanillaHudPolicy.suppresses(HudPartType.EXPERIENCE)) ci.cancel();
    }

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void mcui$suppressHotbar(GuiGraphics graphics, DeltaTracker ticks, CallbackInfo ci) {
        if (VanillaHudPolicy.suppresses(HudPartType.HOTBAR)) ci.cancel();
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void mcui$suppressExperienceBar(GuiGraphics graphics, int x, CallbackInfo ci) {
        if (VanillaHudPolicy.suppresses(HudPartType.EXPERIENCE)) ci.cancel();
    }

    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    private void mcui$suppressJumpMeter(
        PlayerRideableJumping mount,
        GuiGraphics graphics,
        int x,
        CallbackInfo ci
    ) {
        if (VanillaHudPolicy.suppresses(HudPartType.JUMP_BAR)) ci.cancel();
    }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void mcui$suppressVehicleHealth(GuiGraphics graphics, CallbackInfo ci) {
        if (VanillaHudPolicy.suppresses(HudPartType.MOUNT_HEALTH)) ci.cancel();
    }

    @Redirect(
        method = "renderPlayerHealth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderArmor(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIII)V"
        )
    )
    private static void mcui$renderArmorWhenVanillaEnabled(
        GuiGraphics graphics,
        Player player,
        int y,
        int heartRows,
        int rowHeight,
        int x
    ) {
        if (!VanillaHudPolicy.suppresses(HudPartType.ARMOR)) {
            renderArmor(graphics, player, y, heartRows, rowHeight, x);
        }
    }

    @Redirect(
        method = "renderPlayerHealth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"
        )
    )
    private void mcui$renderHeartsWhenVanillaEnabled(
        Gui instance,
        GuiGraphics graphics,
        Player player,
        int x,
        int y,
        int rowHeight,
        int regenerationHeart,
        float maxHealth,
        int health,
        int displayHealth,
        int absorption,
        boolean blinking
    ) {
        if (!VanillaHudPolicy.suppresses(HudPartType.HEALTH_BOX)) {
            renderHearts(
                graphics,
                player,
                x,
                y,
                rowHeight,
                regenerationHeart,
                maxHealth,
                health,
                displayHealth,
                absorption,
                blinking
            );
        }
    }

    @Redirect(
        method = "renderPlayerHealth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderFood(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;II)V"
        )
    )
    private void mcui$renderFoodWhenVanillaEnabled(
        Gui instance,
        GuiGraphics graphics,
        Player player,
        int y,
        int x
    ) {
        if (!VanillaHudPolicy.suppresses(HudPartType.FOOD)) {
            renderFood(graphics, player, y, x);
        }
    }

    @Redirect(
        method = "renderPlayerHealth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
        ),
        require = 2
    )
    private void mcui$renderAirWhenVanillaEnabled(
        GuiGraphics graphics,
        ResourceLocation sprite,
        int x,
        int y,
        int width,
        int height
    ) {
        if (!VanillaHudPolicy.suppresses(HudPartType.AIR)) {
            graphics.blitSprite(sprite, x, y, width, height);
        }
    }
}
