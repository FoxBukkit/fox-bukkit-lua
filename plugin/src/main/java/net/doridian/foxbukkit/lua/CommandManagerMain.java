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

import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.event.Listener;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce;

public class CommandManagerMain implements Listener {
    private final PluginManager pluginManager;
    private final FoxBukkitLua plugin;

    private final HashMap<String, LuaCommand> commandHandlers = new HashMap<>();

    private final HashMap<String, LuaTopic> helpTopics = new HashMap<>();

    public static class LuaCommand extends Command {
        private LuaState luaState;

        private LuaValue handler;

        protected LuaCommand(@NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases) {
            super(name, description, usageMessage, aliases);
        }

        @Override
        public boolean execute(@NotNull CommandSender source, @NotNull String command, @NotNull String[] args) {
            final LuaTable parsedArguments = new LuaTable();
            for (String arg : args) {
                parsedArguments.insert(0, coerce(arg));
            }

            Varargs varargs = LuaValue.varargsOf(new LuaValue[]{
                coerce(source),
                coerce(command),
                parsedArguments
            });

            try {
                final LuaValue ret;
                synchronized (luaState.luaLock) {
                    ret = handler.invoke(varargs).arg1();
                }
                // Return true/non-boolean for handled, false for unhandled (fallthrough)
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

        @Override
        public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
            return ImmutableList.of();
        }
    }

    public static class LuaTopic extends HelpTopic {
        private LuaCommand command;

        private final String name;

        private LuaTopic(LuaCommand command, String name) {
            this.command = command;
            this.name = name;
        }

        @Override
        public boolean canSee(@NotNull CommandSender commandSender) {
            String perm = command.getPermission();
            if (perm == null) {
                return true;
            }
            return commandSender.hasPermission(perm);
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull String getShortText() {
            return command.getDescription();
        }

        @Override
        public @NotNull String getFullText(@NotNull CommandSender forWho) {
            return command.getUsage();
        }
    }

    public CommandManagerMain(FoxBukkitLua plugin_) {
        plugin = plugin_;
        pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(this, plugin);
    }

    public void register(String command, String permission, LuaState thread, LuaValue handler, Map<String, String> info) {
        command = command.trim().toLowerCase();
        synchronized (commandHandlers) {
            LuaCommand luaCommand = new LuaCommand(command, info.getOrDefault("help", ""), info.getOrDefault("usage", ""), new ArrayList<>());
            luaCommand.luaState = thread;
            luaCommand.handler = handler;
            luaCommand.setPermission(permission);
            luaCommand.setLabel(command);
            getCommandMap().getKnownCommands().put(command, luaCommand);
            luaCommand.register(getCommandMap());

            final String helpTopicName = "/" + command;
            LuaTopic helpTopic = helpTopics.get(helpTopicName);
            if (helpTopic == null) {
                helpTopic = new LuaTopic(luaCommand, helpTopicName);
                IndexHelpTopic indexTopic = (IndexHelpTopic)Bukkit.getHelpMap().getHelpTopic(plugin.getName());
                Collection<HelpTopic> allTopics = getAllTopics(indexTopic);
                allTopics.add(helpTopic);
                Bukkit.getHelpMap().addTopic(helpTopic);
            }
            helpTopic.command = luaCommand;
            syncCommands();
        }
    }

    public void unregister(String command, LuaState luaState) {
        command = command.trim().toLowerCase();
        synchronized (commandHandlers) {
            LuaCommand invoker = commandHandlers.get(command);
            if(invoker.luaState == luaState) {
                commandHandlers.remove(command).unregister(getCommandMap());
                getCommandMap().getKnownCommands().remove(command);
                syncCommands();
            }
        }
    }

    public void unregisterAll(LuaState luaState) {
        synchronized (commandHandlers) {
            HashSet<String> toUnregister = new HashSet<>();
            for(Map.Entry<String, LuaCommand> commandHandlerEntry : commandHandlers.entrySet()) {
                if(commandHandlerEntry.getValue().luaState == luaState) {
                    toUnregister.add(commandHandlerEntry.getKey());
                }
            }
            for(String command : toUnregister) {
                commandHandlers.remove(command).unregister(getCommandMap());
                getCommandMap().getKnownCommands().remove(command);
            }
            syncCommands();
        }
    }

    private static Collection<HelpTopic> getAllTopics(IndexHelpTopic indexHelpTopic) {
        try {
            Field f = IndexHelpTopic.class.getDeclaredField("allTopics");
            f.setAccessible(true);
            return (Collection<HelpTopic>) f.get(indexHelpTopic);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

    private void syncCommands() {
        try {
            Server server = plugin.getServer();
            Method m = server.getClass().getDeclaredMethod("syncCommands");
            m.invoke(server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
