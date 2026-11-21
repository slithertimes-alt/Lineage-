package com.lineage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

/** Model-space accessories: head and torso transforms also follow swimming, flight and crouching. */
public final class LightningGodLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final DustParticleOptions BLUE_SPARK = new DustParticleOptions(new Vector3f(0.08f, 0.43f, 1f), 0.55f);
    private static final DustParticleOptions CYAN_SPARK = new DustParticleOptions(new Vector3f(0.32f, 0.85f, 1f), 0.35f);

    public LightningGodLayer(PlayerRenderer parent) { super(parent); }

    @Override
    public void render(PoseStack stack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbAmount, float partialTick, float age, float headYaw, float headPitch) {
        var origin = ClientState.origin();
        if (player != Minecraft.getInstance().player || player.isInvisible() || player.isSpectator()
                || origin == null || !origin.id().getPath().equals("quickstep")) return;
        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
        long phase = (long) Math.floor(age / 2f);
        long seed = phase * 7919L + player.getId() * 997L;
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        boolean god = ClientState.DATA.hasNode("lightning_god");

        // Arcs follow individual limbs rather than hovering in fixed world coordinates.
        bodyArc(stack, buffer, model.body, seed, 0.75, 0.30, god ? 3 : 1);
        bodyArc(stack, buffer, model.leftArm, seed + 13, 0.65, 0.16, 1);
        bodyArc(stack, buffer, model.rightArm, seed + 29, 0.65, 0.16, 1);
        if (!god) return;
        bodyArc(stack, buffer, model.leftLeg, seed + 41, 0.72, 0.16, 1);
        bodyArc(stack, buffer, model.rightLeg, seed + 67, 0.72, 0.16, 1);

        stack.pushPose();
        model.head.translateAndRotate(stack);
        for (int side : new int[]{-1, 1}) horn(stack.last().pose(), buffer, side, seed);
        stack.popPose();

        stack.pushPose();
        model.body.translateAndRotate(stack);
        for (int side : new int[]{-1, 1}) {
            stack.pushPose();
            stack.translate(side * 0.18, 0.16, 0.19);
            float flap = 20f + (float) Math.sin(age * (player.isFallFlying() ? 0.10 : 0.15)) * 18f;
            stack.mulPose(Axis.YP.rotationDegrees(side * flap));
            stack.mulPose(Axis.ZP.rotationDegrees(side * (float) Math.sin(age * 0.09) * 4f));
            wing(stack.last().pose(), buffer, side, age, seed + side * 101);
            stack.popPose();
        }
        stack.popPose();
    }

    private static void bodyArc(PoseStack stack, VertexConsumer out, ModelPart part, long seed,
                                double length, double radius, int count) {
        Random random = new Random(seed);
        stack.pushPose();
        part.translateAndRotate(stack);
        for (int i = 0; i < count; i++) {
            // Some arcs briefly disappear between discharges.
            if (random.nextFloat() < 0.18f) continue;
            double angle = random.nextDouble() * Math.PI * 2;
            Vec3 start = new Vec3(Math.cos(angle) * radius, random.nextDouble() * 0.15, Math.sin(angle) * radius);
            Vec3 end = new Vec3(Math.cos(angle + 1.9) * radius, length, Math.sin(angle + 1.9) * radius);
            bolt(stack.last().pose(), out, start, end, random.nextLong(), 0.07, 0.009f, 1f);
        }
        stack.popPose();
    }

    private static void horn(Matrix4f matrix, VertexConsumer out, int side, long seed) {
        Vec3[] points = {
                new Vec3(side * 0.17, -0.49, -0.04), new Vec3(side * 0.24, -0.66, 0.01),
                new Vec3(side * 0.31, -0.78, 0.09), new Vec3(side * 0.30, -0.93, 0.12),
                new Vec3(side * 0.42, -1.08, 0.15)
        };
        for (int i = 0; i < points.length - 1; i++) {
            // Solid tapered blue crystal, with a bright electrical seam.
            prism(matrix, out, points[i], points[i + 1], 0.055f * (1 - i / 4f),
                    0.055f * (1 - (i + 1) / 4f), 0.04f, 0.26f, 1f, 0.85f);
            bolt(matrix, out, points[i], points[i + 1], seed + side * 37 + i, 0.025, 0.007f, 1f);
        }
        bolt(matrix, out, points[2], points[2].add(side * 0.15, -0.14, -0.04), seed + side,
                0.035, 0.006f, 0.8f);
    }

    private static void wing(Matrix4f matrix, VertexConsumer out, int side, float age, long seed) {
        Vec3 root = Vec3.ZERO;
        Vec3 elbow = new Vec3(side * 0.50, -0.36, 0.10);
        Vec3 tip = new Vec3(side * 1.55, -0.73, 0.18);
        bolt(matrix, out, root, elbow, seed, 0.04, 0.024f, 1f);
        bolt(matrix, out, elbow, tip, seed + 1, 0.07, 0.018f, 1f);
        Vec3 previous = tip;
        // Five swept feather-like branches: a full wing silhouette instead of a closed polygon.
        for (int i = 0; i < 5; i++) {
            float ripple = (float) Math.sin(age * 0.18 + i * 0.7) * 0.055f;
            Vec3 shoulder = elbow.lerp(tip, i * 0.12);
            Vec3 end = new Vec3(side * (1.55 - i * 0.17), -0.38 + i * 0.24 + ripple, 0.25 + i * 0.045);
            Vec3 bend = shoulder.lerp(end, 0.47).add(side * 0.10, -0.08, 0.02);
            bolt(matrix, out, shoulder, bend, seed + 11 + i, 0.055, 0.012f, 1f);
            bolt(matrix, out, bend, end, seed + 21 + i, 0.075, 0.009f, 1f);
            bolt(matrix, out, previous, end, seed + 31 + i, 0.08, 0.005f, 0.55f);
            // A faint blue membrane gives the wing volume between the bright branches.
            quad(matrix, out, root, shoulder, end, previous, 0.02f, 0.18f, 1f, 0.045f);
            previous = end;
        }
    }

    private static void bolt(Matrix4f matrix, VertexConsumer out, Vec3 start, Vec3 end,
                             long seed, double jitter, float thickness, float alpha) {
        Random random = new Random(seed);
        Vec3 previous = start;
        for (int i = 1; i <= 6; i++) {
            Vec3 next = start.lerp(end, i / 6.0);
            if (i < 6) next = next.add((random.nextDouble() - 0.5) * jitter,
                    (random.nextDouble() - 0.5) * jitter, (random.nextDouble() - 0.5) * jitter);
            prism(matrix, out, previous, next, thickness * 3.5f, thickness * 3.5f, 0.03f, 0.22f, 1f, alpha * 0.13f);
            prism(matrix, out, previous, next, thickness * 1.6f, thickness * 1.6f, 0.06f, 0.5f, 1f, alpha * 0.55f);
            prism(matrix, out, previous, next, thickness * 0.5f, thickness * 0.5f, 0.65f, 0.91f, 1f, alpha);
            previous = next;
        }
    }

    /** Four-sided tubes have visible thickness from all camera angles. */
    private static void prism(Matrix4f matrix, VertexConsumer out, Vec3 a, Vec3 b, float ra, float rb,
                              float r, float g, float blue, float alpha) {
        Vec3 direction = b.subtract(a).normalize();
        Vec3 u = direction.cross(Math.abs(direction.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize();
        Vec3 v = direction.cross(u).normalize();
        Vec3[] offsets = {u, v, u.scale(-1), v.scale(-1)};
        for (int i = 0; i < 4; i++) {
            Vec3 p = offsets[i], q = offsets[(i + 1) % 4];
            quad(matrix, out, a.add(p.scale(ra)), a.add(q.scale(ra)), b.add(q.scale(rb)), b.add(p.scale(rb)), r, g, blue, alpha);
        }
    }

    private static void quad(Matrix4f matrix, VertexConsumer out, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float r, float g, float blue, float alpha) {
        for (Vec3 point : new Vec3[]{a, b, c, d})
            out.vertex(matrix, (float) point.x, (float) point.y, (float) point.z).color(r, g, blue, alpha).endVertex();
    }

    /** Small blue motes trail behind motion; attached lightning itself is rendered by the layer. */
    public static void spawnTrail(Minecraft mc) {
        var player = mc.player;
        if (player == null || mc.level == null || mc.isPaused() || player.isSpectator()) return;
        var random = player.getRandom();
        Vec3 movement = player.getDeltaMovement();
        boolean moving = movement.horizontalDistanceSqr() > 0.01;
        int count = moving ? 4 : 2;
        for (int i = 0; i < count; i++) {
            double x = player.getX() - movement.x * 1.4 + (random.nextDouble() - 0.5) * 0.65;
            double z = player.getZ() - movement.z * 1.4 + (random.nextDouble() - 0.5) * 0.65;
            mc.level.addParticle(i % 2 == 0 ? BLUE_SPARK : CYAN_SPARK, x,
                    player.getY() + 0.15 + random.nextDouble() * 1.25, z, 0, 0.012, 0);
        }
    }
}
