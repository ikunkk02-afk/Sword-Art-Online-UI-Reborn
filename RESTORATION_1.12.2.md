# 1.12.2 → Fabric 1.21.1 restoration

Reference: `origin/2.0-1.12.2-ElementRework`, commit `892a40a2a93d8685120d79c47b79515bab9567d8`.
The reference branch is unchanged. Existing working-tree changes are preserved.
This is an evidence-backed implementation report. Empty or dormant code in the selected source branch is identified explicitly rather than represented as a functioning backend.

## Implemented in this restoration pass

| Area | Original evidence | Modern implementation |
|---|---|---|
| Original assets | `assets/saoui/textures`, `sounds`, `sounds.json`; `StringNames.kt` selects `guiedt.png` | All 74 original texture/sound resources present byte-for-byte; SHA-256 regression manifest. HUD/menu paths use the original `guiedt.png`, `entities.png`, and `hud/status_icons` paths. Prior resource-pack aliases retained. |
| Original theme definitions | `assets/saoui/themes/*`, `model/cursor.json` | Original model retained; XML/CSS/XSD archived under `assets/saoui/legacy_1_12_2/themes` to avoid treating unsupported XML as a loadable modern theme. XML execution has NOT been ported. |
| Top-right entity HP | `screens/ingame/IngameGUI.kt`, `renderEnemyHealth` / `getMouseOver` | Independent 64-block entity ray, nearby aggressive entities within ±10/±5/±10, nearest five excluding target, combined health-fraction sort, state-colored mirrored bars, 10-character labels. Mirrored quads disable/restore culling; explicit half-pixel translations preserved. |
| Party HUD | Legacy `PARTY` part | Removed unconditional hidden state; shows populated party snapshots while alive. |
| Popup motion | `elements/gui/Popup.kt` | Drag motion instead of unpressed mouse motion; original opening/button scale curve and close-axis behavior. |
| World indicators | Legacy entity color handlers and renderers | Hostile visibility state, boss state, camera-facing crystal basis, partial-tick rotation, hostile baby scaling and 64-block distance. |
| Death | Legacy options and `DeathParticles` | Default custom death screen, original lifetime/growth, upright shard geometry rotating around Y, and original sound bytes. Modern buffered rendering retains safe translucency instead of mutating global OpenGL blend state. |
| Equipment/items | `BaseFilters`, `elements/custom/InventoryElemenets.kt` | Nested categories, live slot snapshots, item tooltip popup, equipment swap, hotbar selection, accessory slots injected by compatible mods, and single-item drop confirmation via vanilla container packets. Menu-slot IDs are kept distinct from backing inventory indices. Protects carried stacks, changed items and non-pickup slots. |
| Crafting | `CraftingUtil`, `PopupCraft` | Known craftable 2×2 recipes grouped into building, redstone, equipment and miscellaneous categories; original -10/-1/confirm/+1/+10 popup and live quantity footer. Sequential single-operation placement, matching-result wait, output count check, cancellation and ingredient return. Requires an empty grid at start; aborts on player/container change, occupied cursor, unavailable material, full inventory or timeout. Multiplayer acknowledgments and remainder-item behavior still need live tests. |
| Friends | `FriendCore`, `FriendData`, `FriendElement` | Local UUID/name persistence, add online player, remove friend, original separate offline group, inspection and server-gated party invite. Atomic replacement; malformed files cannot be silently overwritten. Imports the old `config/saoui/friend_list.cfg` into `config/mcui/friends.json` without deleting the recoverable source file. |
| Invitations | Original `EventCore` | Deferred popup when opening menu, expiry filtering, local-player lifecycle reset. Requires server SAOMCLib, as do party mutations. |
| Navigation | `ElementRegistry`, `Elements.advancementCategory`, `PopupAdvancement` | Root categories and completed/incomplete children from server-synchronized advancements, requirements popup and previous/next. Unlocked/locked recipe groups and result tooltips. Does not fabricate hidden advancement data unavailable to the client. |
| Profile | `elements/custom/ProfileElement.kt` | Removed obsolete +40px compensation now all four profile actions exist; opaque background/shadow and original text color. Modern character draw uses the original foot baseline instead of bounding-box center, with entity state restored afterward. |
| Player inspection | `PopupPlayerInspect` | Party member and friend inspection, available player attributes or explicit unknown-data text; no invented offline attributes. |
| Font option | `GLCore.setFont`, `CUSTOM_FONT` | Original ASCII texture via modern bitmap provider, consistent measurement/rendering, default-font fallback for unsupported glyphs. Explicit custom font styles are not replaced. |
| Skills/empty roots | `DefaultSkills`, `ElementRegistry` | Sprint, sneak and crafting actions are exposed using their source-backed client actions. Guild remains an empty, server-gated row and Message an empty top-level container, matching the selected branch rather than inventing services absent from it. |
| Options | `OptionCore`, `IngameGUI` | Original `FORCE_HUD` name/default/semantics restored. Configurations written by earlier 1.21.1 builds with `ALWAYS_SHOW` migrate automatically. `DEFAULT_DEBUG` maps to Minecraft's current equivalent debug renderer; dormant `AGGRO_SYSTEM` remains a compatibility switch because the source has no event wiring. |
| Chinese localization | English locale and all mod translation callsites | Added Chinese text for every English locale key, including party/friend events, item descriptions, skills, crafting, quests and options. A parity test rejects missing keys and placeholder mismatches. |

