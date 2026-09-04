/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.mixin.client;

import be.bluexin.mcui.screens.SaoUiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenChromeMixin {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void mcui$renderSaoContainerChrome(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        SaoUiStyle.renderContainerChrome(graphics, leftPos, topPos, imageWidth, imageHeight);
    }
}
