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

local Player = require("Player")

Player:addConsoleExtensions{
    sendReply = function(self, message)
        return self:sendMessage("[FB] " .. message)
    end,

    sendError = function(self, message)
        return self:sendMessage("[FB] [ERROR] " .. message)
    end
}

if not chatAPI then
    local bukkitServer = require("Server"):getBukkitServer()

    local Chat = {
        isAvailable = function(self)
            return false
        end,

        getConsole = Player.getConsole,

        makeButton = function(self, command, label, color, run, addHover)
            return "<BUTTON:" .. command ">" .. label .. "</BUTTON>"
        end,

        getPlayerUUID = function(self, name)
            return nil
        end,

        sendGlobal = function(self, source, type, content)
            bukkitServer:broadcastMessage(content)
        end,

        broadcastLocal = function(self, source, content)
            bukkitServer:broadcastMessage(content)
        end,

        sendLocalToPlayer = function(self, source, content, target)
            target:sendMessage(content)
        end,

        sendLocalToPermission = function(self, source, content, target)
            bukkitServer:broadcastMessage("$" + target, content)
        end,

        sendLocal = function(self, source, content, chatTarget, targetFilter)
            bukkitServer:broadcastMessage("!" .. tostring(targetFilter) .. "!" .. tostring(chatTarget), content)
        end,

        getPlayerNick = function(self, ply_or_uuid)
            return ply_or_uuid:getDisplayName()
        end,
    }

    Player:addExtensions{
        sendXML = function(self, message)
            return Chat:sendLocalToPlayer(message, self)
        end,

        sendReply = function(self, message)
            return self:sendXML("[FB] " .. message)
        end,

        sendError = function(self, message)
            return self:sendXML("[FB] [ERROR] " .. message)
        end,

        getNickName = function(self)
            return self:getDisplayName()
        end
    }

    return Chat
end

local function fixPly(ply)
    if ply and ply.__entity then
        return ply.__entity
    end
    return ply
end

local Chat = {
    isAvailable = function(self)
        return chatAPI:isAvailable()
    end,

    getConsole = function(self)
        return chatAPI:getConsole()
    end,

    makeButton = function(self, command, label, color, run, addHover)
        return chatAPI:makeButton(command, label, color, run, (addHover ~= false))
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
        return chatAPI:sendLocalToPlayer(fixPly(source), content, fixPly(target))
    end,

    sendLocalToPermission = function(self, source, content, target)
        if target then
            return chatAPI:sendLocalToPermission(fixPly(source), content, target)
        else
            return chatAPI:sendLocalToPermission(source, content)
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

return Chat