package com.zalphion.featurecontrol.auth

import com.zalphion.featurecontrol.applications.AppId
import com.zalphion.featurecontrol.features.EnvironmentName
import com.zalphion.featurecontrol.features.FeatureKey
import com.zalphion.featurecontrol.teams.TeamId
import com.zalphion.featurecontrol.users.User
import com.zalphion.featurecontrol.users.UserId

fun Permissions.Companion.multiTenant(user: User, permissions: (TeamId) -> Permissions<*>) = object : Permissions<User> {
    override val principal = user

    override fun teamCreate() = true

    override fun teamRead(teamId: TeamId) = permissions(teamId).teamRead(teamId)
    override fun teamUpdate(teamId: TeamId) = permissions(teamId).teamUpdate(teamId)
    override fun teamDelete(teamId: TeamId) = permissions(teamId).teamDelete(teamId)

    override fun userUpdate(userId: UserId) = principal.userId == userId
    override fun userDelete(userId: UserId) = principal.userId == userId

    override fun memberCreate(teamId: TeamId) = permissions(teamId).memberCreate(teamId)
    override fun memberRead(teamId: TeamId, userId: UserId) = permissions(teamId).memberRead(teamId, userId)

    override fun memberUpdate(teamId: TeamId, userId: UserId) = permissions(teamId).memberUpdate(teamId, userId)
    override fun memberDelete(teamId: TeamId, userId: UserId) = permissions(teamId).memberDelete(teamId, userId)

    override fun applicationCreate(teamId: TeamId) = permissions(teamId).applicationCreate(teamId)
    override fun applicationRead(teamId: TeamId, appId: AppId) = permissions(teamId).applicationRead(teamId, appId)

    override fun applicationUpdate(teamId: TeamId, appId: AppId) = permissions(teamId).applicationUpdate(teamId, appId)
    override fun applicationDelete(teamId: TeamId, appId: AppId) = permissions(teamId).applicationDelete(teamId, appId)

    override fun featureCreate(teamId: TeamId, appId: AppId) = permissions(teamId).featureCreate(teamId, appId)
    override fun featureRead(teamId: TeamId, appId: AppId, key: FeatureKey) = permissions(teamId).featureRead(teamId, appId, key)
    override fun featureUpdate(teamId: TeamId, appId: AppId, key: FeatureKey) = permissions(teamId).featureUpdate(teamId, appId, key)
    override fun featureDelete(teamId: TeamId, appId: AppId, key: FeatureKey) = permissions(teamId).featureDelete(teamId, appId, key)

    override fun featureRead(teamId: TeamId, appId: AppId, featureKey: FeatureKey, environment: EnvironmentName) =
        permissions(teamId).featureRead(teamId, appId, featureKey, environment)
    override fun featureUpdate(teamId: TeamId, appId: AppId, featureKey: FeatureKey, environment: EnvironmentName) =
        permissions(teamId).featureUpdate(teamId, appId, featureKey, environment)

    override fun configRead(teamId: TeamId, appId: AppId) = permissions(teamId).configRead(teamId, appId)
    override fun configUpdate(teamId: TeamId, appId: AppId) = permissions(teamId).configUpdate(teamId, appId)

    override fun configRead(teamId: TeamId, appId: AppId, environment: EnvironmentName) = permissions(teamId).configRead(teamId, appId, environment)
    override fun configUpdate(teamId: TeamId, appId: AppId, environment: EnvironmentName) = permissions(teamId).configUpdate(teamId, appId, environment)
}