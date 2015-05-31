package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.core.FoxBukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

public class LuaThread extends Thread implements Listener {
    private final Globals g;

    private final LinkedBlockingQueue<Invoker> pendingTasks =  new LinkedBlockingQueue<>();

    private final ChatMessageManager chatMessageManager = new ChatMessageManager(this);
    private final EventManager eventManager = new EventManager(this);

    public EventManager getEventManager() {
        return eventManager;
    }

    public ChatMessageManager getChatMessageManager() {
        return chatMessageManager;
    }

    public static abstract class Invoker implements Runnable {
        private volatile LuaValue result = null;
        protected volatile boolean completed = false;
        protected volatile boolean running = false;

        private final LuaThread luaThread;
        public Invoker(LuaThread luaThread) {
            this.luaThread = luaThread;
        }

        public final void waitOnCompletion() {
            try {
                synchronized (this) {
                    while (!this.completed) {
                            this.wait();
                    }
                }
            } catch (InterruptedException e) { }
        }

        public final LuaValue getResult() {
            run(true);
            return result;
        }

        @Override
        public final void run() {
            run(true);
        }

        public final synchronized void run(boolean wait) {
            if(running || completed) {
                return;
            }
            running = true;
            synchronized (luaThread) {
                luaThread.pendingTasks.add(this);
                luaThread.notify();
            }
            if(wait) {
                waitOnCompletion();
            }
        }

        protected final void start() {
            result = invoke();
            completed = true;
            synchronized (this) {
                this.notify();
            }
        }

        protected abstract LuaValue invoke();
    }

    public static class LuaFunctionInvoker extends Invoker {
        private final LuaFunction function;

        public LuaFunctionInvoker(LuaThread luaThread, LuaFunction function) {
            super(luaThread);
            this.function = function;
        }

        @Override
        protected LuaValue invoke() {
            return function.call();
        }
    }

    public void runOnMainThread(final LuaFunction function) {
        FoxBukkit.instance.getServer().getScheduler().scheduleSyncDelayedTask(FoxBukkit.instance, new LuaFunctionInvoker(LuaThread.this, function));
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
            synchronized (g) {
                g.set("__LUA_THREAD__", CoerceJavaToLua.coerce(this));
                g.set("__ROOTDIR__", FoxBukkit.instance.getLuaFolder().getAbsolutePath());
                g.loadfile(new File(FoxBukkit.instance.getLuaFolder(), "init.lua").getAbsolutePath()).call();
            }
            while(true) {
                Invoker invoker;
                while ((invoker = pendingTasks.poll()) != null) {
                    invoker.start();
                }
                synchronized (this) {
                    if(pendingTasks.isEmpty()) {
                        this.wait();
                    }
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
