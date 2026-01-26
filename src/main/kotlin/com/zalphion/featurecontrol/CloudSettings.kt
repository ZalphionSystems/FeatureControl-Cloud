package com.zalphion.featurecontrol

import org.http4k.config.EnvironmentKey
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.secretsmanager.model.SecretId
import org.http4k.lens.duration
import org.http4k.lens.uri
import org.http4k.lens.value
import java.time.Duration

internal object CloudSettings {

    val cdnHost = EnvironmentKey.uri().required("CDN_HOST")
    val csrfTtl = EnvironmentKey.duration().defaulted("CSRF_TTL", Duration.ofHours(8))
    val secretIdKey = EnvironmentKey.value(SecretId).required("APP_SECRET_ID")
    val origin = EnvironmentKey.uri().required("ORIGIN")
    val sessionLength = EnvironmentKey.duration().defaulted("SESSION_LENGTH", Duration.ofDays(7))
    val googleClientId = EnvironmentKey.required("GOOGLE_CLIENT_ID")
    val invitationsRetention = EnvironmentKey.duration().defaulted("INVITATIONS_RETENTION", Duration.ofDays(7))

    // storage
    val teamsTableName = EnvironmentKey.value(TableName).required("TEAMS_TABLE_NAME")
    val usersTableName = EnvironmentKey.value(TableName).required("USERS_TABLE_NAME")
    val membersTableName = EnvironmentKey.value(TableName).required("MEMBERS_TABLE_NAME")
    val applicationsTableName = EnvironmentKey.value(TableName).required("APPLICATIONS_TABLE_NAME")
    val featuresTableName = EnvironmentKey.value(TableName).required("FEATURES_TABLE_NAME")
    val configsTableName = EnvironmentKey.value(TableName).required("CONFIGS_TABLE_NAME")
    val configEnvironmentsTableName = EnvironmentKey.value(TableName).required("CONFIG_ENVIRONMENTS_TABLE_NAME")
    val apiKeysTableName = EnvironmentKey.value(TableName).required("API_KEYS_TABLE_NAME")
    val statesTableName = EnvironmentKey.value(TableName).required("STATES_TABLE_NAME")
    val segmentsTableName = EnvironmentKey.value(TableName).required("SEGMENTS_TABLE_NAME")
    val configVersionsTableName = EnvironmentKey.value(TableName).required("CONFIG_VERSIONS_TABLE_NAME")
}