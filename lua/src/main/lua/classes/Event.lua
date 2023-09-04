--[[

    foxbukkit-lua-lua - ${project.description}
    Copyright © ${year} Doridian (git@doridian.net)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

]]
local eventManager = __LUA_STATE:getEventManager()
local eventPriority = bindClass('org.bukkit.event.EventPriority')

local Player = require('Player')

local next = next

local Event

local _event_mt = {
	__index = {
		register = function(self)
			return Event:register(self)
		end,
		unregister = function(self)
			return Event:unregister(self)
		end,
	},
	__newindex = function()
		error('Readonly')
	end,
	__metatable = false,
}

local readOnlyPlayerJoinCallbacks = {}

Event = {
	register = function(self, event)
		if not event.__info then
			event.__info = {}
			if type(event.class) == 'string' then
				event.class = bindClass(event.class)
			end
			event.priority = event.priority or eventPriority.NORMAL
			if type(event.priority) == 'string' then
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
	registerReadOnlyPlayerJoin = function(self, callback)
		table.insert(readOnlyPlayerJoinCallbacks, callback)
	end,
	unregister = function(self, event)
		if event.__info.listener then
			eventManager:unregister(event.__info.listener)
			event.__info.listener = nil
		end
	end,
	Priority = eventPriority,
}

Event:register{
	class = 'org.bukkit.event.player.PlayerJoinEvent',
	priority = Event.Priority.MONITOR,
	ignoreCancelled = true,
	run = function(self, event)
		local ply = Player:extend(event:getPlayer())
		for _, cb in next, readOnlyPlayerJoinCallbacks do
			cb(ply, event)
		end
	end,
}

return Event
