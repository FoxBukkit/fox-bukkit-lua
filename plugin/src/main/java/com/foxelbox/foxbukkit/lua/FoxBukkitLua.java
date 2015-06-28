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

import com.foxelbox.dependencies.config.Configuration;
import com.foxelbox.dependencies.redis.RedisManager;
import com.foxelbox.dependencies.threading.SimpleThreadCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class FoxBukkitLua extends JavaPlugin {
    public Configuration configuration;
    public RedisManager redisManager;
    private final HashMap<String, LuaState> luaStates = new HashMap<>();
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

    private void cleanupLuaStateList() {
        synchronized (luaStates) {
            for (LuaState t : luaStates.values()) {
                if(!t.isRunning()) {
                    luaStates.remove(t.getModule());
                }
            }
        }
    }

    private void terminateLuaState(String module) {
        synchronized (luaStates) {
            LuaState luaState = luaStates.remove(module);
            if(luaState != null) {
                luaState.terminate();
            }
        }
    }

    private void terminateAllLuaStates() {
        synchronized (luaStates) {
            for(LuaState t : luaStates.values()) {
                t.terminate();
            }
            luaStates.clear();
        }
    }

    private void startLuaState(String module, boolean overwrite) {
        try {
            synchronized (luaStates) {
                LuaState luaState = luaStates.get(module);
                if (luaState != null && luaState.isRunning()) {
                    if (overwrite) {
                        luaState.terminate();
                    } else {
                        return;
                    }
                }
                luaState = new LuaState(module, this);
                luaStates.put(module, luaState);
                luaState.run();
            }
        } catch (Exception e) {
            System.err.println("Error starting Lua state " + module);
            e.printStackTrace();
        }
    }

    private void startAllLuaStates(boolean overwrite) {
        synchronized (luaStates) {
            for(String module : listLuaModules()) {
                startLuaState(module, overwrite);
            }
        }
    }

    private Collection<String> listLuaModules() {
        LinkedList<String> luaModules = new LinkedList<>();
        for(File module : getLuaModulesFolder().listFiles()) {
            if(module.getName().charAt(0) == '.') {
                continue;
            }
            if(module.canRead() && module.isDirectory()) {
                luaModules.add(module.getName());
            } else {
                System.err.println("Invalid Lua module " + module.getName() + " (not a directory or unreadable)");
            }
        }
        return luaModules;
    }

    private void restartAllLuaStates() {
        terminateAllLuaStates();
        startAllLuaStates(false);
    }

    private String makeRunLuaPrefix(CommandSender commandSender) {
        if(commandSender instanceof Player) {
            return "local Player = require('Player'); local self = Player:getByUUID('" + ((Player) commandSender).getUniqueId().toString() + "'); ";
        } else {
            return "local Player = require('Player'); local self = Player:getConsole(); ";
        }
    }

    public boolean makeRunLua(CommandSender commandSender, String state, String string) {
        LuaState luaState;
        synchronized (luaStates) {
            luaState = luaStates.get(state);
        }
        if(luaState == null) {
            return false;
        }

        LuaValue code;
        try {
            synchronized (luaState.luaLock) {
                code = luaState.g.load(makeRunLuaPrefix(commandSender) + string);
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

    @Override
    public void onDisable() {
        terminateAllLuaStates();

        redisManager.stop();

        LuaState.unload();
    }

    public static StringBuilder makeMessageBuilder() {
        return new StringBuilder("\u00a75[FB] \u00a7f");
    }

    @Override
    public void onEnable() {
        getLuaFolder().mkdirs();
        getLuaModulesFolder().mkdirs();
        getLuaScriptsFolder().mkdirs();

        configuration = new Configuration(getDataFolder());
        redisManager = new RedisManager(new SimpleThreadCreator(), configuration);
        commandManagerMaster = new CommandManagerMaster(this);

        LuaState.load(this);

        restartAllLuaStates();

        getServer().getPluginCommand("lua_reload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if (strings.length > 0) {
                    startLuaState(strings[0], true);
                    commandSender.sendMessage(makeMessageBuilder().append("Reloaded Lua module ").append(strings[0]).toString());
                    return true;
                }

                restartAllLuaStates();
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

                startLuaState(strings[0], false);
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

                terminateLuaState(strings[0]);
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
                return makeRunLua(commandSender, strings[0], Utils.concatArray(strings, 1, ""));
            }
        });

        getServer().getPluginCommand("lua_runfile").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if(strings.length < 2) {
                    return false;
                }
                try {
                    File file = new File(getLuaScriptsFolder(), strings[1]);
                    Scanner scanner = new Scanner(file);
                    scanner.useDelimiter("\\A");
                    String contents = scanner.next();
                    scanner.close();
                    return makeRunLua(commandSender, strings[0], contents);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });

        getServer().getPluginCommand("lua_list").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                cleanupLuaStateList();
                StringBuilder ret = makeMessageBuilder().append("Lua modules: ");
                boolean isFirst = true;
                for(String module : listLuaModules()) {
                    if(isFirst) {
                        isFirst = false;
                    } else {
                        ret.append(", ");
                    }
                    ret.append(luaStates.containsKey(module) ? "\u00a72" : "\u00a74");
                    ret.append(module);
                }
                commandSender.sendMessage(ret.toString());
                return true;
            }
        });
    }
}
