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

public class FoxBukkit extends JavaPlugin {
    public static FoxBukkit instance;
    public static FBChatComponent chatComponent;

    public Configuration configuration;

    public RedisManager redisManager;

    private LuaThread luaThread;

    public File getLuaFolder() {
        return new File(getDataFolder(), "lua");
    }

    @Override
    public void onEnable() {
        instance = this;
        configuration = new Configuration(getDataFolder());
        redisManager = new RedisManager(new SimpleThreadCreator(), configuration);
        chatComponent = (FBChatComponent)getServer().getPluginManager().getPlugin("FoxBukkitChatComponent");
        luaThread = new LuaThread();
        luaThread.start();

        getServer().getPluginCommand("fbluareload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                luaThread.terminate();
                luaThread = new LuaThread();
                luaThread.start();
                return true;
            }
        });
    }
}
