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

## Atlas networking and item-data checkpoint

Recorded on `2026-03-31`.

This pass fixed the missing client atlas behavior at the data and transport layers.

What changed:

- Replaced the old no-op Moonlight-style networking shim with a real Fabric payload transport in:
  - `common/src/main/java/net/mehvahdjukaar/moonlight/api/platform/network/ChannelHandler.java`
- Wired common and client packet registration into atlas bootstrap:
  - `common/src/main/java/pepjebs/mapatlases/networking/MapAtlasesNetworking.java`
  - `common/src/main/java/pepjebs/mapatlases/client/MapAtlasesClient.java`
- Replaced the temporary in-memory atlas map collection storage with real item-backed persistence using `ItemStack` custom data:
  - `fabric/src/main/java/pepjebs/mapatlases/map_collection/fabric/IMapCollectionImpl.java`

Important findings:

- The missing HUD / atlas screen path was not only a UI/render problem.
- The old temporary `ChannelHandler` stub was still dropping all C2S and S2C atlas packets.
- The temporary `IMapCollectionImpl` was also not deserializing atlas map ids from the atlas item itself, so client-side atlas collections were often empty even when the atlas item existed.
- The first Fabric payload implementation initially disconnected the client on `clientbound/minecraft:custom_payload`; this was fixed by decoding directly from the original `RegistryFriendlyByteBuf` instead of copying into a plain buffer first.

Verification:

- `./gradlew.bat :fabric:build --console=plain`
  - passes
- `./gradlew.bat :fabric:runClient --console=plain`
  - passes
  - reaches title screen
  - creates and enters a singleplayer world
  - no longer disconnects on atlas custom payload decode during join

Current caveat after this checkpoint:

- The log still reports duplicate atlas map keys once atlas data begins syncing in-world. This is no longer disconnecting the client, but it likely indicates a remaining atlas collection/state-sync issue that should be cleaned up next while validating HUD and atlas overview behavior in-game.

## Atlas collection sync and internal pin checkpoint

Recorded on `2026-03-31`.

This pass improved atlas screen behavior beyond simply opening.

What changed:

- Atlas item map collections now refresh from the current `ItemStack` custom data instead of holding stale in-memory state forever:
  - `fabric/src/main/java/pepjebs/mapatlases/map_collection/fabric/IMapCollectionImpl.java`
- Atlas map collection inserts now treat repeated syncs as expected instead of logging false duplicate-key errors for already-known maps:
  - `common/src/main/java/pepjebs/mapatlases/map_collection/MapCollection.java`
- Internal pin support was partially restored without requiring the external Moonlight mod:
  - `common/src/main/java/pepjebs/mapatlases/integration/moonlight/MoonlightCompat.java`
  - `common/src/main/java/pepjebs/mapatlases/integration/moonlight/CustomDecorationButton.java`
  - `common/src/main/java/pepjebs/mapatlases/client/screen/AtlasOverviewScreen.java`
  - `common/src/main/java/pepjebs/mapatlases/client/screen/PinButton.java`
  - `common/src/main/java/pepjebs/mapatlases/client/screen/PinNameBox.java`
  - `common/src/main/java/pepjebs/mapatlases/lifecycle/MapAtlasesClientEvents.java`
  - `common/src/main/java/pepjebs/mapatlases/networking/C2SMarkerPacket.java`
  - `common/src/main/java/pepjebs/mapatlases/networking/C2SRemoveMarkerPacket.java`

Important findings:

- The atlas GUI feeling “minimal” was partly a state-sync issue, not just a missing render feature.
- The temporary item-backed atlas collection layer needed to re-read live stack data to stay aligned with atlas map updates.
- The previous duplicate map key spam is gone after de-duping repeated map syncs.
- The pin UI was still hard-gated on external Moonlight presence even though the Fabric-only port needs an internal replacement path.

Verification:

- `./gradlew.bat :fabric:compileJava --console=plain`
  - passes
- `./gradlew.bat :fabric:runClient --console=plain`
  - passes
  - reaches title screen
  - creates and enters a singleplayer world
  - no duplicate atlas map key spam observed in the latest launch log

Current caveat after this checkpoint:

