plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":pro"))
    api("com.zalphion.featurecontrol:emails:latest-SNAPSHOT")

    api("org.http4k:http4k-config")
    implementation("org.http4k:http4k-connect-amazon-dynamodb")
    implementation("org.http4k:http4k-connect-amazon-secretsmanager")
    implementation("org.http4k:http4k-connect-amazon-ses")

    ksp("se.ansman.kotshi:compiler:_")

    testImplementation("org.http4k:http4k-connect-amazon-dynamodb-fake")
}