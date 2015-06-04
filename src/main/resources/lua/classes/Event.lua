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

local eventManager = __LUA_STATE:getEventManager()
local eventPriority = luajava.bindClass("org.bukkit.event.EventPriority")

local type = type

local class

local _event_mt = {
    __index = {
        register = function(self)
            return class:register(self)
        end,
        unregister = function(self)
            return class:unregister(self)
        end
    },
    __newindex = function()
        error("Readonly")
    end,
    __metatable = false
}

class = {
	register = function(self, event, priority, callback, ignoreCancelled)
		if not event.__info then
			event.__info = {}
			if type(event.class) == "string" then
				event.class = luajava.bindClass(event.class)
			end
			event.priority = event.priority or eventPriority.NORMAL
			if type(event.priority) == "string" then
				event.priority = eventPriority[event.Priority:upper()]
			end
			event.ignoreCancelled = event.ignoreCancelled or false
			event = setmetatable(event, _event_mt)
		end
		event.__info.listener = eventManager:register(event.class, event.priority, event.ignoreCancelled, function(evt)
			return event:run(evt)
		end)
		return event
	end,

	unregister = function(self, event)
		if event.__info.listener then
			eventManager:unregister(event.__info.listener)
			event.__info.listener = nil
		end
	end,

	Priority = eventPriority
}

return class