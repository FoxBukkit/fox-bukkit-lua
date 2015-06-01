package com.foxelbox.foxbukkit.lua;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandManagerMaster implements Listener {
    private final PluginManager pluginManager;
    public CommandManagerMaster() {
        pluginManager = FoxBukkitLua.instance.getServer().getPluginManager();
        pluginManager.registerEvents(this, FoxBukkitLua.instance);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        final Player who = event.getPlayer();
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

        CommandEvent commandEvent = new CommandEvent(who, cmdStr, argStr);
        pluginManager.callEvent(commandEvent);
        event.setCancelled(commandEvent.isCancelled());
    }

    public static class CommandEvent extends PlayerEvent implements Cancellable {
        private static final HandlerList handlers = new HandlerList();
        private boolean cancelled = false;

        private final String command;

        private final String argStr;

        private String[] parsedArguments = null;
        private String flagStr = null;

        public CommandEvent(Player who, String command, String arguments) {
            super(who);
            this.command = command;
            this.argStr = arguments;
        }

        private static final Pattern ARGUMENT_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

        private synchronized void parseCommandline() {
            if(parsedArguments != null) {
                return;
            }

            if(argStr.isEmpty()) {
                parsedArguments = new String[0];
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
                        parsedArguments = new String[0];
                        return;
                    }
                }
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
            parseCommandline();
            return parsedArguments;
        }

        public String getCommand() {
            return command;
        }

        public String getArgumentString() {
            return argStr;
        }

        public boolean hasFlag(char flag) {
            parseCommandline();
            return flagStr.indexOf(flag) >= 0;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean b) {
            cancelled = b;
        }

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }
}
