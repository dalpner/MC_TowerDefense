package io.github.dalpner.mctd.net;

import io.github.dalpner.mctd.MctdMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/** Game state shown in the HUD of every player taking part in a game. */
public record HudPayload(boolean active, boolean paused, int wave, int maxWaves, boolean endless, byte phase,
                         int countdown, int gold, int lives, int maxLives, int essences, byte[] levels,
                         int creepsLeft, String difficulty, String layout, List<WaveInfo> upcoming)
        implements CustomPayload {

    public static final CustomPayload.Id<HudPayload> ID = new CustomPayload.Id<>(MctdMod.id("hud"));
    public static final PacketCodec<RegistryByteBuf, HudPayload> CODEC = PacketCodec.of(HudPayload::write, HudPayload::read);

    public static final byte PHASE_BUILD = 0;
    public static final byte PHASE_WAVE = 1;
    public static final byte PHASE_VICTORY = 2;
    public static final byte PHASE_DEFEAT = 3;

    /**
     * Preview of an upcoming wave.
     *
     * @param element   element ordinal or -1 for neutral
     * @param kind      creep kind ordinal
     * @param abilities bit mask of ability ordinals
     */
    public record WaveInfo(int number, byte element, byte kind, byte abilities, int count) {
    }

    public static HudPayload inactive() {
        return new HudPayload(false, false, 0, 0, false, PHASE_BUILD, 0, 0, 0, 0, 0, new byte[6], 0, "", "", List.of());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeBoolean(paused);
        buf.writeVarInt(wave);
        buf.writeVarInt(maxWaves);
        buf.writeBoolean(endless);
        buf.writeByte(phase);
        buf.writeVarInt(countdown);
        buf.writeVarInt(gold);
        buf.writeVarInt(lives);
        buf.writeVarInt(maxLives);
        buf.writeVarInt(essences);
        buf.writeByte(levels.length);
        for (byte level : levels) {
            buf.writeByte(level);
        }
        buf.writeVarInt(creepsLeft);
        buf.writeString(difficulty);
        buf.writeString(layout);
        buf.writeVarInt(upcoming.size());
        for (WaveInfo w : upcoming) {
            buf.writeVarInt(w.number());
            buf.writeByte(w.element());
            buf.writeByte(w.kind());
            buf.writeByte(w.abilities());
            buf.writeVarInt(w.count());
        }
    }

    private static HudPayload read(RegistryByteBuf buf) {
        boolean active = buf.readBoolean();
        boolean paused = buf.readBoolean();
        int wave = buf.readVarInt();
        int maxWaves = buf.readVarInt();
        boolean endless = buf.readBoolean();
        byte phase = buf.readByte();
        int countdown = buf.readVarInt();
        int gold = buf.readVarInt();
        int lives = buf.readVarInt();
        int maxLives = buf.readVarInt();
        int essences = buf.readVarInt();
        int levelCount = buf.readByte();
        byte[] levels = new byte[Math.max(0, levelCount)];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = buf.readByte();
        }
        int creepsLeft = buf.readVarInt();
        String difficulty = buf.readString();
        String layout = buf.readString();
        int count = Math.min(buf.readVarInt(), 16);
        List<WaveInfo> upcoming = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            upcoming.add(new WaveInfo(buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readByte(), buf.readVarInt()));
        }
        return new HudPayload(active, paused, wave, maxWaves, endless, phase, countdown, gold, lives, maxLives,
                essences, levels, creepsLeft, difficulty, layout, upcoming);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
