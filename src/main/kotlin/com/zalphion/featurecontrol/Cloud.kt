package com.zalphion.featurecontrol

import com.squareup.moshi.JsonAdapter
import com.zalphion.featurecontrol.crypto.AppSecret
import com.zalphion.featurecontrol.events.localEventBus
import com.zalphion.featurecontrol.plugins.Plugin
import com.zalphion.featurecontrol.storage.StorageDriver
import com.zalphion.featurecontrol.storage.dynamoDb
import dev.forkhandles.result4k.onFailure
import org.http4k.config.Environment
import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.Environment
import org.http4k.connect.amazon.Profile
import org.http4k.connect.amazon.RegionProvider
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.Http
import org.http4k.connect.amazon.secretsmanager.Http
import org.http4k.connect.amazon.secretsmanager.SecretsManager
import org.http4k.connect.amazon.secretsmanager.getSecretValue
import org.http4k.core.HttpHandler
import se.ansman.kotshi.KotshiJsonAdapterFactory
import java.time.Clock
import kotlin.random.Random

@KotshiJsonAdapterFactory
private object CloudJsonAdapterFactory : JsonAdapter.Factory by KotshiCloudJsonAdapterFactory

fun createCore(
    env: Environment,
    internet: HttpHandler,
    clock: Clock,
    random: Random
): Core {
    val region = RegionProvider.Environment(env).orElseThrow()
    val credentialsProvider = CredentialsChain.Environment(env)
        .orElse(CredentialsChain.Profile(env))
        .provider()

    val appSecret = SecretsManager.Http(region, credentialsProvider, internet, clock)
        .getSecretValue(env[CloudSettings.secretIdKey])
        .onFailure { it.reason.throwIt() }
        .let { AppSecret.of(it.SecretString!!) }

    val storageDriver = StorageDriver.dynamoDb(DynamoDb.Http(region, credentialsProvider, internet, clock))

    return CoreBuilder(
        clock = clock,
        random = random,
        config = CoreConfig(
            origin = env[CloudSettings.origin],
            staticUri = env[CloudSettings.cdnHost],
            appSecret = appSecret,
            invitationRetention = env[CloudSettings.invitationsRetention],
            googleClientId = env[CloudSettings.googleClientId],
            csrfTtl = env[CloudSettings.csrfTtl],
            sessionLength = env[CloudSettings.sessionLength],
            teamsStorageName = env[CloudSettings.teamsTableName].value,
            usersStorageName = env[CloudSettings.usersTableName].value,
            membersStorageName = env[CloudSettings.membersTableName].value,
            applicationsStorageName = env[CloudSettings.applicationsTableName].value,
            featuresStorageName = env[CloudSettings.featuresTableName].value,
            configsStorageName = env[CloudSettings.configsTableName].value,
            configEnvironmentsTableName = env[CloudSettings.configEnvironmentsTableName].value,
            apiKeysStorageName = env[CloudSettings.apiKeysTableName].value,
        ),
        storageDriver = storageDriver,
        eventBusFn = ::localEventBus, // TODO sqs bus
        plugins = listOf(
            CloudJsonAdapterFactory.asJsonPlugin(),
            Plugin.pro(
                statesRepositoryName = env[CloudSettings.statesTableName].value,
                segmentsRepositoryName = env[CloudSettings.segmentsTableName].value,
                configVersionsRepositoryName = env[CloudSettings.configVersionsTableName].value
            )
        )
    ).build()
}