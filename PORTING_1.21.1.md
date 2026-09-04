# Sword Art Online UI: Reborn — Fabric 1.21.1 Porting Tracker

Status legend: `[ ]` not started, `[~]` in progress/foundation only, `[x]` ported for the current phase, `[!]` blocked.

## Scope and reference order

This tracker covers the Fabric-only port to Minecraft 1.21.1, Java 21, Kotlin, Mojang Official Mappings, and Gradle Kotlin DSL.

Reference priority:

1. `origin/2.0-1.19.4-port` at `fda4693` — primary architecture source.
2. `origin/1.16.5` at `9a97a13` — mature implementation fallback.
3. `origin/2.0-1.12-features` at `7e1a8b0` — historical feature inventory.

No reference branch is checked out or modified. The 1.21.1 work lives on `fabric-1.21.1`.

## Phase-one foundation

| Status | System | Reference source | 1.21.1 target | MC API rewrite | External dependency | Fabric/client scope | Notes / blockers |
|---|---|---|---|---|---|---|---|
| [x] | Project identity/build | 1.19.4 root + `fabric/` Gradle files | Root Gradle files, `fabric.mod.json` | Yes | Current Loom/Fabric toolchain only | Fabric | Single-module Fabric project; Mojang mappings retained; Java toolchain 21. |
| [x] | License/credits | `LICENSE.md`, source headers | `LICENSE`, README, metadata | No | None | Common | GPL-3.0-or-later and original authors retained. |
| [x] | Template cleanup | Fabric template | Whole workspace | No | None | Both source sets | Example mixins, placeholder entrypoints, IDs, text, URLs, and asset namespace removed. |
| [x] | Constants | `common/.../mcui/Constants.kt` | `src/main/kotlin/be/bluexin/mcui/Constants.kt` | No | SLF4J supplied by Minecraft | Main | Keeps `mcui` and legacy `saoui` namespaces. |
| [x] | Logger helpers | `common/.../util/LoggerHelper.kt` | `src/main/kotlin/be/bluexin/mcui/util/LoggerHelper.kt` | No | SLF4J supplied by Minecraft | Main | Lightweight helpers only. |
| [x] | MCUI core lifecycle | `common/.../MCUICore.kt` | `src/main/kotlin/be/bluexin/mcui/MCUICore.kt` | Yes | Fabric Loader | Main-safe | Koin/theme/config startup is deferred; current init is idempotent and prepares real config storage. |
| [x] | Fabric entrypoints | `fabric/.../MCUIFabricCore.kt` | `src/main/.../fabric/MCUIFabric.kt`, `src/client/.../MCUIFabricClient.kt` | Yes | Fabric Loader/API | Split main/client | No Architectury or Forge platform service layer. |
| [x] | Resource ID helpers | `common/.../util/ResourceLocation.kt` | `src/main/.../util/ResourceIds.kt` | Yes | Minecraft | Main | Uses 1.21.1 `ResourceLocation.fromNamespaceAndPath`. Serialization adapter deferred. |
| [~] | Config | `common/.../config/**`, Fabric config helper | `src/main/.../config/ConfigPaths.kt` | Yes | Fabric Loader | Main-safe, client mod | Stable `config/mcui` root is implemented. Typed settings/persistence await dependency and schema decisions. |
| [x] | Resource loading/reload | Fabric `MCUIFabricCore` reload listener, `themes/loader/**` | `src/client/.../resources/ClientResourceReloads.kt` | Yes | Fabric API, Kotlin Serialization JSON | Client-only | Phase three discovers, parses, validates, compiles, and atomically publishes resource-pack themes on every client resource reload. |
| [x] | Pure Kotlin utilities (selected) | `common/.../util/LayeredMap.kt` | `src/main/.../util/LayeredMap.kt` | No | None | Main | Ported with corrected shadowing and `containsValue`; covered by unit test. |
| [x] | Tests (foundation) | 1.19.4 `common/src/test/**` patterns | `src/test/kotlin/**` | No | Kotlin test/JUnit platform | Build only | Current test covers the migrated layered map. Legacy serde/script tests await their systems. |

## Full source audit

