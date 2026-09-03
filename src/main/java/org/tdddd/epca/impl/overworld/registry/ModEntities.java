package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import org.tdddd.epca.impl.overworld.registry.entities.entity.base.AbstractEpcaEntity;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.*;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIBeckon;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIIBeckon;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.*;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.*;
import org.tdddd.epca.impl.overworld.registry.entities.entity.poverty.*;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeYelloweye;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.Nullthing;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.YawningNya;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Central entity type registration. All mob entities route through helper methods
 * that also register with {@link EpcaEntityManager} for:
 * <ul>
 *   <li>Automatic attribute creation (no manual event.put calls)</li>
 *   <li>Automatic renderer registration (no per-entity renderer code in ClientHandler)</li>
 * </ul>
 * Only entities with truly custom rendering (non-GeckoLib, special shaders, etc.)
 * stay on manual registration paths.
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, epca.MODID);

    // ═══════════════════════════════════════════════════════════════
    //  Resource helpers
    // ═══════════════════════════════════════════════════════════════

    private static ResourceLocation modelLoc(String name) {
        return new ResourceLocation(epca.MODID, "geo/entity/" + name + ".geo.json");
    }

    private static ResourceLocation texLoc(String name) {
        return new ResourceLocation(epca.MODID, "textures/entity/" + name + ".png");
    }

    private static ResourceLocation animLoc(String name) {
        return new ResourceLocation(epca.MODID, "animations/" + name + ".animation.json");
    }

    /** Consumer for AbstractEpcaEntity subclasses that sets model/texture/animation on the entity. */
    public static <T extends AbstractEpcaEntity> Consumer<T> defaultResources(String name) {
        return entity -> {
            entity.model = modelLoc(name);
            entity.texture = texLoc(name);
            entity.animation = animLoc(name);
        };
    }

    // ═══════════════════════════════════════════════════════════════
    //  Registration helpers
    // ═══════════════════════════════════════════════════════════════

    /** Full auto: AbstractEpcaEntity → attributes + auto-renderer (EpcaGeoRenderer). */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractEpcaEntity> RegistryObject<EntityType<T>> registerMob(
            String name, float width, float height,
            EntityType.EntityFactory<T> factory,
            Supplier<AttributeSupplier> attributes) {
        return (RegistryObject) ENTITIES.register(name, () -> {
            EntityType<T> type = EntityType.Builder.of(factory, MobCategory.MONSTER)
                    .sized(width, height).clientTrackingRange(12).build(name);
            return EpcaEntityManager.registerMob(type, attributes);
        });
    }

    /** Existing entity (doesn't extend AbstractEpcaEntity) → attributes + auto-renderer via name. */
    public static <T extends LivingEntity> RegistryObject<EntityType<T>> registerMobWithRender(
            String name, float width, float height,
            EntityType.EntityFactory<T> factory,
            MobCategory category, int trackingRange,
            Supplier<AttributeSupplier> attributes) {
        return ENTITIES.register(name, () -> {
            EntityType<T> type = EntityType.Builder.of(factory, category)
                    .sized(width, height).clientTrackingRange(trackingRange).build(name);
            return EpcaEntityManager.registerMobWithRender(type, attributes,
                    modelLoc(name), texLoc(name), animLoc(name));
        });
    }

    /** Existing entity → auto-renderer with separate model/texture/animation base names. */
    public static <T extends LivingEntity> RegistryObject<EntityType<T>> registerMobWithCustomModel(
            String name, String modelBase, String texBase, String animBase,
            float width, float height,
            EntityType.EntityFactory<T> factory,
            MobCategory category, int trackingRange,
            Supplier<AttributeSupplier> attributes) {
        return ENTITIES.register(name, () -> {
            EntityType<T> type = EntityType.Builder.of(factory, category)
                    .sized(width, height).clientTrackingRange(trackingRange).build(name);
            return EpcaEntityManager.registerMobWithRender(type, attributes,
                    modelLoc(modelBase), texLoc(texBase), animLoc(animBase));
        });
    }

    /** Existing entity with custom renderer → attributes only. */
    public static <T extends LivingEntity> RegistryObject<EntityType<T>> registerMobAttributes(
            String name, float width, float height,
            EntityType.EntityFactory<T> factory,
            MobCategory category, int trackingRange,
            Supplier<AttributeSupplier> attributes) {
        return ENTITIES.register(name, () -> {
            EntityType<T> type = EntityType.Builder.of(factory, category)
                    .sized(width, height).clientTrackingRange(trackingRange).build(name);
            return EpcaEntityManager.registerMobNoRender(type, attributes);
        });
    }

    /** Misc entity, no attributes, custom renderer. */
    public static <T extends Entity> RegistryObject<EntityType<T>> registerMisc(
            String name, float width, float height,
            EntityType.EntityFactory<T> factory,
            MobCategory category) {
        return ENTITIES.register(name, () ->
                EntityType.Builder.of(factory, category)
                        .sized(width, height).clientTrackingRange(12).build(name));
    }

    /** Misc entity with extra config. */
    public static <T extends Entity> RegistryObject<EntityType<T>> registerMiscNoSummon(
            String name, float width, float height,
            EntityType.EntityFactory<T> factory) {
        return ENTITIES.register(name, () ->
                EntityType.Builder.of(factory, MobCategory.MISC)
                        .sized(width, height).noSummon().fireImmune()
                        .clientTrackingRange(12).build(name));
    }

    public static <T extends Entity> RegistryObject<EntityType<T>> registerMiscTracking(
            String name, float width, float height,
            EntityType.EntityFactory<T> factory,
            int trackingRange, int updateInterval) {
        return ENTITIES.register(name, () ->
                EntityType.Builder.of(factory, MobCategory.MISC)
                        .sized(width, height)
                        .clientTrackingRange(trackingRange).updateInterval(updateInterval)
                        .build(name));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Entity registrations
    //  All mob entities use registerMobWithRender → auto-render via EpcaTypeGeoRenderer
    // ═══════════════════════════════════════════════════════════════

    // --- Onesent ---
    public static final RegistryObject<EntityType<Curbug>> CURBUG =
            registerMobWithRender("curbug", 0.5F, 0.4F, Curbug::new, MobCategory.MONSTER, 12, Curbug::setAttributes);
    public static final RegistryObject<EntityType<Ripper>> RIPPER =
            registerMobWithRender("ripper", 0.8F, 0.9F, Ripper::new, MobCategory.MONSTER, 12, Ripper::setAttributes);
    public static final RegistryObject<EntityType<Fins>> FINS =
            registerMobWithRender("fins", 0.8F, 0.9F, Fins::new, MobCategory.MONSTER, 12, Fins::setAttributes);
    public static final RegistryObject<EntityType<Mozzie>> MOZZIE =
            registerMobWithRender("mozzie", 0.7F, 0.8F, Mozzie::new, MobCategory.MONSTER, 12, Mozzie::setAttributes);
    public static final RegistryObject<EntityType<FlyingCarrier>> FLYING_CARRIER =
            registerMobWithRender("flying_carrier", 1.25F, 2.5F, FlyingCarrier::new, MobCategory.MONSTER, 12, FlyingCarrier::setAttributes);
    public static final RegistryObject<EntityType<LightCarrier>> LIGHT_CARRIER =
            registerMobWithRender("light_carrier", 0.9F, 1.5F, LightCarrier::new, MobCategory.MONSTER, 12, LightCarrier::setAttributes);

    // --- Poverty ---
    public static final RegistryObject<EntityType<SmallIncompleteForm>> SMALL_INCOMPLETE_FORM =
            registerMobWithRender("small_incomplete_form", 0.6F, 0.8F, SmallIncompleteForm::new, MobCategory.MONSTER, 12, SmallIncompleteForm::setAttributes);
    public static final RegistryObject<EntityType<MediumIncompleteForm>> MEDIUM_INCOMPLETE_FORM =
            registerMobWithRender("medium_incomplete_form", 0.8F, 1.6F, MediumIncompleteForm::new, MobCategory.MONSTER, 12, MediumIncompleteForm::setAttributes);
    public static final RegistryObject<EntityType<LargeIncompleteForm>> LARGE_INCOMPLETE_FORM =
            registerMobWithRender("large_incomplete_form", 2.5F, 2.5F, LargeIncompleteForm::new, MobCategory.MONSTER, 12, LargeIncompleteForm::setAttributes);
    public static final RegistryObject<EntityType<BiomassSmall>> BIOMASS_SMALL =
            registerMobWithRender("biomass_small", 0.4F, 0.4F, BiomassSmall::new, MobCategory.AMBIENT, 12, BiomassSmall::setAttributes);
    public static final RegistryObject<EntityType<BiomassMedium>> BIOMASS_MEDIUM =
            registerMobWithRender("biomass_medium", 1.0F, 1.25F, BiomassMedium::new, MobCategory.AMBIENT, 12, BiomassMedium::setAttributes);
    public static final RegistryObject<EntityType<LivingFleshSize0>> LIVING_FLESH_SIZE0 =
            registerMobWithCustomModel("living_flesh_size0", "living_flesh", "living_flesh", "living_flesh", 0.8F, 0.8F, LivingFleshSize0::new, MobCategory.MONSTER, 12, LivingFleshSize0::setAttributes);
    public static final RegistryObject<EntityType<LivingFleshSize1>> LIVING_FLESH_SIZE1 =
            registerMobWithCustomModel("living_flesh_size1", "living_flesh", "living_flesh", "living_flesh", 1.1F, 1.1F, LivingFleshSize1::new, MobCategory.MONSTER, 12, LivingFleshSize1::setAttributes);
    public static final RegistryObject<EntityType<LivingFleshSize2>> LIVING_FLESH_SIZE2 =
            registerMobWithCustomModel("living_flesh_size2", "living_flesh", "living_flesh", "living_flesh", 1.5F, 1.5F, LivingFleshSize2::new, MobCategory.MONSTER, 12, LivingFleshSize2::setAttributes);
    public static final RegistryObject<EntityType<LivingFleshSize3>> LIVING_FLESH_SIZE3 =
            registerMobWithCustomModel("living_flesh_size3", "living_flesh", "living_flesh", "living_flesh", 1.8F, 1.8F, LivingFleshSize3::new, MobCategory.MONSTER, 12, LivingFleshSize3::setAttributes);
    public static final RegistryObject<EntityType<LivingFleshSize4>> LIVING_FLESH_SIZE4 =
            registerMobWithCustomModel("living_flesh_size4", "living_flesh", "living_flesh", "living_flesh", 2.2F, 2.0F, LivingFleshSize4::new, MobCategory.MONSTER, 12, LivingFleshSize4::setAttributes);

    // --- Link ---
    public static final RegistryObject<EntityType<StageIBeckon>> STAGE_I_BECKON =
            registerMobWithRender("stage_i_beckon", 0.6F, 2.2F, StageIBeckon::new, MobCategory.MONSTER, 12, StageIBeckon::setAttributes);
    public static final RegistryObject<EntityType<StageIIBeckon>> STAGE_II_BECKON =
            registerMobWithRender("stage_ii_beckon", 0.75F, 5.0F, StageIIBeckon::new, MobCategory.MONSTER, 12, StageIIBeckon::setAttributes);

    // --- Reshape ---
    public static final RegistryObject<EntityType<ReshapeLongarms>> RESHAPE_LONGARMS =
            registerMobWithRender("reshape_longarms", 2.0F, 3.9F, ReshapeLongarms::new, MobCategory.MONSTER, 16, ReshapeLongarms::setAttributes);
    public static final RegistryObject<EntityType<ReshapeYelloweye>> RESHAPE_YELLOWEYE =
            registerMobWithRender("reshape_yelloweye", 2.75F, 3.5F, ReshapeYelloweye::new, MobCategory.MONSTER, 16, ReshapeYelloweye::setAttributes);

    // --- Infested ---
    public static final RegistryObject<EntityType<InfestedZombie>> INFESTED_ZOMBIE =
            registerMobWithRender("infested_zombie", 0.7F, 1.8F, InfestedZombie::new, MobCategory.MONSTER, 12, InfestedZombie::setAttributes);
    public static final RegistryObject<EntityType<WalkingZombieHead>> WALKING_ZOMBIE_HEAD =
            registerMobWithRender("walking_zombie_head", 0.8F, 0.8F, WalkingZombieHead::new, MobCategory.MONSTER, 12, WalkingZombieHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedHusk>> INFESTED_HUSK =
            registerMobWithCustomModel("infested_husk", "infested_zombie", "infested_husk", "infested_zombie", 0.7F, 1.8F, InfestedHusk::new, MobCategory.MONSTER, 12, InfestedHusk::setAttributes);
    public static final RegistryObject<EntityType<WalkingHuskHead>> WALKING_HUSK_HEAD =
            registerMobWithCustomModel("walking_husk_head", "walking_zombie_head", "walking_husk_head", "walking_zombie_head", 0.8F, 0.8F, WalkingHuskHead::new, MobCategory.MONSTER, 12, WalkingHuskHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedDrowned>> INFESTED_DROWNED =
            registerMobWithCustomModel("infested_drowned", "infested_zombie", "infested_drowned", "infested_zombie", 0.7F, 1.8F, InfestedDrowned::new, MobCategory.MONSTER, 12, InfestedDrowned::setAttributes);
    public static final RegistryObject<EntityType<WalkingDrownedHead>> WALKING_DROWNED_HEAD =
            registerMobWithCustomModel("walking_drowned_head", "walking_zombie_head", "walking_drowned_head", "walking_zombie_head", 0.8F, 0.8F, WalkingDrownedHead::new, MobCategory.MONSTER, 12, WalkingDrownedHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedPillager>> INFESTED_PILLAGER =
            registerMobWithCustomModel("infested_pillager", "infested_villager", "infested_pillager", "infested_villager", 0.7F, 1.8F, InfestedPillager::new, MobCategory.MONSTER, 12, InfestedPillager::setAttributes);
    public static final RegistryObject<EntityType<WalkingPillagerHead>> WALKING_PILLAGER_HEAD =
            registerMobWithCustomModel("walking_pillager_head", "walking_villager_head", "walking_pillager_head", "walking_villager_head", 0.8F, 0.8F, WalkingPillagerHead::new, MobCategory.MONSTER, 12, WalkingPillagerHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedVindicator>> INFESTED_VINDICATOR =
            registerMobWithCustomModel("infested_vindicator", "infested_villager", "infested_vindicator", "infested_villager", 0.7F, 1.8F, InfestedVindicator::new, MobCategory.MONSTER, 12, InfestedVindicator::setAttributes);
    public static final RegistryObject<EntityType<WalkingVindicatorHead>> WALKING_VINDICATOR_HEAD =
            registerMobWithCustomModel("walking_vindicator_head", "walking_villager_head", "walking_vindicator_head", "walking_villager_head", 0.8F, 0.8F, WalkingVindicatorHead::new, MobCategory.MONSTER, 12, WalkingVindicatorHead::setAttributes);
    public static final RegistryObject<EntityType<WalkingVillagerHead>> WALKING_VILLAGER_HEAD =
            registerMobWithRender("walking_villager_head", 0.8F, 0.8F, WalkingVillagerHead::new, MobCategory.MONSTER, 12, WalkingVillagerHead::setAttributes);
    public static final RegistryObject<EntityType<WalkingZombieVillagerHead>> WALKING_ZOMBIE_VILLAGER_HEAD =
            registerMobWithCustomModel("walking_zombie_villager_head", "walking_villager_head", "walking_zombie_villager_head", "walking_villager_head", 0.8F, 0.8F, WalkingZombieVillagerHead::new, MobCategory.MONSTER, 12, WalkingZombieVillagerHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedPig>> INFESTED_PIG =
            registerMobWithRender("infested_pig", 0.9F, 1.0F, InfestedPig::new, MobCategory.MONSTER, 12, InfestedPig::setAttributes);
    public static final RegistryObject<EntityType<WalkingPigHead>> WALKING_PIG_HEAD =
            registerMobWithRender("walking_pig_head", 0.8F, 0.8F, WalkingPigHead::new, MobCategory.MONSTER, 12, WalkingPigHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedSheep>> INFESTED_SHEEP =
            registerMobWithRender("infested_sheep", 0.9F, 1.0F, InfestedSheep::new, MobCategory.MONSTER, 12, InfestedSheep::setAttributes);
    public static final RegistryObject<EntityType<WalkingSheepHead>> WALKING_SHEEP_HEAD =
            registerMobWithRender("walking_sheep_head", 0.8F, 0.8F, WalkingSheepHead::new, MobCategory.MONSTER, 12, WalkingSheepHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedCow>> INFESTED_COW =
            registerMobWithRender("infested_cow", 1.0F, 1.1F, InfestedCow::new, MobCategory.MONSTER, 12, InfestedCow::setAttributes);
    public static final RegistryObject<EntityType<WalkingCowHead>> WALKING_COW_HEAD =
            registerMobWithRender("walking_cow_head", 0.8F, 0.8F, WalkingCowHead::new, MobCategory.MONSTER, 12, WalkingCowHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedChicken>> INFESTED_CHICKEN =
            registerMobWithRender("infested_chicken", 0.8F, 0.8F, InfestedChicken::new, MobCategory.MONSTER, 12, InfestedChicken::setAttributes);
    public static final RegistryObject<EntityType<WalkingChickenHead>> WALKING_CHICKEN_HEAD =
            registerMobWithRender("walking_chicken_head", 0.4F, 0.4F, WalkingChickenHead::new, MobCategory.MONSTER, 12, WalkingChickenHead::setAttributes);
    public static final RegistryObject<EntityType<WalkingWolfHead>> WALKING_WOLF_HEAD =
            registerMobWithRender("walking_wolf_head", 0.6F, 0.6F, WalkingWolfHead::new, MobCategory.MONSTER, 12, WalkingWolfHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedEndermite>> INFESTED_ENDERMITE =
            registerMobWithRender("infested_endermite", 0.5F, 0.4F, InfestedEndermite::new, MobCategory.MONSTER, 12, InfestedEndermite::setAttributes);
    public static final RegistryObject<EntityType<InfestedSilverfish>> INFESTED_SILVERFISH =
            registerMobWithRender("infested_silverfish", 0.5F, 0.4F, InfestedSilverfish::new, MobCategory.MONSTER, 12, InfestedSilverfish::setAttributes);
    public static final RegistryObject<EntityType<InfestedSkeleton>> INFESTED_SKELETON =
            registerMobWithRender("infested_skeleton", 0.6F, 1.8F, InfestedSkeleton::new, MobCategory.MONSTER, 12, InfestedSkeleton::setAttributes);
    public static final RegistryObject<EntityType<WalkingSkeletonHead>> WALKING_SKELETON_HEAD =
            registerMobWithRender("walking_skeleton_head", 0.8F, 0.8F, WalkingSkeletonHead::new, MobCategory.MONSTER, 12, WalkingSkeletonHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedFox>> INFESTED_FOX =
            registerMobWithRender("infested_fox", 0.8F, 0.9F, InfestedFox::new, MobCategory.MONSTER, 12, InfestedFox::setAttributes);
    public static final RegistryObject<EntityType<WalkingFoxHead>> WALKING_FOX_HEAD =
            registerMobWithRender("walking_fox_head", 0.6F, 0.6F, WalkingFoxHead::new, MobCategory.MONSTER, 12, WalkingFoxHead::setAttributes);
    public static final RegistryObject<EntityType<InfestedPumpkinHead>> INFESTED_PUMPKIN_HEAD =
            registerMobWithRender("infested_pumpkin_head",  1.0F, 1.0F, InfestedPumpkinHead::new, MobCategory.MONSTER, 12, InfestedPumpkinHead::setAttributes);
    // --- Slimes (createAttributes returns Builder) ---
    public static final RegistryObject<EntityType<InfestedSlimeSize0>> INFESTED_SLIME_SIZE0 =
            registerMobWithRender("infested_slime_size0", 0.5F, 0.5F, InfestedSlimeSize0::new, MobCategory.MONSTER, 12, () -> InfestedSlimeSize0.createAttributes().build());
    public static final RegistryObject<EntityType<InfestedSlimeSize1>> INFESTED_SLIME_SIZE1 =
            registerMobWithRender("infested_slime_size1", 1.0F, 1.0F, InfestedSlimeSize1::new, MobCategory.MONSTER, 12, () -> InfestedSlimeSize1.createAttributes().build());
    public static final RegistryObject<EntityType<InfestedSlimeSize3>> INFESTED_SLIME_SIZE3 =
            registerMobWithRender("infested_slime_size3", 2.5F, 2.5F, InfestedSlimeSize3::new, MobCategory.MONSTER, 12, () -> InfestedSlimeSize3.createAttributes().build());
    public static final RegistryObject<EntityType<InfestedBat>> INFESTED_BAT =
            registerMobWithRender("infested_bat", 0.5F, 0.8F, InfestedBat::new, MobCategory.MONSTER, 12, InfestedBat::setAttributes);

    /** Misc entity with auto-renderer but no attributes. */
    public static <T extends Entity> RegistryObject<EntityType<T>> registerMiscWithRender(
            String name, float width, float height,
            EntityType.EntityFactory<T> factory) {
        return registerMiscWithRender(name, name, width, height, factory);
    }

    /** Misc entity with auto-renderer, using custom resource base name. */
    public static <T extends Entity> RegistryObject<EntityType<T>> registerMiscWithRender(
            String name, String resBase, float width, float height,
            EntityType.EntityFactory<T> factory) {
        return ENTITIES.register(name, () -> {
            EntityType<T> type = EntityType.Builder.of(factory, MobCategory.MISC)
                    .sized(width, height).clientTrackingRange(12).build(name);
            EpcaEntityManager.registerRenderOnly((EntityType<? extends LivingEntity>) type,
                    modelLoc(resBase), texLoc(resBase), animLoc(resBase));
            return type;
        });
    }

    // --- Entities with attributes + custom renderers (not auto-rendered) ---
    public static final RegistryObject<EntityType<InfestedVillager>> INFESTED_VILLAGER =
            registerMobWithRender("infested_villager", 0.7F, 1.8F, InfestedVillager::new, MobCategory.MONSTER, 12, InfestedVillager::setAttributes);
    public static final RegistryObject<EntityType<InfestedZombieVillager>> INFESTED_ZOMBIE_VILLAGER =
            registerMobWithCustomModel("infested_zombie_villager", "infested_villager", "infested_zombie_villager", "infested_villager", 0.7F, 1.8F, InfestedZombieVillager::new, MobCategory.MONSTER, 12, InfestedZombieVillager::setAttributes);
    public static final RegistryObject<EntityType<InfestedWolf>> INFESTED_WOLF =
            registerMobWithRender("infested_wolf", 0.8F, 0.9F, InfestedWolf::new, MobCategory.MONSTER, 12, InfestedWolf::setAttributes);
    public static final RegistryObject<EntityType<InfestedEnderman>> INFESTED_ENDERMAN =
            registerMobWithRender("infested_enderman", 0.9F, 2.5F, InfestedEnderman::new, MobCategory.MONSTER, 12, InfestedEnderman::setAttributes);
    public static final RegistryObject<EntityType<WalkingEndermanHead>> WALKING_ENDERMAN_HEAD =
            registerMobWithRender("walking_enderman_head", 0.9F, 0.9F, WalkingEndermanHead::new, MobCategory.MONSTER, 12, WalkingEndermanHead::setAttributes);
    public static final RegistryObject<EntityType<Nullthing>> NULLTHING =
            registerMobWithRender("nullthing", 0.9F, 0.9F, Nullthing::new, MobCategory.MONSTER, 12, Nullthing::setAttributes);
    public static final RegistryObject<EntityType<YawningNya>> YAWNING_NYA =
            registerMobAttributes("yawning_nya", 0.6F, 1.8F, YawningNya::new, MobCategory.AMBIENT, 8, YawningNya::createAttributes);
    public static final RegistryObject<EntityType<InfestedPlayer>> INFESTED_PLAYER =
            registerMobAttributes("infested_player", 0.6F, 1.8F, InfestedPlayer::new, MobCategory.MONSTER, 12, InfestedPlayer::setAttributes);

    // --- Misc entities (no attributes, custom renderers) ---
    public static final RegistryObject<EntityType<ContaminatedWater>> CONTAMINATED_WATER =
            registerMiscNoSummon("contaminated_water", 5.0F, 5.0F, ContaminatedWater::new);
    public static final RegistryObject<EntityType<ViralBomb>> VIRAL_BOMB =
            registerMiscWithRender("viral_bomb", 0.8F, 0.8F, ViralBomb::new);
    public static final RegistryObject<EntityType<ViralBombII>> VIRAL_BOMB_II =
            registerMiscWithRender("viral_bomb_ii", "viral_bomb", 0.8F, 0.8F, ViralBombII::new);
    public static final RegistryObject<EntityType<SlimeProjectile>> SLIME_PROJECTILE =
            registerMiscWithRender("slime_projectile", 0.3F, 0.3F, SlimeProjectile::new);
    public static final RegistryObject<EntityType<InfestedSpiderWebProjectile>> INFESTED_SPIDER_WEB_PROJECTILE =
            registerMiscWithRender("infested_spider_web_projectile", 0.3F, 0.3F, InfestedSpiderWebProjectile::new);
    public static final RegistryObject<EntityType<InfestedSpiderWebBloodProjectile>> INFESTED_SPIDER_WEB_BLOOD_PROJECTILE =
            registerMiscWithRender("infested_spider_web_blood_projectile", 0.3F, 0.3F, InfestedSpiderWebBloodProjectile::new);
    public static final RegistryObject<EntityType<InfestedCaveSpiderWebProjectile>> INFESTED_CAVE_SPIDER_WEB_PROJECTILE =
            registerMiscWithRender("infested_cave_spider_web_projectile", 0.3F, 0.3F, InfestedCaveSpiderWebProjectile::new);
    public static final RegistryObject<EntityType<AcidBullet>> ACID_BULLET =
            registerMiscWithRender("acid_bullet", 0.4F, 0.4F, AcidBullet::new);
    public static final RegistryObject<EntityType<BiomassEgg>> BIOMASS_EGG =
            registerMiscWithRender("biomass_egg", 0.2F, 0.2F, BiomassEgg::new);
    public static final RegistryObject<EntityType<BoneArrow>> BONE_ARROW =
            registerMiscWithRender("bone_arrow", 0.2F, 0.2F, BoneArrow::new);
    public static final RegistryObject<EntityType<BoneFragment>> BONE_FRAGMENT =
            registerMiscNoSummon("bone_fragment", 0.3F, 0.3F, BoneFragment::new);
    public static final RegistryObject<EntityType<InfestedThrownEnderPearl>> INFESTED_ENDER_PEARL =
            registerMiscTracking("infested_ender_pearl", 0.5F, 0.5F, InfestedThrownEnderPearl::new, 4, 10);

    public static final RegistryObject<EntityType<ReshapeLongarms.CustomPart>> RESHAPE_PART =
            ENTITIES.register("reshape_part",
                    () -> EntityType.Builder.<ReshapeLongarms.CustomPart>of(
                                    ReshapeLongarms.CustomPart::new, MobCategory.MISC)
                            .sized(2.0F, 2.0F) // 默认尺寸，会被 init 覆盖
                            .clientTrackingRange(12) // 确保客户端同步
                            .build("reshape_part")
            );

    // --- Thrown Spears ---
    public static final RegistryObject<EntityType<ThrownWoodenSpear>> THROWN_WOODEN_SPEAR =
            registerMiscTracking("thrown_wooden_spear", 0.5F, 0.5F, ThrownWoodenSpear::new, 4, 20);
    public static final RegistryObject<EntityType<ThrownStoneSpear>> THROWN_STONE_SPEAR =
            registerMiscTracking("thrown_stone_spear", 0.5F, 0.5F, ThrownStoneSpear::new, 4, 20);
    public static final RegistryObject<EntityType<ThrownFlintSpear>> THROWN_FLINT_SPEAR =
            registerMiscTracking("thrown_flint_spear", 0.5F, 0.5F, ThrownFlintSpear::new, 4, 20);
    public static final RegistryObject<EntityType<ThrownCopperSpear>> THROWN_COPPER_SPEAR =
            registerMiscTracking("thrown_copper_spear", 0.5F, 0.5F, ThrownCopperSpear::new, 4, 20);
    public static final RegistryObject<EntityType<ThrownIronSpear>> THROWN_IRON_SPEAR =
            registerMiscTracking("thrown_iron_spear", 0.5F, 0.5F, ThrownIronSpear::new, 4, 20);
    public static final RegistryObject<EntityType<ThrownGoldenSpear>> THROWN_GOLDEN_SPEAR =
            registerMiscTracking("thrown_golden_spear", 0.5F, 0.5F, ThrownGoldenSpear::new, 4, 20);
    public static final RegistryObject<EntityType<ThrownDiamondSpear>> THROWN_DIAMOND_SPEAR =
            registerMiscTracking("thrown_diamond_spear", 0.5F, 0.5F, ThrownDiamondSpear::new, 4, 20);
    public static final RegistryObject<EntityType<ThrownNetheriteSpear>> THROWN_NETHERITE_SPEAR =
            registerMiscTracking("thrown_netherite_spear", 0.5F, 0.5F, ThrownNetheriteSpear::new, 4, 20);
}