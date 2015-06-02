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

local permissionsAPI = __LUA_STATE:getEnhancedPermissionManager()

if not permissionsAPI then
    return {
        getImmunityLevel = function(ply_or_uuid)
            return 0
        end,
        getGroup = function(ply_or_uuid)
            return "default"
        end,
        isAvailable = function(self)
            return false
        end
    }
end

local UUID = luajava.bindClass("java.util.UUID")

local function fixPlyOrUUID(ply_or_uuid)
    if type(ply_or_uuid) == "string" then
        return UUID:fromString(ply_or_uuid)
    elseif ply_or_uuid.__entity then
        return ply_or_uuid.__entity
    end
    return ply_or_uuid
end

return {
    getImmunityLevel = function(ply_or_uuid)
        return permissionsAPI:getImmunityLevel(fixPlyOrUUID(ply_or_uuid))
    end,
    getGroup = function(ply_or_uuid)
        return permissionsAPI:getGroup(fixPlyOrUUID(ply_or_uuid))
    end,
    isAvailable = function(self)
        return permissionsAPI:isAvailable()
    end,
    compareImmunityLevel = function(ply_or_uuid1, ply_or_uuid2)
        local level1 = self:getImmunityLevel(ply_or_uuid1)
        local level2 = self:getImmunityLevel(ply_or_uuid2)
        if level1 > level2 then
            return 1
        elseif level1 < level2 then
            return -1
        else
            return 0
        end
    end
}