- The internal pin path is now wired into the atlas UI flow, but it still needs deeper in-game parity validation and likely more work on marker preview / tracking / HUD rendering to fully match the multiloader reference.

## Not-synced atlas crash fix

Recorded on `2026-03-31`.

Crash reported by user:

- `./gradlew.bat :fabric:runClient`
  - client crashed in-world with:
    - `java.util.ConcurrentModificationException`
    - `pepjebs.mapatlases.map_collection.MapCollection.addNotSynced(MapCollection.java:46)`

Root cause:

- `MapCollection.addNotSynced(...)` used:
  - `notSyncedIds.removeIf(i -> add(i, level));`
- `add(...)` also mutates `notSyncedIds`, so the set was being modified while its own iterator was active.

Fix applied:

- `common/src/main/java/pepjebs/mapatlases/map_collection/MapCollection.java`
  - replaced the `removeIf(...)` logic with snapshot iteration over `List.copyOf(notSyncedIds)`
  - now removes ids only after a successful `add(...)` call returns

Verification:

- `./gradlew.bat :fabric:build --console=plain`
  - passes
- `./gradlew.bat :fabric:runClient --console=plain`
  - passes
  - reaches title screen
  - creates and enters a singleplayer world
  - no longer crashes with `ConcurrentModificationException` in `addNotSynced(...)`

## Delayed atlas open retry

Recorded on `2026-03-31`.

Regression reported by user:

- atlas no longer opened from right click after the recent atlas collection / internal pin changes

Likely cause:

- the atlas open packet can arrive on the client before the client has fully received and materialized the atlas map data
- `MapAtlasesClient.openScreen(...)` was bailing out if `maps.isEmpty()` at that exact moment, so the open request was lost permanently

Fix applied:

- `common/src/main/java/pepjebs/mapatlases/client/MapAtlasesClient.java`
  - added a small pending open-screen state
  - when an atlas has ids but no resolved client maps yet, the open request is deferred instead of discarded
  - the next `cachePlayerState(...)` tick fulfills that pending open once the client atlas map data is available
- `common/src/main/java/pepjebs/mapatlases/lifecycle/MapAtlasesClientEvents.java`
  - clear pending atlas-open state on logout

Verification:

- `./gradlew.bat :fabric:build --console=plain`
  - passes
- `./gradlew.bat :fabric:runClient --console=plain`
  - passes in a clean single run after the fix

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

## 26.1 custom recipe placement checkpoint

Recorded on `2026-03-31`.

Work completed in this pass:

- Fixed atlas custom recipes for the `26.1` crafting contract by overriding `placementInfo()` on:
  - `MapAtlasCreateRecipe`
  - `MapAtlasesAddRecipe`
  - `MapAtlasesCutExistingRecipe`
  - `AntiqueAtlasRecipe`
- Root cause:
  - `26.1` `CustomRecipe` now defaults `placementInfo()` to `PlacementInfo.NOT_PLACEABLE`
  - that allows the recipe to load but prevents the crafting menu from surfacing a craft result for otherwise valid custom recipes
- `MapAtlasCreateRecipe` now exposes placement info from its real configured ingredients
- the other atlas custom recipes now expose minimal placement info so the menu can consider them placeable again
- the antique atlas recipe still uses conservative placement metadata because Supplementaries is still stubbed on the current Fabric-only `26.1` branch

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully after the custom recipe placement fix

Important findings:

- On `26.1`, matching logic alone is no longer enough for `CustomRecipe`; the crafting UI also needs non-`NOT_PLACEABLE` `placementInfo()` metadata
- This was the reason the atlas recipe loaded but did not show in the crafting table after the datapack migration

## 26.1 recipe runtime follow-up checkpoint

Recorded on `2026-03-31`.

Work completed in this pass:

- Fixed the `MapAtlasesAddRecipe` and `MapAtlasesCutExistingRecipe` init-order regression caused by eager static placement metadata touching `MapAtlasesMod.MAP_ATLAS` during `MapAtlasesMod` class initialization
- Changed those recipe classes to build placement metadata lazily in `placementInfo()` instead of static field initialization
- Fixed a second `26.1` datapack/runtime issue in `MapAtlasCreateRecipe`:
  - `PlacementInfo.create(ingredients)` was probing the custom sticky-item tag during recipe decode
  - on `26.1`, that happened before the tag was bound, causing world/datapack load failure
