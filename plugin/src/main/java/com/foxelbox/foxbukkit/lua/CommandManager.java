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

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private final LuaState luaState;

    public CommandManager(LuaState luaState) {
        this.luaState = luaState;
    }

    public void unregisterAll() {
        luaState.plugin.commandManagerMaster.unregisterAll(luaState);
    }

    public void register(String command, String permission, LuaValue handler, LuaTable luaInfo) {
        HashMap<String, String> info = new HashMap<>();
        for(LuaValue key : luaInfo.keys()) {
            info.put(
                    (String)CoerceLuaToJava.coerce(key, String.class),
                    (String)CoerceLuaToJava.coerce(luaInfo.get(key), String.class)
            );
        }
        luaState.plugin.commandManagerMaster.register(command, permission, luaState, handler, info);
    }

    public void unregister(String command) {
        luaState.plugin.commandManagerMaster.unregister(command, luaState);
    }

    public Map<String, String> getInfo(String command) {
        return luaState.plugin.commandManagerMaster.getInfo(command);
    }

    public Map<String, Map<String, String>> getCommands() {
        return luaState.plugin.commandManagerMaster.getCommands();
    }
}