| Status | Major system | Primary 1.19.4 path | Mature/history fallback | Planned 1.21.1 path | MC rewrite? | Main dependencies | Scope | Known blocker / next decision |
|---|---|---|---|---|---|---|---|---|
| [~] | MCUI Core | `common/.../MCUICore.kt` | `1.16.5 src/main/java/com/tencao/saoui/**` | `src/main/kotlin/be/bluexin/mcui/**` | Yes | Previously Koin/KSP plus all theme modules | Main + client bootstrap | Foundation complete; full module graph waits for the loaders it initializes. |
| [x] | Constants/logger | `common/.../Constants.kt`, `util/LoggerHelper.kt` | Same concepts in older core | `src/main/kotlin/be/bluexin/mcui/**` | Minor | SLF4J already provided | Main | Phase-one scope complete. |
| [~] | Config/settings | `common/.../config/**`; Fabric platform helper | `1.16.5 .../config/**` | `src/main/.../config/**` | Yes | Serialization, coroutines; old Forge Config Port/NightConfig | Prefer main-safe storage, client use | Decide native Fabric-friendly persistence and migration format before porting `Setting`/`Settings`. |
| [x] | Theme metadata/manager | `common/.../themes/meta/**` | `1.16.5 .../themes/**`; 1.12 theme packages | Pure DTOs in `src/main/.../themes/**`; compiler/manager/resource adapter in `src/client/.../themes/**` | Yes | Kotlin Serialization JSON | Split main/client | Minimal metadata, discovery, JSON parsing, validation, resolved compilation, fallback, selection, and atomic snapshots are complete without Koin. |
| [~] | Elements (new) | `common/.../themes/elements/{Fragment,Group,Hud,...}.kt` | Compare legacy implementations | `src/client/.../render/element/**` | Yes at rendering boundary | Minecraft types only; JOML supplied by Minecraft | Client for phase-two resolved models | Minimal resolved `Group`, `Rectangle`, `Text`, `Texture`, and `Item` elements are complete. Theme serialization, script-backed values, and the full element catalog remain deferred. |
| [ ] | Legacy Elements | `common/.../themes/elements/legacy/**` | 1.16.5 and 1.12 element packages | `src/main/.../themes/elements/legacy/**` | Yes | XML, MiniScript, Lua, renderer | Split/client-heavy | KSP-generated factories and extensive old rendering calls. Do not delete absent/incomplete features. |
| [ ] | Screen | `common/.../screens/**`, `deprecated/screens/**` | `1.16.5 .../screens/**`; 1.12 GUI packages | `src/client/.../screens/**` | Yes, substantial | Theme, elements, Lua, config | Client-only | 1.21.1 Screen/GuiGraphics/input signatures must be redesigned around compatibility adapters. |
| [x] | HUD | new `themes/elements/Hud.kt`, deprecated `IngameGUI.kt` | Mature 1.16.5 HUD; 1.12 historic HUD | `src/client/.../fabric/client/hud/**` | Yes, substantial | Fabric rendering API | Client-only | Phase-four part composition, per-frame data snapshot, typed bindings, native hotbar, visibility, and selective vanilla replacement are implemented; manual validation remains. |
| [x] | Rendering abstraction | `themes/elements/renderer/**` | Mature GL calls in 1.16.5/1.12 | `src/client/.../render/**` | Yes, central | `GuiGraphics`; JOML/LWJGL supplied by Minecraft | Client-only | Phase-two operations, 1.21.1 adapter, transform/scissor safety, Visitor, color contract, and minimal elements are implemented. |
| [~] | GLCore | `common/.../GLCore.kt` | Mature `1.16.5 .../GLCore.kt`; 1.12 GL helpers | Optional future `src/client/.../render/compat/**` | Yes, complete rewrite | New render operations | Client-only | The compatibility strategy is defined; no `LegacyGlCompat` was added because no migrated legacy caller needs it yet. New code must never target a GLCore monolith. |
| [ ] | MiniScript | `common/.../themes/miniscript/**` | 1.16.5 `themes/util/**` | `src/main/.../themes/miniscript/**` and client context adapters | Yes for game context | JEL, Serialization, Lua mapping | Split | gnu-jel Java 21 compatibility and generated bindings must be proven before inclusion. |
| [ ] | Lua | `common/.../themes/scripting/**` | 1.16.5/1.12 scripting implementations | `src/main/.../themes/scripting/**` | Limited MC; major runtime work | LuaJ, BCEL, LuaJ-KSP/KSP, optional JNLua | Split | Security sandbox, Java 21 bytecode/runtime compatibility, and generator publishing are unresolved. |
| [ ] | CSS | Style parsing/usage under theme loader/renderer; theme `style.css` assets | 1.16.5/1.12 theme code/assets | `src/main/.../themes/style/**` | Indirect | ph-css, ph-commons | Mostly main | Defer until the element style contract is stable. |
| [~] | XML / serialization | `themes/serde/**`, `themes/loader/{Xml,Json}ThemeLoader.kt` | Mature 1.16.5 `themes/util/xml/**`; 1.12 JAXB/theme models | `src/main/.../themes/**` | ResourceLocation parsing moved to compiler | Kotlin Serialization JSON 1.11.0 | Main with client resource adapter | Minimal resolved JSON is complete. XML/xmlutil and legacy Gson polymorphic element JSON remain deferred. |
| [ ] | Commands | `common/.../commands/**` | Older debug/config commands | `src/client/.../commands/**` or safe main registration | Yes | Fabric command API, theme/config | Client mod | Command source/registration context and client-vs-server semantics need review. |
| [x] | Resource loading | `themes/loader/**`, Fabric reload listener | Older resource/theme scanners | Split loader models + client Fabric adapter | Yes | Fabric API, Kotlin Serialization JSON | Client adapter | Uses `ResourceManager.listResources/getResource`; no filesystem/JAR scanning. Same-location player-pack overrides follow Minecraft's selected pack stack. |
| [~] | Mixins | `common/.../mixin/ModConfigMixin.java`, Fabric mixin JSON | 1.16.5/1.12 mixins/ATs | `src/client/java/be/bluexin/mcui/mixin/client/GuiHudSuppressionMixin.java` | Yes | Mixin | Client-only | One precise GUI mixin gates only the vanilla sub-elements that Fabric 1.21.1 combines into `HOTBAR_AND_BARS`; it never cancels `Gui.render`. |
| [x] | Fabric platform code (foundation) | `fabric/src/main/**` | None | `src/main/.../fabric`, `src/client/.../fabric/client` | Yes | Fabric Loader/API | Fabric-only | Phase-one entrypoints/reload boundary done. Further callbacks arrive with their systems. |
| [ ] | Forge platform code | `forge/src/main/**` | 1.16.5 is Forge | None in this phase | N/A | Forge/KotlinForForge | Excluded | Explicitly out of scope; retained only as behavioral reference. |
| [ ] | Social/party integrations | `social/**`, deprecated party/friend elements | 1.16.5/1.12 SAOMCLib integrations | Undecided | Yes | Former FTB Library/Teams or replacement API | Client/integration | Optional integration contract must be isolated; no hard dependency in phase one. |
| [~] | Assets/theme packs | `common/src/main/resources/assets/{mcui,saoui}/**` | 1.16.5/1.12 full asset history | `src/main/resources/assets/{mcui,saoui}/**` | Resource metadata may need updates | None/runtime loaders | Client resources | Original logo plus explicitly named `mcui:development_test` theme only. Historical SAO assets remain deferred until the loader contract stabilizes. |
| [ ] | KSP generated code | Koin modules, LuaJ exposure/factories/typings | Not used in older branches the same way | `build/generated/ksp/**` if retained | No | KSP + processors | Build-time | Do not add until a ported subsystem needs it and processor/Kotlin versions are verified. |
| [ ] | Full legacy tests | `common/src/test/**` | Ad hoc older validation | `src/test/**` | Some | Serialization, XML, Lua, Koin/MockK | Build-time | Re-enable per subsystem rather than importing a non-compiling test suite. |

