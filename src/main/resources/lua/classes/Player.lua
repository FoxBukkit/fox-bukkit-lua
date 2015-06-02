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

local bukkitServer = require("Server"):getBukkitServer()
local UUID = luajava.bindClass("java.util.UUID")

local Chat = require("Chat")
local Permission = require("Permission")

local table_insert = table.insert

local playerStorage = require("Storage"):create("getUniqueId", {
	sendXML = function(self, message)
		return Chat:sendLocalToPlayer(message, self.__entity)
	end,

	sendReply = function(self, message)
		return self:sendXML("<color name=\"dark_purple\">[FB]</color> " .. message)
	end,

	compareImmunityLevel = function(self, other)
		return Permission:compareImmunityLevel(self, other)
	end,

	fitsImmunityRequirement = function(self, other, requirement)
		return Permission:fitsImmunityRequirement(self, other, requirement)
	end,

	getImmunityLevel = function(self)
		return Permission:getImmunityLevel(self)
	end,

	getGroup = function(self)
		return Permission:getGroup(self)
	end
})

return {
	getByUUID = function(self, uuid)
		if type(uuid) == "string" then
			uuid = UUID:fromString(uuid)
		end
		return playerStorage(bukkitServer:getPlayer(uuid))
	end,

	getAll = function(self)
		local players = {}
		local iter = bukkitServer:getOnlinePlayers()
		for i = 1, #iter do
			table_insert(players, playerStorage(iter[i]))
		end
		return players
	end,

	findSingle = function(self, match, nomatch, immunitydelta, immunityply)
		local matches = self:find(match, nomatch, immunitydelta, immunityply)
		if #matches ~= 1 then
			return nil
		end
		return matches[1]
	end,

	find = function(self, match, nomatch, immunitydelta, immunityply)
		match = match:lower()
		local matches = {}
		for _, ply in next, self:getAll() do
			if ply ~= nomatch and
				(not immunitydelta or ply == immunityply or immunityply:fitsImmunityRequirement(ply, immunitydelta)) and
				(ply:getName():lower():find(match, 1, true) or ply:getDisplayName():lower():find(match, 1, true))
			then
				table_insert(matches, ply)
			end
		end
		return matches
	end,

	extend = function(self, player)
		return playerStorage(player)
	end
}