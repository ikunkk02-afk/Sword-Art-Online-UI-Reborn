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
| [~] | Resource loading/reload | Fabric `MCUIFabricCore` reload listener, `themes/loader/**` | `src/client/.../resources/ClientResourceReloads.kt` | Yes | Fabric API | Client-only | Reload registration and monotonic revision are real. Theme discovery/parse/apply remains phase two. |
| [x] | Pure Kotlin utilities (selected) | `common/.../util/LayeredMap.kt` | `src/main/.../util/LayeredMap.kt` | No | None | Main | Ported with corrected shadowing and `containsValue`; covered by unit test. |
| [x] | Tests (foundation) | 1.19.4 `common/src/test/**` patterns | `src/test/kotlin/**` | No | Kotlin test/JUnit platform | Build only | Current test covers the migrated layered map. Legacy serde/script tests await their systems. |

## Full source audit

| Status | Major system | Primary 1.19.4 path | Mature/history fallback | Planned 1.21.1 path | MC rewrite? | Main dependencies | Scope | Known blocker / next decision |
|---|---|---|---|---|---|---|---|---|
| [~] | MCUI Core | `common/.../MCUICore.kt` | `1.16.5 src/main/java/com/tencao/saoui/**` | `src/main/kotlin/be/bluexin/mcui/**` | Yes | Previously Koin/KSP plus all theme modules | Main + client bootstrap | Foundation complete; full module graph waits for the loaders it initializes. |
| [x] | Constants/logger | `common/.../Constants.kt`, `util/LoggerHelper.kt` | Same concepts in older core | `src/main/kotlin/be/bluexin/mcui/**` | Minor | SLF4J already provided | Main | Phase-one scope complete. |
| [~] | Config/settings | `common/.../config/**`; Fabric platform helper | `1.16.5 .../config/**` | `src/main/.../config/**` | Yes | Serialization, coroutines; old Forge Config Port/NightConfig | Prefer main-safe storage, client use | Decide native Fabric-friendly persistence and migration format before porting `Setting`/`Settings`. |
| [ ] | Theme metadata/manager | `common/.../themes/meta/**` | `1.16.5 .../themes/**`; 1.12 theme packages | `src/client/.../themes/meta/**` with pure models in main where possible | Yes | Serialization, loader stack, scripting | Mostly client | Coupled to ResourceManager, ResourceLocation changes, Koin modules, and unfinished 1.19.4 refactor. |
| [~] | Elements (new) | `common/.../themes/elements/{Fragment,Group,Hud,...}.kt` | Compare legacy implementations | `src/client/.../render/element/**` | Yes at rendering boundary | Minecraft types only; JOML supplied by Minecraft | Client for phase-two resolved models | Minimal resolved `Group`, `Rectangle`, `Text`, `Texture`, and `Item` elements are complete. Theme serialization, script-backed values, and the full element catalog remain deferred. |
| [ ] | Legacy Elements | `common/.../themes/elements/legacy/**` | 1.16.5 and 1.12 element packages | `src/main/.../themes/elements/legacy/**` | Yes | XML, MiniScript, Lua, renderer | Split/client-heavy | KSP-generated factories and extensive old rendering calls. Do not delete absent/incomplete features. |
| [ ] | Screen | `common/.../screens/**`, `deprecated/screens/**` | `1.16.5 .../screens/**`; 1.12 GUI packages | `src/client/.../screens/**` | Yes, substantial | Theme, elements, Lua, config | Client-only | 1.21.1 Screen/GuiGraphics/input signatures must be redesigned around compatibility adapters. |
| [~] | HUD | new `themes/elements/Hud.kt`, deprecated `IngameGUI.kt` | Mature 1.16.5 HUD; 1.12 historic HUD | `src/client/.../fabric/client/hud/MCUIHudRenderer.kt` | Yes, substantial | Fabric rendering API | Client-only | Phase-two non-invasive callback and smoke tree are complete. Full MCUI HUD composition and vanilla suppression remain deferred. |
| [x] | Rendering abstraction | `themes/elements/renderer/**` | Mature GL calls in 1.16.5/1.12 | `src/client/.../render/**` | Yes, central | `GuiGraphics`; JOML/LWJGL supplied by Minecraft | Client-only | Phase-two operations, 1.21.1 adapter, transform/scissor safety, Visitor, color contract, and minimal elements are implemented. |
| [~] | GLCore | `common/.../GLCore.kt` | Mature `1.16.5 .../GLCore.kt`; 1.12 GL helpers | Optional future `src/client/.../render/compat/**` | Yes, complete rewrite | New render operations | Client-only | The compatibility strategy is defined; no `LegacyGlCompat` was added because no migrated legacy caller needs it yet. New code must never target a GLCore monolith. |
| [ ] | MiniScript | `common/.../themes/miniscript/**` | 1.16.5 `themes/util/**` | `src/main/.../themes/miniscript/**` and client context adapters | Yes for game context | JEL, Serialization, Lua mapping | Split | gnu-jel Java 21 compatibility and generated bindings must be proven before inclusion. |
| [ ] | Lua | `common/.../themes/scripting/**` | 1.16.5/1.12 scripting implementations | `src/main/.../themes/scripting/**` | Limited MC; major runtime work | LuaJ, BCEL, LuaJ-KSP/KSP, optional JNLua | Split | Security sandbox, Java 21 bytecode/runtime compatibility, and generator publishing are unresolved. |
| [ ] | CSS | Style parsing/usage under theme loader/renderer; theme `style.css` assets | 1.16.5/1.12 theme code/assets | `src/main/.../themes/style/**` | Indirect | ph-css, ph-commons | Mostly main | Defer until the element style contract is stable. |
| [ ] | XML / serialization | `themes/serde/**`, `themes/loader/{Xml,Json}ThemeLoader.kt` | Mature 1.16.5 `themes/util/xml/**`; 1.12 JAXB/theme models | `src/main/.../themes/serde/**` | ResourceLocation adapters need rewrite | Kotlin Serialization, xmlutil | Main with client resource adapter | Validate current Kotlin/xmlutil compatibility and preserve legacy formats/schemas. |
| [ ] | Commands | `common/.../commands/**` | Older debug/config commands | `src/client/.../commands/**` or safe main registration | Yes | Fabric command API, theme/config | Client mod | Command source/registration context and client-vs-server semantics need review. |
| [~] | Resource loading | `themes/loader/**`, Fabric reload listener | Older resource/theme scanners | Split loader models + client Fabric adapter | Yes | Fabric API, Serialization/XML/CSS | Client adapter | Foundation listener is ready; parsing/apply intentionally absent. |
| [ ] | Mixins | `common/.../mixin/ModConfigMixin.java`, Fabric mixin JSON | 1.16.5/1.12 mixins/ATs | `src/client/java/be/bluexin/mcui/mixin/**` only if required | Yes | Mixin | Client-only where possible | Template mixins removed. Upstream config mixin is tied to Forge Config Port and is not carried forward. |
| [x] | Fabric platform code (foundation) | `fabric/src/main/**` | None | `src/main/.../fabric`, `src/client/.../fabric/client` | Yes | Fabric Loader/API | Fabric-only | Phase-one entrypoints/reload boundary done. Further callbacks arrive with their systems. |
| [ ] | Forge platform code | `forge/src/main/**` | 1.16.5 is Forge | None in this phase | N/A | Forge/KotlinForForge | Excluded | Explicitly out of scope; retained only as behavioral reference. |
| [ ] | Social/party integrations | `social/**`, deprecated party/friend elements | 1.16.5/1.12 SAOMCLib integrations | Undecided | Yes | Former FTB Library/Teams or replacement API | Client/integration | Optional integration contract must be isolated; no hard dependency in phase one. |
| [ ] | Assets/theme packs | `common/src/main/resources/assets/{mcui,saoui}/**` | 1.16.5/1.12 full asset history | `src/main/resources/assets/{mcui,saoui}/**` | Resource metadata may need updates | None/runtime loaders | Client resources | Only the original logo is present now. Bulk assets move with validated loaders to avoid implying working themes. |
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
| SLF4J API | Supplied transitively by Minecraft/Fabric runtime | Yes, no explicit artifact | Existing MCUI logging API | Verified by compilation/runtime log | Do not duplicate or shade. |

### Deferred or removed from the active build

| Legacy dependency | 1.19.4 reference version | Needed in phase one? | Current call sites | Compatibility / acquisition assessment | Decision |
|---|---:|---|---|---|---|
| Kotlin Serialization | `1.7.3` | No | Setting/theme JSON and serde | Version selection deferred until the data models are ported to Kotlin 2.4.x | Do not add yet. |
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

## Phase-one verification record

Verified on 2026-09-04 with Microsoft OpenJDK 21.0.8.9:

- `./gradlew clean build --no-daemon --no-parallel`: successful; Kotlin/main/client compilation, unit tests, sources remap, and production jar remap all completed.
- `./gradlew runClient --no-daemon --no-parallel`: Minecraft 1.21.1 window opened and completed initial resources/audio/atlas loading.
- Log evidence: MCUI common initialization, client initialization, and resource reload revision 1 all ran on the render thread.
- No MCUI/Fabric Mixin crash, `ClassNotFoundException`, or `NoSuchMethodError` was observed.
- Mojang session/profile endpoints produced TLS timeout warnings for the generated development account; these did not prevent the main menu and are unrelated to MCUI initialization.