## Dependency audit

### Included in phase one

| Dependency | Version source | Needed now | Evidence / role | Java 21/current Kotlin | Packaging decision |
|---|---|---|---|---|---|
| Minecraft | `1.21.1` from template | Yes | Mojang types and client runtime | Verified by Gradle/toolchain build | Loom-managed. |
| Mojang Official Mappings | Loom | Yes | Required mapping namespace | Verified by compilation | Keep; do not add Yarn. |
| Fabric Loader | `0.19.5` from template | Yes | Entrypoints, environment, metadata/config path | Verified by build/client launch | `modImplementation`. |
| Fabric API | `0.116.17+1.21.1` from template | Yes | Client resource reload registration and later lifecycle hooks | Verified by build/client launch | `modImplementation`. |
| Fabric Language Kotlin | `1.13.13+kotlin.2.4.10` from template | Yes | Kotlin entrypoint/runtime | Verified by build/client launch | `modImplementation`. |
| Kotlin test | Kotlin plugin version | Tests only | Foundation unit tests | Verified by `test` | Test scope only. |
| Kotlin Serialization JSON | `1.11.0` | Yes | Theme metadata/HUD DTO decoding and ARGB serializer | Kotlin's 2.4.10 documentation recommends runtime 1.11.0; dependency gate and final build verified | `implementation`; only JSON is explicitly added. Compiler plugin is `org.jetbrains.kotlin.plugin.serialization` 2.4.10. |
| SLF4J API | Supplied transitively by Minecraft/Fabric runtime | Yes, no explicit artifact | Existing MCUI logging API | Verified by compilation/runtime log | Do not duplicate or shade. |

### Deferred or removed from the active build

| Legacy dependency | 1.19.4 reference version | Needed in phase one? | Current call sites | Compatibility / acquisition assessment | Decision |
|---|---:|---|---|---|---|
| kotlinx-coroutines | `1.8.1` | No | Debounced config saves | No active call sites; Java 21 is not the first blocker | Do not add yet; reconsider with Settings. |
| xmlutil | `0.86.2` | No | Legacy XML theme deserialization | Old build was pinned for an upstream issue; current Kotlin compatibility must be revalidated | Defer to serde phase. |
| gnu-jel | `2.1.3` custom fork/submodule | No | MiniScript expression compiler | Not on a normal public coordinate in the old build; Java 21 behavior unproven | Defer; no submodule initialized. |
| ph-css / ph-commons | `6.5.0` / `10.1.6` | No | CSS parser | No active style call sites in phase one | Defer to CSS/style phase. |
| LuaJ / BCEL | Git revision / `6.7.0` | No | Lua runtime and bytecode support | Git/JitPack coordinate and Java 21 sandbox behavior require dedicated verification | Defer to Lua phase. |
| LuaJ-KSP / KSP | `0.1-alpha-10` / `2.0.20-1.0.25` | No | Generated Lua bindings/typings | Processor is sourced from submodule/private-style repositories in old build; no active annotations | Defer; no submodule. |
| Koin + annotations/processor | `3.5.6` / `1.3.1` | No | Module wiring for theme/loader/script services | Current foundation has no DI graph; old generated module API couples all deferred systems | Remove for now; prefer explicit construction until DI is justified. |
| FTB Library / Teams | `1902.x` and commented out | No | Optional party/team integration | Not active even in the 1.19.4 build | Keep optional/out of build. |
| Forge Config API Port | Git/Modrinth revision `2TybfFU8` | No | Old Fabric implementation of Forge config abstractions | Ties common code to Forge config classes and a mixin | Replace with a Fabric-native decision later. |
| NightConfig | `3.6.3` | No | Transitive/config backend | No active phase-one call sites | Do not add. |
| Architectury | `8.2.89` + plugin | No | Fabric/Forge common-module abstraction | Fabric-only scope makes it unnecessary complexity | Removed from architecture. |
| Explicit SLF4J `1.7.36` | `1.7.36` | No | Previously shaded | Runtime already supplies logging API/binding | Do not add/shade. |
| JSR-305 | `3.0.1` | No | Old annotations | No active call sites | Do not add. |
| MockK/Koin test | `1.13.16` / old Koin | No | Deferred subsystem tests | No active tests need them | Do not add. |

## Git submodules

The reference `.gitmodules` uses SSH URLs for `dep-submodules/luaj-ksp` and `dep-submodules/gnu-jel`. Neither is initialized or copied into the 1.21.1 branch. Phase one has no call sites for either dependency. If a later phase proves a submodule is still necessary, its URL must be HTTPS; a normal public Maven artifact is preferred when available and verifiably equivalent.

## Verification checklist

- [x] Target branch is `fabric-1.21.1`; reference branches are remote read-only refs.
- [x] Template identifiers/content removed by global search.
- [x] Main/client source-set boundary established.
- [x] Java and Kotlin compile target/toolchain set to 21.
- [x] Mojang Official Mappings retained.
- [x] `./gradlew clean build` succeeds after foundation changes.
- [x] `./gradlew runClient` reaches the Minecraft main menu (Java 21 process window: `Minecraft* 1.21.1`; stopped manually after verification).
- [x] Initialization and resource reload messages confirmed in `run/logs/latest.log`.

## Phase-two rendering foundation

The rendering data flow is now:

`resolved Element tree -> RenderingElementVisitor -> GuiRenderOperations -> MinecraftGuiRenderOperations -> GuiGraphics`