- Replaced atlas-create placement metadata with a tag-free explicit preview ingredient set:
  - `slime_ball` / `honey_bottle`
  - `book`
- Kept the actual recipe matching logic unchanged so the recipe still uses the real sticky-item tag for behavior

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully
- `./gradlew.bat :fabric:runClient --console=plain`
  - launches successfully
  - loads recipes and datapacks successfully
  - reaches title screen, creates and enters a singleplayer world, and shuts down cleanly

Important findings:

- `placementInfo()` on `26.1` must avoid touching custom tag-backed ingredients during early recipe decode if those tags may not be bound yet
- For atlas create, placement metadata can be a safe preview subset while the real matching logic remains tag-driven

## Atlas create recipe data-component checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/MapAtlases`

Work completed in this pass:

- Compared the atlas create flow against `F:/MapAtlases` and confirmed the key behavioral difference:
  - the reference implementation does not depend on old filled-map custom NBT
  - it builds the crafted atlas from the map id data component directly
- Updated `MapAtlasCreateRecipe.assemble(...)` to stop rejecting valid filled maps that do not carry old custom data
- Reworked atlas creation so the crafted result stores the selected map id directly into the atlas item data using the old `maps` int-array compatibility path
- Updated `MapAtlasItem.onCraftedBy(...)` so atlas conversion runs before selected-slice validation
  - this lets crafted atlases hydrate their stored map ids into the live map collection before validation logic runs

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully
- `./gradlew.bat :fabric:runClient --console=plain`
  - launches successfully
  - loads recipes and datapacks successfully
  - reaches title screen, creates and enters a singleplayer world, and shuts down cleanly

Important findings:

- The prior atlas create path still assumed old-style map custom data and could return `ItemStack.EMPTY` for perfectly valid `26.1` filled maps
- The `F:/MapAtlases` reference confirms that the robust `26.1` atlas create path should key off `DataComponents.MAP_ID` instead

## Fabric helper parity checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for behavior expectations
- `F:/MapAtlases` only as a spot-check for atlas create history, not as the primary source of truth

Work completed in this pass:

- Backed out an incomplete `26.1` HUD/client render migration attempt that was not yet compatible with the new extracted GUI pipeline
- Restored the clean working baseline by re-excluding the unfinished HUD/widget classes from the active Fabric compile set
- Replaced the placeholder Fabric platform helper implementation in `fabric/src/main/java/pepjebs/mapatlases/fabric/PlatStuffImpl.java` with live behavior:
  - `drawString(...)` now renders through `GuiGraphicsExtractor`
  - `isSimple(...)` now forces the custom atlas-create recipe down the explicit ingredient-matching path instead of relying on an unfinished simple-recipe shortcut
  - `findMatches(...)` now performs real one-to-one ingredient matching for non-simple shapeless inputs

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully

Important findings:

- The `26.1` HUD port is a larger rendering rewrite because GUI HUD drawing now runs through `GuiGraphicsExtractor` and Fabric’s new HUD element registry, not the old `GuiGraphics` callback path
- The Fabric helper shim was still carrying placeholder returns from the earlier bootstrapping pass; restoring real ingredient matching removes a latent source of atlas recipe regressions while keeping the working baseline stable

## Fabric client bootstrap checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch

Work completed in this pass:

- Moved Fabric client-only startup out of the main mod initializer and into a real Fabric `client` entrypoint
- Updated `fabric.mod.json` to declare `pepjebs.mapatlases.fabric.MapAtlasesFabricClient` under `entrypoints.client`
- Simplified `MapAtlasesFabric` so the main initializer now only owns common/server wiring
- Updated `MapAtlasesFabricClient` to own:
  - client tick hooks
  - disconnect cleanup
  - atlas keybinding registration
