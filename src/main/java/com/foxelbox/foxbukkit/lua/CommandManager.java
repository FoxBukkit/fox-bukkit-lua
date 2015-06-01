package com.foxelbox.foxbukkit.lua;

import org.luaj.vm2.LuaValue;

public class CommandManager {
    private final LuaThread luaThread;

    public CommandManager(LuaThread luaThread) {
        this.luaThread = luaThread;
    }

    public void unregisterAll() {
        FoxBukkitLua.instance.commandManagerMaster.unregisterAll(luaThread);
    }

    public void register(String command,  LuaValue handler) {
        FoxBukkitLua.instance.commandManagerMaster.register(command, luaThread, handler);
    }

    public void unregister(String command) {
        FoxBukkitLua.instance.commandManagerMaster.unregister(command, luaThread);
    }
}