MiniScript-backed values are deliberately absent from this layer. Future theme/script code will evaluate `CDouble`, `CBoolean`, or equivalent expressions into ordinary `ResolvedTransform`, `ResolvedRenderState`, numbers, booleans, colors, and resource IDs before rendering. This isolates renderer correctness and Java/Minecraft API migration from the script runtime; it is not a permanent removal of MiniScript capability.

| Status | Phase-two system | Implementation and actual 1.21.1 API |
|---|---|---|
| [x] | Rendering abstraction | `GuiRenderOperations` is the element-facing vocabulary. No element receives `GuiGraphics` or calls Minecraft rendering directly. |
| [x] | Minecraft 1.21.1 adapter | `MinecraftGuiRenderOperations` is the only normal MCUI boundary to `GuiGraphics`. It uses Mojang Official Mapping names verified against the resolved 1.21.1 classes. |
| [x] | Transform | `GuiGraphics.pose().pushPose/popPose/translate/scale`; every Visitor push is paired in `try/finally`, with frame-close leak recovery as a final safeguard. |
| [x] | Rectangle | `GuiGraphics.fill(left, top, right, bottom, argb)`. No `BufferBuilder` is used for MCUI rectangles. |
| [x] | Texture | `GuiGraphics.blit(ResourceLocation, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight)`. Source and full texture dimensions are explicit. |
| [x] | Text | `GuiGraphics.drawString(Minecraft.font, Component/String, x, y, argb, shadow)`; centered X is calculated from the matching `Font.width` overload. |
| [x] | Item | `GuiGraphics.renderItem` followed by `renderItemDecorations`; legacy pop-time animation is intentionally deferred. |
| [x] | Scissor stack | Local clip corners are transformed into GUI logical coordinates, nested rectangles are intersected, and `GuiGraphics.enableScissor/disableScissor` performs the authoritative GUI-scale/framebuffer conversion. The 1.21.1 `GuiGraphics` implementation was inspected and also maintains a native nested intersection stack. |
| [x] | ARGB color | `ArgbColor` defines one `0xAARRGGBB` contract. White, semi-transparent white, red, and transparent channel decoding have unit coverage. `fill` and font APIs receive ARGB unchanged. |
| [x] | Element Visitor | Resolved `GroupElement`, `RectangleElement`, `TextElement`, `TextureElement`, and `ItemElement` are visited and rendered only through operations. Children retain stable Z ordering. |
| [x] | State safety tests | A fake-operation test forces a draw exception inside a clipped nested tree and proves scissor and matrix pops still occur in the expected order. |
| [x] | HUD integration | Fabric API `HudRenderCallback.EVENT` with `GuiGraphics` and `DeltaTracker`; it appends MCUI rendering and does not suppress or replace vanilla HUD layers. |
| [x] | Development smoke test | Requires both Fabric development environment and JVM property `-Dmcui.renderSmokeTest=true`. The tree draws a translucent panel, two texts, the existing `mcui:icon.png`, a clipped probe, transforms, and a decorated three-count item stack. |
| [~] | GLCore compatibility | No bridge is necessary for the new element path. If a later legacy element is migrated, a narrow `LegacyGlCompat` may translate only that behavior into `GuiRenderOperations`; it must not own global GL state or become a dependency of new elements. |

### Controlled direct render-state use

Normal rectangles, text, items, transforms, and scissors use `GuiGraphics` only. A textured rectangle is the sole exception: Minecraft 1.21.1 has no public tinted `ResourceLocation` blit overload, while vanilla's alpha-overlay path explicitly enables blending and changes shader color. The adapter therefore reads the prior shader color, queries prior blend enablement, applies tint/alpha around `GuiGraphics.blit`, and restores both in `finally`. It does not change blend functions, shaders, depth, or culling.

### Minimal smoke element tree

```text
Root Group (translate 16,16)
|- translucent Rectangle
|- title Text
|- scaled phase Text
|- existing MCUI logo Texture
|- clipped child Group
|  |- oversized translucent Rectangle
|  `- scaled clip-status Text
`- decorated Item (3 golden apples)
```

### Phase-two verification record

- [x] `compileClientKotlin` succeeds against Minecraft 1.21.1 Mojang mappings.
- [x] Unit tests pass, including ARGB and exception-safe render-state restoration.
- [x] `./gradlew clean build --no-daemon --no-parallel` succeeds, including tests, source jar, and remapped production jar.
- [x] `runClient` reaches the main menu, quick-loads an integrated-server world, and shuts down cleanly after each visual run.
- [x] Fabric HUD callback executes in-world and the resolved smoke tree renders through Visitor -> operations -> 1.21.1 adapter.
- [x] Rectangle and semi-transparent alpha render correctly; the world remains visible through the panel.
- [x] Component text, shadow, scaling transform, and the existing `mcui:icon.png` texture render correctly.
- [x] The nested red probe is clipped at the expected logical edge and later item/vanilla rendering proves the scissor is restored.
- [x] The golden-apple `ItemElement` and count decoration (`3`) render correctly.
- [x] Explicit GUI scales 2, 3, and 4 were visually verified in-world. A live Scale-4 resize from an 1800x1100 launch window to a smaller supported window retained the same logical layout and clip.
- [x] Vanilla health, hunger, experience, hotbar, crosshair, and tutorial overlay remain enabled.
- [x] `latest.log` contains no mixin crash, `ClassNotFoundException`, `NoSuchMethodError`, render-state `IllegalStateException`, pose/scissor underflow, leaked-state recovery, or OpenGL error spam. Mojang authentication/public-key network failures remain the same unrelated development-account issue recorded in phase one.

### Rendering work intentionally not migrated

- Legacy GLCore state and helpers (manual shaders, arbitrary blend functions, depth/cull/normal state, raw tessellation).
- Legacy element catalog and its compatibility adapters.
- Item pop-time animation.
- Rotation, arbitrary matrices, gradients, lines, nine-slice sprites, tooltips, entity/model rendering, and custom buffer/shader operations.
- Theme-defined/evaluated transforms, colors, visibility, resource IDs, and data-driven element construction.
- Full HUD part routing, game-state data sources, theme switching, and vanilla HUD suppression.

