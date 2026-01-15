package com.zalphion.featurecontrol

import org.http4k.config.EnvironmentKey
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.lens.duration
import org.http4k.lens.int
import org.http4k.lens.uri
import org.http4k.lens.value
import java.time.Duration

internal object CloudSettings {
    // general
    val pageSize = EnvironmentKey.int().defaulted("PAGE_SIZE", 100)
    val cdnHost = EnvironmentKey.uri().required("CDN_HOST")
    val csrfTtl = EnvironmentKey.duration().defaulted("CSRF_TTL", Duration.ofHours(8))

    // core
    val applicationsTableName = EnvironmentKey.value(TableName).required("APPLICATIONS_TABLE_NAME")
    val featuresTableName = EnvironmentKey.value(TableName).required("FEATURES_TABLE_NAME")
    val apiKeysTableName = EnvironmentKey.value(TableName).required("API_KEYS_TABLE_NAME")

    // users
    val origin = EnvironmentKey.uri().required("ORIGIN")
    val sessionLength = EnvironmentKey.duration().defaulted("SESSION_LENGTH", Duration.ofDays(7))
    val usersTableName = EnvironmentKey.value(TableName).required("USERS_TABLE_NAME")

    // social
    val googleClientId = EnvironmentKey.required("GOOGLE_CLIENT_ID")

    // teams
    val teamsTableName = EnvironmentKey.value(TableName).required("TEAMS_TABLE_NAME")
    val membersTableName = EnvironmentKey.value(TableName).required("MEMBERS_TABLE_NAME")
    val invitationsRetention = EnvironmentKey.duration().defaulted("INVITATIONS_RETENTION", Duration.ofDays(7))

    // configs
    val configPropertiesTableName = EnvironmentKey.value(TableName).required("CONFIG_PROPERTIES_TABLE_NAME")
    val configValuesTableName = EnvironmentKey.value(TableName).required("CONFIG_VALUES_TABLE_NAME")
}