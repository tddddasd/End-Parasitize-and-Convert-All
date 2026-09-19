package org.tdddd.epca.impl.overworld.data;

/**
 * 1.20.1 里这里声明了 {@code Capability<IItemHandler> ARMOR_HANDLER}
 * （{@code CapabilityManager.get(new CapabilityToken<>(){})}），
 * 用于把实体的活体护甲箱物品栏暴露成 Forge 的物品能力。
 *
 * <p><b>26.1.2</b>：{@code net.neoforged.neoforge.capabilities.Capability} /
 * {@code CapabilityManager} / {@code CapabilityToken} 已从平台删除，{@code ARMOR_HANDLER}
 * 字段无法保留。26.1.2 的做法是在模组总线的
 * {@code net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent} 上
 * {@code registerEntity(Capabilities.Item.ENTITY, type, provider)}（
 * 或 {@code registerBlockEntity(Capabilities.Item.BLOCK, ...)}）来暴露物品栏；
 * 相关注册应在拥有该实体/方块实体的类里完成。
 *
 * <p>全工程检索确认 {@code ARMOR_HANDLER} <b>没有任何调用点</b>，
 * 因此这里只保留类名与包名（维持原有形状），不再声明字段。
 */
public final class ModCapabilities {

    private ModCapabilities() {
    }
}
