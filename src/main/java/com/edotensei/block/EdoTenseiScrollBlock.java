package com.edotensei.block;

import com.edotensei.EdoTargets;
import com.edotensei.ModRegistry;
import com.edotensei.item.BloodSyringeItem;
import com.edotensei.jutsu.Jutsus;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pergaminho do Edo Tensei (bloco no chao).
 * - Clique direito com a seringa de sangue: guarda o sangue.
 * - Shift + clique direito (maos vazias): executa o ritual no mob selado para o qual o pergaminho aponta.
 */
public class EdoTenseiScrollBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
    private static final double RANGE = 12.0D;
    /** cos(~53 graus): o alvo precisa estar na direcao para onde o pergaminho aponta. */
    private static final double ALIGN_DOT = 0.6D;

    public EdoTenseiScrollBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .strength(0.3F)
                .sound(SoundType.WOOL)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EdoTenseiScrollBlockEntity(pos, state);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!level.isClientSide && be instanceof EdoTenseiScrollBlockEntity scroll && !scroll.getEntityId().isEmpty()) {
                Block.popResource(level, pos, BloodSyringeItem.create(scroll.getEntityId()));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EdoTenseiScrollBlockEntity scroll)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Colocar a seringa com sangue dentro do pergaminho
        if (held.is(ModRegistry.BLOOD_SYRINGE.get())) {
            if (!scroll.getEntityId().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.edotensei.scroll_full"), true);
                return InteractionResult.CONSUME;
            }
            String id = BloodSyringeItem.getEntity(held);
            if (id.isEmpty()) return InteractionResult.CONSUME;
            scroll.setEntityId(id);
            if (!player.getAbilities().instabuild) held.shrink(1);
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.edotensei.blood_inserted",
                    EdoTargets.nameOf(id)), true);
            return InteractionResult.CONSUME;
        }

        // Ritual: shift + clique (com as duas maos vazias, regra do proprio Minecraft)
        if (player.isShiftKeyDown()) {
            ritual((ServerLevel) level, pos, state, scroll, (ServerPlayer) player);
            return InteractionResult.CONSUME;
        }

        // Status
        String id = scroll.getEntityId();
        if (id.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.edotensei.scroll_empty"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.edotensei.scroll_loaded",
                    EdoTargets.nameOf(id)), true);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean isSealedBy(Mob mob, Player player) {
        return mob.getPersistentData().hasUUID(Jutsus.SEALER_TAG)
                && mob.getPersistentData().getUUID(Jutsus.SEALER_TAG).equals(player.getUUID());
    }

    private static void ritual(ServerLevel level, BlockPos pos, BlockState state, EdoTenseiScrollBlockEntity scroll, ServerPlayer player) {
        if (!player.isCreative() && !Jutsus.isLearned(player, Jutsus.EDO_TENSEI)) {
            player.displayClientMessage(Component.translatable("message.edotensei.not_learned"), true);
            return;
        }
        String entityId = scroll.getEntityId();
        if (!EdoTargets.isValidType(entityId)) {
            player.displayClientMessage(Component.translatable("message.edotensei.scroll_empty"), true);
            return;
        }

        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 facing = Vec3.atLowerCornerOf(state.getValue(FACING).getNormal());
        AABB area = new AABB(pos).inflate(RANGE);

        Mob best = null;
        double bestDist = Double.MAX_VALUE;
        boolean sealedButNotAligned = false;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, m -> m.isAlive() && isSealedBy(m, player))) {
            Vec3 diff = mob.position().subtract(center);
            Vec3 flat = new Vec3(diff.x, 0.0D, diff.z);
            if (flat.lengthSqr() > 0.01D && flat.normalize().dot(facing) < ALIGN_DOT) {
                sealedButNotAligned = true;
                continue;
            }
            double dist = diff.lengthSqr();
            if (dist < bestDist) {
                bestDist = dist;
                best = mob;
            }
        }

        if (best == null) {
            player.displayClientMessage(Component.translatable(
                    sealedButNotAligned ? "message.edotensei.target_not_aligned" : "message.edotensei.no_sealed_target"), true);
            return;
        }

        // O mob selado morre (sem deixar corpo)...
        Vec3 at = best.position();
        float yRot = best.getYRot();
        best.getPersistentData().putBoolean(EdoTargets.NO_CORPSE_TAG, true);
        best.setNoAi(false);
        best.invulnerableTime = 0;
        best.hurt(level.damageSources().magic(), Float.MAX_VALUE);
        if (best.isAlive()) best.discard();

        // ...e quem tinha o sangue toma o lugar dele, obedecendo quem invocou.
        if (EdoTargets.summon(level, entityId, at, yRot, player) == null) {
            player.displayClientMessage(Component.translatable("message.edotensei.scroll_empty"), true);
            return;
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.0D, at.z, 80, 0.5D, 1.0D, 0.5D, 0.05D);
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.4F);

        scroll.setEntityId("");
        player.displayClientMessage(Component.translatable("message.edotensei.summoned",
                EdoTargets.nameOf(entityId)), true);
    }
}
