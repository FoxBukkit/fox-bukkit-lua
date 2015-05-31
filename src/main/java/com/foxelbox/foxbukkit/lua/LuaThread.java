package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.core.FoxBukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;

public class LuaThread extends Thread implements Listener {
    private final Globals g;

    public LuaThread() {
        this(JsePlatform.debugGlobals());
    }

    public LuaThread(Globals g) {
        this.g = g;
    }

    @Override
    public void run() {

    }

    public void terminate() {
        HandlerList.unregisterAll(FoxBukkit.instance);
    }
}
