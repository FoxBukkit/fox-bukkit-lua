--[[

    This file is part of FoxBukkitLua.

    FoxBukkitLua is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FoxBukkitLua is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FoxBukkitLua.  If not, see <http://www.gnu.org/licenses/>.

]]
local eventPriority = luajava.bindClass("org.bukkit.event.EventPriority")

local eventManager = __LUA_THREAD:getEventManager()

return {
	register = function(self, event, priority, callback, b)
		if type(event) == "string" then
			event = luajava.bindClass(event)
		end
		if type(priority) == "string" then
			priority = eventPriority[priority:upper()]
		end
		return eventManager:register(event, priority, b or false, callback)
	end,

	unregister = function(self, listener)
		return eventManager:unregister(listener)
	end,

	Priority = eventPriority
}