# player-animator downgrade shim (compile+run substitute)

player-animator has **no Minecraft 26.1.2 release** (Modrinth tops out at 1.21.7), so EPCA's four
player-animation call sites are kept compiling by this source root.

* It is **not** part of `src/` - it is a separate source set so the real tree stays a faithful
  reflection of the mod.
* It re-declares the exact class/method surface EPCA uses, under the real package names
  (`dev.kosmx.playerAnim.**`), with **no-op** bodies.
* Behaviour loss per file is documented in `../PORT-STATUS.md` -> "player-animator 降级".

## Wire it into the Gradle build (phase 2)

```groovy
sourceSets.main.java.srcDir 'compat/playeranimator'
```

Do this **only** while no official 26.1.2 build exists. When one appears: delete this directory and
add the real jar as a dependency - no EPCA call site has to change.

## Census

The canonical census in `../javac-census-full.log` **excludes** this shim, so the 31
`dev.kosmx.playerAnim.*` errors are visible and honestly counted. `javac-census-with-shim.log`
includes it, proving the shim is signature-complete (that run should only lose those errors).
