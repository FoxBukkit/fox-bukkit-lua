--[[

    This file is part of FoxBukkit.

    FoxBukkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FoxBukkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FoxBukkit.  If not, see <http://www.gnu.org/licenses/>.

]]
local _entity_mt = {
	__index = function(tbl, idx)
		local storageTbl = rawget(tbl, 'storage')
		local myValue = storageTbl[idx]
		if myValue ~= nil then
			return myValue
		end

		local entity = rawget(tbl, 'entity')
		local entityValue = entity[idx]
		if entityValue and type(entityValue) == 'function' then
			myValue = function(self, ...)
				return entityValue(entity, ...)
			end
			storageTbl[idx] = myValue
			return myValue
		else
			return entityValue
		end
	end,
	__newindex = function(tbl, idx, value)
		local entity = rawget(tbl, 'entity')
		if entity[idx] ~= nil then
			entity[idx] = value
			return
		end
		rawget(tbl, 'storage')[idx] = value
	end,
	__metatable = false
}

local _storage_mt = {
	__call = function (self, entity)
		if not entity then
			return nil
		end

		local entityID = entity[self.idFunction](entity)

		local storage = self.storage[entityID]
		if not storage then
			storage = {}
			self.storage[entityID] = storage
		end
		return setmetatable({
			entity = entity,
			storage = storage
		}, _entity_mt)
	end,

	__metatable = false,
	__newindex = function()
		error("Readonly")
	end
}

_storage_mt.__index = _storage_mt

return {
	create = function(idFunction)
		return setmetatable({
			storage = {},
			idFunction = idFunction
		}, _storage_mt)
	end
}