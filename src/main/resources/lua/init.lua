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

local luaThread = __LUA_THREAD
local includeDir = luaThread:getModuleDir()
package.path = includeDir .. "/classes/?.lua;" .. luaThread:getRootDir() .. "/classes/?.lua"

table.insert(package.searchers, 2, function(module)
    return luaThread:loadPackagedFile("classes/" .. module .. ".lua")
end)

for k,v in pairs(package.searchers) do
    print(tostring(k) .. "      " .. tostring(v))
end

local _dofile = dofile
local _loadfile = loadfile
function dofile(file)
	return _dofile(includeDir .. '/' .. file)
end
function loadfile(file)
    return _loadfile(includeDir .. '/' .. file)
end

dofile("main.lua")