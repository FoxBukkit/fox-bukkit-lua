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
        getConsole = notImpl,
        sendGlobal = notImpl,
        broadcastLocal = notImpl,
        sendLocalToPlayer = notImpl,
        sendLocalToPermission = notImpl,
        sendLocal = notImpl,
        getPlayerNick = notImpl,
        getPlayerUUID = notImpl,
        isAvailable = function(self)
            return false
        end
    }
end

local function fixPly(ply)
    if ply and ply.__entity then
        return ply.__entity
    end
    return ply
end

local Player = require("Player")
local Chat = {
    getConsole = function(self)
        return chatAPI:getConsole()
    end,
    getPlayerUUID  = function(self, name)
        return chatAPI:getPlayerUUID(name)
    end,
    sendGlobal = function(self, source, type, content)
        return chatAPI:sendGlobal(fixPly(source), type, content)
    end,
    broadcastLocal = function(self, source, content)
        return chatAPI:broadcastLocal(fixPly(source), content)
    end,
    sendLocalToPlayer = function(self, source, content, target)
        if target then
            return chatAPI:sendLocalToPlayer(fixPly(source), content, fixPly(target))
        else
            return chatAPI:sendLocalToPlayer(source, fixPly(content))
        end
    end,
    sendLocalToPermissionm = function(self, source, content, target)
        if target then
            return chatAPI:sendLocalToPermissionm(fixPly(source), content, target)
        else
            return chatAPI:sendLocalToPermissionm(source, content)
        end
    end,
    sendLocal = function(self, source, content, chatTarget, targetFilter)
        return chatAPI:sendLocal(fixPly(source), content, chatTarget, targetFilter)
    end,
    getPlayerNick = function(self, ply_or_uuid)
        if ply_or_uuid.__entity then
            return chatAPI:getPlayerNick(ply_or_uuid.__entity)
        else
            return chatAPI:getPlayerNick(ply_or_uuid)
        end
    end
}

Player:addExtensions{
    sendXML = function(self, message)
        return Chat:sendLocalToPlayer(message, self)
    end,

    sendReply = function(self, message)
        return self:sendXML("<color name=\"dark_purple\">[FB]</color> " .. message)
    end,

    sendError = function(self, message)
        return self:sendXML("<color name=\"dark_red\">[FB]</color> " .. message)
    end,

    getNickName = function(self)
        return Chat:getPlayerNick(self)
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