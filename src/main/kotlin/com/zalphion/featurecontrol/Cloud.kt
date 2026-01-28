package com.zalphion.featurecontrol

import com.zalphion.featurecontrol.auth.PermissionsFactory
import com.zalphion.featurecontrol.auth.multiTenant
import com.zalphion.featurecontrol.crypto.AppSecret
import com.zalphion.featurecontrol.emails.EmailSender
import com.zalphion.featurecontrol.emails.email
import com.zalphion.featurecontrol.emails.ses
import com.zalphion.featurecontrol.events.localEventBus
import com.zalphion.featurecontrol.plugins.Plugin
import com.zalphion.featurecontrol.plugins.PluginFactory
import com.zalphion.featurecontrol.storage.StorageDriver
import com.zalphion.featurecontrol.storage.dynamoDb
import com.zalphion.featurecontrol.web.LOGIN_PATH
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
import org.http4k.connect.amazon.ses.Http
import org.http4k.connect.amazon.ses.SES
import org.http4k.core.HttpHandler
import java.time.Clock
import kotlin.random.Random

fun Plugin.Companion.cloud() = object: PluginFactory<Plugin>(
    permissionsFactoryFn = { PermissionsFactory.multiTenant(it) }
) {
    override fun createInternal(core: Core) = object: Plugin {}
}

fun createCloud(
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

    return createCore(
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
            Plugin.email(
                emails = EmailSender.ses(SES.Http(region, credentialsProvider, internet, clock)),
                loginUri = env[CloudSettings.origin].path(LOGIN_PATH),
            ),
            Plugin.pro(
                statesRepositoryName = env[CloudSettings.statesTableName].value,
                segmentsRepositoryName = env[CloudSettings.segmentsTableName].value,
                configVersionsRepositoryName = env[CloudSettings.configVersionsTableName].value
            ),
            Plugin.cloud()
        )
    )
}