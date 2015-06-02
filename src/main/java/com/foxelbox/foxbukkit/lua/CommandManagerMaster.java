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
    private final HashMap<String, LuaCommandHandler> commandHandlers = new HashMap<>();

    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

    public CommandManagerMaster() {
        pluginManager = FoxBukkitLua.instance.getServer().getPluginManager();
        pluginManager.registerEvents(this, FoxBukkitLua.instance);
    }

    public void register(String command, String permission, LuaState thread, LuaValue handler) {
        synchronized (commandHandlers) {
            commandHandlers.put(command.trim().toLowerCase(), new LuaCommandHandler(permission, thread, handler));
        }
    }

    public void unregister(String command, LuaState luaState) {
        command = command.trim().toLowerCase();
        synchronized (commandHandlers) {
            LuaCommandHandler invoker = commandHandlers.get(command);
            if(invoker.luaState == luaState) {
                commandHandlers.remove(command);
            }
        }
    }

    public void unregisterAll(LuaState luaState) {
        synchronized (commandHandlers) {
            Iterator<LuaCommandHandler> iterator = commandHandlers.values().iterator();
            while(iterator.hasNext()) {
                LuaCommandHandler invoker = iterator.next();
                if(invoker.luaState == luaState) {
                    iterator.remove();
                }
            }
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

        final LuaCommandHandler invoker;
        synchronized (commandHandlers) {
            invoker = commandHandlers.get(cmdStr);
        }
        Player source = event.getPlayer();
        if(invoker == null || !source.hasPermission(invoker.permission)) {
            return;
        }

        final LuaTable parsedArguments;
        final String flagStr;
        //Parse CMDLine
        parsedArguments = new LuaTable();
        if(argStr.isEmpty()) {
            flagStr = "";
            return;
        }

        String myArgStr = argStr;
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
        //END parse

        Varargs varargs = LuaValue.varargsOf(new LuaValue[]{
                coerce(source),
                coerce(cmdStr),
                parsedArguments,
                coerce(argStr),
                coerce(flagStr)
        });
        final LuaValue ret;
        synchronized (invoker.luaState.luaLock) {
            ret = invoker.function.invoke(varargs).arg1();
        }
        // Return true/nonboolean for handled, false for unhandled (fallthrough)
        if(ret == null || !ret.isboolean() || ((LuaBoolean)ret).booleanValue()) {
            event.setCancelled(true);
        }
    }
}