- Registered atlas keybindings on `26.1` by extending `Minecraft.options.keyMappings` during client bootstrap and then calling `KeyMapping.resetMapping()`
- Used a reflective field write for `Options.keyMappings` because the old Fabric keybinding helper route available in cache was not usable in this deobfuscated `26.1` setup

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully
- `./gradlew.bat :fabric:runClient --console=plain`
  - launches successfully
  - loads resources and datapacks successfully
  - reaches title screen, creates and enters a singleplayer world, and shuts down cleanly

Important findings:

- For this `26.1` toolchain, a proper Fabric client entrypoint is the cleanest place to restore client-only registration instead of conditionally running client init from the main initializer
- The cached standalone Fabric keybinding helper artifact is built against intermediary names and is not directly usable from the current deobfuscated compile path, so local registration had to avoid that dependency

## Wrapped map packet sync checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for intended wrapped map update behavior

Work completed in this pass:

- Restored the wrapped map packet payload path in `common/src/main/java/pepjebs/mapatlases/networking/S2CMapPacketWrapper.java`
  - the wrapper now serializes and deserializes the full `ClientboundMapItemDataPacket` instead of only carrying atlas-side metadata
  - `26.1` packet encode/decode now goes through `ClientboundMapItemDataPacket.STREAM_CODEC`
  - added a strict `RegistryFriendlyByteBuf` guard because the packet codec requires registry-aware buffers
- Replaced the placeholder no-op map wrapper handler in `common/src/main/java/pepjebs/mapatlases/client/MapAtlasesClient.java`
  - the client now forwards the wrapped packet into the vanilla client connection handler
  - atlas-side map metadata is then patched back onto the client map saved data using the existing accessor mixin
- Restored the texture identifier constants in `MapAtlasesClient` that the later `26.1` client UI/HUD restore will need
- Replaced the temporary `debugIsMapUpdated(...)` stub with the fading packed-light behavior used to highlight recently updated maps

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully

Important findings:

- The prior Fabric-only bootstrap path could build and launch, but it was not preserving the actual wrapped map update payloads that drive client-side map refresh behavior
- The next atlas parity steps can now build on top of a real map data update path instead of a metadata-only placeholder

## Atlas UI prep checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for intended atlas UI and map-decoration behavior
- local `26.1` deobfuscated client jars to confirm the current screen, widget, and map rendering APIs

Work completed in this pass:

- Re-opened the excluded atlas UI compile path briefly to capture the real `26.1` failure set, then restored the exclusions to keep the Fabric baseline green
- Added missing `26.1` atlas render texture identifiers to `common/src/main/java/pepjebs/mapatlases/client/MapAtlasesClient.java`
  - `map_border`
  - `map_hovered`
- Added common helpers in `MapAtlasesClient` for mutable map-decoration access and dirtying:
  - `getMutableDecorations(MapItemSavedData)`
  - `markDecorationsDirty(MapItemSavedData)`
- Extended `common/src/main/java/pepjebs/mapatlases/mixin/MapItemSavedDataAccessor.java` with:
  - `@Accessor("decorations")`
  - `@Invoker("setDecorationsDirty")`

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully

Important findings:

- The atlas UI/HUD port is blocked less by widget polish and more by core `26.1` API shifts:
  - `Screen` and `AbstractWidget` now render through `extractRenderState(GuiGraphicsExtractor, ...)`
  - `MapRenderer` now uses `MapRenderState` extraction instead of the old direct render path
  - `MapItemSavedData.decorations` is private and must be reached through accessors instead of direct field mutation
- With the new accessor and helper layer in place, the next UI port steps can target the actual `26.1` APIs instead of carrying old direct-field assumptions forward

## Atlas widget migration checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for atlas screen behavior and layout
- local `26.1` deobfuscated client jars for the new widget/input API signatures

Work completed in this pass:

- Ported the staged atlas bookmark/button classes from the old `GuiGraphics` / integer-click API to the `26.1` widget surface:
  - `BookmarkButton`
  - `SliceBookmarkButton`
  - `SliceArrowButton`
  - `DimensionBookmarkButton`
  - `CartographyTableAtlasButton`
