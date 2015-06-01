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

local cmdManager = __LUA_THREAD:getCommandManager()

local Player = require('Player')

local _flags_mt = {
    __index = {
        has = function(self, flag)
            return self.str:find(flag, 1, true) ~= nil
        end
    },
    __metatable = false,
    __newindex = function()
        error("Readonly")
    end
}

return {
    register = function(self, cmd, func)
        return cmdManager:register(cmd, function(ply, cmd, args, argStr, flagStr)
            flagStr = setmetatable({
                str = flagStr
            }, _flags_mt)
            if ply.getUniqueId then
                ply = Player:extend(ply)
            end
            return func(ply, cmd, args, argStr, flagStr)
        end)
    end,
    unregister = function(self, cmd)
        cmdManager:unregister(cmd)
    end
}
