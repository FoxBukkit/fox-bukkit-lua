/**
 * This file is part of FoxBukkitLua.
 *
 * FoxBukkitLua is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitLua is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitLua.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.lua;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

public class LuaThread extends Thread implements Listener {
    public final Globals g;
    private volatile boolean running = true;
    private final String module;

    private final LinkedBlockingQueue<Invoker> pendingTasks =  new LinkedBlockingQueue<>();

    private final EnhancedChatMessageManager enhancedChatMessageManager;
    private final EventManager eventManager = new EventManager(this);
    private final CommandManager commandManager = new CommandManager(this);

    public LuaThread(String module) {
        this(JsePlatform.debugGlobals(), module);
    }

    public LuaThread(Globals g, String module) {
        EnhancedChatMessageManager ecmm = null;
        try {
            Plugin ecp = FoxBukkitLua.instance.getServer().getPluginManager().getPlugin("FoxBukkitChatComponent");
            if(ecp != null) {
                ecmm = new EnhancedChatMessageManager(this, ecp);
            }
        } catch (Throwable t) {
            ecmm = null;
        }
        if(ecmm == null) {
            System.err.println("Could not find FoxBukkitChatComponent. Disabling enhanced chat API.");
        }
        enhancedChatMessageManager = ecmm;

        this.g = g;
        this.module = module;
        setName("LuaThread - " + module);
    }

    public String getModule() {
        return module;
    }

    public boolean isRunning() {
        return running;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public EnhancedChatMessageManager getEnhancedChatMessageManager() {
        return enhancedChatMessageManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public static abstract class Invoker implements Runnable {
        private volatile LuaValue result = null;
        protected volatile boolean running = false;

        private final LuaThread luaThread;
        public Invoker(LuaThread luaThread) {
            this.luaThread = luaThread;
        }

        public final Invoker waitOnCompletion() {
            try {
                synchronized (this) {
                    while (this.running) {
                        this.wait();
                    }
                }
            } catch (InterruptedException e) { }
            return this;
        }

        public final Invoker reset() {
            synchronized (this) {
                if(running) {
                    waitOnCompletion();
                }
                result = null;
            }
            return this;
        }

        public final LuaValue getResult() {
            run(true);
            return result;
        }

        @Override
        public final void run() {
            run(true);
        }

        public final void run(boolean wait) {
            synchronized (this) {
                if (!running) {
                    running = true;
                    synchronized (luaThread) {
                        luaThread.pendingTasks.add(this);
                        luaThread.notify();
                    }
                }
                if (wait) {
                    waitOnCompletion();
                }
            }
        }

        protected final void start() {
            synchronized (this) {
                try {
                    synchronized (luaThread.g) {
                        result = invoke();
                    }
                } catch (Exception e) {
                    System.err.println("Exception running Invoker");
                    e.printStackTrace();
                    result = null;
                }
                running = false;
                this.notify();
            }
        }

        protected abstract LuaValue invoke();
    }

    public static class LuaFunctionInvoker extends Invoker {
        private final LuaValue function;

        public LuaFunctionInvoker(LuaThread luaThread, LuaValue function) {
            super(luaThread);
            this.function = function;
        }

        @Override
        protected LuaValue invoke() {
            return function.call();
        }
    }

    public void runOnMainThread(final LuaValue function) {
        FoxBukkitLua.instance.getServer().getScheduler().scheduleSyncDelayedTask(FoxBukkitLua.instance, new LuaFunctionInvoker(LuaThread.this, function));
    }

    public void runOnLuaThread(final LuaValue function) {
        new LuaFunctionInvoker(LuaThread.this, function).run(false);
    }

    @Override
    public void run() {
        try {
            synchronized (g) {
                g.set("__LUA_THREAD__", CoerceJavaToLua.coerce(this));
                g.set("__ROOTDIR__", FoxBukkitLua.instance.getLuaFolder().getAbsolutePath());
                g.set("__MODULEDIR__", new File(FoxBukkitLua.instance.getLuaModulesFolder(), module).getAbsolutePath());
                g.loadfile(new File(FoxBukkitLua.instance.getLuaFolder(), "init.lua").getAbsolutePath()).call();
            }
            while (running) {
                Invoker invoker;
                while ((invoker = pendingTasks.poll()) != null) {
                    invoker.start();
                }
                synchronized (this) {
                    if (pendingTasks.isEmpty()) {
                        this.wait();
                    }
                }
            }
        } catch (InterruptedException e) {

        } catch (Exception e) {
            e.printStackTrace();
            terminate(false);
        }
    }

    public void terminate() {
        terminate(true);
    }

    private synchronized void terminate(boolean doJoin) {
        if(!running) {
            return;
        }
        running = false;

        synchronized (this) {
            synchronized (g) {
                running = false;
                pendingTasks.clear();
                eventManager.unregisterAll();
                if(doJoin) {
                    this.notify();
                }
            }
        }

        if(doJoin) {
            try {
                this.join();
            } catch (Exception e) { }

            //Ensure there are no leftover events. At this point the thread has ended so it is impossible for more to come up
            eventManager.unregisterAll();
            pendingTasks.clear();
        }
    }
}
