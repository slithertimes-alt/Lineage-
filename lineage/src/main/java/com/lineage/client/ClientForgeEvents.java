package com.lineage.client;

import com.lineage.Lineage;
import com.lineage.client.screen.EvolutionScreen;
import com.lineage.client.screen.OriginSelectScreen;
import com.lineage.network.Network;
import com.lineage.network.Packets;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Lineage.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // First join (or /lineage reset): force the origin selection screen.
        if (ClientState.dataReceived && !ClientState.awaitingServer && ClientState.origin() == null
                && !ClientState.origins.isEmpty() && mc.screen == null) {
            mc.setScreen(new OriginSelectScreen());
        }

        boolean canAct = mc.screen == null && ClientState.origin() != null;
        for (int i = 0; i < KeyBinds.ABILITY.length; i++) {
            KeyMapping key = KeyBinds.ABILITY[i];
            while (key.consumeClick()) {
                if (canAct) Network.CHANNEL.sendToServer(new Packets.UseAbility(i));
            }
        }
        while (KeyBinds.EVOLUTIONS.consumeClick()) {
            if (canAct) mc.setScreen(new EvolutionScreen());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
    }
}
