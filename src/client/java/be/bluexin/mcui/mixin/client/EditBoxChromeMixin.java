/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.mixin.client;

import be.bluexin.mcui.screens.SaoUiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class EditBoxChromeMixin extends AbstractWidget {
    protected EditBoxChromeMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void mcui$renderSaoEditBoxChrome(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        SaoUiStyle.renderEditBoxChrome(graphics, getX(), getY(), getWidth(), getHeight(), isFocused());
    }
}
