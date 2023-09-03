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

import com.foxelbox.foxbukkit.chat.ChatHelper;
import com.foxelbox.foxbukkit.chat.FoxBukkitChat;
import com.foxelbox.foxbukkit.chat.MessageHelper;
import com.foxelbox.foxbukkit.chat.json.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class EnhancedChatMessageManager {
    private final ChatHelper chatHelper;
    private final LuaState luaState;
    private final FoxBukkitChat chatPlugin;

    public EnhancedChatMessageManager(LuaState luaState, Plugin enhancedChatPlugin) {
        try {
            chatPlugin = (FoxBukkitChat)enhancedChatPlugin;
            chatHelper = chatPlugin.chatHelper;
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
        ChatMessageOut chatMessageOut = new ChatMessageOut(chatPlugin, new UserInfo(chatPlugin, source));
        chatMessageOut.contents = content;
        chatMessageOut.type = MessageType.valueOf(type.toUpperCase());
        chatHelper.sendMessage(chatMessageOut);
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
        UserInfo from = new UserInfo(Utils.getCommandSenderUUID(source), source.getName());
        ChatMessageOut chatMessageOut = new ChatMessageOut(chatPlugin, from);
        chatMessageOut.finalizeContext = true;
        chatMessageOut.contents = content;
        chatMessageOut.to = new MessageTarget(TargetType.valueOf(chatTarget.toUpperCase()), targetFilter);
        chatHelper.sendMessage(chatMessageOut);
    }

    public String getPlayerNick(Player ply) {
        return chatPlugin.playerHelper.getPlayerNick(ply);
    }

    public String getPlayerNick(UUID uuid) {
        return chatPlugin.playerHelper.getPlayerNick(uuid);
    }

    public UUID getPlayerUUID(String name) {
        return chatPlugin.playerHelper.getUUIDByName(name);
    }

    public boolean isAvailable() {
        return true;
    }
}
