/* SPDX-License-Identifier: GPL-3.0-or-later */
package be.bluexin.mcui.mixin.client;

import be.bluexin.mcui.effects.SaoDeathParticles;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathEffectsMixin {
    @Inject(method = "tickDeath", at = @At("TAIL"))
    private void mcui$deathEffects(CallbackInfo ci) {
        SaoDeathParticles.onDeathTick((LivingEntity) (Object) this);
    }
}
