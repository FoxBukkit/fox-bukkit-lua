function include(file) 
	return dofile(__ROOTDIR__ .. '/' .. file)
end

local function makeClass(tbl)
	return setmetatable(tbl, {
		__index = tbl,
		__metatable = false,
		__newindex = function()
			error("Readonly")
		end
	})
end

C = {}

function includeClass(clsName)
	local cls = C[clsName]
	if not cls then
		cls = makeClass(include("classes/" .. clsName .. ".lua"))
		C[clsName] = cls
	end
	return cls
end

includeClass("Storage")
includeClass("Player")
includeClass("Event")
includeClass("Chat")

include("main.lua")
