package com.lineage.origin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lineage.Lineage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads origins from data/<namespace>/lineage/origins/*.json. Reloads with /reload. */
public class OriginManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static volatile Map<ResourceLocation, Origin> origins = Collections.emptyMap();
    private static volatile Map<ResourceLocation, String> raw = Collections.emptyMap();

    public OriginManager() {
        super(GSON, "lineage/origins");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, Origin> loaded = new LinkedHashMap<>();
        Map<ResourceLocation, String> rawJson = new LinkedHashMap<>();
        objects.forEach((id, element) -> {
            try {
                JsonObject obj = GsonHelper.convertToJsonObject(element, "origin");
                loaded.put(id, Origin.fromJson(id, obj));
                rawJson.put(id, GSON.toJson(obj));
            } catch (Exception ex) {
                Lineage.LOGGER.error("Failed to load origin {}: {}", id, ex.getMessage());
            }
        });
        origins = Collections.unmodifiableMap(loaded);
        raw = Collections.unmodifiableMap(rawJson);
        Lineage.LOGGER.info("Loaded {} origin(s)", loaded.size());
    }

    public static Origin get(ResourceLocation id) { return id == null ? null : origins.get(id); }
    public static Iterable<ResourceLocation> ids() { return origins.keySet(); }
    public static Map<ResourceLocation, String> rawJson() { return raw; }
}
