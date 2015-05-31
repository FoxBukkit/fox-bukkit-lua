local BukkitServer = luajava.bindClass("org.bukkit.Bukkit")
local LuaThread = __LUA_THREAD__

return {
	getBukkitServer = function()
		return BukkitServer
	end,
	runOnMainThread = function(func)
		LuaThread:runOnMainThread(func)
	end
}