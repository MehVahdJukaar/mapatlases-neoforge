# AGENT.md

## What this repo is

This is the editable working copy for the `Map Atlases` Fabric port to Minecraft/Fabric `26.1`.

- Working repo: `F:/mapatlases-neoforge`
- Working branch: `26.1`
- `origin`: `https://github.com/R2bEEaton/mapatlases-neoforge`
- `upstream`: `https://github.com/MehVahdJukaar/mapatlases-neoforge`

## Source of truth

Use the separate local reference checkout for behavior, visuals, and code structure:

- Reference repo: `F:/mapatlases-neoforge-ref`
- Reference branch: `multiloader`

The goal is a faithful Fabric-only port of the `multiloader` implementation, not a redesign and not a fresh rewrite.

## Problem to solve

Port the mod from its current `1.20.1` multiloader codebase to Fabric / Minecraft `26.1`, preserving all user-facing features that exist in the `multiloader` branch as closely as possible.

That includes, at minimum:

- atlas item behavior
- atlas overview screen and interaction model
- minimap / HUD behavior
- map rendering and map collection behavior
- networking
- cartography integration
- lectern integration
- existing textures and screen design
- existing Fabric-side integrations as far as they still make sense on `26.1`

Only Fabric matters for this port. Multiloader support is not required in the final result.

## Current setup status

The correct repo is now cloned and prepared:

- local `26.1` branch created
- remotes are configured correctly
- repo layout confirmed:
  - `common/` contains most shared gameplay, UI, networking, mixins, and rendering logic
  - `fabric/` contains Fabric bootstrap and Fabric-specific integrations
  - `forge/` exists but is out of scope for this port

## Immediate blocker

The current baseline does not build cleanly in this environment before any `26.1` work starts.

Observed on `2026-03-30`:

- `./gradlew.bat :fabric:build` fails during dependency resolution
- missing dependency:
  - `net.mehvahdjukaar:moonlight:1.20-2.15.5`

Gradle searched the configured repositories and did not find that artifact.

This means the first practical task is to repair dependency resolution for the current baseline, or otherwise redirect the build to a valid Moonlight artifact/source, before doing the full `26.1` API migration.

## Important things learned so far

1. The previous `F:/MapAtlases` repo was the wrong starting point for this task.
2. The correct codebase is `mapatlases-neoforge`, and the right reference branch is `multiloader`.
3. This repo already uses an Architectury-style structure:
   - root build
   - `common/`
   - `fabric/`
   - `forge/`
4. The Fabric port will likely involve most changes in:
   - `common/src/main/java/pepjebs/mapatlases/...`
   - `fabric/src/main/java/pepjebs/mapatlases/...`
5. The shared code already includes the features that matter:
   - `client/screen/AtlasOverviewScreen.java`
   - `client/ui/MapAtlasesHUD.java`
   - `item/MapAtlasItem.java`
   - `lifecycle/MapAtlasesClientEvents.java`
   - `lifecycle/MapAtlasesServerEvents.java`
   - `networking/...`
   - `mixin/...`
6. Because the goal is a faithful port, the `multiloader` branch should drive:
   - UI layout
   - textures
   - behavior
   - code organization
   - feature parity
7. Do not simplify features just to get a green build unless absolutely necessary; prefer porting the real implementation forward.

## Baseline checkpoint: 1.20.1 Fabric restored

Recorded on `2026-03-30`.

- Removed `forge` from the active Gradle build graph because it was blocking `:fabric:build` during configuration:
  - `settings.gradle`: dropped `include("forge")`
  - `gradle.properties`: changed `enabled_platforms` from `fabric,forge` to `fabric`
- Replaced the dead Moonlight Maven coordinates with published CurseMaven Selene artifacts:
  - `common/build.gradle`
    - replaced `net.mehvahdjukaar:moonlight:${moonlight_version}`
    - with `modCompileOnly("curse.maven:selene-499980:5942982")`
  - `fabric/build.gradle`
    - replaced `net.mehvahdjukaar:moonlight-fabric:${moonlight_version}`
    - with `modImplementation("curse.maven:selene-499980:5942982")`
  - `forge/build.gradle`
    - updated the Moonlight dependency comment path to a published CurseMaven artifact too, even though Forge is no longer in the active build graph
- Baseline source compatibility fix for the newer published Moonlight API:
  - `common/src/main/java/pepjebs/mapatlases/integration/moonlight/MoonlightCompat.java`
  - replaced the obsolete `Utils.rayTrace(player, true, 0)` call with the available overload using `ClipContext`
  - cast the result to `BlockHitResult` before reading the marker position

## Baseline verification

Commands run on `2026-03-30`:

- `./gradlew.bat :fabric:build`
- `./gradlew.bat :fabric:runClient`

Results:

- `./gradlew.bat :fabric:build`
  - passes successfully on the current baseline after the dependency and small source compatibility fixes
- `./gradlew.bat :fabric:runClient`
  - now launches with Moonlight `1.20-2.13.33`
  - reaches normal client startup, mod initialization, resource reload, atlas creation, and sound engine startup
  - the process was still running when the command timeout was hit, which is good enough to treat the baseline as launchable in this environment
  - log also shows expected dev-environment noise:
    - Mojang auth `401` during local dev startup
    - missing optional compat classes/resources from non-installed companion mods
    - legacy shader/sound warnings from included dev mods

Known baseline caveats before 26.1 work:

- `common` still emits warnings about missing Forge `Dist` annotations on some transitive classes during compile
- the build still uses the old Architectury Loom / Java 17-era multiloader setup and must be replaced for Minecraft `26.1`
- Moonlight is only restored for the `1.20.1` baseline; it is not assumed to be available for the final `26.1` target

## Recommended next steps

1. Fix or replace the unresolved Moonlight dependency so the current baseline can build.
2. Record the exact current baseline once it builds:
   - `./gradlew.bat :fabric:build`
   - optionally `./gradlew.bat :fabric:runClient`
3. Audit version-sensitive pieces for the `26.1` jump:
   - Loom / Gradle / Java toolchain
   - mappings setup
   - Fabric API and loader versions
   - networking payload APIs
   - menu / screen registration
   - map rendering APIs
   - recipe serializers
   - item model / item definition changes
4. Port the Fabric target first while keeping the `multiloader` branch open in `F:/mapatlases-neoforge-ref` for comparison.

## Working rule

When in doubt:

- prefer the `multiloader` reference implementation
- preserve behavior and visuals
- adapt only the parts forced by `26.1` API changes
