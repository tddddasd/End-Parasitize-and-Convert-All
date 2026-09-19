package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

import java.util.UUID;

/**
 * 当寄生虫的跟随目标发生变化时触发。
 * 可取消：取消后不会改变跟随目标。
 *
 * <p>26.1.2: {@code @Cancelable} 已被删除；可取消性改由
 * {@link ICancellableEvent} 标记接口表达（{@code IParasite.java:39} 会调用 {@code isCanceled()}）。
 */
public class ParasiteFollowEvent extends LivingEvent implements ICancellableEvent {
    private final UUID oldTarget;
    private final UUID newTarget;

    public ParasiteFollowEvent(LivingEntity entity, UUID oldTarget, UUID newTarget) {
        super(entity);
        this.oldTarget = oldTarget;
        this.newTarget = newTarget;
    }

    /**
     * 旧的跟随目标 UUID（可能为 null）
     */
    public UUID getOldTarget() {
        return oldTarget;
    }

    /**
     * 新的跟随目标 UUID（可能为 null）
     */
    public UUID getNewTarget() {
        return newTarget;
    }

    /**
     * 判断是否为开始跟随（旧为 null，新不为 null）
     */
    public boolean isStartFollow() {
        return oldTarget == null && newTarget != null;
    }

    /**
     * 判断是否为取消跟随（旧不为 null，新为 null）
     */
    public boolean isStopFollow() {
        return oldTarget != null && newTarget == null;
    }

    /**
     * 判断是否为切换跟随目标（旧和新都不为 null 且不同）
     */
    public boolean isSwitchTarget() {
        return oldTarget != null && newTarget != null && !oldTarget.equals(newTarget);
    }
}