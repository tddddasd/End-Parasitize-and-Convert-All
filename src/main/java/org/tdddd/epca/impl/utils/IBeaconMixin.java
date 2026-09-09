package org.tdddd.epca.impl.utils;

import net.minecraft.world.item.Item;

public interface IBeaconMixin {
    Item getPaymentItem();
    void setPaymentItem(Item item);
    long getLastParasiteEffectTime();
    void setLastParasiteEffectTime(long time);
}