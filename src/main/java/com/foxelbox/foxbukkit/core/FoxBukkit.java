package com.foxelbox.foxbukkit.core;

import com.foxelbox.dependencies.config.Configuration;
import com.foxelbox.dependencies.redis.RedisManager;
import com.foxelbox.dependencies.threading.SimpleThreadCreator;
import com.foxelbox.foxbukkit.chatcomponent.FBChatComponent;
import com.foxelbox.foxbukkit.lua.LuaThread;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class FoxBukkit extends JavaPlugin {
    public static FoxBukkit instance;
    public static FBChatComponent chatComponent;

    public Configuration configuration;

    public RedisManager redisManager;

    private final HashMap<String, LuaThread> luaThreadList = new HashMap<>();

    public File getLuaFolder() {
        return new File(getDataFolder(), "base");
    }

    public File getLuaModulesFolder() {
        return new File(getDataFolder(), "modules");
    }

    private void cleanupLuaThreadList() {
        synchronized (luaThreadList) {
            for (LuaThread t : luaThreadList.values()) {
                if(!t.isRunning()) {
                    luaThreadList.remove(t.getModule());
                }
            }
        }
    }

    private void terminateLuaThread(String module) {
        synchronized (luaThreadList) {
            LuaThread luaThread = luaThreadList.remove(module);
            if(luaThread != null) {
                luaThread.terminate();
            }
        }
    }

    private void terminateAllLuaThreads() {
        synchronized (luaThreadList) {
            for(LuaThread t : luaThreadList.values()) {
                t.terminate();
            }
            luaThreadList.clear();
        }
    }

    private void startLuaThread(String module, boolean overwrite) {
        synchronized (luaThreadList) {
            LuaThread luaThread = luaThreadList.get(module);
            if(luaThread != null && luaThread.isRunning()) {
                if(overwrite) {
                    luaThread.terminate();
                } else {
                    return;
                }
            }
            luaThread = new LuaThread(module);
            luaThreadList.put(module, luaThread);
            luaThread.start();
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
    }

    private StringBuilder makeMessageBuilder() {
        return new StringBuilder("\u00a75[FB] \u00a7f");
    }

    @Override
    public void onEnable() {
        getLuaFolder().mkdirs();
        getLuaModulesFolder().mkdirs();

        instance = this;
        configuration = new Configuration(getDataFolder());
        redisManager = new RedisManager(new SimpleThreadCreator(), configuration);
        chatComponent = (FBChatComponent)getServer().getPluginManager().getPlugin("FoxBukkitChatComponent");

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
                if(strings.length > 0) {
                    startLuaThread(strings[0], false);
                    commandSender.sendMessage(makeMessageBuilder().append("Loaded Lua module ").append(strings[0]).toString());
                    return true;
                }
                return false;
            }
        });

        getServer().getPluginCommand("lua_unload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if(strings.length > 0) {
                    terminateLuaThread(strings[0]);
                    commandSender.sendMessage(makeMessageBuilder().append("Unloaded Lua module ").append(strings[0]).toString());
                    return true;
                }
                return false;
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
                return false;
            }
        });
    }
}
