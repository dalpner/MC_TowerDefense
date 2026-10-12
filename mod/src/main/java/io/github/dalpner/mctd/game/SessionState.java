package io.github.dalpner.mctd.game;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

/** Saves all running games with the world (stored in the overworld's data folder as mctd_sessions.dat). */
public class SessionState extends PersistentState {
    public static final String NAME = "mctd_sessions";
    public static final PersistentState.Type<SessionState> TYPE =
            new PersistentState.Type<>(SessionState::new, SessionState::fromNbt, null);

    private NbtList loaded = new NbtList();

    public SessionState() {
    }

    private static SessionState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        SessionState state = new SessionState();
        state.loaded = nbt.getList("sessions", NbtElement.COMPOUND_TYPE);
        return state;
    }

    /** Sessions read from disk when the world was loaded. */
    public NbtList loaded() {
        return loaded;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.put("sessions", SessionManager.writeSessions());
        return nbt;
    }
}
