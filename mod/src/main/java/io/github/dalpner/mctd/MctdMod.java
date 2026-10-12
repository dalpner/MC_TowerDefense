package io.github.dalpner.mctd;

import io.github.dalpner.mctd.command.TdCommand;
import io.github.dalpner.mctd.game.Interactions;
import io.github.dalpner.mctd.game.SessionManager;
import io.github.dalpner.mctd.game.SmokeTest;
import io.github.dalpner.mctd.net.FxPayload;
import io.github.dalpner.mctd.net.HudPayload;
import io.github.dalpner.mctd.registry.ModBlocks;
import io.github.dalpner.mctd.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MC Tower Defense: Element-TD style tower defense inside Minecraft. */
public class MctdMod implements ModInitializer {
    public static final String MOD_ID = "mctd";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModItems.register();
        PayloadTypeRegistry.playS2C().register(HudPayload.ID, HudPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FxPayload.ID, FxPayload.CODEC);
        TdCommand.register();
        SessionManager.registerEvents();
        Interactions.register();
        SmokeTest.register();
        LOGGER.info("MC Tower Defense loaded: {} towers ready", ModBlocks.towers().size());
    }
}
