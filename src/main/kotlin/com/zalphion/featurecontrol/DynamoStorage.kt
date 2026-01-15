package com.zalphion.featurecontrol

import com.zalphion.featurecontrol.apikeys.ApiKeyStorage
import com.zalphion.featurecontrol.apikeys.dynamoDb
import com.zalphion.featurecontrol.configs.ConfigStorage
import com.zalphion.featurecontrol.configs.dynamoDb
import com.zalphion.featurecontrol.crypto.Base64String
import com.zalphion.featurecontrol.features.EnvironmentName
import com.zalphion.featurecontrol.features.FeatureKey
import com.zalphion.featurecontrol.applications.ApplicationStorage
import com.zalphion.featurecontrol.features.FeatureStorage
import com.zalphion.featurecontrol.features.dynamoDb
import com.zalphion.featurecontrol.members.MemberStorage
import com.zalphion.featurecontrol.members.dynamoDb
import com.zalphion.featurecontrol.applications.AppId
import com.zalphion.featurecontrol.applications.dynamoDb
import com.zalphion.featurecontrol.teams.TeamId
import com.zalphion.featurecontrol.teams.TeamStorage
import com.zalphion.featurecontrol.teams.dynamoDb
import com.zalphion.featurecontrol.users.EmailAddress
import com.zalphion.featurecontrol.users.UserId
import com.zalphion.featurecontrol.users.UserStorage
import com.zalphion.featurecontrol.users.dynamoDb
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.value
import org.http4k.format.ConfigurableMoshi

internal val appIdAttr = Attribute.value(AppId).required("appId")
internal val environmentNameAttr = Attribute.value(EnvironmentName).required("environmentName")
internal val featureKeyAttr = Attribute.value(FeatureKey).required("featureKey")
internal val teamIdAttr = Attribute.value(TeamId).required("teamId")
internal val hashedApiKeyAttr = Attribute.value(Base64String).required("hashedApiKey")
internal val encryptedApiKeyAttr = Attribute.value(Base64String).required("encryptedApiKey")
internal val userIdAttr = Attribute.value(UserId).required("userId")
internal val emailAddressAttr = Attribute.value(EmailAddress).required("emailAddress")

internal fun CoreStorage.Companion.dynamoDb(
    dynamoDb: DynamoDb,
    applicationsTableName: TableName,
    featuresTableName: TableName,
    apiKeysTableName: TableName,
    usersTableName: TableName,
    teamsTableName: TableName,
    membersTableName: TableName,
    configPropertiesTableName: TableName,
    configValuesTableName: TableName,
    json: ConfigurableMoshi
) = CoreStorage(
    applications = ApplicationStorage.dynamoDb(dynamoDb, applicationsTableName, json),
    features = FeatureStorage.dynamoDb(dynamoDb, featuresTableName, json),
    apiKeys = ApiKeyStorage.dynamoDb(dynamoDb, apiKeysTableName),
    users = UserStorage.dynamoDb(dynamoDb, usersTableName, json),
    teams = TeamStorage.dynamoDb(dynamoDb, teamsTableName, json),
    members = MemberStorage.dynamoDb(dynamoDb, membersTableName, json),
    configs = ConfigStorage.dynamoDb(dynamoDb, configPropertiesTableName, configValuesTableName, json)
)