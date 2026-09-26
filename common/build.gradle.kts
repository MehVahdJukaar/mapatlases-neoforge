plugins {
    id("com.possible-triangle.common")
}

common {
    //pinned so the build doesn't need to hit maven.neoforged.net to list versions
    neoformVersion = "26.1.2-1"
    accessWidener()
}

val moonlight_version: String by extra
val supplementaries_classes: String by extra

dependencies {
    modCompileOnly("net.mehvahdjukaar:moonlight-common:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-common:${moonlight_version}")

    compileOnly(files(supplementaries_classes))
    modCompileOnly("curse.maven:the-twilight-forest-227639:7797302")

    modCompileOnly("curse.maven:emi-580555:6420931")
    modCompileOnly("curse.maven:jei-238222:7420587")
    modCompileOnly("curse.maven:roughly-enough-items-310111:6199140")

    modCompileOnly("curse.maven:curios-309927:6529130")

    /*


    modCompileOnly("curse.maven:xaeros-minimap-263420:4812568")

   */
}
