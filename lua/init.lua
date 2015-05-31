package.path = __ROOTDIR__ .. "/classes/?.lua;" .. __ROOTDIR__ .. "/?.lua"

local rootDir = __ROOTDIR__
function include(file) 
	return dofile(rootDir .. '/' .. file)
end

include("main.lua")
