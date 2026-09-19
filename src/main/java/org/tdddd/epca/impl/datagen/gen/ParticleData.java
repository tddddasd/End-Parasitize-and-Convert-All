package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.data.ParticleDescriptionProvider;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModParticles;

/**
 * 数据生成器：自动为模组中所有粒子生成 particles/*.json 纹理描述。
 *
 * <p><b>26.1.2 改动</b>：{@code ParticleDescriptionProvider} 的构造器只剩
 * {@code (PackOutput)}（{@code ExistingFileHelper} 已删除）；描述方法名由
 * {@code addParticle(...)} 变为 {@code sprite(ParticleType, Identifier)}。
 */
public class ParticleData extends ParticleDescriptionProvider {

    public ParticleData(PackOutput output) {
        super(output);
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

    private void addParticle(DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> particle, String textureName) {
        // 26.1.2: addParticle(...) 改名为 spriteSet(...)（单个纹理的重载）
        spriteSet(particle.get(), Identifier.fromNamespaceAndPath(epca.MODID, textureName));
    }
}
