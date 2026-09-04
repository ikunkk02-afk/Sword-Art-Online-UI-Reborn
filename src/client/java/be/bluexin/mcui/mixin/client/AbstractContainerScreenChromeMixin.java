/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.mixin.client;

import be.bluexin.mcui.screens.SaoUiStyle;
import be.bluexin.mcui.screens.SaoScreenPolicy;
import be.bluexin.mcui.screens.SaoInventoryScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenChromeMixin extends Screen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Shadow protected int titleLabelX;
    @Shadow protected int titleLabelY;
    @Shadow protected int inventoryLabelX;
    @Shadow protected int inventoryLabelY;
    @Shadow @Final protected Component playerInventoryTitle;

    protected AbstractContainerScreenChromeMixin(Component title) {
        super(title);
    }

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void mcui$renderSaoContainerChrome(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!SaoScreenPolicy.shouldRenderContainerFrame(Minecraft.getInstance().screen)) return;
        SaoUiStyle.renderContainerChrome(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void mcui$renderSaoSlotChrome(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (!SaoScreenPolicy.shouldRenderContainerSlots(Minecraft.getInstance().screen) || !slot.isActive()) return;
        boolean equipment = Minecraft.getInstance().screen instanceof SaoInventoryScreen
            && ((slot.index >= 5 && slot.index <= 8) || slot.index == 45);
        SaoUiStyle.renderSlot(graphics, slot.x - 1, slot.y - 1, 18, equipment);
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void mcui$renderSaoContainerLabels(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!SaoScreenPolicy.shouldRenderContainerFrame(Minecraft.getInstance().screen)) return;
        Component fittedTitle = SaoUiStyle.fitText(title, Math.max(1, imageWidth - titleLabelX - 8));
        Component fittedInventory = SaoUiStyle.fitText(playerInventoryTitle, Math.max(1, imageWidth - inventoryLabelX - 8));
        graphics.drawString(font, fittedTitle, titleLabelX, titleLabelY, SaoUiStyle.getTITLE(), false);
        graphics.drawString(font, fittedInventory, inventoryLabelX, inventoryLabelY, SaoUiStyle.getLIGHT_TEXT(), false);
        ci.cancel();
    }
}
