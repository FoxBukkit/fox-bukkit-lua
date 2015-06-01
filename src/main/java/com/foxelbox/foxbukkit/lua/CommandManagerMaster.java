package com.foxelbox.foxbukkit.lua;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.PluginManager;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import static org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandManagerMaster implements Listener {
    private final PluginManager pluginManager;
    private final HashMap<String, LuaCommandInvoker> commandHandlers = new HashMap<>();

    public CommandManagerMaster() {
        pluginManager = FoxBukkitLua.instance.getServer().getPluginManager();
        pluginManager.registerEvents(this, FoxBukkitLua.instance);
    }

    public void register(String command, LuaThread thread, LuaValue handler) {
        synchronized (commandHandlers) {
            commandHandlers.put(command.trim().toLowerCase(), new LuaCommandInvoker(command, thread, handler));
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
            for(LuaCommandInvoker invoker : commandHandlers.values()) {
                if(invoker.luaThread == luaThread) {
                    commandHandlers.remove(invoker.command);
                }
            }
        }
    }

    private class LuaCommandInvoker extends LuaThread.Invoker {
        private CommandManagerMaster.ParsedCommandLine commandLine;
        private final LuaValue function;
        private final String command;
        private final LuaThread luaThread;

        public LuaCommandInvoker setCommandLine(CommandManagerMaster.ParsedCommandLine commandLine) {
            this.commandLine = commandLine;
            return this;
        }

        public LuaCommandInvoker(String command, LuaThread luaThread, LuaValue function) {
            super(luaThread);
            this.luaThread = luaThread;
            this.function = function;
            this.command = command;
        }

        @Override
        protected LuaValue invoke() {
            Varargs varargs = LuaValue.varargsOf(new LuaValue[] {
                    coerce(commandLine.getSource()),
                    coerce(commandLine.getCommand()),
                    coerce(commandLine.getArguments()),
                    coerce(commandLine.getArgumentString()),
                    coerce(commandLine.getFlagsString())
            });
            return function.invoke(varargs).arg1();
        }

        private synchronized LuaValue doRun(CommandManagerMaster.ParsedCommandLine commandLine) {
            reset();
            setCommandLine(commandLine);
            return getResult();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();

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
        if(invoker == null) {
            return;
        }

        final LuaValue ret = invoker.doRun(new ParsedCommandLine(event.getPlayer(), cmdStr, argStr));

        // Return true/nonboolean for continue, false for cancel
        if(ret != null && ret.isboolean()) {
            boolean retB = ((LuaBoolean)ret).booleanValue();
            event.setCancelled(!retB);
        } else {
            event.setCancelled(true);
        }
    }

    public static class ParsedCommandLine {
        private final String[] parsedArguments;
        private final String flagStr;
        private final String rawArguments;
        private final String command;
        private final CommandSender source;

        private static final Pattern ARGUMENT_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

        public ParsedCommandLine(CommandSender source, String command, String rawArguments) {
            this.rawArguments = rawArguments;
            this.command = command;
            this.source = source;

            if(rawArguments.isEmpty()) {
                parsedArguments = new String[0];
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
                        parsedArguments = new String[0];
                        return;
                    }
                } else {
                    flagStr = "";
                }
            } else {
                flagStr = "";
            }

            if(myArgStr.isEmpty()) {
                parsedArguments = new String[0];
                return;
            }

            ArrayList<String> arguments = new ArrayList<>();
            Matcher m = ARGUMENT_PATTERN.matcher(myArgStr);
            while(m.find()) {
                String str = m.group(1);
                if(str.charAt(0) == '"') {
                    str = str.substring(1, str.length() - 1);
                }
                arguments.add(str);
            }

            parsedArguments = arguments.toArray(new String[arguments.size()]);
        }

        public String[] getArguments() {
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
