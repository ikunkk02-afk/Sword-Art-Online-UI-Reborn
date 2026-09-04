/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.mixin.client;

import be.bluexin.mcui.screens.SaoUiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Inject(
        method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void mcui$renderSaoMenuBackground(
        GuiGraphics graphics,
        int left,
        int top,
        int right,
        int bottom,
        CallbackInfo ci
    ) {
        SaoUiStyle.renderMenuBackground(graphics, left, top, right, bottom);
        ci.cancel();
    }
}
