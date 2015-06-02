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

local luaState = __LUA_STATE
local includeDir = luaState:getModuleDir()
local File = luajava.bindClass("java.io.File")

package.path = includeDir .. "/classes/?.lua;" .. luaState:getRootDir() .. "/classes/?.lua"

table.insert(package.searchers, 3, function(module)
    return luaState:loadPackagedFile("classes/" .. module .. ".lua")
end)

local _dofile = dofile
local _loadfile = loadfile
function dofile(file)
	return _dofile(includeDir .. '/' .. file)
end
function loadfile(file)
    return _loadfile(includeDir .. '/' .. file)
end

local function _scandir(dir, cb, recursive, rootDirLen)
    local iter = dir:listFiles()
    for i = 1, #iter do
        local v = iter[i]
        if recursive and v:isDirectory() then
            _scandir(v, cb, recursive, rootDir)
        else
            cb(v:getAbsolutePath():sub(rootDirLen))
        end
    end
end
function scandir(dir, cb, recursive)
    if type(dir) == "string" then
        dir = luajava.new(File, includeDir .. '/' .. dir)
    end
    _scandir(dir, cb, recursive, dir:getAbsolutePath():len() + 2)
end

scandir('autorun', function(file)
    dofile('autorun/' .. file)
end, true)
