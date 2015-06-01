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

import com.foxelbox.foxbukkit.chatcomponent.FBChatComponent;
import com.foxelbox.foxbukkit.chatcomponent.RedisHandler;
import com.foxelbox.foxbukkit.chatcomponent.json.ChatMessageIn;
import com.foxelbox.foxbukkit.chatcomponent.json.ChatMessageOut;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class EnhancedChatMessageManager {
    private final RedisHandler redisHandler;

    public EnhancedChatMessageManager(LuaThread luaThread, Plugin enhancedChatPlugin) {
        try {
            FBChatComponent chatComponent = (FBChatComponent) enhancedChatPlugin;
            redisHandler = chatComponent.getRedisHandler();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.luaThread = luaThread;
    }

    private final LuaThread luaThread;

    public void sendGlobal(CommandSender source, String type, String content) {
        ChatMessageIn chatMessageIn = new ChatMessageIn(source);
        chatMessageIn.contents = content;
        chatMessageIn.type = type;
        sendGlobal(chatMessageIn);
    }

    public void sendGlobal(ChatMessageIn chatMessageIn) {
        RedisHandler.sendMessage(chatMessageIn);
    }

    public void sendLocal(ChatMessageOut chatMessageOut) {
        redisHandler.onMessage(chatMessageOut);
    }
}