- Updated those classes to render through `GuiGraphicsExtractor` and `RenderPipelines.GUI_TEXTURED`
- Updated click handling to the `MouseButtonEvent` / `MouseButtonInfo` signatures used by `AbstractWidget` in `26.1`
- Removed the remaining Moonlight UI-only helper usage from `CartographyTableAtlasButton`
  - replaced `LangBuilder.getReadableName(...)` with the mod’s own atlas screen name helper path

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully

Important findings:

- The atlas UI package can be migrated incrementally while still excluded from the active Fabric source set, which lets the branch stay buildable between `26.1` UI port slices
- The button layer is a good fit for direct `26.1` adaptation because its logic is still close to the reference branch; the more expensive work remains the map widget, HUD, and full overview screen render path

## Atlas screen helper migration checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for atlas screen control flow and helper behavior
- local `26.1` deobfuscated client jars for `KeyEvent` and resource identifier API changes

Work completed in this pass:

- Updated the small atlas screen control buttons to the `26.1` click signature:
  - `PinButton`
  - `ShearButton`
- Began migrating `AtlasOverviewScreen` off the old pre-`26.1` helper signatures:
  - switched atlas background texture field from `ResourceLocation` to `Identifier`
  - updated dimension sorting to use `ResourceKey.identifier()`
  - updated screen keyboard handling from integer key parameters to `KeyEvent`
  - updated keybind matching to use `KeyMapping.matches(KeyEvent)`
  - updated the local readable-name helper to accept `Identifier`

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully

Important findings:

- The next atlas overview screen work is now concentrated in the render path and decoration/map-widget logic, not the basic input or identifier plumbing
- Keeping these helper migrations committed in small slices is reducing the eventual `26.1` compile burst when the atlas screen package is re-included

## Decoration bookmark migration checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for atlas marker/bookmark behavior
- local `26.1` deobfuscated client jars for decoration record access and widget/input signatures

Work completed in this pass:

- Ported `common/src/main/java/pepjebs/mapatlases/client/screen/DecorationBookmarkButton.java` further toward the `26.1` client API:
  - updated bookmark key handling from integer key parameters to `KeyEvent`
  - updated click handling to `MouseButtonEvent`
  - migrated widget rendering to `GuiGraphicsExtractor`
  - migrated the bookmark overlay blits to `RenderPipelines.GUI_TEXTURED`
- Reworked the vanilla decoration branch to use `26.1` `MapDecoration` record accessors:
  - `type()`
  - `x()`
  - `y()`
  - `name()`
  - `getSpriteLocation()`
- Removed direct field mutation of `MapItemSavedData.decorations` in favor of the shared accessor helpers added earlier:
  - `MapAtlasesClient.getMutableDecorations(...)`
  - `MapAtlasesClient.markDecorationsDirty(...)`

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully

Important findings:

- Atlas marker/bookmark behavior is now much closer to the real `26.1` data model because it no longer assumes the old mutable decoration field or pre-record decoration API
- The remaining atlas UI blockers are increasingly concentrated in the render-heavy classes, especially `PinNameBox`, `MapWidget`, `AbstractAtlasWidget`, `AtlasOverviewScreen`, and `MapAtlasesHUD`

## Pin name box migration checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for atlas pin naming flow
- local `26.1` deobfuscated client jars and bundled JOML classes for the new widget/input/render-state API surface

Work completed in this pass:

- Ported `common/src/main/java/pepjebs/mapatlases/client/screen/PinNameBox.java` to the `26.1` widget API:
  - updated rendering from `GuiGraphics` to `GuiGraphicsExtractor`
  - updated keyboard handling from integer key params to `KeyEvent`
  - updated click handling to `MouseButtonEvent`
  - updated wheel handling to the new `mouseScrolled(double, double, double, double)` signature
- Preserved the marker index scrolling/animation bookkeeping and the moonlight marker-preview call flow, but adapted it to the extracted GUI render path

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully

Important findings:

- The atlas UI port is now down to the classes that actually draw and animate the atlas map itself
- The next high-value work item is the map rendering core in `MapWidget` and `AbstractAtlasWidget`, because that is the biggest remaining technical blocker before the atlas overview screen can be re-included

