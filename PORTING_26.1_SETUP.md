# 26.1 Port Setup

## Active workspace

- Working repo: `F:/mapatlases-neoforge`
- Branch: `26.1`
- `origin`: `https://github.com/R2bEEaton/mapatlases-neoforge`
- `upstream`: `https://github.com/MehVahdJukaar/mapatlases-neoforge`

## Reference workspace

- Reference repo: `F:/mapatlases-neoforge-ref`
- Branch: `multiloader`

This reference checkout is the source of truth for behavior, assets, screen design, feature scope, and code structure during the Fabric 26.1 port.

## Porting priority

- Only the Fabric target matters for this port
- Preserve the existing `multiloader` branch's behavior and visuals as closely as possible
- Reuse original code and textures first, then adapt only the API seams required by Minecraft/Fabric `26.1`

## Current baseline

- The correct multiloader codebase is now cloned and ready
- A local `26.1` branch exists in the working repo
- Fabric code is split across `common/` and `fabric/`

## Current blocker

- The current 1.20.1 baseline does not build as-is in this environment because `net.mehvahdjukaar:moonlight:1.20-2.15.5` is not resolving from the configured repositories
- Before the 26.1 port work can proceed cleanly, the dependency setup needs to be updated or redirected to a working source
