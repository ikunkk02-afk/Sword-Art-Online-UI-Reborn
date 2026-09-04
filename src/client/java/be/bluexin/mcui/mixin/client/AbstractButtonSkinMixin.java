/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.mixin.client;

import be.bluexin.mcui.screens.SaoUiStyle;
import be.bluexin.mcui.screens.SaoScreenPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonSkinMixin extends AbstractWidget {
    protected AbstractButtonSkinMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void mcui$renderSaoButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (!SaoScreenPolicy.shouldSkinWidgets(client.screen)) return;
        SaoUiStyle.renderButton(
            graphics,
            getX(),
            getY(),
            getWidth(),
            getHeight(),
            getMessage(),
            isHovered(),
            isFocused(),
            isHovered() && client.mouseHandler.isLeftPressed(),
            active,
            false,
            alpha,
            null,
            false
        );
        ci.cancel();
    }
}
