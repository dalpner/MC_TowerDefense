package io.github.dalpner.mctd.client;

import io.github.dalpner.mctd.core.Ability;
import io.github.dalpner.mctd.core.CreepKind;
import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.net.HudPayload;
import io.github.dalpner.mctd.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Game panel in the top left corner and tower details next to the crosshair. */
public final class HudOverlay {
    private static final int PADDING = 3;
    private static final int LINE = 10;

    private HudOverlay() {
    }

    static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null) {
            return;
        }
        HudPayload hud = ClientState.hud();
        if (hud.active()) {
            renderPanel(context, client.textRenderer, hud);
        }
        TowerPreview.Target target = TowerPreview.target();
        if (target != null) {
            renderTowerInfo(context, client.textRenderer, target);
        }
    }

    private static void renderPanel(DrawContext context, TextRenderer font, HudPayload hud) {
        List<Text> lines = new ArrayList<>();
        MutableText waveLine = hud.endless() && hud.wave() > hud.maxWaves()
                ? Text.translatable("mctd.hud.wave_endless", hud.wave()).formatted(Formatting.GOLD)
                : Text.translatable("mctd.hud.wave", hud.wave(), hud.maxWaves()).formatted(Formatting.GOLD);
        waveLine.append(Text.literal("  ").append(Text.translatable("mctd.difficulty." + hud.difficulty()))
                .formatted(Formatting.DARK_GRAY));
        lines.add(waveLine);
        lines.add(Text.literal("❤ " + hud.lives() + "/" + hud.maxLives()).formatted(Formatting.RED)
                .append(Text.literal("   ⛃ " + hud.gold()).formatted(Formatting.YELLOW))
                .append(Text.literal("   ✦ " + hud.essences()).formatted(Formatting.AQUA)));
        MutableText levels = Text.empty();
        for (Element e : Element.values()) {
            int level = e.ordinal() < hud.levels().length ? hud.levels()[e.ordinal()] : 0;
            levels.append(Text.literal(Texts.symbol(e) + level + "  ")
                    .formatted(level > 0 ? Texts.color(e) : Formatting.DARK_GRAY));
        }
        lines.add(levels);
        if (hud.paused()) {
            lines.add(Text.translatable("mctd.hud.paused").formatted(Formatting.GRAY));
        } else {
            switch (hud.phase()) {
                case HudPayload.PHASE_BUILD -> lines.add(Text.translatable("mctd.hud.next_in",
                        Texts.seconds(ClientState.countdown())).formatted(Formatting.GREEN));
                case HudPayload.PHASE_WAVE -> lines.add(Text.translatable("mctd.hud.creeps_left", hud.creepsLeft())
                        .formatted(Formatting.RED));
                case HudPayload.PHASE_VICTORY -> lines.add(Text.translatable("mctd.hud.victory").formatted(Formatting.GOLD));
                default -> lines.add(Text.translatable("mctd.hud.defeat").formatted(Formatting.DARK_RED));
            }
        }
        if (!hud.upcoming().isEmpty()) {
            lines.add(Text.translatable("mctd.hud.upcoming").formatted(Formatting.GRAY));
            for (HudPayload.WaveInfo info : hud.upcoming()) {
                lines.add(waveInfo(info));
            }
        }
        int width = 0;
        for (Text line : lines) {
            width = Math.max(width, font.getWidth(line));
        }
        int x = 4;
        int y = 4;
        int height = lines.size() * LINE;
        context.fill(x - PADDING, y - PADDING, x + width + PADDING, y + height + PADDING - 2, 0x90000000);
        for (Text line : lines) {
            context.drawTextWithShadow(font, line, x, y, 0xFFFFFF);
            y += LINE;
        }
    }

    private static Text waveInfo(HudPayload.WaveInfo info) {
        Element element = info.element() >= 0 && info.element() < Element.values().length
                ? Element.values()[info.element()] : null;
        CreepKind kind = info.kind() >= 0 && info.kind() < CreepKind.values().length
                ? CreepKind.values()[info.kind()] : CreepKind.NORMAL;
        MutableText text = Text.literal(" " + info.number() + ": ").formatted(Formatting.GRAY)
                .append(Texts.element(element))
                .append(Text.literal(" · ").formatted(Formatting.DARK_GRAY))
                .append(Texts.kind(kind).formatted(kind.bossLike() ? Formatting.DARK_RED : Formatting.WHITE))
                .append(Text.literal(" ×" + info.count()).formatted(Formatting.DARK_GRAY));
        for (Ability a : Ability.values()) {
            if ((info.abilities() & (1 << a.ordinal())) != 0) {
                text.append(Text.literal(" " + Texts.symbol(a)).formatted(Formatting.LIGHT_PURPLE));
            }
        }
        return text;
    }

    private static void renderTowerInfo(DrawContext context, TextRenderer font, TowerPreview.Target target) {
        TowerType type = target.type();
        List<Text> lines = new ArrayList<>();
        lines.add(Texts.towerName(type, target.tier()));
        lines.addAll(Texts.towerStats(type, target.tier()));
        lines.add(Text.translatable("mctd.hud.tower_hint").formatted(Formatting.DARK_GRAY));
        int width = 0;
        for (Text line : lines) {
            width = Math.max(width, font.getWidth(line));
        }
        int x = context.getScaledWindowWidth() / 2 + 14;
        int y = context.getScaledWindowHeight() / 2 - lines.size() * LINE / 2;
        context.fill(x - PADDING, y - PADDING, x + width + PADDING, y + lines.size() * LINE + PADDING - 2, 0x90000000);
        for (Text line : lines) {
            context.drawTextWithShadow(font, line, x, y, 0xFFFFFF);
            y += LINE;
        }
    }
}
