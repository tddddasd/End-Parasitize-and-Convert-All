package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ParticleDescriptionProvider;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModParticles;

/**
 * 数据生成器：自动为模组中所有粒子生成 particles/*.json 纹理描述。
 */
public class ParticleData extends ParticleDescriptionProvider {

    public ParticleData(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, existingFileHelper);
    }

    @Override
    protected void addDescriptions() {
        // 遍历 ModParticles 中注册的粒子
        addParticle(ModParticles.SPLASHI, "splashi");
        addParticle(ModParticles.COTH, "coth");
        addParticle(ModParticles.BLEEDING, "bleeding");
        addParticle(ModParticles.LIVING_FLESH, "living_flesh");
        addParticle(ModParticles.WAVE, "wave");
        addParticle(ModParticles.WAVE_SMALL, "wave_small");
        addParticle(ModParticles.INFESTIVE_GAS, "infestive_gas");
        addParticle(ModParticles.BIOMASS, "biomass");
        addParticle(ModParticles.BIOMASS_BOOM_SMALL, "biomass_boom_small");
        addParticle(ModParticles.BIOMASS_BOOM_MEDI, "biomass_boom_medi");
        addParticle(ModParticles.F_ADAPTATION, "f_adaptation");
        addParticle(ModParticles.P_ADAPTATION, "p_adaptation");
    }

    private void addParticle(RegistryObject<SimpleParticleType> particle, String textureName) {
        sprite(particle.get(), new ResourceLocation(epca.MODID, textureName));
    }
}
