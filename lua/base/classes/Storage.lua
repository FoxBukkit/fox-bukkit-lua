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