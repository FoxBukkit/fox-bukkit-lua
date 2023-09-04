--[[

    foxbukkit-lua-lua - ${project.description}
    Copyright © ${year} Doridian (git@doridian.net)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

]]
local luaState = __LUA_STATE
local URL = bindClass('java.net.URL')

return {
	openConnection = function(_, url)
		return luajava.new(URL, url):openConnection()
	end,
	runReqeust = function(_, connection)
		local stream = connection:getInputStream()
		local data = luaState:readStream(stream)
		return data
	end,
	get = function(self, url)
		local conn = self:openConnection(url)
		return self:runReqeust(conn)
	end,
}