### Recommended phase-three order

Start with a minimal Theme loader that produces the resolved element model without pulling the renderer back into MiniScript or legacy GL state. Follow it with the full HUD system that selects and supplies those trees, then migrate legacy elements incrementally behind narrow compatibility adapters. Legacy elements should be last because they depend on both the loader contract and the HUD/render lifecycle being stable.

## Phase-three theme loading foundation

The runtime data flow is now:

`ResourceManager -> discovery -> metadata/HUD JSON parsing -> validation -> ThemeCompiler -> immutable resolved themes -> atomic ThemeSnapshot -> existing RenderingElementVisitor`

Rendering code does not import `Json`, `ResourceManager`, or theme metadata. Theme DTOs do not import `GuiGraphics`, the Minecraft client singleton, or rendering APIs. `ThemeCompiler` is the only conversion boundary from serialized values to `ResourceLocation`, `ArgbColor`, `ResolvedTransform`, `ResolvedRenderState`, and resolved elements.

### Historical format investigation

The three reference branches were inspected without checkout or modification.

- The 1.19.4 modern detector calls `ResourceManager.listResources("themes")` and recognizes metadata only when its resource path ends in `/theme.mcui.json`.
- A modern theme root is `assets/<namespace>/themes/<theme-name>/`. HUD candidates are exactly `hud.json` and `hud.xml` beside the metadata. `settings.json`, `fragments/`, `widgets/`, and `scripts/` are also rooted there.
- Theme ID is generated as `<resource namespace>:<last theme-root path segment>`. It is not historically declared in metadata. A display name also falls back to that final path segment.
- 1.19.4 metadata contains required `format` plus `version`, `fragments`, `widgets`, and `scripts`. Its checked-in `mcui:alpha` schema requires only `format`; checked-in metadata examples contain `{version, format}`.
- The 1.19.4 detector also recognizes metadata-less `hud.xml`/`hud.json` themes. `themes/<name>/hud.*` becomes `<namespace>:<name>` (`MODERN_LEGACY_SAOUI`). An even older `assets/<namespace>/themes/hud.*` layout derived its name from the resource pack filename (`LEGACY_SAOUI`).
- 1.12 discovers themes by manually scanning the mod JAR plus resource-pack folders/ZIPs. Its JSON HUD uses `parts` and object keys such as `ElementGroup:name`, `GLRectangle:name`, and `GLString:name`, with many string/expression wrapper values. That scanner and schema are historical references only: direct filesystem/JAR scanning is not carried forward.
- The 1.19.4 `JsonThemeLoader` still uses Gson with generated/adapter-driven legacy classes and contains a `TODO` to move to Kotlin Serialization. The newer `Group`, `Rectangle`, and `Text` model was not wired into a complete checked-in JSON HUD schema. Therefore the path/metadata conventions are retained, while phase three defines a small explicit resolved-value HUD subset instead of claiming compatibility with the unfinished refactor or expression-heavy 1.12 JSON.

### Final resource and ID conventions

```text
assets/<namespace>/themes/<theme-name>/theme.mcui.json
assets/<namespace>/themes/<theme-name>/hud.json
```

The discovered ID is `<namespace>:<theme-name>`. Metadata may now include an optional `id`; when present it must be a legal ID and must match the resource-derived ID. Discovery is not restricted to MCUI-owned namespaces, so third-party namespaces work. Both `mcui` and historical `saoui` roots are naturally discovered and retain distinct IDs. Metadata-less legacy themes are reported with their resource, inferred ID when possible, pack, and the explicit reason that their element schema is deferred.

`theme.mcui.json` supports:

- Required: `format`.
- Historical: `version`, `fragments`, `widgets`, `scripts`.
- Phase-three descriptive/compatibility fields: `id`, `name`, `authors`, `description`, `website`, `supportedVersion`, `parent`, `extends`, and `namespace`.
- `parent`/`extends` IDs are parsed and validated but inheritance is deliberately not executed yet.
- `mcui:resolved-v1` is the native phase-three format. `mcui:alpha` metadata remains accepted with a warning and is compiled only if its `hud.json` already matches the resolved subset.
- For `mcui:resolved-v1`, compiler validation additionally requires non-empty `name`, `version`, and at least one non-empty `authors` entry; the looser historical defaults remain parseable for `mcui:alpha` compatibility.

### Minimal HUD JSON subset

`hud.json` is `{ "version": "1", "root": <element> }`. Every element has `type`, optional `name`, `enabled` (default `true`), and `transform` with `x/y/z` (default `0`) and one uniform `scale` (default `1`). Supported types are:

- `group`: recursive `children`.
- `rectangle`: required positive `width`, `height`, and `color`.
- `text`: required `text`; optional ARGB `color` (white), `shadow` (`false`), and `centered` (`false`).
- `texture`: required legal `texture` ResourceLocation and positive `width`/`height`; optional `u/v`, source size, full texture size, and ARGB `tint`.

ARGB accepts `#RRGGBB` (normalized to opaque), `#AARRGGBB`, `0xAARRGGBB`, or a 32-bit JSON integer. Old RGBA interpretation is not implicit. JSON `Item` is deferred because safe 1.21.1 `ItemStack` component/registry serialization would expand this phase; the phase-two resolved `ItemElement` remains available to the renderer.

Unknown element types and missing/invalid structural fields invalidate the entire theme. Optional unsupported behavior becomes a warning. A syntactically valid but missing texture is checked once at compile time and warned; it is not looked up every frame.

### Discovery, pack priority, and reload atomicity

