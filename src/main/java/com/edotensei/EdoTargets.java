package com.edotensei;

import com.edotensei.entity.HokageEntity;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Regras de quem pode ser revivido pelo Edo Tensei e como invocar. */
public final class EdoTargets {
    private EdoTargets() {}

    public static final String OWNER_TAG = "edotensei_owner";
    public static final String NO_CORPSE_TAG = "edotensei_no_corpse";

    private static final TagKey<EntityType<?>> NO_LIFE = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "no_life"));
    private static final TagKey<EntityType<?>> CLONE = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "clone"));

    /** Mobs "de verdade" do Narutoto (NPCs, ninjas, bandidos, bijuus...). Ignora dummies, clones e projeteis. */
    public static boolean isRevivable(Entity entity) {
        if (!(entity instanceof Mob) || entity instanceof HokageEntity) return false;
        EntityType<?> type = entity.getType();
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (key == null || !key.getNamespace().equals("narutoto")) return false;
        if (type.getCategory() == MobCategory.MISC) return false;
        return !type.is(NO_LIFE) && !type.is(CLONE);
    }

    public static Component nameOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && ForgeRegistries.ENTITY_TYPES.containsKey(rl)) {
            return ForgeRegistries.ENTITY_TYPES.getValue(rl).getDescription();
        }
        return Component.literal(id);
    }

    public static boolean isValidType(String id) {
        return !id.isEmpty() && EntityType.byString(id).isPresent();
    }

    @Nullable
    public static UUID ownerOf(Entity entity) {
        if (entity instanceof HokageEntity h) return h.getOwnerUUID();
        return entity.getPersistentData().hasUUID(OWNER_TAG) ? entity.getPersistentData().getUUID(OWNER_TAG) : null;
    }

    /** Invoca o mob de id "namespace:nome" no lugar indicado, obedecendo o jogador. */
    @Nullable
    public static Mob summon(ServerLevel level, String id, Vec3 at, float yRot, ServerPlayer owner) {
        Optional<EntityType<?>> type = EntityType.byString(id);
        if (type.isEmpty()) return null;
        Entity created = type.get().create(level);
        if (!(created instanceof Mob mob)) {
            if (created != null) created.discard();
            return null;
        }
        mob.moveTo(at.x, at.y, at.z, yRot, 0.0F);
        if (mob instanceof HokageEntity hokage) {
            hokage.setOwner(owner.getUUID());
        } else {
            mob.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
            mob.setPersistenceRequired();
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(owner.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(mob); // o EntityJoinLevelEvent adiciona as metas de seguir/proteger
        return mob;
    }
}
