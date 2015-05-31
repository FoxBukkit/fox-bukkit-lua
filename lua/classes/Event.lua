--local bukkitServer = luajava.bindClass("org.bukkit.bukkit")

local eventPriority = luajava.bindClass("org.bukkit.event.EventPriority")

return {
	register = function(event, priority, b, callback)
		if type(event) == 'string' then
			event = luajava.bindClass(event)
		end
		if type(priority) == 'string' then
			priority = eventPriority[priority:upper()]
		end
		return __LUA_THREAD__.eventManager.register(event, priority, b, callback)
	end,

	unregister = function(listener)
		return __LUA_THREAD__.eventManager.unregister(listener)
	end,

	Priority = eventPriority
}