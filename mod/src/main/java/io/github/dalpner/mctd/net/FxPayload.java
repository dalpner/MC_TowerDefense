package io.github.dalpner.mctd.net;

import io.github.dalpner.mctd.MctdMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/** A batch of visual effects (tower shots, explosions, deaths...) that the client turns into particles and sounds. */
public record FxPayload(List<Entry> entries) implements CustomPayload {
    public static final CustomPayload.Id<FxPayload> ID = new CustomPayload.Id<>(MctdMod.id("fx"));
    public static final PacketCodec<RegistryByteBuf, FxPayload> CODEC = PacketCodec.of(FxPayload::write, FxPayload::read);

    public static final byte BEAM = 0;
    public static final byte CHAIN = 1;
    public static final byte SPLASH = 2;
    public static final byte PULSE = 3;
    public static final byte DEATH = 4;
    public static final byte LEAK = 5;
    public static final byte BUILD = 6;
    public static final byte UPGRADE = 7;
    public static final byte SELL = 8;
    public static final byte HEAL = 9;
    public static final byte FALL = 10;
    public static final byte RISE = 11;
    public static final byte ELEMENT = 12;
    public static final byte SPAWN = 13;

    /** Visual flavour of an attack. */
    public static final byte STYLE_ARROW = 0;
    public static final byte STYLE_CANNON = 1;
    public static final byte STYLE_ELEMENT = 2;

    /**
     * One effect from (x1,y1,z1) to (x2,y2,z2).
     *
     * @param element element ordinal or -1 for neutral
     * @param style   one of the STYLE constants
     * @param size    radius for area effects, tier for shots
     */
    public record Entry(byte type, byte element, byte style, float x1, float y1, float z1,
                        float x2, float y2, float z2, float size) {
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeByte(e.type());
            buf.writeByte(e.element());
            buf.writeByte(e.style());
            buf.writeFloat(e.x1());
            buf.writeFloat(e.y1());
            buf.writeFloat(e.z1());
            buf.writeFloat(e.x2());
            buf.writeFloat(e.y2());
            buf.writeFloat(e.z2());
            buf.writeFloat(e.size());
        }
    }

    private static FxPayload read(RegistryByteBuf buf) {
        int count = Math.min(buf.readVarInt(), 4096);
        List<Entry> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new Entry(buf.readByte(), buf.readByte(), buf.readByte(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
        }
        return new FxPayload(list);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
