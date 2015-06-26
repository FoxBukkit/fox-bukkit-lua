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

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.PluginManager;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce;

public class CommandManagerMaster implements Listener {
    private final PluginManager pluginManager;
    private final HashMap<String, LuaCommandHandler> commandHandlers = new HashMap<>();
    private final HashMap<String, Map<String, String>> commandInfo = new HashMap<>();

    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

    public CommandManagerMaster(FoxBukkitLua plugin) {
        pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(this, plugin);

        plugin.getServer().getPluginCommand("fbl").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                processCommand(commandSender, Utils.concatArray(strings, 0, ""));
                return true;
            }
        });
    }

    public void register(String command, String permission, LuaState thread, LuaValue handler, Map<String, String> info) {
        command = command.trim().toLowerCase();
        synchronized (commandHandlers) {
            commandHandlers.put(command, new LuaCommandHandler(permission, thread, handler));
            commandInfo.put(command, info);
        }
    }

    public void unregister(String command, LuaState luaState) {
        command = command.trim().toLowerCase();
        synchronized (commandHandlers) {
            LuaCommandHandler invoker = commandHandlers.get(command);
            if(invoker.luaState == luaState) {
                commandHandlers.remove(command);
                commandInfo.remove(command);
            }
        }
    }

    public void unregisterAll(LuaState luaState) {
        synchronized (commandHandlers) {
            HashSet<String> toUnregister = new HashSet<>();
            for(Map.Entry<String, LuaCommandHandler> commandHandlerEntry : commandHandlers.entrySet()) {
                if(commandHandlerEntry.getValue().luaState == luaState) {
                    toUnregister.add(commandHandlerEntry.getKey());
                }
            }
            for(String command : toUnregister) {
                commandHandlers.remove(command);
                commandInfo.remove(command);
            }
        }
    }

    public Map<String, String> getInfo(String command) {
        synchronized (commandHandlers) {
            return commandInfo.get(command.trim().toLowerCase());
        }
    }

    public Map<String, Map<String, String>> getCommands() {
        synchronized (commandHandlers) {
            return (Map<String, Map<String, String>>)commandInfo.clone();
        }
    }

    private class LuaCommandHandler {
        private final LuaValue function;
        private final String permission;
        private final LuaState luaState;

        public LuaCommandHandler(String permission, LuaState luaState, LuaValue function) {
            this.luaState = luaState;
            this.function = function;
            this.permission = permission;
        }
    }

    public boolean processCommand(CommandSender source, String message) {
        int splitter = message.indexOf(' ');
        if(splitter == 0) {
            return false;
        }

        String cmdStr, argStr;

        if(splitter > 0) {
            cmdStr = message.substring(0, splitter);
            argStr = message.substring(splitter + 1).trim();
        } else {
            cmdStr = message;
            argStr = "";
        }

        cmdStr = cmdStr.trim().toLowerCase();

        final LuaCommandHandler invoker;
        synchronized (commandHandlers) {
            invoker = commandHandlers.get(cmdStr);
        }
        //Player source = event.getPlayer();
        if(invoker == null || !source.hasPermission(invoker.permission)) {
            return false;
        }

        final LuaTable parsedArguments;
        final String flagStr;
        //Parse CMDLine
        parsedArguments = new LuaTable();
        if(argStr.isEmpty()) {
            flagStr = "";
        } else {
            String myArgStr = argStr;
            if (myArgStr.length() > 1 && myArgStr.charAt(0) == '-') {
                char firstFlag = myArgStr.charAt(1);
                if ((firstFlag >= 'a' && firstFlag <= 'z') || (firstFlag >= 'A' && firstFlag <= 'Z')) {
                    int spacePos = myArgStr.indexOf(' ');
                    if (spacePos > 0) {
                        flagStr = myArgStr.substring(1, spacePos).toLowerCase();
                        myArgStr = myArgStr.substring(spacePos + 1).trim();
                    } else {
                        flagStr = myArgStr.toLowerCase();
                        myArgStr = "";
                    }
                } else {
                    flagStr = "";
                }
            } else {
                flagStr = "";
            }

            if(!myArgStr.isEmpty()) {
                Matcher m = ARGUMENT_PATTERN.matcher(myArgStr);
                while(m.find()) {
                    String str = m.group(1);
                    if(str.charAt(0) == '"') {
                        str = str.substring(1, str.length() - 1);
                    }
                    parsedArguments.insert(0, coerce(str));
                }
            }
        }
        //END parse

        Varargs varargs = LuaValue.varargsOf(new LuaValue[]{
                coerce(source),
                coerce(cmdStr),
                parsedArguments,
                coerce(argStr),
                coerce(flagStr)
        });

        try {
            final LuaValue ret;
            synchronized (invoker.luaState.luaLock) {
                ret = invoker.function.invoke(varargs).arg1();
            }
            // Return true/nonboolean for handled, false for unhandled (fallthrough)
            if (ret == null || !ret.isboolean() || ((LuaBoolean) ret).booleanValue()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            source.sendMessage(FoxBukkitLua.makeMessageBuilder().append("Internal error running command").toString());
            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if(message.length() < 2) {
            return;
        }
        event.setCancelled(processCommand(event.getPlayer(), message.substring(1)));
    }
}
