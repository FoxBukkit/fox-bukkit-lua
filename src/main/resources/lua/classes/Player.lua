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
local UUID = bindClass("java.util.UUID")

local table_insert = table.insert
local type = type

local playerExt = {}

local playerStorage = require("Storage"):create("getUniqueId", "player", playerExt)

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
		if not iter.length then
			iter = iter:iterator()
			while iter:hasNext() do
				table_insert(players, playerStorage(iter:next()))
			end
		else
			for i = 1, #iter do
				table_insert(players, playerStorage(iter[i]))
			end
		end
		return players
	end,

	findSingle = function(self, match, nomatch, immunitydelta, immunityply, permission)
		local matches = self:find(match, nomatch, immunitydelta, immunityply, permission, true)
		if #matches ~= 1 then
			return nil
		end
		return matches[1]
	end,

	find = function(self, match, nomatch, immunitydelta, immunityply, permission, forbidMultiple)
		local ignoreName = false
		local availablePlayers
		if match then
			match = match:lower()
			local matchFirst = match:sub(1,1)
			if matchFirst == "@" then
				availablePlayers = {playerStorage(bukkitServer:getPlayerExact(match:sub(2)))}
				ignoreName = true
			elseif matchFirst == "*" then
				forbidMultiple = false
				match = match:sub(2)
			elseif matchFirst == "$" then
				availablePlayers = {self:getByUUID(match:sub(2))}
				ignoreName = true
			end

			if match:len() < 1 then
				ignoreName = true
			end
		else
			ignoreName = true
		end

		if not availablePlayers then
			availablePlayers = self:getAll()
		end

		local matches = {}
		for _, ply in next, availablePlayers do
			if ply ~= nomatch and
				(not immunitydelta or ply == immunityply or immunityply:fitsImmunityRequirement(ply, immunitydelta)) and
				(ignoreName or ply:getName():lower():find(match, 1, true) or ply:getDisplayName():lower():find(match, 1, true)) and
				(not permission or ply:hasPermission(permission))
			then
				table_insert(matches, ply)
			end
		end

		if forbidMultiple and #matches ~= 1 then
			return {}
		end

		return matches
	end,

	extend = function(self, player)
		return playerStorage(player)
	end,

	addExtensions = function(self, extensions)
		for k, v in next, extensions do
			playerExt[k] = v
		end
	end
}