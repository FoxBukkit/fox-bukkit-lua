package com.foxelbox.foxbukkit.lua;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandManager implements Listener {
    private final LuaThread luaThread;

    public CommandManager(LuaThread luaThread) {
        this.luaThread = luaThread;
        FoxBukkitLua.instance.getServer().getPluginManager().registerEvents(this, FoxBukkitLua.instance);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {

    }
}
