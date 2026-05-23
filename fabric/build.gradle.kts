plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val supplementaries_version: String by extra
val trinkets_version: String by extra
val cca_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-fabric:${moonlight_version}")
    modImplementation("net.mehvahdjukaar:supplementaries-fabric:${supplementaries_version}")

    modCompileOnly("com.terraformersmc:modmenu:4.0.6")

    modCompileOnly("dev.emi:trinkets:${trinkets_version}")
    /*
        modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-base:${cca_version}")
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:${cca_version}")
    modCompileOnly("curse.maven:config-menus-forge-544048:4672356")
     */
}