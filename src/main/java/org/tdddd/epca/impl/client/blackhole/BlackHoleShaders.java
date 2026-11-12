package org.tdddd.epca.impl.client.blackhole;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlackHoleShaders {

    public static final ResourceLocation BLACKHOLE = new ResourceLocation(epca.MODID, "blackhole");
    public static final ResourceLocation LENS = new ResourceLocation(epca.MODID, "blackhole_lens");

    private static ShaderInstance blackHole;
    private static ShaderInstance lens;

    private BlackHoleShaders() { }

    public static ShaderInstance blackHole() {
        return blackHole;
    }

    public static ShaderInstance lens() {
        return lens;
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), BLACKHOLE, DefaultVertexFormat.POSITION_TEX),
                shader -> blackHole = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), LENS, DefaultVertexFormat.POSITION_TEX),
                shader -> lens = shader);
    }
}