`ThemeResourceLoader` uses only the supplied client `ResourceManager`. `listResources("themes", ...)` discovers metadata and legacy candidates, while `getResource` resolves HUD files and textures. Minecraft 1.21.1's `FallbackResourceManager.listResources` builds the selected result for each exact ResourceLocation from the ordered pack stack and returns a path-sorted map; higher-priority definitions replace lower ones. Metadata, HUD, and textures can therefore each be overridden at their normal exact resource paths. MCUI performs no filesystem scan and does not read only its own JAR.

Discovery, reads, parsing, validation, and compilation populate temporary collections. `ThemeManager.apply` publishes a completed immutable `ThemeSnapshot` through one `AtomicReference.set`. A loader-wide fatal failure rejects the apply and preserves the previous snapshot. Per-theme errors remove that theme from the new snapshot; if the configured active theme is missing or invalid, the snapshot uses a built-in empty root. There is never a partially filled live map.

Development chooses `mcui:development_test`; production chooses `mcui:default`, which can be supplied or overridden by a player pack. If absent, production renders the empty fallback. No debug text is selected for ordinary production users.

### Phase-three implementation status

| Status | System | Result |
|---|---|---|
| [x] | Kotlin Serialization | Plugin `2.4.10` matches Kotlin; JSON runtime `1.11.0`; dependency-only clean-build gate passed before implementation. |
| [x] | Pure theme data | `ThemeId`, metadata/document/element/transform DTOs, ARGB serializer, issues, and validation result are in main sources. |
| [x] | JSON parser | Strict structured decoding with unknown-field tolerance for forward metadata compatibility; comments/trailing commas match the 1.19.4 parser tolerance. |
| [x] | Theme compiler | Applies defaults, validates IDs/dimensions/transforms/colors/hierarchy, checks textures once, dispatches element types, and counts the stable resolved tree. |
| [x] | Theme manager | Immutable theme map and active theme in one atomic snapshot; deterministic preferred theme and empty fallback. No Koin/global mutable graph. |
| [x] | Resource-pack discovery | Original `themes/<name>/theme.mcui.json` convention through the Minecraft ResourceManager; all namespaces including `mcui` and `saoui`. |
| [x] | Atomic reload | Every Fabric client resource reload performs a full temporary load and one apply; monotonic revision and summary/error logging included. |
| [x] | Development theme | Explicit `mcui:development_test` metadata and HUD JSON; Rectangle, Text, Texture, Group, and Transform pass through the real pipeline. |
| [x] | HUD source | `MCUIHudRenderer` reads `ThemeManager.activeTheme.hudRoot`; the phase-two hardcoded smoke tree is removed. |
| [x] | Invalid theme behavior | Unknown type test logged resource/theme/field/path, produced loaded=0/failed=1, selected `mcui:empty`, and did not crash or leave a partial tree. |
| [x] | Player pack override | A temporary pack overriding only the same `mcui:themes/development_test/hud.json` won over the built-in HUD and rendered changed text/color/size; test pack was removed afterward. |

### Phase-three verification record

- [x] Dependency gate: `clean build` succeeded immediately after adding only the serialization plugin and JSON runtime.
- [x] Unit tests cover historical/minimal metadata, descriptive fields, missing required format, valid minimal HUD, defaults, ARGB normalization, recursive children, unknown type, missing fields, invalid ResourceLocation, missing-texture warning, Definition-to-Resolved conversion, active selection, fallback, valid snapshot replacement, and fatal-load preservation.
- [x] Initial in-world reload: revision 1, discovered 1, loaded 1, failed 0, active `mcui:development_test`.
- [x] In-world rendering visually verified the panel rectangle/alpha, title and phase text, existing `mcui:icon.png`, nested Group, translation, uniform scale, and Z ordering.
- [x] F3+T after changing the built output to `Rendering / Theme Phase 3 B`: revision 2 discovered/parsed/compiled/applied the theme without restarting Minecraft.
- [x] Unknown root type during a later F3+T: revision 3 logged the exact resource, theme, `root.type`, and unknown value; active became `mcui:empty`; renderer remained stable.
- [x] Restored valid JSON during the same client process: revision 4 loaded successfully and restored `mcui:development_test`.
- [x] A separate client run enabled a temporary `pack_format: 34` player pack. Minecraft listed it after the mod resources and its same-path HUD override was visible in-world.
- [x] Both client runs quick-played the existing integrated-server world and stopped cleanly. Authentication/public-key TLS timeouts are the same unrelated development-account network issue from phases one and two.

### Still deferred after phase three

- XML/xmlutil, CSS/ph-css, Lua/LuaJ, MiniScript/JEL, KSP/LuaJ-KSP, and Koin.
- Legacy 1.12 key-discriminator elements and expressions/cache wrappers, fragments, widgets, settings execution, scripts, and theme inheritance. The historical top-level `parts` map is now restored with the resolved phase-four element schema.
- Registry-serialized static JSON ItemStack, complete legacy elements, screen replacement, social integrations, and configuration/selection UI.
- Complete historical SAO theme assets; only the existing logo is reused by the development theme.

## Phase-four HUD composition foundation

Status: **Implemented, awaiting manual user validation.** No client launch, world entry, resource-pack test, screenshot test, key simulation, or automated test execution was performed in this phase.

The runtime data flow is now:

`Minecraft client frame -> HudDataProvider -> immutable HudDataSnapshot -> HudRenderCoordinator -> visible ResolvedHud parts -> RenderingElementVisitor -> GuiRenderOperations`

JSON is still parsed and compiled only during resource reload. No resolved element reads `Minecraft.getInstance()`, a player, level, or inventory. The provider copies the nine hotbar stacks and hand stacks for the current frame, and no `LocalPlayer`, `ClientLevel`, `Inventory`, vehicle, or effect instance is retained across frames.

### Historical HUD findings

