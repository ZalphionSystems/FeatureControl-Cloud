package com.zalphion.featurecontrol

import org.http4k.client.Java8HttpClient
import org.http4k.config.Environment
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.serverless.ApiGatewayV2FnLoader
import org.http4k.serverless.AppLoader
import org.http4k.serverless.AwsLambdaEventFunction
import java.security.SecureRandom
import java.time.Clock
import kotlin.random.asKotlinRandom

private val appLoader = AppLoader { envMap ->
    createCore(
        env = Environment.from(envMap),
        internet = Java8HttpClient(),
        clock = Clock.systemUTC(),
        random = SecureRandom().asKotlinRandom()
    ).getRoutes()
}

@Suppress("Unused")
class ApiGatewayLambda: AwsLambdaEventFunction(ApiGatewayV2FnLoader(appLoader))

fun main() {
    appLoader(System.getenv())
        .asServer(SunHttp(80))
        .start()
        .also { println("Started server on http://localhost:${it.port()}") }
}