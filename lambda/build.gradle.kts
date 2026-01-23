dependencies {
    implementation(project(":cloud"))
    implementation(platform(osslibs.http4k.bom))
    implementation(libs.http4k.serverless.lambda)
}