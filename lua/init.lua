package.path = __ROOTDIR__ .. "/classes/?.lua;" .. __ROOTDIR__ .. "/?.lua"

function include(file) 
	return dofile(__ROOTDIR__ .. '/' .. file)
end

include("main.lua")
