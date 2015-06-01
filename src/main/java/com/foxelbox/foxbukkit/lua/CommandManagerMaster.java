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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce;

public class CommandManagerMaster implements Listener {
    private final PluginManager pluginManager;
    private final HashMap<String, LuaCommandInvoker> commandHandlers = new HashMap<>();

    public CommandManagerMaster() {
        pluginManager = FoxBukkitLua.instance.getServer().getPluginManager();
        pluginManager.registerEvents(this, FoxBukkitLua.instance);
    }

    public void register(String command, String permission, LuaThread thread, LuaValue handler) {
        synchronized (commandHandlers) {
            commandHandlers.put(command.trim().toLowerCase(), new LuaCommandInvoker(command, permission, thread, handler));
        }
    }

    public void unregister(String command, LuaThread luaThread) {
        command = command.trim().toLowerCase();
        synchronized (commandHandlers) {
            LuaCommandInvoker invoker = commandHandlers.get(command);
            if(invoker.luaThread == luaThread) {
                commandHandlers.remove(command);
            }
        }
    }

    public void unregisterAll(LuaThread luaThread) {
        synchronized (commandHandlers) {
            Iterator<LuaCommandInvoker> iterator = commandHandlers.values().iterator();
            while(iterator.hasNext()) {
                LuaCommandInvoker invoker = iterator.next();
                if(invoker.luaThread == luaThread) {
                    iterator.remove();
                }
            }
        }
    }

    private class LuaCommandInvoker {
        private final LuaValue function;
        private final String command;
        private final String permission;
        private final LuaThread luaThread;

        public LuaCommandInvoker(String command, String permission, LuaThread luaThread, LuaValue function) {
            this.luaThread = luaThread;
            this.function = function;
            this.command = command;
            this.permission = permission;
        }

        public LuaValue invoke(ParsedCommandLine commandLine) {
            Varargs varargs = LuaValue.varargsOf(new LuaValue[]{
                coerce(commandLine.getSource()),
                coerce(commandLine.getCommand()),
                commandLine.getArguments(),
                coerce(commandLine.getArgumentString()),
                coerce(commandLine.getFlagsString())
            });
            synchronized (luaThread.luaLock) {
                return function.invoke(varargs).arg1();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if(message.length() < 2) {
            return;
        }
        message = message.substring(1);

        int splitter = message.indexOf(' ');
        if(splitter == 0) {
            return;
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

        final LuaCommandInvoker invoker;
        synchronized (commandHandlers) {
            invoker = commandHandlers.get(cmdStr);
        }
        Player source = event.getPlayer();
        if(invoker == null || !source.hasPermission(invoker.permission)) {
            return;
        }


        final LuaValue ret = invoker.invoke(new ParsedCommandLine(source, cmdStr, argStr));
        // Return true/nonboolean for handled, false for unhandled (fallthrough)
        if(ret == null || !ret.isboolean() || ((LuaBoolean)ret).booleanValue()) {
            event.setCancelled(true);
        }
    }

    public static class ParsedCommandLine {
        private final LuaTable parsedArguments;
        private final String flagStr;
        private final String rawArguments;
        private final String command;
        private final CommandSender source;

        private static final Pattern ARGUMENT_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

        public ParsedCommandLine(CommandSender source, String command, String rawArguments) {
            this.rawArguments = rawArguments;
            this.command = command;
            this.source = source;

            this.parsedArguments = new LuaTable();
            if(rawArguments.isEmpty()) {
                flagStr = "";
                return;
            }

            String myArgStr = rawArguments;
            if(myArgStr.length() > 1 && myArgStr.charAt(0) == '-') {
                char firstFlag = myArgStr.charAt(1);
                if((firstFlag >= 'a' && firstFlag <= 'z') || (firstFlag >= 'A' && firstFlag <= 'Z')) {
                    int spacePos = myArgStr.indexOf(' ');
                    if (spacePos > 0) {
                        flagStr = myArgStr.substring(1, spacePos).toLowerCase();
                        myArgStr = myArgStr.substring(spacePos + 1).trim();
                    } else {
                        flagStr = myArgStr.toLowerCase();
                        return;
                    }
                } else {
                    flagStr = "";
                }
            } else {
                flagStr = "";
            }

            if(myArgStr.isEmpty()) {
                return;
            }

            ArrayList<String> arguments = new ArrayList<>();
            Matcher m = ARGUMENT_PATTERN.matcher(myArgStr);
            while(m.find()) {
                String str = m.group(1);
                if(str.charAt(0) == '"') {
                    str = str.substring(1, str.length() - 1);
                }
                parsedArguments.insert(0, coerce(str));
            }
        }

        public LuaTable getArguments() {
            return parsedArguments;
        }

        public String getArgumentString() {
            return rawArguments;
        }

        public boolean hasFlag(char flag) {
            return flagStr.indexOf(flag) >= 0;
        }

        public String getFlagsString() {
            return flagStr;
        }

        public CommandSender getSource() {
            return source;
        }

        public String getCommand() {
            return command;
        }
    }
}
