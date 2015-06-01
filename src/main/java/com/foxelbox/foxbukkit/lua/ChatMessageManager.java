/**
 * This file is part of FoxBukkit.
 *
 * FoxBukkit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkit.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.chatcomponent.RedisHandler;
import com.foxelbox.foxbukkit.chatcomponent.json.ChatMessageIn;
import com.foxelbox.foxbukkit.chatcomponent.json.ChatMessageOut;
import com.foxelbox.foxbukkit.core.FoxBukkit;

public class ChatMessageManager {
    private final RedisHandler redisHandler = FoxBukkit.chatComponent.getRedisHandler();

    private final LuaThread luaThread;

    public ChatMessageManager(LuaThread luaThread) {
        this.luaThread = luaThread;
    }

    public void sendGlobal(ChatMessageIn chatMessageIn) {
        RedisHandler.sendMessage(chatMessageIn);
    }

    public void sendMessage(ChatMessageOut chatMessageOut) {
        redisHandler.onMessage(chatMessageOut);
    }
}