## Atlas item asset migration checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- local `26.1` item asset format under `assets/*/items/*.json`
- `F:/MapAtlases` only as a structural reference for the new `items/atlas.json` placement and `minecraft:context_dimension` selector shape

Work completed in this pass:

- Added the missing `26.1` item asset entrypoint at `common/src/main/resources/assets/map_atlases/items/atlas.json`
- Wired the atlas item to the new `minecraft:select` / `minecraft:context_dimension` model format so the existing atlas item models are reachable again at runtime
  - overworld -> `map_atlases:item/atlas_overworld`
  - nether -> `map_atlases:item/atlas_nether`
  - end -> `map_atlases:item/atlas_end`
  - fallback -> `map_atlases:item/atlas_generic`

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully

Important findings:

- The missing atlas item texture was not a code registration issue; it was a `26.1` asset layout problem because the repo only had the old `models/item/*.json` path and not the required `items/atlas.json` indirection
- This restores the atlas item’s basic visible model path, but locked-state and extended-dimension item variant parity still need a follow-up pass if we want the full old selector behavior back under the `26.1` item model system

## Minimap HUD restoration checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for minimap layout, behavior, text placement, anchoring, and sound flow
- local `26.1` Fabric rendering API (`fabric-rendering-v1`) for HUD element registration and extracted GUI rendering

Work completed in this pass:

- Re-enabled the atlas render core in the Fabric source set:
  - `common/src/main/java/pepjebs/mapatlases/client/AbstractAtlasWidget.java`
  - `common/src/main/java/pepjebs/mapatlases/client/ui/MapAtlasesHUD.java`
- Replaced the old `GuiGraphics` / `PoseStack` HUD path in `MapAtlasesHUD` with the real `26.1` HUD API:
  - `HudElement`
  - `GuiGraphicsExtractor`
  - `Matrix3x2fStack`
  - `RenderPipelines.GUI_TEXTURED`
- Restored actual minimap HUD registration in `fabric/src/main/java/pepjebs/mapatlases/client/fabric/MapAtlasesClientImpl.java` through `HudElementRegistry.attachElementAfter(...)`
- Restored minimap zoom key behavior by wiring `MapAtlasesClient.decreaseHoodZoom()` / `increaseHoodZoom()` back to the Fabric HUD instance
- Preserved the original minimap layout logic:
  - anchoring and offsets
  - background texture usage
  - player arrow/icon handling
  - cardinal letters
  - coords / chunk coords / biome text
  - follow-player / rotate-player behavior
  - atlas page-turn sound on active-map change

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully
- `./gradlew.bat :fabric:runClient --console=plain`
  - passes successfully
  - reaches title screen
  - creates and enters a singleplayer world
  - shuts down cleanly

Important findings:

- The minimap HUD path is now compatible with the `26.1` extracted rendering model and no longer depends on the old pre-`26.1` Fabric HUD callback style
- The right-click atlas overview is still blocked by the screen stack, not by HUD registration or map rendering itself
- I performed a compile probe with `client/screen/**` temporarily re-included to measure the remaining overview-screen blockers
  - the first failures are concentrated in `AtlasOverviewScreen.java` and `MapWidget.java`
  - the main remaining issues are:
    - old `GuiGraphics`-based render methods
    - old blend / `GlStateManager` usage
    - dependencies on still-excluded helper classes such as `CompoundTooltip`
    - the remaining `26.1` screen extraction and input-signature migration
- The screen package was re-excluded after the probe so the branch stays green while the overview screen is ported intentionally instead of half-enabled

## Fabric keybind registration checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for Fabric client init behavior
- local `fabric-key-mapping-api-v1` `2.0.4+e2bdee7847`

Work completed in this pass:

- Replaced the temporary reflection-based keybinding registration path in `fabric/src/main/java/pepjebs/mapatlases/fabric/MapAtlasesFabricClient.java` with the real Fabric API helper:
  - `KeyMappingHelper.registerKeyMapping(...)`
- Kept the existing atlas keybindings unchanged:
  - open atlas
  - place pin
  - minimap zoom in / out
  - slice up / down

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully
- `./gradlew.bat :fabric:runClient --console=plain`
  - passes successfully
  - reaches title screen
  - creates and enters a singleplayer world
  - shuts down cleanly

