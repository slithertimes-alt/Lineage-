package com.lineage.client;

import com.google.gson.JsonParser;
import com.lineage.Lineage;
import com.lineage.origin.Origin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Entry points called by network packets on the client. Only ever loaded on the client. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void onDefinitions(Map<ResourceLocation, String> raw) {
        Map<ResourceLocation, Origin> map = new LinkedHashMap<>();
        raw.forEach((id, json) -> {
            try {
                map.put(id, Origin.fromJson(id, JsonParser.parseString(json).getAsJsonObject()));
            } catch (Exception e) {
                Lineage.LOGGER.error("Client failed to parse origin {}: {}", id, e.getMessage());
            }
        });
        ClientState.origins = map;
    }

    public static void onData(CompoundTag tag) {
        ClientState.DATA.load(tag);
        ClientState.dataReceived = true;
        ClientState.awaitingServer = false;
    }
}
