/**
 * This file is part of FoxBukkitLua-plugin.
 *
 * FoxBukkitLua-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitLua-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitLua-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.lua.compiler.LuaJC;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.IOException;

public class LuaState implements Listener, Runnable {
    final Object luaLock = new Object();
    Globals g;
    private volatile boolean running = true;
    private final String module;

    final FoxBukkitLua plugin;

    private final EnhancedPermissionManager enhancedPermissionManager;
    private final EventManager eventManager = new EventManager(this);
    private final CommandManager commandManager = new CommandManager(this);

    private static Plugin enhancedPermissionPlugin = null;
    private static boolean loaded = false;

    public static synchronized void load(FoxBukkitLua plugin) {
        if(loaded) {
            return;
        }
        loaded = true;

        enhancedPermissionPlugin = plugin.getServer().getPluginManager().getPlugin("FoxBukkitPermissions");

        if(enhancedPermissionPlugin == null) {
            System.err.println("Could not find FoxBukkitPermissions. Disabling enhanced permissions API.");
        } else {
            System.out.println("Hooked FoxBukkitPermissions. Enabled enhanced permissions API.");
        }
    }

    public static synchronized void unload() {
        enhancedPermissionPlugin = null;
        loaded = false;
    }

    public LuaState(String module, FoxBukkitLua plugin) {
        this.plugin = plugin;

        if(enhancedPermissionPlugin != null) {
            enhancedPermissionManager = new EnhancedPermissionManager(this, enhancedPermissionPlugin);
        } else {
            enhancedPermissionManager = null;
        }

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

    public EnhancedPermissionManager getEnhancedPermissionManager() {
        return enhancedPermissionManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public Runnable createLuaValueRunnable(final LuaValue function) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (luaLock) {
                    function.call();
                }
            }
        };
    }

    public FoxBukkitLua getFoxBukkitLua() {
        return plugin;
    }

    public String getRootDir() {
        try {
            return plugin.getLuaFolder().getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

    public String getModuleDir() {
        try {
            return new File(plugin.getLuaModulesFolder(), module).getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

    public LuaValue loadPackagedFile(String name) {
        synchronized (luaLock) {
            try {
                String className = "lua." + LuaJC.toStandardJavaClassName(name);
                LuaFunction value = (LuaFunction)Class.forName(className).newInstance();
                value.initupvalue1(g);
                return value;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static boolean initialized = false;
    private synchronized void initialize() {
        if(initialized) {
            return;
        }
        initialized = true;

        File overrideInit = new File(getRootDir(), "boot.lua");
        if(overrideInit.exists()) {
            g.loadfile(overrideInit.getAbsolutePath()).call();
        } else {
            loadPackagedFile("boot").call();
        }
    }

    @Override
    public void run() {
        synchronized (luaLock) {
            g = JsePlatform.standardGlobals();

            try {
                LuaJC.install(g, plugin.getDataFolder().getCanonicalPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            initialize();

            g.set("__LUA_STATE", CoerceJavaToLua.coerce(this));
            File overrideInit = new File(getRootDir(), "init.lua");
            if(overrideInit.exists()) {
                g.loadfile(overrideInit.getAbsolutePath()).call();
            } else {
                loadPackagedFile("init").call();
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
