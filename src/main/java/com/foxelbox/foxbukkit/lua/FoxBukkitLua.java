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

import com.foxelbox.dependencies.config.Configuration;
import com.foxelbox.dependencies.redis.RedisManager;
import com.foxelbox.dependencies.threading.SimpleThreadCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class FoxBukkitLua extends JavaPlugin {
    public static FoxBukkitLua instance;

    public Configuration configuration;
    public RedisManager redisManager;
    private final HashMap<String, LuaState> luaThreadList = new HashMap<>();
    public CommandManagerMaster commandManagerMaster;

    public File getLuaFolder() {
        return new File(getDataFolder(), "base");
    }

    public File getLuaModulesFolder() {
        return new File(getDataFolder(), "modules");
    }

    public File getLuaScriptsFolder() {
        return new File(getDataFolder(), "scripts");
    }

    private void cleanupLuaThreadList() {
        synchronized (luaThreadList) {
            for (LuaState t : luaThreadList.values()) {
                if(!t.isRunning()) {
                    luaThreadList.remove(t.getModule());
                }
            }
        }
    }

    private void terminateLuaThread(String module) {
        synchronized (luaThreadList) {
            LuaState luaState = luaThreadList.remove(module);
            if(luaState != null) {
                luaState.terminate();
            }
        }
    }

    private void terminateAllLuaThreads() {
        synchronized (luaThreadList) {
            for(LuaState t : luaThreadList.values()) {
                t.terminate();
            }
            luaThreadList.clear();
        }
    }

    private void startLuaThread(String module, boolean overwrite) {
        synchronized (luaThreadList) {
            LuaState luaState = luaThreadList.get(module);
            if(luaState != null && luaState.isRunning()) {
                if(overwrite) {
                    luaState.terminate();
                } else {
                    return;
                }
            }
            luaState = new LuaState(module);
            luaThreadList.put(module, luaState);
            luaState.run();
        }
    }

    private void startAllLuaThreads(boolean overwrite) {
        synchronized (luaThreadList) {
            for(String module : listLuaModules()) {
                startLuaThread(module, overwrite);
            }
        }
    }

    private Collection<String> listLuaModules() {
        LinkedList<String> luaModules = new LinkedList<>();
        for(File module : getLuaModulesFolder().listFiles()) {
            if(module.canRead() && module.isDirectory()) {
                luaModules.add(module.getName());
            } else {
                System.err.println("Invalid Lua module " + module.getName() + " (not a directory or unreadable)");
            }
        }
        return luaModules;
    }

    private void restartAllLuaThreads() {
        terminateAllLuaThreads();
        startAllLuaThreads(false);
    }

    @Override
    public void onDisable() {
        terminateAllLuaThreads();
        configuration = null;
        redisManager = null;
        commandManagerMaster = null;
        instance = null;
        LuaState.clearCache();
    }

    private StringBuilder makeMessageBuilder() {
        return new StringBuilder("\u00a75[FB] \u00a7f");
    }

    @Override
    public void onEnable() {
        getLuaFolder().mkdirs();
        getLuaModulesFolder().mkdirs();
        getLuaScriptsFolder().mkdirs();

        instance = this;
        configuration = new Configuration(getDataFolder());
        redisManager = new RedisManager(new SimpleThreadCreator(), configuration);
        commandManagerMaster = new CommandManagerMaster();

        restartAllLuaThreads();

        getServer().getPluginCommand("lua_reload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if(strings.length > 0) {
                    startLuaThread(strings[0], true);
                    commandSender.sendMessage(makeMessageBuilder().append("Reloaded Lua module ").append(strings[0]).toString());
                    return true;
                }

                restartAllLuaThreads();
                commandSender.sendMessage(makeMessageBuilder().append("Reloaded all Lua modules").toString());
                return true;
            }
        });

        getServer().getPluginCommand("lua_load").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if(strings.length < 1) {
                    return false;
                }

                startLuaThread(strings[0], false);
                commandSender.sendMessage(makeMessageBuilder().append("Loaded Lua module ").append(strings[0]).toString());
                return true;
            }
        });

        getServer().getPluginCommand("lua_unload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if(strings.length < 1) {
                    return false;
                }

                terminateLuaThread(strings[0]);
                commandSender.sendMessage(makeMessageBuilder().append("Unloaded Lua module ").append(strings[0]).toString());
                return true;
            }
        });

        getServer().getPluginCommand("lua_run").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if(strings.length < 2) {
                    return false;
                }

                LuaState luaState;
                synchronized (luaThreadList) {
                    luaState = luaThreadList.get(strings[0]);
                }
                if(luaState == null) {
                    return false;
                }

                LuaValue code;
                try {
                    synchronized (luaState.luaLock) {
                        code = luaState.g.load(Utils.concatArray(strings, 1, ""));
                    }
                } catch (Exception e) {
                    commandSender.sendMessage(makeMessageBuilder().append("Error in Lua code: ").append(e.getMessage()).toString());
                    return true;
                }

                final LuaValue ret;
                synchronized (luaState.luaLock) {
                    ret = code.call();
                }
                commandSender.sendMessage(makeMessageBuilder().append("Code = ").append(ret).toString());
                return true;
            }
        });

        getServer().getPluginCommand("lua_runfile").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if(strings.length < 2) {
                    return false;
                }

                LuaState luaState;
                synchronized (luaThreadList) {
                    luaState = luaThreadList.get(strings[0]);
                }
                if(luaState == null) {
                    return false;
                }

                LuaValue code;
                try {
                    synchronized (luaState.luaLock) {
                        code = luaState.g.loadfile(new File(getLuaScriptsFolder(), strings[1]).getAbsolutePath());
                    }
                } catch (Exception e) {
                    commandSender.sendMessage(makeMessageBuilder().append("Error in Lua file: ").append(e.getMessage()).toString());
                    return true;
                }

                final LuaValue ret;
                synchronized (luaState.luaLock) {
                    ret = code.call();
                }
                commandSender.sendMessage(makeMessageBuilder().append("Code = ").append(ret).toString());
                return true;
            }
        });

        getServer().getPluginCommand("lua_list").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                cleanupLuaThreadList();
                StringBuilder ret = makeMessageBuilder().append("Lua modules: ");
                boolean isFirst = true;
                for(String module : listLuaModules()) {
                    if(isFirst) {
                        isFirst = false;
                    } else {
                        ret.append(", ");
                    }
                    ret.append(luaThreadList.containsKey(module) ? "\u00a72" : "\u00a74");
                    ret.append(module);
                }
                commandSender.sendMessage(ret.toString());
                return true;
            }
        });
    }
}