- All three reference lines use the same 13 part names: `HEALTH_BOX`, `HOTBAR`, `EXPERIENCE`, `CROSS_HAIR`, `ARMOR`, `JUMP_BAR`, `AM2BARS`, `PARTY`, `FOOD`, `EFFECTS`, `AIR`, `MOUNT_HEALTH`, and `ENTITY_HEALTH_HUD`.
- The 1.12 JSON HUD already used a top-level `parts` object keyed by those enum names. Its child encoding and expression values were tightly coupled to JEL/MiniScript and are not copied.
- The 1.16.5 implementation replaced individual Forge `IngameGUI` methods. It sourced health, food, air, armor, experience, mount state, jump charge, effects, hotbar slots, nearby entities, and targets from one mutable draw context.
- Historical hotbar items delegated model, count, and durability rendering to Minecraft's item renderer. The phase-four hotbar preserves that division through `GuiGraphics.renderItem` and `renderItemDecorations`.
- Historical entity health acquired nearby/target entities with custom capability and ray-trace logic. That target acquisition is deliberately deferred rather than embedded in the new snapshot.

### HUD definition and Phase 3 compatibility

`hud.json` now accepts both fields:

```json
{
  "version": "2",
  "root": { "type": "group", "children": [] },
  "parts": {
    "HEALTH_BOX": { "type": "group", "children": [] },
    "HOTBAR": { "type": "hotbar" }
  }
}
```

`root` is optional and remains the phase-three global overlay. A phase-three document containing only `root` compiles to `ResolvedHud.globalOverlay`, renders exactly once, and suppresses no vanilla HUD. `parts` is optional and uses historical names with the resolved element schema. At least one of `root` or `parts` must be present.

`TransformDefinition` also has an optional `anchor` (`TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`, `CENTER`, `BOTTOM_LEFT`, `BOTTOM_CENTER`, or `BOTTOM_RIGHT`). Anchors use `GuiGraphics` logical width/height; x/y are logical-pixel offsets and are never manually multiplied by GUI scale.

### Resolved HUD and coordinator

`ResolvedTheme` owns a `ResolvedHud(globalOverlay, parts)`. The part map is immutable after resource reload. `HudRenderCoordinator` draws the global overlay first, then a stable part order, and consults `HudPartVisibility` for every part. `RenderingElementVisitor` receives the current snapshot for dynamic elements; static rectangle/text/texture/item behavior is unchanged.

### HudDataSnapshot

The per-frame snapshot contains:

- player health, maximum health, absorption, armor;
- food, fixed maximum food, saturation, fixed maximum saturation;
- air and maximum air;
- experience progress, level, and whether the current game mode exposes experience;
- selected hotbar slot, copied nine-slot hotbar, copied main-hand and off-hand stacks;
- effect ID, duration, amplifier, ambient/visible/icon/beneficial flags for each active effect;
- riding state, living-mount presence/health/maximum health;
- jump-capable mount presence, jump charge, and jump cooldown;
- crosshair target kind, attack strength, and ready state;
- creative, spectator, survival-HUD, underwater, dead, and first-person flags;
- GUI logical width/height, GUI scale metadata, and partial tick.

The provider reads only the current `Minecraft.player`, `level`, and `gameMode` during capture. Mount health is taken only from a current `LivingEntity` vehicle. Jump data uses `LocalPlayer.jumpableVehicle()`, `getJumpRidingScale()`, and the mount's `getJumpCooldown()` adapter path.

### Typed dynamic bindings

`HudValueSource` supports `PLAYER_HEALTH`, `PLAYER_MAX_HEALTH`, `PLAYER_ABSORPTION`, `FOOD`, `MAX_FOOD`, `SATURATION`, `MAX_SATURATION`, `AIR`, `MAX_AIR`, `ARMOR`, `EXPERIENCE_PROGRESS`, `EXPERIENCE_LEVEL`, `MOUNT_HEALTH`, `MOUNT_MAX_HEALTH`, `JUMP_PROGRESS`, and `HOTBAR_SELECTED_SLOT`.

For a progress bar, current-value sources normalize against their matching maximum (armor uses the vanilla 20-point scale). For dynamic text, the same enum yields the raw numeric display value. Text elements must declare exactly one of `text` or `valueSource`; there is no interpolation or expression syntax.

`HudItemSource` supports `HOTBAR_SLOT_0` through `HOTBAR_SLOT_8`, `MAIN_HAND`, and `OFF_HAND`. `hud_item`/`dynamic_item` resolves one copied stack from the snapshot while the existing static `ItemElement` remains unchanged.

### Progress bar and hotbar elements

`progress`, `progress_bar`, and `bar` compile to `ProgressBarElement`. Required fields are positive `width`/`height`, `foregroundColor`, and `valueSource`; `backgroundColor` is optional. Directions are left-to-right, right-to-left, top-to-bottom, and bottom-to-top. Values are clamped to `[0, 1]` before fill dimensions are calculated.

`hotbar` compiles to `HotbarElement`. It draws all nine copied stacks, selected-slot framing, optional per-slot background, configurable slot size/spacing and item offsets, and optional decorations. Item models stay in Minecraft's renderer. `GuiGraphics.renderItemDecorations` supplies count text, durability bars, and the native item cooldown overlay.

`effects`/`effect_list` compiles to `EffectListElement`. It consumes only effect snapshots and renders resource-pack-aware `textures/mob_effect/<id>.png` icons with localized names, amplifier level, remaining duration, theme-defined row/background/accent colors, and an entry limit. It does not retain `MobEffectInstance` objects or read the player from the renderer.

### Visibility policy

