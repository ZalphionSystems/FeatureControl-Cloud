package com.zalphion.featurecontrol.auth

import com.zalphion.featurecontrol.Core
import com.zalphion.featurecontrol.entitlements.ProEntitlement
import com.zalphion.featurecontrol.users.User
import com.zalphion.featurecontrol.users.UserId

fun PermissionsFactory.Companion.multiTenant(core: Core) = object: PermissionsFactory {
    private val roleBased = PermissionsFactory.roleBased(core.users, core.members)
    private val teamMembership = PermissionsFactory.teamMembership(core.users, core.members)
    private val zeroTrust = ZeroTrustPermissions()

    override fun create(userId: UserId): Permissions<User>? {
        val user = core.users[userId] ?: return null
        val memberships = core.members.list(userId)

        val permissions = core.teams
            .batchGet(memberships.map { it.teamId })
            .associate { it.teamId to core.getEntitlements(it.teamId) }
            .mapValues { (_, entitlements) ->
                if (ProEntitlement.RoleBasedPermissions in entitlements) {
                    roleBased.create(userId)
                } else {
                    teamMembership.create(userId)
                }
            }

        return Permissions.multiTenant(user) { teamId -> permissions[teamId] ?: zeroTrust }
    }
}