Important findings:

- The old reflection hack was not faithful to Fabric `26.1` and is the most likely reason the keybindings were not appearing in Controls
- The right-click atlas overview and full world-map GUI are still blocked by the excluded `client/screen/**` port, not by the keybinding registration path

## Atlas overview reintegration checkpoint

Recorded on `2026-03-31`.

Reference used in this pass:

- `F:/mapatlases-neoforge-ref` `multiloader` branch for atlas screen opening flow and screen construction rules
- local Fabric Loom `26.1` transformed client API metadata for `KeyEvent`, `MouseButtonEvent`, `MouseButtonInfo`, `AbstractWidget`, `EditBox`, and `Screen`

Work completed in this pass:

- Re-enabled the atlas overview client sources in the active Fabric build by removing the temporary source-set exclusions for:
  - `common/src/main/java/pepjebs/mapatlases/client/CompoundTooltip.java`
  - `common/src/main/java/pepjebs/mapatlases/client/screen/**`
  - `common/src/main/java/pepjebs/mapatlases/integration/moonlight/CustomDecorationButton.java`
- Restored the atlas screen stack to the real `26.1` transformed input API:
  - `AtlasOverviewScreen`
  - `MapWidget`
  - `PinNameBox`
  - `DecorationBookmarkButton`
  - `SliceBookmarkButton`
  - `SliceArrowButton`
  - `DimensionBookmarkButton`
  - `PinButton`
  - `ShearButton`
- Replaced old `Screen.hasShiftDown()` / `hasControlDown()` / `hasAltDown()` usage with explicit modifier checks that work in the current `26.1` client surface
- Fixed the temporary `CompoundTooltip` callsite drift in `DecorationBookmarkButton`
- Fixed `CartographyTableAtlasButton` to stop reaching `AbstractContainerScreen.leftPos` / `topPos` directly from outside the screen class
- Implemented the real atlas screen open path in `MapAtlasesClient.openScreen(...)`
  - atlas from player inventory config when no lectern position is supplied
  - atlas from lectern book when a lectern position is supplied
  - `maps.addNotSynced(level)` before screen open, matching the reference branch behavior
  - `Minecraft#setScreen(new AtlasOverviewScreen(...))` only when the atlas has maps to show

Verification results:

- `./gradlew.bat :fabric:compileJava --console=plain`
  - passes successfully with the atlas screen package included in the active Fabric source set
- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully
- `./gradlew.bat :fabric:runClient --console=plain`
  - passes successfully, reaches title screen, creates and enters a world, and shuts down cleanly

Important findings:

- The earlier event-style `KeyEvent` / `MouseButtonEvent` direction was correct; the failed old-style signature backtrack was caused by checking raw jar APIs instead of the transformed compile surface Loom is actually exposing in this toolchain
- The right-click atlas GUI path is no longer blocked by source-set exclusions or a stubbed `openScreen(...)`; the next parity work can move on to the remaining excluded client mixin/render layers instead of the overview screen bootstrap

## Atlas GUI persistence checkpoint

Recorded on `2026-03-31`.

Work completed in this pass:

- Fixed a client atlas-state regression in `common/src/main/java/pepjebs/mapatlases/map_collection/MapCollection.java`
  - atlas map id serialization is now deterministic via sorted ids
  - `serializeNBT()` now uses the same deterministic `getAllIds()` path
- Fixed the Fabric item-backed atlas collection sync in `fabric/src/main/java/pepjebs/mapatlases/map_collection/fabric/IMapCollectionImpl.java`
  - `addNotSynced(level)` now persists newly materialized map ids back onto the atlas `ItemStack`
  - this prevents late map syncs from being lost on the next `matchesStackData()` refresh

Verification results:

- `./gradlew.bat :fabric:build --console=plain`
  - passes successfully

Important findings:

- The atlas GUI reopen regression was likely caused by the client atlas collection rebuilding itself from stale stack data every tick
- Successful `addNotSynced(...)` resolutions must write back to the stack on Fabric 26.1, otherwise the client can immediately discard the synced map ids and treat the atlas as empty again
