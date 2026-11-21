package com.lineage.event;

import com.lineage.Lineage;
import com.lineage.ability.AbilityManager;
import com.lineage.command.LineageCommand;
import com.lineage.data.LineageData;
import com.lineage.data.LineageProvider;
import com.lineage.network.Network;
import com.lineage.origin.Evolution;
import com.lineage.origin.OriginManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Lineage.MODID)
public final class CommonEvents {
    private CommonEvents() {}

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new OriginManager());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        LineageCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(Lineage.MODID, "data"), new LineageProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        event.getOriginal().reviveCaps();
        LineageData.get(newPlayer).copyFrom(LineageData.get(event.getOriginal()));
        event.getOriginal().invalidateCaps();
    }

    /** Fires on login and on /reload: push definitions + state to the client. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            fullSync(player);
        } else {
            event.getPlayerList().getPlayers().forEach(CommonEvents::fullSync);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) fullSync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) fullSync(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Evolution.onDimensionChange(p, event.getTo().location());
            fullSync(p);
        }
    }

    private static void fullSync(ServerPlayer p) {
        Network.sendDefinitions(p);
        AbilityManager.refresh(p);
        Network.sendData(p);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer p)) return;
        int t = p.tickCount;
        if (t % 10 == 0) AbilityManager.tick(p);
        if (t % 20 == 0) Evolution.tickSources(p);
        if (t % 60 == 0) {
            LineageData d = LineageData.get(p);
            if (d.dirty) Network.sendData(p);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        float amount = AbilityManager.modifyDamage(p, event.getSource(), event.getAmount());
        if (amount <= 0f) event.setCanceled(true);
        else event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            Evolution.onKill(killer, event.getEntity());
        }
    }
}
