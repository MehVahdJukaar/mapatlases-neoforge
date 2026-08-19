plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val codecui_version: String by extra
val supplementaries_version: String by extra
val trinkets_version: String by extra
val cca_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-fabric:${moonlight_version}")
    modRuntimeOnly("net.mehvahdjukaar:codecui-fabric:${codecui_version}")
    // Supplementaries drags in an old moonlight that publishes its jar with a "-fabric" classifier.
    // Loom keeps that classifier when conflict resolution bumps the version to ours (which has none),
    // so it ends up looking for a jar that was never produced. We declare moonlight directly anyway.
    modCompileOnly("net.mehvahdjukaar:supplementaries-fabric:${supplementaries_version}") {
        exclude(group = "net.mehvahdjukaar", module = "moonlight-fabric")
    }

    modCompileOnly("com.terraformersmc:modmenu:4.0.6")

    modCompileOnly("dev.emi:trinkets:${trinkets_version}")
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-base:${cca_version}")
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:${cca_version}")
    modCompileOnly("curse.maven:config-menus-forge-544048:4672356")
    modCompileOnly("curse.maven:emi-580555:6420930")
    modCompileOnly("curse.maven:jei-238222:7420583")
    modCompileOnly("curse.maven:roughly-enough-items-310111:6199139")
    modCompileOnly("curse.maven:roughly-enough-items-310111:6199140")
    modCompileOnly("curse.maven:curios-309927:6529130")
    modCompileOnly("curse.maven:the-twilight-forest-227639:7797302")

}