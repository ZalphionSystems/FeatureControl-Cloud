dependencies {
    api(project(":pro"))
    api("com.zalphion.featurecontrol:emails:latest-SNAPSHOT")
    api(osslibs.http4k.config)

    implementation(platform(osslibs.http4k.bom))
    implementation(libs.http4k.connect.amazon.dynamodb)
    implementation(libs.http4k.connect.amazon.secretsmanager)
    implementation(libs.http4k.connect.amazon.ses)

    testImplementation(libs.http4k.connect.amazon.dynamodb.fake)
}