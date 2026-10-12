package io.github.dalpner.mctd.game;

import io.github.dalpner.mctd.MctdMod;
import io.github.dalpner.mctd.core.ArenaLayout;
import io.github.dalpner.mctd.core.Difficulty;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.core.sim.GameConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Converts sessions to and from NBT. */
final class SessionCodec {
    private SessionCodec() {
    }

    static NbtCompound write(Session session) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("slot", session.slot());
        nbt.putUuid("owner", session.owner());
        NbtList members = new NbtList();
        for (UUID uuid : session.members()) {
            NbtCompound m = new NbtCompound();
            m.putUuid("uuid", uuid);
            ReturnPoint point = session.returnPoints().get(uuid);
            if (point != null) {
                m.put("return", point.toNbt());
            }
            members.add(m);
        }
        nbt.put("members", members);

        Game.Snapshot s = session.game().snapshot();
        GameConfig c = s.config();
        nbt.putString("difficulty", c.difficulty().id());
        nbt.putString("layout", c.layout().id());
        nbt.putLong("seed", c.seed());
        nbt.putInt("maxWaves", c.maxWaves());
        nbt.putBoolean("guardians", c.elementGuardians());
        nbt.putInt("gold", s.gold());
        nbt.putInt("lives", s.lives());
        nbt.putInt("essences", s.essences());
        nbt.putIntArray("levels", s.levels());
        nbt.putInt("nextWave", s.nextWave());
        nbt.putInt("essenceUpTo", s.essenceGrantedUpTo());
        nbt.putBoolean("endless", s.endless());
        nbt.putString("phase", s.phase());
        nbt.putInt("kills", s.kills());
        nbt.putInt("leaks", s.leaks());
        nbt.putLong("goldEarned", s.goldEarned());
        nbt.putDouble("damage", s.damageDealt());
        NbtList towers = new NbtList();
        for (Game.TowerState t : s.towers()) {
            NbtCompound tn = new NbtCompound();
            tn.putInt("x", t.x());
            tn.putInt("z", t.z());
            tn.putString("type", t.type());
            tn.putInt("tier", t.tier());
            tn.putString("mode", t.mode());
            tn.putInt("kills", t.kills());
            tn.putDouble("damage", t.damage());
            tn.putInt("invested", t.invested());
            towers.add(tn);
        }
        nbt.put("towers", towers);
        return nbt;
    }

    static Session read(MinecraftServer server, ServerWorld world, NbtCompound nbt) {
        try {
            Difficulty difficulty = Difficulty.byId(nbt.getString("difficulty"));
            ArenaLayout layout = ArenaLayout.byId(nbt.getString("layout"));
            if (difficulty == null || layout == null || !nbt.containsUuid("owner")) {
                return null;
            }
            GameConfig config = new GameConfig(difficulty, layout, nbt.getLong("seed"),
                    Math.max(1, nbt.getInt("maxWaves")), nbt.getBoolean("guardians"));
            List<Game.TowerState> towers = new ArrayList<>();
            NbtList towerList = nbt.getList("towers", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < towerList.size(); i++) {
                NbtCompound tn = towerList.getCompound(i);
                towers.add(new Game.TowerState(tn.getInt("x"), tn.getInt("z"), tn.getString("type"), tn.getInt("tier"),
                        tn.getString("mode"), tn.getInt("kills"), tn.getDouble("damage"), tn.getInt("invested")));
            }
            Game.Snapshot snapshot = new Game.Snapshot(config, nbt.getInt("gold"), nbt.getInt("lives"),
                    nbt.getInt("essences"), nbt.getIntArray("levels"), nbt.getInt("nextWave"), nbt.getInt("essenceUpTo"),
                    nbt.getBoolean("endless"), nbt.getString("phase"), towers, nbt.getInt("kills"), nbt.getInt("leaks"),
                    nbt.getLong("goldEarned"), nbt.getDouble("damage"));
            Game game = Game.restore(snapshot);
            int slot = nbt.getInt("slot");
            Session session = new Session(server, world, slot, SessionManager.originOf(slot), game, nbt.getUuid("owner"));
            Map<UUID, ReturnPoint> returns = session.returnPoints();
            NbtList members = nbt.getList("members", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < members.size(); i++) {
                NbtCompound m = members.getCompound(i);
                if (!m.containsUuid("uuid")) {
                    continue;
                }
                UUID uuid = m.getUuid("uuid");
                session.members().add(uuid);
                if (m.contains("return", NbtElement.COMPOUND_TYPE)) {
                    returns.put(uuid, ReturnPoint.fromNbt(m.getCompound("return")));
                }
            }
            return session;
        } catch (RuntimeException e) {
            MctdMod.LOGGER.error("Could not load a tower defense game", e);
            return null;
        }
    }

    static BlockPos origin(int slot) {
        return SessionManager.originOf(slot);
    }
}
