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

## 26.1 toolchain conversion checkpoint

Recorded on `2026-03-30` after the baseline checkpoint.

What changed:

- Switched the project to a Fabric-only `26.1` toolchain:
  - Gradle wrapper: `9.4.1`
  - Fabric Loom: `1.15.5`
  - Fabric Loader: `0.18.5`
  - Fabric API: `0.144.4+26.1`
  - Java toolchain target: `25`
- Removed the old root Architectury / CurseGradle / Modrinth multiloader build logic from the active build
- Kept the on-disk `common/` + `fabric/` source layout, but changed the active build so `fabric` now compiles:
  - `fabric/src/main/java`
  - `fabric/src/main/resources`
  - `common/src/main/java`
  - `common/src/main/resources`
- Removed the active `:common` Gradle project from the `26.1` build graph and folded shared sources into the `fabric` module source sets
- Disabled the access widener path temporarily for the `26.1` build because Loom now expects the widener to be in the `official` namespace and the existing file is still `named`

Build result after toolchain conversion:

- `./gradlew.bat :fabric:build`
  - now configures and compiles against real Minecraft `26.1` sources
  - no longer fails in old dependency resolution / Forge / Architectury setup
  - currently fails during Java compilation, which is the expected next stage

Current `26.1` compile blocker categories:

1. Mojang-name / 26.1 API renames in vanilla classes
   - confirmed examples from compile output:
     - `net.minecraft.resources.ResourceLocation` no longer resolves
       - `26.1` uses `net.minecraft.resources.Identifier`
     - `GuiGraphics` no longer resolves
       - `26.1` exposes `GuiGraphicsExtractor`
     - `RenderType` moved under `net.minecraft.client.renderer.rendertype.RenderType`
     - `Material` moved under `net.minecraft.client.resources.model.sprite.Material`
     - `RecipeSerializer` is now a record-like value instead of the old interface shape
     - `InteractionResultHolder` no longer exists in the old form
2. Planned dependency removals that now need internal replacements
   - `Moonlight`
   - `Architectury @ExpectPlatform`
   - optional integrations whose old dependencies are not yet reintroduced for `26.1`
3. `26.1` resource/build migration follow-ups
   - access widener must be converted from `named` to `official` or replaced with another access strategy later

Important interpretation:

- The project is now past the build-system migration stage.
- Remaining failures are source porting work:
  - internal Moonlight replacement
  - Architectury replacement
  - 26.1 vanilla/Fabric API adaptation

## Internal compatibility shim checkpoint

Recorded on `2026-03-31`.

Work completed in this pass:

- Added local replacement stubs for the small subset of Architectury / Moonlight platform APIs that the mod imports directly:
  - `dev.architectury.injectables.annotations.ExpectPlatform`
  - `net.mehvahdjukaar.moonlight.api.platform.PlatHelper`
  - `net.mehvahdjukaar.moonlight.api.platform.ClientHelper`
  - `net.mehvahdjukaar.moonlight.api.platform.RegHelper`
  - `net.mehvahdjukaar.moonlight.api.platform.network.*`
  - `net.mehvahdjukaar.moonlight.api.platform.configs.*`
- Rewired `IMapCollection.get(...)` to the Fabric implementation directly instead of the old Architectury expectation path
- Replaced the old CCA-backed `IMapCollectionImpl` superclass dependency with a temporary in-memory implementation so the code can keep compiling while the real 26.1 persistence replacement is still pending
- Reduced active compile noise by removing optional integration entrypoints from `fabric.mod.json` and excluding currently non-essential integration source files from the active `26.1` compile

Current compile state after this pass:

- `./gradlew.bat :fabric:compileJava`
  - still fails
  - but the failure surface has shifted further away from missing external platform classes and more toward real `26.1` source migration

Dominant remaining blocker categories now:

1. Vanilla/Mojang class moves and renames
   - `ResourceLocation` -> `Identifier`
   - `GuiGraphics` -> `GuiGraphicsExtractor`
   - `RenderType` package move
   - `Material` package move
   - `Util` package move
   - `InteractionResultHolder` removal/replacement
2. Rendering and HUD API changes
   - old Fabric HUD callback usage
   - old client texture/material helper usage
3. Recipe serializer migration
   - old serializer interface-style code no longer matches 26.1
4. Still-unported optional/common integrations
   - old Moonlight map/marker implementation files
   - old Curios / ImmediatelyFast / other compat classes

Conclusion from this checkpoint:

- The remaining work is now mostly real code porting, not build setup.
- The next practical steps are:
  - migrate vanilla identifiers and GUI/render types
  - rewrite recipe serializers for 26.1
  - replace the old Moonlight marker layer with internal classes or temporary no-op shims while the atlas UI is brought forward

## Client isolation checkpoint

Recorded on `2026-03-31`.

Work completed in this pass:

- Replaced several optional integration classes with temporary local no-op shims so absent companion mods stop dominating the `26.1` compile:
  - `CuriosCompat`
  - `TrinketsCompat`
  - `ImmediatelyFastCompat`
  - `SupplementariesCompat`
  - `SupplementariesClientCompat`
  - `TwilightForestCompat`
  - `XaeroMinimapCompat`
- Replaced the old Moonlight-heavy marker/client classes with temporary internal placeholders to keep core code compiling while the real marker port is deferred:
  - `MoonlightCompat`
  - `ClientMarkers`
  - `ClientMarkersRenderer`
  - `EntityRadar`
  - `CustomDecorationButton`