## Verification

- `gradlew.bat compileClientKotlin test --rerun-tasks`: 32 tests, zero failures/errors after the latest logic and localization changes.
- Resource regression: all 74 original texture/sound files checked against the selected commit's hashes.
- Entity selection regression: target-only, six rows, ordering, duplicates and invalid health.
- Friend persistence regression: restart, rename/deduplication, deletion, preservation of malformed files and legacy cfg migration.
- Localization regression: complete English-to-Chinese key coverage and matching format placeholders.
- Profile geometry regression: four actions plus original pointer coordinate align to orb center.
- Development client launch after the latest changes: resource reload `discovered=2, loaded=2, failed=0`; Chinese-localized integrated world entered and 20 advancements loaded. Mojang profile/public-key service timeouts were external network failures, not a mod resource/mixin failure.
- Native in-world screenshot comparison and multiplayer inventory/party interaction have NOT been completed. A successful build alone is not visual or network parity evidence.

## Remaining acceptance work

- Reconcile every legacy HUD XML element and screen animation against this branch and the supplied screenshots, including multiple GUI scales; profile source corrections are implemented but not screenshot-verified.
- Validate crafting quantity/queue/cancellation/ingredient cleanup and navigation against live server state, including slow/rejected transactions.
- Verify crafting queue/remainder handling, accessory integrations, death particles and world indicators in a live multiplayer session.
- Determine the original player aggression/custom-state synchronization contract against the corresponding library; the selected UI branch contains unwired state methods, not evidence that modern gameplay should be invented.
- The branch's skill registry is explicitly marked unused and contains no RPG progression backend. Guild/message entries are empty in source. A real service for those features must come from another authoritative branch or compatible server mod.
- Validate screen routing and the original `DEFAULT_DEBUG`/dormant `AGGRO_SYSTEM` compatibility switches in-game.

## Manual acceptance scenes

1. Survival: skeleton and creeper near the player → two top-right cards; point at a passive mob alone → its card appears; point at a sixth entity with five hostiles nearby → six sorted rows.
2. Open/close nested equipment/items menus, drag popup, release mouse → popup stops following; verify animation and profile panel at GUI scales 1–4.
3. Equip into empty/occupied armor slot, test binding armor, hotbar swaps, changed item while popup is open, full inventory, and drop cancellation → no unintended cursor stack or item loss.
4. Craft unlocked 2×2 recipe, disconnect while waiting, open another container, test full inventory and stale result → no action in another world/container.
5. Receive party invitation with menu closed, reopen, accept/decline/expire; verify party HP rows and reconnect cleanup.
6. Add/rename/remove friend, restart, compare online state by UUID. Toggle custom font and inspect English plus Chinese text and layout widths.
