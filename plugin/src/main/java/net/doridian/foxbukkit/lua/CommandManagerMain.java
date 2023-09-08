/*
 * foxbukkit-lua-plugin - ${project.description}
 * Copyright © ${year} Doridian (git@doridian.net)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.doridian.foxbukkit.lua;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce;

public class CommandManagerMain implements Listener {
    private final PluginManager pluginManager;
    private final FoxBukkitLua plugin;
    private final HashMap<String, LuaCommandHandler> commandHandlers = new HashMap<>();
    private final HashMap<String, Map<String, String>> commandInfo = new HashMap<>();

    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

    public class CommandManagerPluginCommand extends Command {
        protected CommandManagerPluginCommand(@NotNull String name) {
            super(name);
        }

        protected CommandManagerPluginCommand(@NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases) {
            super(name, description, usageMessage, aliases);
        }

        @Override
        public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
            return true;
        }
    }

    public CommandManagerMain(FoxBukkitLua plugin_) {
        plugin = plugin_;
        pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(this, plugin);

        plugin.getServer().getPluginCommand("fbl").setExecutor((commandSender, command, s, strings) -> {
            processCommand(commandSender, Utils.concatArray(strings, 0, ""));
            return true;
        });
    }

    public void register(String command, String permission, LuaState thread, LuaValue handler, Map<String, String> info) {
        command = command.trim().toLowerCase();
        synchronized (commandHandlers) {
            commandHandlers.put(command, new LuaCommandHandler(permission, thread, handler));
            commandInfo.put(command, info);

            CommandManagerPluginCommand dummyCommand = new CommandManagerPluginCommand(command);
            dummyCommand.setPermission(permission);
            dummyCommand.setLabel(command);
            getCommandMap().getKnownCommands().put(command, dummyCommand);
        }
    }

    public void unregister(String command, LuaState luaState) {
        command = command.trim().toLowerCase();
        synchronized (commandHandlers) {
            LuaCommandHandler invoker = commandHandlers.get(command);
            if(invoker.luaState == luaState) {
                commandHandlers.remove(command);
                commandInfo.remove(command);

                getCommandMap().getKnownCommands().remove(command);
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

                getCommandMap().getKnownCommands().remove(command);
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
        //Parse CMDLine
        parsedArguments = new LuaTable();
        if(!argStr.isEmpty()) {
            Matcher m = ARGUMENT_PATTERN.matcher(argStr);
            while(m.find()) {
                String str = m.group(1);
                if(str.charAt(0) == '"') {
                    str = str.substring(1, str.length() - 1);
                }
                parsedArguments.insert(0, coerce(str));
            }
        }
        //END parse

        Varargs varargs = LuaValue.varargsOf(new LuaValue[]{
                coerce(source),
                coerce(cmdStr),
                parsedArguments,
                coerce(argStr)
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

    private static CommandMap getCommandMap() {
        try {
            Field f = SimplePluginManager.class.getDeclaredField("commandMap");
            f.setAccessible(true);

            return (CommandMap)f.get(Bukkit.getPluginManager());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
