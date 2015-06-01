package com.foxelbox.foxbukkit.lua;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.HashMap;

public class CommandManager implements Listener {
    private final LuaThread luaThread;

    private final HashMap<String, LuaCommandInvoker> commandHandlers = new HashMap<>();

    public CommandManager(LuaThread luaThread) {
        this.luaThread = luaThread;
        FoxBukkitLua.instance.getServer().getPluginManager().registerEvents(this, FoxBukkitLua.instance);
    }

    public void unregisterAll() {
        synchronized (commandHandlers) {
            commandHandlers.clear();
        }
        HandlerList.unregisterAll(this);
    }

    public void register(String command, LuaValue handler) {
        synchronized (commandHandlers) {
            commandHandlers.put(command.trim().toLowerCase(), new LuaCommandInvoker(handler));
        }
    }

    public void unregister(String command) {
        synchronized (commandHandlers) {
            commandHandlers.remove(command.trim().toLowerCase());
        }
    }

    private class LuaCommandInvoker extends LuaThread.Invoker {
        private CommandManagerMaster.ParsedCommandLine commandLine;
        private final LuaValue function;

        public LuaCommandInvoker setCommandLine(CommandManagerMaster.ParsedCommandLine commandLine) {
            this.commandLine = commandLine;
            return this;
        }

        public LuaCommandInvoker(LuaValue function) {
            super(luaThread);
            this.function = function;
        }

        @Override
        protected LuaValue invoke() {
            return function.call(CoerceJavaToLua.coerce(commandLine));
        }

        private synchronized LuaValue doRun(CommandManagerMaster.ParsedCommandLine commandLine) {
            reset();
            setCommandLine(commandLine);
            return getResult();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(final CommandManagerMaster.CommandEvent event) {
        final LuaCommandInvoker invoker;
        synchronized (commandHandlers) {
            invoker = commandHandlers.get(event.getCommand());
        }
        if(invoker == null) {
            return;
        }

        final LuaValue ret = invoker.doRun(event.getParsedCommandLine());

        // Return true/nonboolean for continue, false for cancel
        if(ret != null && ret.isboolean()) {
            boolean retB = ((LuaBoolean)ret).booleanValue();
            event.setCancelled(!retB);
        } else {
            event.setCancelled(true);
        }
    }
}
