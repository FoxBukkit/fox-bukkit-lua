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
