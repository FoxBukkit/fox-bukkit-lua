local BukkitServer = luajava.bindClass("org.bukkit.Bukkit")
local UUID = luajava.bindClass("java.util.UUID")

local playerStorage = C.Storage.create()
local function makePlayerFromBukkitPlayer(bukkitPlayer)
	return playerStorage(bukkitPlayer, bukkitPlayer:getUniqueId())
end

return {
	getByUUID = function(uuid)
		if type(uuid) == "string" then
			uuid = UUID:fromString(uuid)
		end
		return makePlayerFromBukkitPlayer(BukkitServer:getPlayer(uuid))
	end,

	getAll = function()
		local players = {}
		for _, ply in pairs(BukkitServer:getOnlinePlayers()) do
			players:insert(makePlayerFromBukkitPlayer(ply))
		end
		return players
	end
}