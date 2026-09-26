plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val codecui_version: String by extra
val supplementaries_classes: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    modRuntimeOnly("net.mehvahdjukaar:codecui-neoforge:${codecui_version}")

    compileOnly(files(supplementaries_classes))
    modCompileOnly("curse.maven:the-twilight-forest-227639:7797302")
    modCompileOnly("curse.maven:curios-309927:6529130")
    modCompileOnly("maven.modrinth:route-relic:0.1.3-NF")

    modCompileOnly("curse.maven:emi-580555:6420931")
    modCompileOnly("curse.maven:jei-238222:7420587")
    modCompileOnly("curse.maven:roughly-enough-items-310111:6199140")
}