| Part | Phase-four visibility |
|---|---|
| `HEALTH_BOX`, `ARMOR` | Survival HUD permitted, not spectator, alive. |
| `FOOD` | Same survival conditions and no current vehicle that exposes mount health, matching vanilla's food/mount-health substitution. |
| `AIR` | Same survival conditions and underwater or air below maximum. |
| `HOTBAR` | Not spectator and alive. Spectator hotbar remains vanilla. |
| `EXPERIENCE` | Game mode exposes XP, alive, not spectator, and no active jump-capable mount. |
| `CROSS_HAIR` | First-person and alive; spectator crosshair may still render. |
| `EFFECTS` | At least one active effect requests an icon. |
| `MOUNT_HEALTH` | Current vehicle is living, alive, not spectator. |
| `JUMP_BAR` | Current vehicle is jump-capable, alive, not spectator. |
| `AM2BARS`, `PARTY` | Reserved/integration-only; no phase-four data provider. |
| `ENTITY_HEALTH_HUD` | Rendering entry retained; target acquisition and visibility deferred to Phase 5+. |

### Selective vanilla replacement

The active theme suppresses vanilla only when its `ResolvedHud.parts` contains the corresponding part. A `root`-only or partial theme leaves every absent vanilla element intact.

| MCUI part | Vanilla mapping | Mechanism |
|---|---|---|
| `CROSS_HAIR` | Crosshair | Precise `renderCrosshair` HEAD gate. |
| `EFFECTS` | Status-effect icons | Precise `renderEffects` HEAD gate. |
| `EXPERIENCE` | XP level text | Precise `renderExperienceLevel` HEAD gate. |
| `HOTBAR` | Normal item hotbar | Precise `renderItemHotbar` HEAD gate. |
| `EXPERIENCE` | XP bar | Precise `renderExperienceBar` HEAD gate. |
| `JUMP_BAR` | Mount jump meter | Precise `renderJumpMeter` HEAD gate. |
| `MOUNT_HEALTH` | Vehicle hearts | Precise `renderVehicleHealth` HEAD gate. |
| `HEALTH_BOX` | Player hearts/absorption | Redirect only the `renderHearts` call inside `renderPlayerHealth`. |
| `ARMOR` | Armor icons | Redirect only the `renderArmor` call inside `renderPlayerHealth`. |
| `FOOD` | Food icons | Redirect only the `renderFood` call inside `renderPlayerHealth`. |
| `AIR` | Air/bursting-air sprites | Redirect the two direct air sprite blits inside `renderPlayerHealth`. |
| `PARTY`, `AM2BARS`, `ENTITY_HEALTH_HUD` | No direct vanilla element | No suppression. |

The actual Fabric Rendering API module resolved by `fabric-api 0.116.17+1.21.1` is `fabric-rendering-v1 5.2.1`. It provides the post-HUD `HudRenderCallback`, but not the later `HudLayerRegistrationCallback`, `IdentifiedLayer`, or layer replace/remove operations. MCUI therefore uses the official callback for its own rendering and one client-only `GuiHudSuppressionMixin` for every precise vanilla gate. It does not replace `Gui`, cancel `Gui.render`, touch server logic, or suppress absent theme parts.

### Development theme and deferred work

`mcui:development_test` now provides visibly distinct `HEALTH_BOX`, `FOOD`, `EXPERIENCE`, `AIR`, `HOTBAR`, `CROSS_HAIR`, `MOUNT_HEALTH`, `JUMP_BAR`, and `EFFECTS` parts. It includes health/food/XP/air/mount/jump progress bars, dynamic health/mount-health/level text, the native nine-slot hotbar, and a snapshot-driven effect list. Its small `root` label intentionally exercises phase-three compatibility.

Mount health data, jump data, part visibility, bindings, rendering entries, vanilla gates, and development layouts are implemented. Effect snapshots, lifecycle/suppression, and the initial icon/name/duration list element are implemented; richer SAO-specific effect styling remains deferred. `ENTITY_HEALTH_HUD` keeps its part type and coordinator entry, but target acquisition and entity snapshots are deferred to Phase 5+.

### Manual validation follow-up

The first user screenshots exposed two composition gaps rather than stale cached state: the development theme lacked `MOUNT_HEALTH`, `JUMP_BAR`, and `EFFECTS`, and custom food visibility did not mirror vanilla's mount-health substitution. The follow-up adds those three parts, treats only `LivingEntity.showVehicleHealth()` mounts as mount-health providers, switches FOOD/MOUNT and EXPERIENCE/JUMP every frame from the snapshot, and replaces the overlapping vanilla effect icons with the themed effect list. These corrections are implemented and await another manual user validation pass.

No Lua, LuaJ, MiniScript, JEL, KSP, Koin, XML/xmlutil, CSS/ph-css, Forge/NeoForge, screen replacement, party implementation, or final SAO assets were added.

### Phase-four compile record

Verified on 2026-09-04 with the configured Java 21 toolchain:

- `.\gradlew.bat compileKotlin compileClientKotlin --no-daemon --no-parallel`: successful after correcting one nullable registry-key conversion and one cross-source-set Kotlin smart cast.
- `.\gradlew.bat compileClientJava --no-daemon --no-parallel`: successful; this additional source-set check covers the client-only Java Mixin that the preferred Kotlin task selection does not execute.
- Manual-validation correction: the same Kotlin compile selection succeeded after fixing the effect-icon texture call's missing zero-origin UV arguments.
- No tests, client launch, world entry, resource reload, screenshot, input simulation, temporary resource pack, or game-runtime validation was performed. Runtime behavior remains **awaiting manual user validation**.

## Phase-one verification record

Verified on 2026-09-04 with Microsoft OpenJDK 21.0.8.9:

- `./gradlew clean build --no-daemon --no-parallel`: successful; Kotlin/main/client compilation, unit tests, sources remap, and production jar remap all completed.
- `./gradlew runClient --no-daemon --no-parallel`: Minecraft 1.21.1 window opened and completed initial resources/audio/atlas loading.
- Log evidence: MCUI common initialization, client initialization, and resource reload revision 1 all ran on the render thread.
- No MCUI/Fabric Mixin crash, `ClassNotFoundException`, or `NoSuchMethodError` was observed.
- Mojang session/profile endpoints produced TLS timeout warnings for the generated development account; these did not prevent the main menu and are unrelated to MCUI initialization.
