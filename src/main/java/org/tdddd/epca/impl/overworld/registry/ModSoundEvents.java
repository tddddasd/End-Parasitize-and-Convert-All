package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, epca.MODID);
    public static final RegistryObject<SoundEvent> SMALL_EXPLOSION = register("small_explosion");
    public static final RegistryObject<SoundEvent> BIG_EXPLOSION = register("big_explosion");
    public static final RegistryObject<SoundEvent> SLAM = register("slam");
    public static final RegistryObject<SoundEvent> NULLTHING_ATTACK0 = register("nullthing_attack0");
    public static final RegistryObject<SoundEvent> NULLTHING_RUN = register("nullthing_run");
    public static final RegistryObject<SoundEvent> NULLTHING_STAND1 = register("nullthing_stand1");
    public static final RegistryObject<SoundEvent> NULLTHING_STAND2 = register("nullthing_stand2");
    public static final RegistryObject<SoundEvent> INCOMPLETE_FORM_IDLE = register("incomplete_form_idle");
    public static final RegistryObject<SoundEvent> INCOMPLETE_FORM_HUNT = register("incomplete_form_hurt");
    public static final RegistryObject<SoundEvent> INCOMPLETE_FORM_DEATH = register("incomplete_form_death");
    public static final RegistryObject<SoundEvent> WALKING_HEAD_DEATH = register("walking_head_death");
    public static final RegistryObject<SoundEvent> WALKING_HEAD_SAY = register("walking_head_say");
    public static final RegistryObject<SoundEvent> INFESTED_COW_DEATH = register("infested_cow_death");
    public static final RegistryObject<SoundEvent> INFESTED_COW_HURT = register("infested_cow_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_COW_IDLE = register("infested_cow_idle");
    public static final RegistryObject<SoundEvent> INFESTED_COW_STEP = register("infested_cow_step");
    public static final RegistryObject<SoundEvent> INFESTED_ENDERMAN_PORTAL = register("infested_enderman_portal");
    public static final RegistryObject<SoundEvent> INFESTED_ENDERMAN_SCREAM = register("infested_enderman_scream");
    public static final RegistryObject<SoundEvent> INFESTED_ENDERMAN_IDLE = register("infested_enderman_idle");
    public static final RegistryObject<SoundEvent> INFESTED_ENDERMAN_HURT = register("infested_enderman_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_ENDERMAN_DEATH = register("infested_enderman_death");
    public static final RegistryObject<SoundEvent> INFESTED_ENDERMAN_TARGETING = register("infested_enderman_targeting");
    public static final RegistryObject<SoundEvent> INFESTED_PIG_DEATH = register("infested_pig_death");
    public static final RegistryObject<SoundEvent> INFESTED_PIG_HURT = register("infested_pig_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_PIG_IDLE = register("infested_pig_idle");
    public static final RegistryObject<SoundEvent> INFESTED_PIG_STEP = register("infested_pig_step");
    public static final RegistryObject<SoundEvent> INFESTED_HUSK_DEATH = register("infested_husk_death");
    public static final RegistryObject<SoundEvent> INFESTED_HUSK_HURT = register("infested_husk_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_HUSK_IDLE = register("infested_husk_idle");
    public static final RegistryObject<SoundEvent> INFESTED_SHEEP_DEATH = register("infested_sheep_death");
    public static final RegistryObject<SoundEvent> INFESTED_SHEEP_HURT = register("infested_sheep_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_SHEEP_IDLE = register("infested_sheep_idle");
    public static final RegistryObject<SoundEvent> INFESTED_SHEEP_STEP = register("infested_sheep_step");
    public static final RegistryObject<SoundEvent> INFESTED_VILLAGER_DEATH = register("infested_villager_death");
    public static final RegistryObject<SoundEvent> INFESTED_VILLAGER_HURT = register("infested_villager_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_VILLAGER_IDLE = register("infested_villager_idle");
    public static final RegistryObject<SoundEvent> INFESTED_ZOMBIE_DEATH = register("infested_zombie_death");
    public static final RegistryObject<SoundEvent> INFESTED_ZOMBIE_HURT = register("infested_zombie_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_ZOMBIE_IDLE = register("infested_zombie_idle");
    public static final RegistryObject<SoundEvent> INFESTED_VINDICATOR_DEATH = register("infested_vindicator_death");
    public static final RegistryObject<SoundEvent> INFESTED_VINDICATOR_HURT = register("infested_vindicator_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_VINDICATOR_IDLE = register("infested_vindicator_idle");
    public static final RegistryObject<SoundEvent> INFESTED_VINDICATOR_TARGETING = register("infested_vindicator_targeting");
    public static final RegistryObject<SoundEvent> INFESTED_WOLF_DEATH = register("infested_wolf_death");
    public static final RegistryObject<SoundEvent> INFESTED_WOLF_GROWL = register("infested_wolf_growl");
    public static final RegistryObject<SoundEvent> INFESTED_WOLF_HOWL = register("infested_wolf_howl");
    public static final RegistryObject<SoundEvent> INFESTED_WOLF_HURT = register("infested_wolf_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_WOLF_WHINE = register("infested_wolf_whine");
    public static final RegistryObject<SoundEvent> INFESTED_SKELETON_DEATH = register("infested_skeleton_death");
    public static final RegistryObject<SoundEvent> INFESTED_SKELETON_HURT = register("infested_skeleton_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_SKELETON_IDLE = register("infested_skeleton_idle");
    public static final RegistryObject<SoundEvent> INFESTED_SKELETON_STEP = register("infested_skeleton_step");
    public static final RegistryObject<SoundEvent> INFESTED_FOX_AGGRO = register("infested_fox_aggro");
    public static final RegistryObject<SoundEvent> INFESTED_FOX_BITE = register("infested_fox_bite");
    public static final RegistryObject<SoundEvent> INFESTED_FOX_DEATH = register("infested_fox_death");
    public static final RegistryObject<SoundEvent> INFESTED_FOX_HURT = register("infested_fox_hurt");
    public static final RegistryObject<SoundEvent> INFESTED_FOX_SCREECH = register("infested_fox_screech");
    public static final RegistryObject<SoundEvent> RESHAPE_STEP = register("reshape_step");
    public static final RegistryObject<SoundEvent> RESHAPE_LONGARMS_DEATH = register("reshape_longarms_death");
    public static final RegistryObject<SoundEvent> RESHAPE_LONGARMS_HURT = register("reshape_longarms_hurt");
    public static final RegistryObject<SoundEvent> RESHAPE_LONGARMS_IDLE = register("reshape_longarms_idle");
    public static final RegistryObject<SoundEvent> RESHAPE_YELLOWEYE_ATTACK = register("reshape_yelloweye_attack");
    public static final RegistryObject<SoundEvent> RESHAPE_YELLOWEYE_GASSING = register("reshape_yelloweye_gassing");
    public static final RegistryObject<SoundEvent> RESHAPE_YELLOWEYE_IDLE = register("reshape_yelloweye_idle");
    public static final RegistryObject<SoundEvent> RESHAPE_YELLOWEYE_HURT = register("reshape_yelloweye_hurt");
    public static final RegistryObject<SoundEvent> BECKON_STAGE1 = register("beckon_stage1");
    public static final RegistryObject<SoundEvent> BECKON_STAGE2 = register("beckon_stage2");
    public static final RegistryObject<SoundEvent> RIPPER_IDLE = register("ripper_idle");
    public static final RegistryObject<SoundEvent> RIPPER_STEP = register("ripper_step");
    public static final RegistryObject<SoundEvent> RIPPER_HUNT = register("ripper_hurt");
    public static final RegistryObject<SoundEvent> RIPPER_DEATH = register("ripper_death");
    public static final RegistryObject<SoundEvent> CURBUG_EVOLVE = register("curbug_evolve");
    public static final RegistryObject<SoundEvent> CURBUG_SAY = register("curbug_say");
    public static final RegistryObject<SoundEvent> MOZZIE_IDLE = register("mozzie_idle");
    public static final RegistryObject<SoundEvent> MOZZIE_HURT = register("mozzie_hurt");
    public static final RegistryObject<SoundEvent> MOZZIE_DEATH = register("mozzie_death");
    public static final RegistryObject<SoundEvent> PHASE0 = register("phase0");
    public static final RegistryObject<SoundEvent> PHASE1 = register("phase1");
    public static final RegistryObject<SoundEvent> PHASE2 = register("phase2");
    public static final RegistryObject<SoundEvent> PHASE3 = register("phase3");
    public static final RegistryObject<SoundEvent> PHASE4 = register("phase4");
    public static final RegistryObject<SoundEvent> PHASE5 = register("phase5");
    public static final RegistryObject<SoundEvent> PHASE6 = register("phase6");
    public static final RegistryObject<SoundEvent> PHASE7 = register("phase7");
    public static final RegistryObject<SoundEvent> PHASE8 = register("phase8");
    public static final RegistryObject<SoundEvent> PHASE9 = register("phase9");
    public static final RegistryObject<SoundEvent> PHASE10 = register("phase10");
    public static final RegistryObject<SoundEvent> PARCIAL_ADAPTATION = register("parcial_adaptation");
    public static final RegistryObject<SoundEvent> FULL_ADAPTATION = register("full_adaptation");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(epca.MODID, name)
        ));
    }
}