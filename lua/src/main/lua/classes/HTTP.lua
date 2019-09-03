--[[

    This file is part of FoxBukkitLua-lua.

    FoxBukkitLua-lua is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FoxBukkitLua-lua is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FoxBukkitLua-lua.  If not, see <http://www.gnu.org/licenses/>.

]]

local luaState = __LUA_STATE
local URL = bindClass("java.net.URL")

return {
    openConnection = function(self, url)
        return luajava.new(URL, url):openConnection()
    end,

    runReqeust = function(self, connection, options)
        local stream = connection:getInputStream()
        local data = luaState:readStream(stream)
        return data
    end,

    get = function(self, url, options)
        local conn = self:openConnection(url)
        return self:runReqeust(conn, options)
    end
}
