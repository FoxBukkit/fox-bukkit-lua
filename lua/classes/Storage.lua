local _entity_mt = {
	__index = function(tbl, idx)
		local myValue = rawget(tbl, idx)
		if myValue == nil then
			myValue = rawget(tbl, 'storage')[idx]
			if myValue == nil then
				local entity = rawget(tbl, 'entity')
				local func = entity[idx]
				if func then
					myValue = function(self, ...)
						return func(entity, ...)
					end
					rawset(tbl, idx, myValue)
				end
			end
		end
		return myValue
	end,
	__newindex = function(tbl, idx, value)
		rawget(tbl, 'storage')[idx] = value
	end,
	__metatable = false
}

local _storage_mt = {
	__call = function (self, entity, entityID)
		if not entity then
			return nil
		end

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
	create = function()
		return setmetatable({
			storage = {}
		}, _storage_mt)
	end
}