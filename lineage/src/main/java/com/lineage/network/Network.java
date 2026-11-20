package com.lineage.network;

import com.lineage.Lineage;
import com.lineage.data.LineageData;
import com.lineage.origin.OriginManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Lineage.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Network() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, Packets.SyncDefinitions.class, Packets.SyncDefinitions::encode,
                Packets.SyncDefinitions::decode, Packets.SyncDefinitions::handle);
        CHANNEL.registerMessage(id++, Packets.SyncData.class, Packets.SyncData::encode,
                Packets.SyncData::decode, Packets.SyncData::handle);
        CHANNEL.registerMessage(id++, Packets.ChooseOrigin.class, Packets.ChooseOrigin::encode,
                Packets.ChooseOrigin::decode, Packets.ChooseOrigin::handle);
        CHANNEL.registerMessage(id++, Packets.UseAbility.class, Packets.UseAbility::encode,
                Packets.UseAbility::decode, Packets.UseAbility::handle);
        CHANNEL.registerMessage(id++, Packets.UnlockNode.class, Packets.UnlockNode::encode,
                Packets.UnlockNode::decode, Packets.UnlockNode::handle);
    }

    /** Sends all loaded origin definitions (as JSON) so the client UI can render them. */
    public static void sendDefinitions(ServerPlayer p) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Packets.SyncDefinitions(OriginManager.rawJson()));
    }

    /** Sends this player's Lineage state to their client. */
    public static void sendData(ServerPlayer p) {
        LineageData d = LineageData.get(p);
        d.dirty = false;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Packets.SyncData(d.save()));
    }
}