- Updated the active Fabric source set to exclude the heaviest client-only screen/render sources and some client-only mixins so the build can focus on core `26.1` gameplay/data migrations first
- Started moving shared code toward `26.1` names in a few core places:
  - `MapAtlasesMod.res(...)` now returns `Identifier`
  - packet wrapper / marker packet identifiers updated to `Identifier`
  - `Util` imports updated to `net.minecraft.util.Util`
  - `PlatStuff` render helper signatures switched to `GuiGraphicsExtractor`
- Replaced the top-level client bootstrap classes with temporary compile-oriented shims:
  - `MapAtlasesClient`
  - `fabric/.../MapAtlasesClientImpl`

Compile result after this checkpoint:

- `./gradlew.bat :fabric:compileJava`
  - still fails
  - but the failure surface is now more clearly concentrated in actual `26.1` core migrations rather than missing optional dependencies

Dominant remaining blocker categories now:

1. Recipe system migration
   - old custom recipe constructors still expect `ResourceLocation`
   - `RecipeSerializer` is no longer an interface and the old serializer implementation shape must be rewritten
   - `SimpleCraftingRecipeSerializer` no longer exists in the old form
2. Core item / NBT / map API changes
   - `ItemStack` tag accessors changed
   - some NBT getters now return `Optional`
   - `Level` and `ResourceKey` accessors changed shape
   - map packet and map saved-data APIs changed
3. Remaining client-only stragglers still entering compile
   - `MapVertexConsumer`
   - `CompoundTooltip`
   - residual references to excluded screen classes
4. Networking / teleport / mixin drift
   - `C2STeleportPacket`
   - `MapItemSavedDataAccessor`
   - packet constructor / codec changes around map packets

Conclusion from this checkpoint:

- The port is now past the “missing dependency / missing compat library” phase.
- The next productive chunk is a focused `26.1` API rewrite for:
  - recipes
  - item/NBT access
  - map packet handling
  - the minimal remaining client glue needed for compile

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

## 26.1 baseline runtime checkpoint

Recorded on `2026-03-31`.

Work completed in this pass:

- Ported the remaining server-side and data-path compile blockers so the active `26.1` baseline now builds again:
  - packet buffer optional handling moved to explicit lambdas for `FriendlyByteBuf`
  - map id handling moved to `MapId` / `DataComponents.MAP_ID`
  - banner marker removal logic updated for modern `MapBanner` / `MapDecoration`
  - teleport packet updated to the modern server / level APIs
  - lectern and cartography mixins retargeted to modern vanilla methods and value IO APIs
  - `MapItemSavedDataMixin` retargeted from `Inventory.contains(ItemStack)` to `Inventory.contains(Predicate<ItemStack>)`
- Replaced the previous stubbed registry helper behavior with real registry writes in the local `RegHelper` shim
- Updated atlas item construction to set its `Item.Properties` id up front, which is required by modern item initialization
- Fixed Fabric metadata drift:
  - `fabric.mod.json` now depends on `fabricloader`, not the nonexistent `fabric` mod id
  - common mixin config was pruned to match the currently excluded client-only mixins

Verification results:

- `./gradlew.bat :fabric:compileJava --console=plain`
  - passes
- `./gradlew.bat :fabric:build --console=plain`
  - passes
- `./gradlew.bat :fabric:runClient --console=plain`
  - now launches successfully
  - reached title screen, created and entered a singleplayer world, and shut down cleanly

Current state after this checkpoint:

- The active Fabric `26.1` baseline is now buildable and launchable again.
- Remaining work is primarily feature restoration and parity work:
  - re-enable and port the excluded atlas UI / HUD / in-hand rendering classes
  - reintroduce the excluded client mixins one by one against `26.1`
  - replace the temporary Moonlight marker/render placeholders with behavior-matching internal implementations
  - restore creative-tab wiring and other non-critical registry-side polish that is still stubbed

## 26.1 recipe and datapack layout checkpoint

Recorded on `2026-03-31`.

Work completed in this pass:

- Updated atlas recipe datapack files to the `26.1` resource layout:
  - moved custom recipes from `data/map_atlases/recipes/` to `data/map_atlases/recipe/`
  - moved the sticky crafting tag from `data/map_atlases/tags/items/` to `data/map_atlases/tags/item/`
- Updated the sticky item tag contents for the Fabric-only target:
  - replaced the legacy optional `#forge:slimeballs` entry with optional `#c:slime_balls`
- Updated the optional Supplementaries antique atlas recipe condition to Fabric resource conditions:
  - replaced the old Forge `mod_loaded` condition with `fabric:load_conditions`
- Updated the custom atlas crafting recipe ingredient JSON to the actual `26.1` ingredient format:
  - string item ids instead of legacy object ingredients
  - `#map_atlases:sticky_crafting_items` for the tag ingredient
- Replaced the temporary `PlatStuff` assertion stubs with direct Fabric-only delegation so recipe codec decode and other platform calls work again at runtime
- Added an English translation for the sticky atlas item tag to avoid Fabric convention-tag warning noise in recipe viewers

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully after the datapack path and ingredient-format migration
- `./gradlew.bat :fabric:runClient --console=plain`
  - launches successfully
  - loads atlas recipes without parse errors
  - reaches title screen, creates and enters a singleplayer world, and shuts down cleanly

Important findings:

- Minecraft `26.1` recipe data is loaded from `data/<namespace>/recipe/`, not `data/<namespace>/recipes/`
- Minecraft `26.1` item tags are loaded from `data/<namespace>/tags/item/`, not `tags/items/`
- `26.1` ingredient JSON for recipes now uses string forms such as:
  - `"minecraft:book"`
  - `"#map_atlases:sticky_crafting_items"`
- The prior placeholder `PlatStuff` implementation was still reachable during custom recipe codec decode and had to be replaced with direct Fabric delegation before datapack loading would succeed
