package com.zalphion.featurecontrol

import com.zalphion.featurecontrol.crypto.AppSecret
import com.zalphion.featurecontrol.events.localEventBus
import org.http4k.config.Environment
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.Http
import org.http4k.core.HttpHandler
import java.time.Clock
import kotlin.random.Random

fun createCloudCore(
    env: Environment,
    credentialsProvider: CredentialsProvider,
    region: Region,
    appSecret: AppSecret,
    internet: HttpHandler,
    clock: Clock,
    random: Random
) = CoreBuilder(
    clock = clock,
    random = random,
    origin = env[CloudSettings.origin],
    staticUri = env[CloudSettings.cdnHost],
    storageFn = { json -> CoreStorage.dynamoDb(
        dynamoDb = DynamoDb.Http(region, credentialsProvider, internet, clock),
        applicationsTableName = env[CloudSettings.applicationsTableName],
        featuresTableName = env[CloudSettings.featuresTableName],
        apiKeysTableName = env[CloudSettings.apiKeysTableName],
        usersTableName = env[CloudSettings.usersTableName],
        membersTableName = env[CloudSettings.membersTableName],
        teamsTableName = env[CloudSettings.teamsTableName],
        configPropertiesTableName = env[CloudSettings.configPropertiesTableName],
        configValuesTableName = env[CloudSettings.configValuesTableName],
        json = json
    ) },
    appSecret = appSecret,
    eventBusFn = ::localEventBus, // TODO sqs bus
    plugins = listOf(
        // TODO pro plugin
    )
).build {
    config = config.copy(
        pageSize = env[CloudSettings.pageSize],
        invitationRetention = env[CloudSettings.invitationsRetention],
    )
}