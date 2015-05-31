package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.core.FoxBukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

public class LuaThread extends Thread implements Listener {
    private final Globals g;

    private final LinkedBlockingQueue<Runnable> pendingTasks =  new LinkedBlockingQueue<>();

    private final ChatMessageManager chatMessageManager = new ChatMessageManager(this);
    private final EventManager eventManager = new EventManager(this);

    public void invoke(Runnable runnable) {
        pendingTasks.add(runnable);
        synchronized (this) {
            this.notify();
        }
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
            g.set("__LUA_THREAD__", new LuaUserdata(this));
            g.set("__ROOTDIR__", FoxBukkit.instance.getLuaFolder().getAbsolutePath());
            g.loadfile(new File(FoxBukkit.instance.getLuaFolder(), "init.lua").getAbsolutePath()).call();
            while(true) {
                Runnable runnable;
                while ((runnable = pendingTasks.poll()) != null) {
                    runnable.run();
                }
                synchronized (this) {
                    this.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void terminate() {
        HandlerList.unregisterAll(FoxBukkit.instance);
        pendingTasks.clear();
    }
}
