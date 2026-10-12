package io.github.dalpner.mctd.client;

import io.github.dalpner.mctd.block.TowerBlock;
import io.github.dalpner.mctd.net.FxPayload;
import io.github.dalpner.mctd.net.HudPayload;
import io.github.dalpner.mctd.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.render.RenderLayer;

public class MctdClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        for (TowerBlock block : ModBlocks.towers()) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());
        }
        ClientPlayNetworking.registerGlobalReceiver(HudPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientState.update(payload)));
        ClientPlayNetworking.registerGlobalReceiver(FxPayload.ID,
                (payload, context) -> context.client().execute(() -> FxRenderer.play(payload)));
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> HudOverlay.render(drawContext));
        ClientTickEvents.END_CLIENT_TICK.register(TowerPreview::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.reset());
    }
}
