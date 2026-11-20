package com.lineage.client;

import com.lineage.ability.Ability;
import com.lineage.data.LineageData;
import com.lineage.origin.Origin;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Client-side mirror of the server's Lineage state and origin definitions. */
public final class ClientState {
    public static final LineageData DATA = new LineageData();
    public static Map<ResourceLocation, Origin> origins = Collections.emptyMap();
    public static boolean dataReceived = false;
    /** True after we asked the server to set our origin and before it answered. */
    public static boolean awaitingServer = false;

    private ClientState() {}

    public static Origin origin() {
        ResourceLocation id = DATA.getOrigin();
        return id == null ? null : origins.get(id);
    }

    public static List<Ability> activeAbilities() {
        Origin o = origin();
        return o == null ? List.of() : o.activeAbilitiesFor(DATA);
    }

    public static void reset() {
        DATA.reset();
        origins = Collections.emptyMap();
        dataReceived = false;
        awaitingServer = false;
    }
}
