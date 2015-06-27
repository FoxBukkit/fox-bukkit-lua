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

local chatAPI = __LUA_STATE:getEnhancedChatMessageManager()

if not chatAPI then
    local function notImpl()
        error("Enhanced chat API not available")
    end

    return {
        sendGlobal = notImpl,
        broadcastLocal = notImpl,
        sendLocalToPlayer = notImpl,
        sendLocalToPermission = notImpl,
        sendLocal = notImpl,
        getPlayerNick = notImpl,
        isAvailable = function(self)
            return false
        end
    }
end

local Player = require("Player")
local Chat = chatAPI

Player:addExtensions{
    sendXML = function(self, message)
        return Chat:sendLocalToPlayer(message, self.__entity)
    end,

    sendReply = function(self, message)
        return self:sendXML("<color name=\"dark_purple\">[FB]</color> " .. message)
    end,

    sendError = function(self, message)
        return self:sendXML("<color name=\"dark_red\">[FB]</color> " .. message)
    end,

    getNickName = function(self)
        return Chat:getPlayerNick(self.__entity)
    end
}

Player:addConsoleExtensions{
    sendReply = function(self, message)
        return self:sendMessage("[FB] " .. message)
    end,

    sendError = function(self, message)
        return self:sendMessage("[FB] [ERROR] " .. message)
    end
}

return Chat