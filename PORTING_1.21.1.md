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
| [ ] | Elements (new) | `common/.../themes/elements/{Fragment,Group,Hud,...}.kt` | Compare legacy implementations | `src/main/.../themes/elements/**` plus client renderer adapters | Yes at rendering boundary | Serialization, MiniScript, JOML | Split | Preserve element model while removing PoseStack leakage; validate hierarchy/visitor behavior first. |
| [ ] | Legacy Elements | `common/.../themes/elements/legacy/**` | 1.16.5 and 1.12 element packages | `src/main/.../themes/elements/legacy/**` | Yes | XML, MiniScript, Lua, renderer | Split/client-heavy | KSP-generated factories and extensive old rendering calls. Do not delete absent/incomplete features. |
| [ ] | Screen | `common/.../screens/**`, `deprecated/screens/**` | `1.16.5 .../screens/**`; 1.12 GUI packages | `src/client/.../screens/**` | Yes, substantial | Theme, elements, Lua, config | Client-only | 1.21.1 Screen/GuiGraphics/input signatures must be redesigned around compatibility adapters. |
| [ ] | HUD | new `themes/elements/Hud.kt`, deprecated `IngameGUI.kt` | Mature 1.16.5 HUD; 1.12 historic HUD | `src/client/.../hud/**` | Yes, substantial | Theme, elements, Fabric HUD API | Client-only | Select correct 1.21.1 HUD callback/layer API and vanilla suppression strategy. |
| [ ] | Rendering abstraction | `themes/elements/renderer/**` | Mature GL calls in 1.16.5/1.12 | `src/client/.../render/**` | Yes, central | Minecraft rendering, JOML | Client-only | Highest-value phase-two boundary: GuiGraphics, scissor, texture, text, item, matrices, state restoration. |
| [ ] | GLCore | `common/.../GLCore.kt` | Mature `1.16.5 .../GLCore.kt`; 1.12 GL helpers | `src/client/.../render/compat/**` | Yes, complete rewrite | RenderSystem/Minecraft | Client-only | Old global GL state/font assumptions are incompatible; port behavior, not method names. |
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

## Phase-two recommendation (do not start during phase one)

Start with the rendering abstraction, not GLCore/HUD/Screen/Theme wholesale. Define and test a small 1.21.1 client-only adapter around `GuiGraphics`, matrix/scissor state, textures, text, and item rendering. Then port a minimal new-style Element + HUD path through it. GLCore's old global-state behavior should be decomposed behind that adapter rather than migrated as a monolith.

## Phase-one verification record

Verified on 2026-09-04 with Microsoft OpenJDK 21.0.8.9:

- `./gradlew clean build --no-daemon --no-parallel`: successful; Kotlin/main/client compilation, unit tests, sources remap, and production jar remap all completed.
- `./gradlew runClient --no-daemon --no-parallel`: Minecraft 1.21.1 window opened and completed initial resources/audio/atlas loading.
- Log evidence: MCUI common initialization, client initialization, and resource reload revision 1 all ran on the render thread.
- No MCUI/Fabric Mixin crash, `ClassNotFoundException`, or `NoSuchMethodError` was observed.
- Mojang session/profile endpoints produced TLS timeout warnings for the generated development account; these did not prevent the main menu and are unrelated to MCUI initialization.
