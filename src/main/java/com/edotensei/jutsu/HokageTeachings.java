package com.edotensei.jutsu;

import com.edotensei.entity.HokageEntity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Jutsus que os Hokages ensinam ao jogador (interagir com o Hokage mostra as opcoes).
 *
 * O Narutoto guarda natureza de chakra e jutsus como advancements, entao "aprender" = conceder o advancement.
 * Nao ha dependencia de compilacao com o Narutoto: tudo e feito pela API do Minecraft (e reflexao para a flag de interface).
 */
public final class HokageTeachings {
    private HokageTeachings() {}

    public enum Teaching {
        //        id           quem ensina                     advancement do Narutoto           flag extra de interface (opcional)
        MOKUTON("mokuton",     HokageEntity.Kind.HASHIRAMA, "narutoto:nature_wood",       "if_show_wood"),
        HIRAISHIN("hiraishin", HokageEntity.Kind.TOBIRAMA,  "narutoto:learn_hiraishin",   null),
        SUITON("suiton",       HokageEntity.Kind.TOBIRAMA,  "narutoto:nature_water",      null),
        RAITON("raiton",       HokageEntity.Kind.HIRUZEN,   "narutoto:nature_lightning",  null);

        public final String id;
        public final HokageEntity.Kind teacher;
        public final String advancement;
        @Nullable
        public final String uiFlag;

        Teaching(String id, HokageEntity.Kind teacher, String advancement, @Nullable String uiFlag) {
            this.id = id;
            this.teacher = teacher;
            this.advancement = advancement;
            this.uiFlag = uiFlag;
        }

        public Component displayName() {
            return Component.translatable("jutsu.edotensei." + id);
        }
    }

    public static List<Teaching> forKind(HokageEntity.Kind kind) {
        List<Teaching> list = new ArrayList<>();
        for (Teaching t : Teaching.values()) {
            if (t.teacher == kind) list.add(t);
        }
        return list;
    }

    @Nullable
    public static Teaching byId(String id) {
        for (Teaching t : Teaching.values()) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }

    // ------------------------------------------------------------ advancements

    @Nullable
    private static Advancement advancementOf(ServerPlayer player, Teaching t) {
        return player.server.getAdvancements().getAdvancement(new ResourceLocation(t.advancement));
    }

    public static boolean hasLearned(ServerPlayer player, Teaching t) {
        Advancement adv = advancementOf(player, t);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /** Concede o aprendizado. false = o Narutoto nao tem esse advancement (mod nao instalado/versao diferente). */
    private static boolean grant(ServerPlayer player, Teaching t) {
        Advancement adv = advancementOf(player, t);
        if (adv == null) return false;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        if (!progress.isDone()) {
            List<String> remaining = new ArrayList<>();
            for (String criterion : progress.getRemainingCriteria()) remaining.add(criterion);
            for (String criterion : remaining) player.getAdvancements().award(adv, criterion);
        }
        if (t.uiFlag != null) setNarutotoFlag(player, t.uiFlag, true);
        return true;
    }

    /** Liga uma flag booleana do PlayerVariables do Narutoto (ex.: if_show_wood) e sincroniza com o cliente. Falha em silencio. */
    private static void setNarutotoFlag(ServerPlayer player, String field, boolean value) {
        try {
            Class<?> vars = Class.forName("net.mcreator.narutoto.network.NarutotoModVariables");
            Field capField = vars.getField("PLAYER_VARIABLES_CAPABILITY");
            Capability<?> cap = (Capability<?>) capField.get(null);
            Object instance = player.getCapability(cap).orElse(null);
            if (instance == null) return;
            Field flag = instance.getClass().getDeclaredField(field);
            flag.setAccessible(true);
            flag.setBoolean(instance, value);
            Method sync = instance.getClass().getMethod("syncPlayerVariables", Entity.class);
            sync.invoke(instance, player);
        } catch (Throwable ignored) {
            // sem o Narutoto (ou com outra versao) o advancement ja basta
        }
    }

    // ---------------------------------------------------------------- dialogo

    /** Mostra, no chat, o que este Hokage pode ensinar (cada opcao e clicavel). */
    public static void offer(ServerPlayer player, HokageEntity hokage) {
        List<Teaching> options = forKind(hokage.getKind());
        if (options.isEmpty()) return;

        player.sendSystemMessage(Component.translatable("message.edotensei.teach.offer").withStyle(ChatFormatting.YELLOW));
        for (Teaching t : options) {
            if (hasLearned(player, t)) {
                player.sendSystemMessage(Component.translatable("message.edotensei.teach.known", t.displayName())
                        .withStyle(ChatFormatting.GRAY));
            } else {
                Style style = Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/edotensei_aprender " + t.id))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("message.edotensei.teach.hover", t.displayName())));
                player.sendSystemMessage(Component.translatable("message.edotensei.teach.option", t.displayName()).withStyle(style));
            }
        }
    }

    /** Chamado pelo comando /edotensei_aprender (o clique na opcao). Valida que o Hokage certo esta por perto. */
    public static int requestLearn(ServerPlayer player, String id) {
        Teaching t = byId(id);
        if (t == null) return 0;

        List<HokageEntity> near = player.level().getEntitiesOfClass(HokageEntity.class,
                player.getBoundingBox().inflate(8.0D), h -> h.isAlive() && h.getKind() == t.teacher);
        if (near.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.edotensei.teach.too_far",
                    Component.translatable("entity.edotensei." + t.teacher.id)).withStyle(ChatFormatting.RED));
            return 0;
        }

        if (hasLearned(player, t)) {
            player.sendSystemMessage(Component.translatable("message.edotensei.teach.already", t.displayName())
                    .withStyle(ChatFormatting.GRAY));
            return 0;
        }

        if (!grant(player, t)) {
            player.sendSystemMessage(Component.translatable("message.edotensei.teach.unavailable").withStyle(ChatFormatting.RED));
            return 0;
        }

        player.sendSystemMessage(Component.translatable("message.edotensei.teach.learned",
                Component.translatable("entity.edotensei." + t.teacher.id), t.displayName()).withStyle(ChatFormatting.GREEN));
        return 1;
    }
}
