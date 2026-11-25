package com.mrlion303.vfxshadersmod.mixin;

import com.lowdragmc.photon.client.fx.IEffect;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.mojang.blaze3d.vertex.ParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.mrlion303.vfxshadersmod.client.photon.PhotonWorldParticleEngine;

/**
 * Desvía únicamente los objetos de Photon al motor privado de VFXShaderMod.
 */
@Mixin(value = IFXObject.class)
public interface IFXObjectMixin {
    @Redirect(
            method = "emmit(Lcom/lowdragmc/photon/client/fx/IEffect;Lorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Vector3f;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V"
            )
    )
    private void vfxshadersmod$redirectPhotonParticle(ParticleEngine originalEngine, Particle particle) {
        PhotonWorldParticleEngine.addParticle(particle);
    }
}
