package com.lineage.client;

import com.lineage.Lineage;
import com.lineage.client.screen.EvolutionScreen;
import com.lineage.client.screen.OriginSelectScreen;
import com.lineage.network.Network;
import com.lineage.network.Packets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
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

    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
        }
    }

    /** Draws a jagged pair of lightning wings and branching horns in the final form. */
    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || !ClientState.DATA.hasNode("lightning_god")) return;
        var origin = ClientState.origin();
        if (origin == null || !origin.id().getPath().equals("quickstep")) return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();

        float[][] wing = {
                {0.15f, 1.42f, 0.14f}, {0.43f, 1.67f, 0.22f}, {0.62f, 1.50f, 0.29f},
                {0.82f, 1.78f, 0.37f}, {1.02f, 1.48f, 0.42f}, {1.20f, 1.62f, 0.50f},
                {1.02f, 1.20f, 0.39f}, {0.73f, 1.05f, 0.29f}, {0.15f, 1.42f, 0.14f}
        };
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < wing.length - 1; i++) {
                drawLightningLine(lines, pose, side, wing[i], wing[i + 1], 58, 208, 255, 240);
            }
            drawLightningLine(lines, pose, side, wing[0], wing[3], 222, 251, 255, 255);
            drawLightningLine(lines, pose, side, wing[0], wing[5], 222, 251, 255, 255);
            float[][] horn = {
                    {0.13f, 1.84f, -0.02f}, {0.23f, 2.05f, 0.00f},
                    {0.34f, 2.01f, 0.02f}, {0.44f, 2.25f, 0.03f}
            };
            for (int i = 0; i < horn.length - 1; i++) {
                drawLightningLine(lines, pose, side, horn[i], horn[i + 1], 88, 221, 255, 255);
            }
        }
        poseStack.popPose();
    }

    private static void drawLightningLine(VertexConsumer lines, PoseStack.Pose pose, int side,
                                          float[] from, float[] to, int red, int green, int blue, int alpha) {
        lines.vertex(pose.pose(), side * from[0], from[1], from[2]).color(red, green, blue, alpha)
                .normal(pose.normal(), 0, 1, 0).endVertex();
        lines.vertex(pose.pose(), side * to[0], to[1], to[2]).color(red, green, blue, alpha)
                .normal(pose.normal(), 0, 1, 0).endVertex();
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
            case "quickstep" -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.ENCHANT;
        };
        boolean evolved = !ClientState.DATA.unlockedIds().isEmpty();
        ParticleOptions evolvedAura = switch (origin.id().getPath()) {
            case "ember" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "merling" -> ParticleTypes.GLOW;
            case "windwalker" -> ParticleTypes.END_ROD;
            case "glowcap" -> ParticleTypes.HAPPY_VILLAGER;
            case "quickstep" -> ParticleTypes.ELECTRIC_SPARK;
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
        if (origin.id().getPath().equals("quickstep") && ClientState.DATA.hasNode("lightning_god")
                && player.tickCount % 2 == 0) {
            for (int side : new int[]{-1, 1}) {
                mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        player.getX() + side * (0.45 + random.nextDouble() * 0.7),
                        player.getY() + 1.05 + random.nextDouble() * 0.75,
                        player.getZ() + 0.25 + random.nextDouble() * 0.3,
                        (random.nextDouble() - 0.5) * 0.08, 0.015, (random.nextDouble() - 0.5) * 0.08);
            }
            if (player.tickCount % 6 == 0) {
                mc.level.addParticle(ParticleTypes.FLASH, player.getX(), player.getY() + 2.0, player.getZ(), 0, 0, 0);
            }
        }
    }
}
