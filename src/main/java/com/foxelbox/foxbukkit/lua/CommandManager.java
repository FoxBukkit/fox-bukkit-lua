package com.foxelbox.foxbukkit.lua;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class CommandManager implements Listener {
    private final LuaThread luaThread;

    public CommandManager(LuaThread luaThread) {
        this.luaThread = luaThread;
        FoxBukkitLua.instance.getServer().getPluginManager().registerEvents(this, FoxBukkitLua.instance);
    }

    public void unregisterAll() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(CommandManagerMaster.CommandEvent event) {

    }
}
