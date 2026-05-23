plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val supplementaries_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

    modCompileOnly("net.mehvahdjukaar:supplementaries-neoforge:${supplementaries_version}")
    modCompileOnly("curse.maven:the-twilight-forest-227639:7797302")
    modCompileOnly("maven.modrinth:immediatelyfast:1.6.10+1.21.1-neoforge")
    modCompileOnly("curse.maven:curios-309927:6274154")

    modCompileOnly("curse.maven:emi-580555:6420931")
    modCompileOnly("curse.maven:jei-238222:7420587")
    modCompileOnly("curse.maven:roughly-enough-items-310111:6199140")
}