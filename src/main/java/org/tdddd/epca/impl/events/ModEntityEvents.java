package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedPlayer;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.Nullthing;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.YawningNya;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.*;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIBeckon;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIIBeckon;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.*;
import org.tdddd.epca.impl.overworld.registry.entities.entity.poverty.*;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeYelloweye;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEntities;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    /**
     * All entity attributes are now routed through EpcaEntityManager.
     * ModEntities.registerMobAttributes() adds each type → EpcaEntityManager.registerMobNoRender()
     * stores the attribute supplier. Then this single call processes them all.
     */
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        EpcaEntityManager.createAttributes(event);
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.CURBUG.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Curbug::checkBuglinSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.RIPPER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Ripper::checkRupterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.SMALL_INCOMPLETE_FORM.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SmallIncompleteForm::checkSmallIncompleteFormSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_ZOMBIE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedZombie::checkInfestedZombieSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_ZOMBIE_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingZombieHead::checkWalkingZombieHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_HUSK.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedHusk::checkInfestedHuskSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_HUSK_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingHuskHead::checkWalkingHuskHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_DROWNED_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingDrownedHead::checkWalkingDrownedHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_DROWNED.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedDrowned::checkInfestedDrownedSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.MEDIUM_INCOMPLETE_FORM.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MediumIncompleteForm::checkMediumIncompleteFormSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.BIOMASS_SMALL.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BiomassSmall::checkBiomassSmallSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.STAGE_I_BECKON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, StageIBeckon::checkStageIBeckonSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_PILLAGER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedPillager::checkInfestedPillagerSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_PILLAGER_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingPillagerHead::checkWalkingPillagerHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_VINDICATOR.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedVindicator::checkInfestedVindicatorSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_VINDICATOR_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingVindicatorHead::checkWalkingVindicatorHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_VILLAGER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedVillager::checkInfestedVillagerSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_VILLAGER_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingVillagerHead::checkWalkingVillagerHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_ZOMBIE_VILLAGER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedZombieVillager::checkInfestedZombieVillagerSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_ZOMBIE_VILLAGER_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingZombieVillagerHead::checkWalkingZombieVillagerHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.FINS.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Fins::checkFinsSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_PIG.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedPig::checkInfestedPigSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_PIG_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingPigHead::checkWalkingPigHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_SHEEP.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedSheep::checkInfestedSheepSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_SHEEP_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingSheepHead::checkWalkingSheepHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.LARGE_INCOMPLETE_FORM.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LargeIncompleteForm::checkLargeIncompleteFormSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_COW.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedCow::checkInfestedCowSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_COW_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingCowHead::checkWalkingCowHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.NULLTHING.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Nullthing::checkNullthingSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.MOZZIE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mozzie::checkGnatSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.YAWNING_NYA.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, YawningNya::checkNyaSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_SLIME_SIZE0.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedSlimeSize0::checkInfestedSlimeSize0SpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_SLIME_SIZE1.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedSlimeSize1::checkInfestedSlimeSize1SpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_SLIME_SIZE3.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedSlimeSize3::checkInfestedSlimeSize3SpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.LIVING_FLESH_SIZE0.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LivingFleshSize0::checkLivingFleshSize0SpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.LIVING_FLESH_SIZE1.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LivingFleshSize1::checkLivingFleshSize1SpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.LIVING_FLESH_SIZE2.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LivingFleshSize2::checkLivingFleshSize2SpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.LIVING_FLESH_SIZE3.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LivingFleshSize3::checkLivingFleshSize3SpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.LIVING_FLESH_SIZE4.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LivingFleshSize4::checkLivingFleshSize4SpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_WOLF.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedWolf::checkInfestedWolfSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_WOLF_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingWolfHead::checkWalkingWolfHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.RESHAPE_LONGARMS.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ReshapeLongarms::checkReshapeLongarmsSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.BIOMASS_MEDIUM.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BiomassMedium::checkBiomassMediumSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.STAGE_II_BECKON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, StageIIBeckon::checkStageIIBeckonSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_CHICKEN_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingChickenHead::checkWalkingChickenHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_CHICKEN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedChicken::checkInfestedChickenSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.FLYING_CARRIER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FlyingCarrier::checkFlyingCarrierSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_ENDERMAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedEnderman::checkInfestedEndermanSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_ENDERMAN_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingEndermanHead::checkWalkingEndermanHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_ENDERMITE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedEndermite::checkInfestedEndermiteSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_SILVERFISH.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedSilverfish::checkInfestedSilverfishSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_PLAYER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedPlayer::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.LIGHT_CARRIER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LightCarrier::checkLightCarrierSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_SKELETON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedSkeleton::checkInfestedSkeletonSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_SKELETON_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingSkeletonHead::checkWalkingSkeletonHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.RESHAPE_YELLOWEYE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ReshapeYelloweye::checkReshapeYelloweyeSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_FOX.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedFox::checkInfestedFoxSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.WALKING_FOX_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WalkingFoxHead::checkWalkingFoxHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.INFESTED_PUMPKIN_HEAD.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, InfestedPumpkinHead::checkInfestedPumpkinHeadSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
    }
}
