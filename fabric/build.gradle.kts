plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val codecui_version: String by extra
val supplementaries_classes: String by extra
val modmenu_version: String by extra
val trinkets_version: String by extra
val cca_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-fabric:${moonlight_version}")
    modRuntimeOnly("net.mehvahdjukaar:codecui-fabric:${codecui_version}")
    compileOnly(files(supplementaries_classes))

    modCompileOnly("com.terraformersmc:modmenu:${modmenu_version}")

    modCompileOnly("dev.emi:trinkets:${trinkets_version}")
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-base:${cca_version}")
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:${cca_version}")
    modCompileOnly("curse.maven:config-menus-forge-544048:4672356")
    modCompileOnly("curse.maven:emi-580555:6420930")
    modCompileOnly("mezz.jei:jei-26.1.2-fabric-api:29.16.0.48")
    modRuntimeOnly("mezz.jei:jei-26.1.2-fabric:29.16.0.48")
    modCompileOnly("curse.maven:roughly-enough-items-310111:6199139")
    modCompileOnly("curse.maven:roughly-enough-items-310111:6199140")
    modCompileOnly("curse.maven:curios-309927:6529130")
    modCompileOnly("curse.maven:the-twilight-forest-227639:7797302")

}