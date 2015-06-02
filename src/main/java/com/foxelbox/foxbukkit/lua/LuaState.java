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
import org.luaj.vm2.Prototype;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class LuaState implements Listener, Runnable {
    final Object luaLock = new Object();
    Globals g;
    private volatile boolean running = true;
    private final String module;

    private final EnhancedChatMessageManager enhancedChatMessageManager;
    private final EventManager eventManager = new EventManager(this);
    private final CommandManager commandManager = new CommandManager(this);

    public LuaState(String module) {
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

        this.module = module;
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

    public Runnable createLuaValueRunnable(final LuaValue function) {
        return new Runnable() {
            @Override
            public void run() {
                synchronized (luaLock) {
                    function.call();
                }
            }
        };
    }

    public FoxBukkitLua getFoxBukkitLua() {
        return FoxBukkitLua.instance;
    }

    public String getRootDir() {
        return FoxBukkitLua.instance.getLuaFolder().getAbsolutePath();
    }

    public String getModuleDir() {
        return new File(FoxBukkitLua.instance.getLuaModulesFolder(), module).getAbsolutePath();
    }

    private static final HashMap<String, Prototype> packagedCompiles = new HashMap<>();
    static void clearCache() {
        packagedCompiles.clear();
    }
    public LuaValue loadPackagedFile(String name) {
        Prototype p = packagedCompiles.get(name);
        if(p == null) {
            InputStream inputStream = LuaState.class.getResourceAsStream("/lua/" + name);
            if(inputStream == null) {
                return Globals.error("open "+name+": File not found");
            }
            synchronized (luaLock) {
                try {
                    p = g.loadPrototype(inputStream, name, "bt");
                } catch (IOException e) {
                    return Globals.error("compile "+name+": "+e);
                }
            }
        }
        synchronized (luaLock) {
            try {
                return g.loader.load(p, "bt", g);
            } catch (IOException e) {
                return Globals.error("load "+name+": "+e);
            }
        }
    }

    @Override
    public void run() {
        synchronized (luaLock) {
            g = JsePlatform.debugGlobals();
            g.set("__LUA_STATE", CoerceJavaToLua.coerce(this));
            File overrideInit = new File(getRootDir(), "init.lua");
            if(overrideInit.exists()) {
                g.loadfile(overrideInit.getAbsolutePath()).call();
            } else {
                loadPackagedFile("init.lua").call();
            }
        }
    }

    public synchronized void terminate() {
        if(!running) {
            return;
        }
        running = false;

        synchronized (this) {
            synchronized (luaLock) {
                running = false;
                eventManager.unregisterAll();
                commandManager.unregisterAll();
            }
        }
    }
}
