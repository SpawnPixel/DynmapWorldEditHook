package com.spawnpixel.mc.dynmapworldedithook;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class DynmapWorldeditHook extends JavaPlugin implements Listener, CommandExecutor {
    private static Logger log;

    Plugin dynmap;
    DynmapAPI dynmapAPI;
    Plugin worldEdit;
    WorldEdit worldEditAPI;
    boolean reload = false;

    @Override
    public void onLoad() {
        log = this.getLogger();
        info("Loaded");
    }

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(this, this);
        pm.registerEvents(new OurServerListener(), this);

        dynmap = pm.getPlugin("dynmap");
        worldEdit = pm.getPlugin("WorldEdit");
        if (dynmap != null && worldEdit != null && dynmap.isEnabled() && worldEdit.isEnabled()) {
            activate();
        }
    }

    private class OurServerListener implements Listener {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginEnable(PluginEnableEvent event) {
            Plugin p = event.getPlugin();
            String name = p.getDescription().getName();
            if (name.equals("dynmap") || name.equals("WorldEdit")) {
                if (dynmap.isEnabled() && worldEdit.isEnabled()) {
                    activate();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static void info(String msg) {
        log.log(Level.INFO, msg);
    }

    void activate() {
        if (reload) {
            worldEditAPI.getEventBus().unregister(this);
        } else {
            reload = true;
        }

        info("Activating...");

        dynmapAPI = (DynmapAPI) dynmap;
        worldEditAPI = WorldEdit.getInstance();

        worldEditAPI.getEventBus().register(this);

        info("Activated");
    }

    @Subscribe
    public void onEditSessionEvent(EditSessionEvent event) {
        Actor actor = event.getActor();
        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        LocalSession localSession = manager.get(actor);
        Region region;
        World selectionWorld = localSession.getSelectionWorld();
        try {
            if (selectionWorld == null) throw new IncompleteRegionException();
            region = localSession.getSelection(selectionWorld);
        } catch (IncompleteRegionException ex) {
            return;
        }

        CuboidRegion boundingBox = region.getBoundingBox();
        BlockVector3 from = boundingBox.getPos1();
        BlockVector3 to = boundingBox.getPos2();
        String worldName = event.getWorld().getName();

        org.bukkit.World world = Bukkit.getWorld(worldName);

        Location fromLocation = new Location(world, Math.min(from.getBlockX(), to.getBlockX()), Math.min(from.getBlockY(), to.getBlockY()), Math.min(from.getBlockZ(), to.getBlockZ()));
        Location toLocation = new Location(world, Math.max(from.getBlockX(), to.getBlockX()), Math.max(from.getBlockY(), to.getBlockY()), Math.max(from.getBlockZ(), to.getBlockZ()));

        dynmapAPI.triggerRenderOfVolume(
                fromLocation,
                toLocation
        );
    }
}
