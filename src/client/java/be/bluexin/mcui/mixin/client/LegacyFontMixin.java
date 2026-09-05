/* SPDX-License-Identifier: GPL-3.0-or-later */
package be.bluexin.mcui.mixin.client;

import be.bluexin.mcui.config.SaoOption;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Apply the legacy font to both measurement and rendering; explicit mod fonts remain intact. */
@Mixin(Font.class)
public abstract class LegacyFontMixin {
    @Unique private static final ResourceLocation MCUI_LEGACY_FONT = ResourceLocation.fromNamespaceAndPath("saoui", "legacy");

    @ModifyVariable(method = "getFontSet", at = @At("HEAD"), argsOnly = true)
    private ResourceLocation mcui$legacyFont(ResourceLocation requested) {
        return SaoOption.CUSTOM_FONT.invoke() && Style.DEFAULT_FONT.equals(requested) ? MCUI_LEGACY_FONT : requested;
    }
}
