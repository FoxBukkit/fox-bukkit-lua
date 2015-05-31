package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.core.FoxBukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.concurrent.LinkedBlockingQueue;

public class LuaThread extends Thread implements Listener {
    private final Globals g;

    private final LinkedBlockingQueue<Runnable> pendingTasks =  new LinkedBlockingQueue<>();

    public void invoke(Runnable runnable) {
        pendingTasks.add(runnable);
        this.notify();
    }

    public LuaThread() {
        this(JsePlatform.debugGlobals());
    }

    public LuaThread(Globals g) {
        this.g = g;
    }

    @Override
    public void run() {
        try {
            while(true) {
                //TODO: Run initializer Lua
                Runnable runnable;
                while ((runnable = pendingTasks.poll()) != null) {
                    runnable.run();
                }
                this.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void terminate() {
        HandlerList.unregisterAll(FoxBukkit.instance);
    }
}
