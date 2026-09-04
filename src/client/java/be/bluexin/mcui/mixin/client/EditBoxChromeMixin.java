/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.mixin.client;

import be.bluexin.mcui.screens.SaoUiStyle;
import be.bluexin.mcui.screens.SaoScreenPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class EditBoxChromeMixin extends AbstractWidget {
    protected EditBoxChromeMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Redirect(
        method = "renderWidget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
        )
    )
    private void mcui$renderSaoEditBoxBackground(
        GuiGraphics graphics,
        ResourceLocation sprite,
        int x,
        int y,
        int width,
        int height
    ) {
        if (SaoScreenPolicy.shouldSkinWidgets(Minecraft.getInstance().screen)) {
            SaoUiStyle.renderEditBoxBackground(graphics, x, y, width, height, active);
        } else {
            graphics.blitSprite(sprite, x, y, width, height);
        }
    }

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void mcui$renderSaoEditBoxChrome(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!SaoScreenPolicy.shouldSkinWidgets(Minecraft.getInstance().screen)) return;
        SaoUiStyle.renderEditBoxChrome(
            graphics,
            getX(),
            getY(),
            getWidth(),
            getHeight(),
            isHovered(),
            isFocused(),
            active
        );
    }
}
