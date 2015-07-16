/**
 * This file is part of FoxBukkitLua-plugin.
 *
 * FoxBukkitLua-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitLua-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitLua-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.chat.ChatQueueHandler;
import com.foxelbox.foxbukkit.chat.FoxBukkitChat;
import com.foxelbox.foxbukkit.chat.MessageHelper;
import com.foxelbox.foxbukkit.chat.Messages;
import com.foxelbox.foxbukkit.chat.json.ChatMessageIn;
import com.foxelbox.foxbukkit.chat.json.ChatMessageOut;
import com.foxelbox.foxbukkit.chat.json.MessageTarget;
import com.foxelbox.foxbukkit.chat.json.UserInfo;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class EnhancedChatMessageManager {
    private final ChatQueueHandler chatQueueHandler;
    private final LuaState luaState;
    private final FoxBukkitChat chatPlugin;

    public EnhancedChatMessageManager(LuaState luaState, Plugin enhancedChatPlugin) {
        try {
            chatPlugin = (FoxBukkitChat)enhancedChatPlugin;
            chatQueueHandler = chatPlugin.getChatQueueHandler();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.luaState = luaState;
    }

    private CommandSender getConsole() {
        return luaState.plugin.getServer().getConsoleSender();
    }

    public String makeButton(String command, String label, String color, boolean run, boolean addHover) {
        return MessageHelper.button(command, label, color, run, addHover);
    }

    public void sendGlobal(CommandSender source, String type, String content) {
        ChatMessageIn chatMessageIn = new ChatMessageIn(chatPlugin, source);
        chatMessageIn.contents = content;
        chatMessageIn.type = Messages.MessageType.valueOf(type.toUpperCase());
        chatQueueHandler.sendMessage(chatMessageIn);
    }

    public void broadcastLocal(CommandSender source, String content) {
        sendLocal(source, content, "all", null);
    }

    public void sendLocalToPlayer(CommandSender source, String content, CommandSender target) {
        sendLocal(source, content, "player", new String[] { Utils.getCommandSenderUUID(target).toString() });
    }

    public void sendLocalToPermission(CommandSender source, String content, String target) {
        sendLocal(source, content, "permission", new String[] { target });
    }

    public void broadcastLocal(String content) {
        broadcastLocal(getConsole(), content);
    }

    public void sendLocalToPlayer(String content, CommandSender target) {
        sendLocalToPlayer(getConsole(), content, target);
    }

    public void sendLocalToPermission(String content, String target) {
        sendLocalToPermission(getConsole(), content, target);
    }

    public void sendLocal(CommandSender source, String content, String chatTarget, String[] targetFilter) {
        ChatMessageOut chatMessageOut = new ChatMessageOut();
        chatMessageOut.server = null;
        chatMessageOut.from = new UserInfo(Utils.getCommandSenderUUID(source), source.getName());
        chatMessageOut.context = UUID.randomUUID();
        chatMessageOut.finalizeContext = true;
        chatMessageOut.contents = content;
        chatMessageOut.to = new MessageTarget(Messages.TargetType.valueOf(chatTarget.toUpperCase()), targetFilter);
        chatQueueHandler.onMessage(chatMessageOut);
    }

    public String getPlayerNick(Player ply) {
        return chatPlugin.getPlayerNick(ply);
    }

    public String getPlayerNick(UUID uuid) {
        return chatPlugin.getPlayerNick(uuid);
    }

    public UUID getPlayerUUID(String name) {
        return UUID.fromString(chatPlugin.playerHelper.playerNameToUUID.get(name));
    }

    public boolean isAvailable() {
        return true;
    }
}
