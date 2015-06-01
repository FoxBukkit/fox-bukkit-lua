local eventPriority = luajava.bindClass("org.bukkit.event.EventPriority")

local eventManager = __LUA_THREAD__:getEventManager()

return {
	register = function(event, priority, callback, b)
		if type(event) == 'string' then
			event = luajava.bindClass(event)
		end
		if type(priority) == 'string' then
			priority = eventPriority[priority:upper()]
		end
		return eventManager:register(event, priority, b or false, callback)
	end,

	unregister = function(listener)
		return eventManager:unregister(listener)
	end,

	Priority = eventPriority
}