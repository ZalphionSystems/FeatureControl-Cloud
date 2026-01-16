package com.zalphion.featurecontrol

import org.http4k.config.EnvironmentKey
import org.http4k.connect.amazon.secretsmanager.model.SecretId
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
    val secretIdKey = EnvironmentKey.value(SecretId).required("APP_SECRET_ID")

    // users
    val origin = EnvironmentKey.uri().required("ORIGIN")
    val sessionLength = EnvironmentKey.duration().defaulted("SESSION_LENGTH", Duration.ofDays(7))

    // social
    val googleClientId = EnvironmentKey.required("GOOGLE_CLIENT_ID")

    // teams
    val invitationsRetention = EnvironmentKey.duration().defaulted("INVITATIONS_RETENTION", Duration.ofDays(7))
}