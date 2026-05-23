plugins {
    id("com.possible-triangle.common")
}

common {
    accessWidener()
}

val moonlight_version: String by extra
val supplementaries_version: String by extra


dependencies {
    modCompileOnly("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

    modImplementation("net.mehvahdjukaar:supplementaries-neoforge:${supplementaries_version}")
    modImplementation("curse.maven:the-twilight-forest-227639:7797302")
    modCompileOnly("maven.modrinth:immediatelyfast:1.6.10+1.21.1-neoforge")

    modCompileOnly("curse.maven:emi-580555:6420931")
    modCompileOnly("curse.maven:jei-238222:7420587")
    modCompileOnly("curse.maven:roughly-enough-items-310111:6199140")

    modCompileOnly("curse.maven:curios-309927:6274154")

    /*


    modCompileOnly("curse.maven:xaeros-minimap-263420:4812568")

   */
}
