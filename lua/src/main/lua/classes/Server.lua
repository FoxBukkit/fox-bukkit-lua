--[[

    This file is part of FoxBukkitLua-lua.

    FoxBukkitLua-lua is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FoxBukkitLua-lua is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FoxBukkitLua-lua.  If not, see <http://www.gnu.org/licenses/>.

]]

local luaState = __LUA_STATE
local plugin = luaState:getFoxBukkitLua()
local bukkitServer = plugin:getServer()
local scheduler = bukkitServer:getScheduler()

return {
	getBukkitServer = function(self)
		return bukkitServer
	end,
    runConsoleCommand = function(self, cmd)
        bukkitServer:dispatchCommand(bukkitServer:getConsoleSender(), cmd)
    end,
	runOnMainThread = function(self, func, delay)
        if delay then
            return scheduler:scheduleSyncDelayedTask(
                plugin,
                luaState:createLuaValueRunnable(func),
                delay
            )
        end
	    return scheduler:scheduleSyncDelayedTask(
	        plugin,
	        luaState:createLuaValueRunnable(func)
	    )
	end
}