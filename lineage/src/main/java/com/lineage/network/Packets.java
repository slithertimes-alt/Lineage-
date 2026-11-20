package com.lineage.network;

import com.lineage.ability.AbilityManager;
import com.lineage.client.ClientHooks;
import com.lineage.data.LineageData;
import com.lineage.origin.Evolution;
import com.lineage.origin.EvolutionNode;
import com.lineage.origin.Origin;
import com.lineage.origin.OriginManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class Packets {
    private static final int MAX_JSON = 1 << 18;

    private Packets() {}

    // ============ Server -> Client ============

    public static class SyncDefinitions {
        private final Map<ResourceLocation, String> json;

        public SyncDefinitions(Map<ResourceLocation, String> json) { this.json = json; }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(json.size());
            json.forEach((k, v) -> {
                buf.writeResourceLocation(k);
                buf.writeUtf(v, MAX_JSON);
            });
        }

        public static SyncDefinitions decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            Map<ResourceLocation, String> m = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) m.put(buf.readResourceLocation(), buf.readUtf(MAX_JSON));
            return new SyncDefinitions(m);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onDefinitions(json)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SyncData {
        private final CompoundTag tag;

        public SyncData(CompoundTag tag) { this.tag = tag; }

        public void encode(FriendlyByteBuf buf) { buf.writeNbt(tag); }

        public static SyncData decode(FriendlyByteBuf buf) { return new SyncData(buf.readNbt()); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onData(tag)));
            ctx.get().setPacketHandled(true);
        }
    }

    // ============ Client -> Server ============

    public static class ChooseOrigin {
        private final ResourceLocation origin;

        public ChooseOrigin(ResourceLocation origin) { this.origin = origin; }

        public void encode(FriendlyByteBuf buf) { buf.writeResourceLocation(origin); }

        public static ChooseOrigin decode(FriendlyByteBuf buf) { return new ChooseOrigin(buf.readResourceLocation()); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null) return;
                LineageData d = LineageData.get(p);
                boolean hasValidOrigin = OriginManager.get(d.getOrigin()) != null;
                if (!hasValidOrigin && OriginManager.get(origin) != null) {
                    AbilityManager.setOrigin(p, origin);
                } else {
                    Network.sendData(p); // always answer so the client UI stays in sync
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class UseAbility {
        private final int slot;

        public UseAbility(int slot) { this.slot = slot; }

        public void encode(FriendlyByteBuf buf) { buf.writeVarInt(slot); }

        public static UseAbility decode(FriendlyByteBuf buf) { return new UseAbility(buf.readVarInt()); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p != null) AbilityManager.use(p, slot);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class UnlockNode {
        private final String nodeId;

        public UnlockNode(String nodeId) { this.nodeId = nodeId; }

        public void encode(FriendlyByteBuf buf) { buf.writeUtf(nodeId, 128); }

        public static UnlockNode decode(FriendlyByteBuf buf) { return new UnlockNode(buf.readUtf(128)); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null) return;
                LineageData d = LineageData.get(p);
                Origin o = OriginManager.get(d.getOrigin());
                if (o != null) {
                    EvolutionNode n = o.node(nodeId);
                    if (n != null && Evolution.canUnlock(o, n, d)) {
                        d.spendPoints(n.cost());
                        d.unlock(n.id());
                        AbilityManager.refresh(p);
                        p.displayClientMessage(Component.literal("\u00a7aEvolved: \u00a7f" + n.name()), true);
                    }
                }
                Network.sendData(p);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
