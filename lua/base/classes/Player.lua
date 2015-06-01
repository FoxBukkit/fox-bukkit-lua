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
local BukkitServer = require('Server'):getBukkitServer()
local UUID = luajava.bindClass("java.util.UUID")

local Chat = require('Chat')

local playerStorage = require('Storage'):create('getUniqueId', {
	sendXML = function(self, message)
		return Chat:sendLocalToPlayer(message, self.__entity)
	end
})

return {
	getByUUID = function(self, uuid)
		if type(uuid) == "string" then
			uuid = UUID:fromString(uuid)
		end
		return playerStorage(BukkitServer:getPlayer(uuid))
	end,

	getAll = function(self)
		local players = {}
		for _, ply in pairs(BukkitServer:getOnlinePlayers()) do
			players:insert(playerStorage(ply))
		end
		return players
	end
}