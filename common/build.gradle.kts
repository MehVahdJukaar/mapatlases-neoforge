plugins {
    id("com.possible-triangle.common")
}

common {
    accessWidener()
}

val moonlight_version: String by extra
val supplementaries_version: String by extra


dependencies {
    //@jar skips moonlight's module metadata: its jar variants are tagged neoforge-only, so in this module gradle
    //would otherwise fall back to the access transformer variant and the whole api would be missing from the classpath
    modCompileOnly("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}@jar")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

    modCompileOnly("net.mehvahdjukaar:supplementaries-neoforge:${supplementaries_version}")
    modCompileOnly("curse.maven:the-twilight-forest-227639:7797302")

    modCompileOnly("curse.maven:emi-580555:6420931")
    modCompileOnly("curse.maven:jei-238222:7420587")
    modCompileOnly("curse.maven:roughly-enough-items-310111:6199140")

    modCompileOnly("curse.maven:curios-309927:6529130")

    /*


    modCompileOnly("curse.maven:xaeros-minimap-263420:4812568")

   */
}
