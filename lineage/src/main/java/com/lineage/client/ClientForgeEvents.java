package com.lineage.client;

import com.lineage.Lineage;
import com.lineage.client.screen.EvolutionScreen;
import com.lineage.client.screen.OriginSelectScreen;
import com.lineage.network.Network;
import com.lineage.network.Packets;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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

        spawnBodyAura(mc);

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

    /** Small origin-specific particles make a player's lineage visible in-world. */
    private static void spawnBodyAura(Minecraft mc) {
        if (mc.level == null || !ClientState.dataReceived || mc.player.tickCount % 4 != 0) return;
        var origin = ClientState.origin();
        if (origin == null || mc.player.isInvisible()) return;

        ParticleOptions aura = switch (origin.id().getPath()) {
            case "ember" -> ParticleTypes.FLAME;
            case "merling" -> ParticleTypes.BUBBLE;
            case "windwalker" -> ParticleTypes.CLOUD;
            case "glowcap" -> ParticleTypes.SPORE_BLOSSOM_AIR;
            default -> ParticleTypes.ENCHANT;
        };
        boolean evolved = !ClientState.DATA.unlockedIds().isEmpty();
        ParticleOptions evolvedAura = switch (origin.id().getPath()) {
            case "ember" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "merling" -> ParticleTypes.GLOW;
            case "windwalker" -> ParticleTypes.END_ROD;
            case "glowcap" -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.WITCH;
        };

        var player = mc.player;
        var random = player.getRandom();
        for (int i = 0; i < 2; i++) {
            mc.level.addParticle(aura,
                    player.getX() + (random.nextDouble() - 0.5) * 0.8,
                    player.getY() + 0.15 + random.nextDouble() * 1.45,
                    player.getZ() + (random.nextDouble() - 0.5) * 0.8,
                    0, 0.012, 0);
        }
        if (evolved && player.tickCount % 8 == 0) {
            mc.level.addParticle(evolvedAura,
                    player.getX() + (random.nextDouble() - 0.5) * 0.6,
                    player.getY() + 0.3 + random.nextDouble() * 1.2,
                    player.getZ() + (random.nextDouble() - 0.5) * 0.6,
                    0, 0.02, 0);
        }
    }
}
