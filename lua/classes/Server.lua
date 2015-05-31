local BukkitServer = luajava.bindClass("org.bukkit.Bukkit")

return {
	getBukkitServer = function()
		return BukkitServer
	end,
	runOnMainThread = function(func)
		__LUA_THREAD__:runOnMainThread(func)
	end
}