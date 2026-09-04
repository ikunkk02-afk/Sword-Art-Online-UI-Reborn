# Original SAOUI Fidelity Parity

This file is the acceptance checklist for the Original UI Fidelity Reset. The visual authority is `origin/1.16.5`; `origin/2.0-1.12-features` is used only for missing or ambiguous behavior, and `origin/2.0-1.19.4-port` is architecture guidance only.

Status meanings:

- `[EXACT]`: the original value or behavior is directly evidenced and the 1.21.1 implementation reproduces it.
- `[ADAPTED]`: Minecraft/Fabric/API or a demonstrated legacy defect required a modern implementation, while the evidenced visual intent is retained.
- `[PARTIAL]`: only the evidenced subset is restored.
- `[UNKNOWN]`: no sufficient original implementation evidence exists. It must not be filled by visual invention.

`DEFERRED / NO ORIGINAL EVIDENCE` is deliberately stronger than “approximately implemented.” It means that the production theme leaves the unsupported visual absent or delegates to vanilla.

## Screen and menu parity checklist

| Component | Original Source | Original Asset | Original Position | Original Size | Original Child Offset | Original UV | Original Animation | Original Sound | 1.21.1 Implementation | Parity Status |
|---|---|---|---|---|---|---|---|---|---|---|
| Ingame menu root | `screens/menus/IngameMenu.kt`; `screens/CoreGui.kt` | none | `(width / 2 - 10, (height - elementCount * 20) / 2)` | root transform, no panel | all descendants relative to root | none | no 1.16.5 entrance transition | `orb_dropdown` on init | Relative legacy element tree; no fixed backdrop/action panel | `[EXACT]` geometry; `[ADAPTED]` wall-clock/root safety |
| Top-level categories | `IngameMenu.kt`; `IconElement.kt` | `menu/icons/{profile,social,message,navigation,settings}.png` | `(0, 25 * index)` | visible `19×19`; hitbox `20×20`; icon `16×16` | child X `25`; child Y separator `20` | `sao/gui.png (1,26,19,19)` on logical `256×256`; whole icon `64×64` sampled to `16×16` | none | open `menu_popup`; same-button close `dialog_close` | Five original category icons, original ordering and state transitions | `[EXACT]` |
| Hierarchical expansion | `CategoryButton`; `Elements.kt`; `Extensions.kt` | child icon assets | relative to current parent, never a fixed right panel | per child kind | label descendants at `actualWidth + 5` | child-specific | first item immediate, then every 3 legacy units | open/close sounds follow original mouse paths | True recursive tree; same-button toggle; click-out closes prior branch | `[ADAPTED]` (safe keyboard/legacy drift fixes) |
| Child centering | `IconElement.drawChildren()` | none | `parent + (childrenXOffset, -centering)` | at most seven listed rows | `centering=((c+c%2-2)*20)/2`; subsequent Y `+20` | none | visible siblings snap to recomputed positions as in source | none | Formula is used directly for every visible window | `[EXACT]` |
| Seven-item window | `IconElement.childrenOrderedForRendering()` | none | cyclic window around selection/scroll | seven listed children maximum | initial `scroll=-3` | none | first/last row alpha halved for a seven-row window | none | Cyclic seven-row window, wheel scroll, selected row centered | `[ADAPTED]` — original highlighted branch accidentally returned more than seven; cap is enforced |
| IconLabelElement | `IconLabelElement.kt`; `Elements.kt` | `textures/slot.png` plus original icon | relative row | minimum `84×18`; sibling rows equalized to widest | descendant X `width+5` | slot `(0,40,84,18)` on `256×256`; icon whole texture to `16×16 @ +1,+1` | hidden→visible opacity `0→1`, duration `4f`, linear | inherited Category behavior | Original slot slice and measured Minecraft-font width | `[EXACT]` geometry/UV; `[ADAPTED]` 100ms monotonic time |
| Dynamic label width | `IconLabelElement.idealSize`; `Elements.plusAssign` | `slot.png` | unchanged | `max(26 + font.width(label), 84)`; siblings use common maximum | `width+5` | same slot slice stretched only horizontally | none | none | Computed after localization with current Minecraft font | `[EXACT]` |
| Menu tree | `themes/sao/menu.xml`; runtime `IngameMenu.getDefaultElements()` | original menu icons | rooted under five categories | original element metrics | recursive | original icons/slot | original category sequencing | original menu sounds | PROFILE→SKILLS/profile; SOCIAL→GUILD/PARTY/FRIEND; MESSAGE; NAVIGATION; SETTINGS→OPTIONS/MENU/LOGOUT | `[ADAPTED]` — runtime/XML conflicts and removed SAOMCLib integrations are recorded below |
| Disabled/WIP state | `menu.xml`; `IngameMenu.kt` | original icons | original tree positions | original element metrics | n/a | original | no invented transition | none | SKILLS, GUILD, PARTY, FRIEND, MESSAGE and NAVIGATION remain disabled where no modern backend exists | `[ADAPTED]` compatibility; original grey state `[EXACT]` |
| Menu hover/focus | `IconElement.kt`; `IconLabelElement.kt`; `style.css` | `sao/gui.png`, icon assets | element bounds | unchanged | n/a | same | no invented glow | none | normal `#FFFFFF/#888888`, hover `#C99B13/#FFFFFF`, disabled `#7C7C7C/#FFFFFF`; every non-focus element (including closed children and Profile content) uses `opacity/2` | `[EXACT]` color/focus opacity; glint `[PARTIAL]` |
| Mouse-over glint | `IconElement.MOUSE_OVER_EFFECT`; legacy timing helper | enchanted-item glint | clipped to hovered element | element bounds | n/a | Minecraft glint texture | two passes, scale 8, rotations -50/+10; legacy clock is defective/static | none | No invented border; base hover colors retained | `[PARTIAL]` — safe exact glint parity is not established |
| Root move | `INeoParent.move`; `Animation.kt`; `BasicAnimation.kt`; `Animator.kt` | none | `destination += delta` | n/a | nested branch width | none | duration `10f`, cubic Bézier `(0.86,0,0.07,1)` | none | 250ms wall-clock Bézier tween; opening/closing a top category animates mouse-displaced X/Y back to destination and only an applied nested shift is reversed | `[ADAPTED]` — matches observed speed and fixes documented rightward-drift defect |
| UI movement/parallax | `CoreGui.kt`; `OptionCore.UI_MOVEMENT` | none | root accumulates mouse delta `×0.25` | n/a | entire tree | none | per mouse movement | none | yaw/pitch offset `mouse-from-center ×0.125°`; view restored on close/world change | `[ADAPTED]` — safe lifecycle restoration added |
| Profile content | `ProfileElement.kt`; `IconElement.drawChildren()` | `menu/parts/profilebg.png`; `sao/gui.png`; armor icons | declared `(-190,-153)`; after the one listed SKILLS row leaves `(+25,+20)` on the child matrix, final offset is `(-165,-133)` from PROFILE | `165×256`; entity size `40` | non-listed, so it consumes no row/cap but receives the accumulated child transform | profile `(0,0,165,256)` on logical `256×256` (the PNG is 2×); shadow `(200,85,56,30)` | original `move()` cancels animation | none | Panel/shadow/name/stats stay in Background pass and entity stays in Draw pass; half-alpha panel with opaque `#555555` non-focus text, mouse-height shadow, mount-first model choice, and view deltas (`screenWidth/3.5`, `20`) are restored; modern stats substitute unavailable fields | `[PARTIAL]` — old PlayerStats/SAOMCLib capability fields are unavailable |
| Popup composition | `screens/util/Popup.kt` | `menu/parts/alertbg.png`; confirm/cancel icons | starts at `(width/2,height/2)`; after first mouse movement follows full mouse delta until release resets tracking | base `220×160`, dynamic text height | buttons evenly divided; two-button X `-64,46`; one-button X `-9` | alert bands: `(0,0,256,64)`, `(0,64,256,32)`, `(0,96,256,32)`, `(0,128,256,32)`, `(0,160,256,96)` | open `20f` easeInQuint; close `10f` linear | open `message`; close `dialog_close` | GuiGraphics band composition, source mouse movement, dynamic expansion, icon-only `19×19` buttons | `[ADAPTED]` — 500/250ms wall clock and modern Screen lifecycle |
| PopupYesNo | `PopupYesNo` | confirm/cancel icons | popup button formula | visible/hitbox `19/20` | n/a | icon/full; gui background slice | popup animation | `message`, `dialog_close`; `confirm.ogg` has no legacy call site | Blue confirm `#4782E3→#629DFF`; red cancel `#E34747→#FF6262` | `[EXACT]` visual tokens; `[ADAPTED]` action lifecycle |
| PopupNotice | `PopupNotice`; `IngameMenu.tick()` | confirm icon, `alertbg.png` | centered | one icon button | n/a | same | popup animation | same | Original one-button popup structure exists; the obsolete missing-SAOMCLib-server automatic trigger is not fabricated | visual component `[ADAPTED]`; old integration trigger `[PARTIAL]` |
| NotificationAlert | `screens/util/NotificationAlert.kt` | original menu icon; vanilla toast background | toast-stack position | `160×32` | icon `(6,8)`; text X `25` | legacy toast slice `(0,96,160,32)` | shown for `5000ms` | `message` on first draw | Dedicated modern Toast with original one/two-line Y positions (`12` or `7/18`) and colors | visual/geometry `[ADAPTED]` to named modern toast sprite; old Party event producers `[PARTIAL]` |
| CraftingAlert | `screens/util/CraftingAlert.kt` | vanilla toast/item icon | source is entirely commented out | source is entirely commented out | source is entirely commented out | source is entirely commented out | source is entirely commented out | commented source references `message` | No production implementation is invented from inactive code | `[UNKNOWN]` / `DEFERRED / NO ORIGINAL EVIDENCE` |
| Death UI | `screens/ingame/DeathGui.kt` | `hud/buttons/death.png` | centered | fixed `280×100` | none | full original texture (legacy source call used `256×256` region) | black fade `counter/40` true client ticks | none | No title/cause/score/modern buttons; click or Escape confirms; modern safe respawn/disconnect | `[ADAPTED]` lifecycle; evidenced visual/position `[EXACT]` |
| Inventory | `screens/menus/InventoryGui.kt`; `ContainerElements.kt`; `CraftingElement.kt` | `profilebg.png` is evidence for ProfileElement, not a completed inventory screen | active original layout absent/commented | unknown | unknown | unknown | unknown | unknown | Vanilla `InventoryScreen` and `InventoryMenu` retained without invented SAO panels/chrome | `[UNKNOWN]` / `DEFERRED / NO ORIGINAL EVIDENCE` |
| Title screen | 1.16.5 `MainMenu` contains no complete custom layout | logo asset only | no authoritative layout | unknown | unknown | logo full asset | unknown | unknown | Existing accessibility/entry surface retained as a modern compatibility screen with no claim of original fidelity | `[ADAPTED]` / `MODERN COMPATIBILITY` |
| Generic SaoUiStyle / SaoIconButton | modern compatibility implementation; no 1.16.5 universal widget equivalent | original assets only where explicitly selected | theme-defined compatibility layouts | theme-defined compatibility sizes | n/a | per modern screen | formal legacy menu/popup/death bypass generic transitions | vanilla widget sounds | Kept only for compatibility screens; it is not the IngameMenu element model | `[ADAPTED]` / `MODERN COMPATIBILITY` |
| Exact vanilla screen routing | modern port architecture; current compatibility contract | none | n/a | n/a | n/a | n/a | none | none | Only exact vanilla title/pause/inventory/death classes are replaced; arbitrary mod subclasses remain untouched | `[ADAPTED]` |

## HUD parity checklist

Definitions used below:

- `N = font.width(player scoreboard name)`
- `U = (1 + (N + 4) / 5.0) * 5`, algebraically `N + 9` for integer font widths
- `T = ceil(current health)` text and `H = font.width(T)`
- `L = font.width(Component.translatable("displayLvShort", level))`

| Component | Original Source | Original Asset | Original Position | Original Size | Original Child Offset | Original UV | Original Animation | Original Sound | 1.21.1 Implementation | Parity Status |
|---|---|---|---|---|---|---|---|---|---|---|
| HEALTH_BOX | `themes/sao/hud.xml`; `HudDrawContext.kt`; `IngameGUI.kt` | `textures/sao/gui.png` | root `(2,2)`; HP bar local `(18+U,3)`; text panel `(U+132,12)` | frame `16+U+234` by 15; HP fill `215×9`; value panel `H+10×13` | dynamic U/H chain | frame `(0,0,16,15)`, stretch `(16,0,5,15)`, tail `(21,0,234,15)`, fill `(0,188,215,9)`, caps `(60,15)`/`(70,15)` | health recurrence factor `gameTimeDelay(partial)*0.075`; full health snaps; no ghost | none | Dedicated legacy resolved element computes text metrics each frame and draws original slices | `[ADAPTED]` renderer/time/data boundary; geometry/UV `[EXACT]` |
| FOOD | `hud.xml`; `HudDrawContext.kt` | `sao/gui.png` | absolute `(20+U,12)` inside health group | output `113×ratio ×2` | follows U | `(0,193,115×ratio,2)` compressed to output 113 | decline snap; recovery uses legacy smoothing when enabled | none | Drawn inside legacy health group after AIR; rotten tint supported | `[ADAPTED]` state mapping; geometry/UV `[EXACT]` |
| AIR | `hud.xml`; `StatusEffects.getEffects()` | `sao/gui.png` | absolute `(20+U,5)` inside health group | `215×ratio ×9` | follows U | `(0,188,215,9)` | none | none | Original `isInWater && air < maxAir` WET predicate; tint `#802ADDF5`; no fade/slide | modern field mapping `[ADAPTED]`; predicate/geometry/UV `[EXACT]` |
| EXPERIENCE | `hud.xml`; language `displayLvShort`; vanilla experience visibility | `sao/gui.png` | `(U+144+H,14)` | `L+10×13` | follows U and health text width H | left `(65,15,2,13)→5×13`; middle `(66,15,5,13)→L×13`; right `(78,15,3,13)→5×13` | none | none | One localized `LV %d` label with dynamic panel width, drawn only in a game mode that exposes experience; XML string-before-background order retained | `[EXACT]` geometry/UV/text rule; modern game-mode predicate `[ADAPTED]` |
| HOTBAR | `hud.xml`; `IngameGUI.kt` | `sao/gui.png` | vertical root `(screenW-24,screenH/2)`; rows `y=-99+22i`; offhand `y=121` | slot `20×20`; item offset `(+2,+2)` | 22 vertical | `(0,25,20,20)` | selected slot switches instantly; vanilla item-pop only | none | Vertical hotbar uses replacement tint, not stacked or eased selection; vanilla item render preserved | vertical geometry `[EXACT]`; item/pop `[ADAPTED]`; horizontal option `[PARTIAL]` |
| CROSS_HAIR | `hud.xml`; `IngameGUI.renderCrosshair()` | `sao/gui.png` | screen center | `1×1` | none | `(0,0,1,1)` | none | none | Theme dot replaces vanilla crosshair | element `[EXACT]`; duplicate-draw legacy bug fix `[ADAPTED]` |
| ARMOR | no SAO `hud.xml` entry; `IngameGUI.renderArmor()` delegated only to the theme | none | no drawn component | no drawn component | none | none | none | none | Empty formal part suppresses the vanilla row without inventing SAO armor art | no-output behavior `[EXACT]`; graphical design `[UNKNOWN]` / `DEFERRED` |
| EFFECTS | `hud.xml`; `StatusEffects.kt` | `sao/status_icons/*.png` | `(U+248,2)` | icons `16×16` | X step 11 (5px overlap) | whole physical `64×64` icon with logical `16×16` sampling | none | none | Dynamic U placement, source-order mapped icons/player states, no entry/part animation; effects without an original mapping are omitted | `[ADAPTED]` modern effect/state lookup; geometry/order `[EXACT]` |
| MOUNT_HEALTH | absent from 1.16.5 and 1.12 SAO `hud.xml`; `IngameGUI.renderHealthMount()` delegated only to the theme | none | no drawn component | no drawn component | none | none | none | none | Empty production part preserves the evidenced no-output result without cloning player-health artwork | no-output behavior `[EXACT]`; graphical design `[UNKNOWN]` / `DEFERRED` |
| JUMP_BAR | `hud.xml`; legacy vanilla HUD call | legacy `icons.png`; modern equivalent HUD sprites | `(screenW/2-91,screenH-29)` | `182×5` | none | legacy y 84 background/y 89 progress | none | none | Same geometry/fill direction using 1.21.1 jump-bar sprites; no fade/slide | `[ADAPTED]` asset/API |
| ENTITY_HEALTH_HUD | `hud.xml`; `IngameGUI.kt`; `RenderCapability.kt` | `sao/entities.png` | root `(screenW-20,35)` | max five; row 15; background `80×15`; foreground `79×ratio ×14 @ y+1.5` | Y step 15 | logical atlas `256×256`: bg `(1,30,255,30)`, fg `(1,0,255,30)`; fixed XML fill `#F40000`; frame overlays fill | none | none | Right-edge card, right-to-left fill, source AABB (`±10/±5/±10`), nearest-five then health-sorted modern KILLER-equivalent nearby candidates, no invented first-person gate, name at `-font.width(name)-5`; the populated legacy target field is not drawn because active `sao/hud.xml` never references it | `[ADAPTED]` candidate capability mapping; geometry/color/order/UV `[EXACT]` |
| PARTY | original SAOMCLib-backed HUD branch | original party/player assets and remote data | unavailable without backend | unavailable | unavailable | unavailable | none established for current backend | none | Empty production part | `[PARTIAL]` / dependency unavailable |

## Reference constants

Formal screen/menu values are centralized in `LegacySaoMetrics`; formal HUD coordinates, dimensions, UVs, colors, and thresholds are centralized in `LegacySaoHudMetrics` or remain declarative in `hud.json`. Each constant group names its `origin/1.16.5` source. Modern compatibility-only title/widget spacing is deliberately kept out of both legacy constant sets.

## Evidence-backed timing conversion

The legacy `Animation.duration` comments call their unit a tick. In the 1.16.5 implementation, however, `Animator.tick(ClientTickEvent)` does not filter event phase. Forge emits both START and END client tick phases, so the observed counter advances at approximately 40 units per second. The fidelity conversion therefore uses:

| Legacy value | Observed 1.16.5 duration | Modern value | Status |
|---:|---:|---:|---|
| `3f` scheduled child interval | about 75ms | 75ms monotonic interval | `[ADAPTED]` |
| `4f` label opacity | about 100ms | 100ms linear | `[ADAPTED]` |
| `10f` root move / popup close | about 250ms | 250ms | `[ADAPTED]` |
| `20f` popup open | about 500ms | 500ms | `[ADAPTED]` |

This preserves normal-runtime visual speed. It is not called `[EXACT]` because the old counter slowed with client ticks while the modern monotonic clock remains stable.

## `menu.xml` migration and conflicts

The XML is retained as declarative evidence, but the 1.16.5 runtime did not load it and its hard-coded `getDefaultElements()` conflicts with it in a few places. The migration rule is source-first when runtime behavior is explicit:

| XML/runtime item | Migration |
|---|---|
| PROFILE / SOCIAL / MESSAGE / NAVIGATION / SETTINGS order | reproduced exactly |
| PROFILE → SKILLS plus non-listed ProfileElement | reproduced; Skills stays runtime-disabled |
| SOCIAL → GUILD / PARTY / FRIEND | reproduced; unavailable SAOMCLib actions stay disabled |
| MESSAGE and NAVIGATION | reproduced as disabled/WIP |
| SETTINGS → OPTIONS / MENU / LOGOUT | reproduced |
| XML `OPTIONS` icon token | mapped to the actual `option.png`/runtime `OPTION` enum; `[ADAPTED]` |
| Runtime `LOGOUT` action | direct safe disconnect/world-exit flow; no invented confirmation popup |
| Runtime item-filter EQUIPMENT/ITEMS categories | deferred because the old external item-filter registry is absent; `[PARTIAL]` |
| Runtime unreachable NAVIGATION→QUEST child | not presented while its parent is disabled; recorded `[PARTIAL]` |
| Modern Stats/Advancements/Accessibility entries | not inserted into the original top-level tree; vanilla options/pause screens remain reachable through compatibility entries |

## `style.css` static migration

The following `RRGGBBAA` CSS values are converted to modern `AARRGGBB` without changing channels:

| State | Original | Modern token |
|---|---|---|
| default background / text | `#FFFFFFFF` / `#888888FF` | `#FFFFFFFF` / `#FF888888` |
| hover background / text | `#C99B13FF` / `#FFFFFFFF` | `#FFC99B13` / `#FFFFFFFF` |
| disabled background / text | `#7C7C7CFF` / `#FFFFFFFF` | `#FF7C7C7C` / `#FFFFFFFF` |
| confirm / confirm hover | `#4782E3FF` / `#629DFFFF` | `#FF4782E3` / `#FF629DFF` |
| cancel / cancel hover | `#E34747FF` / `#FF6262FF` | `#FFE34747` / `#FFFF6262` |
| popup background/title | `#BBBBBBFF` / `#555555FF` | `#FFBBBBBB` / `#FF555555` |
| death / hardcore death | `#C94141FF` / `#990000FF` | `#FFC94141` / `#FF990000` |
| HP very-low/low/very-damaged/damaged/okay/good/creative | CSS HP classes | `#FFBD0000`, `#FFF40000`, `#FFF47800`, `#FFF4BD00`, `#FFEDEB38`, `#FF93F43E`, `#FF4CEDC5` |

The CSS provides no spacing, padding, font size, or animation duration. Such values are not attributed to it. Generic title/container layout tokens remain `[ADAPTED]` modern compatibility values, and formal legacy menu/popup/death geometry bypasses those generic tokens.

## Asset and sound audit

The shipped legacy menu icons, `slot.png`, `sao/gui.png`, `sao/entities.png`, `menu/parts/alertbg.png`, `menu/parts/profilebg.png`, `hud/buttons/death.png`, and six OGG files are byte-identical to `origin/1.16.5`. No icon is regenerated, recolored into a new visual family, substituted with Unicode, or independently scaled above its original logical display size.

Sound call-site status:

| Sound | Evidenced use | Status |
|---|---|---|
| `orb_dropdown.ogg` | IngameMenu init | `[EXACT]` trigger |
| `menu_popup.ogg` | mouse-open Category | `[EXACT]` trigger |
| `dialog_close.ogg` | same-button Category close and Popup close | `[EXACT]` trigger |
| `message.ogg` | Popup open; legacy notifications/mentions | Popup `[EXACT]`; notification integration `[PARTIAL]` |
| `confirm.ogg` | registered but no production call site in 1.16.5/1.12 | `[UNKNOWN]` / intentionally unused |
| `particles_death.ogg` | legacy particle subsystem, not the DeathGui click | `[PARTIAL]` / intentionally not attached to death confirmation |

## Manual acceptance checklist

- Open the pause menu at several GUI scales and verify the root begins at the original center formula with five 19px icons at 25px intervals and no large panel.
- Toggle the same category, switch siblings, open nested SETTINGS children, and verify rightward tree expansion, focus alpha, original slot backgrounds, dynamic widths, and staged reveal.
- Use a localization with longer labels and verify sibling widths equalize and the next level begins five pixels after the actual row width.
- The stock fidelity tree has no enabled branch with seven items. When a future evidenced integration supplies one, verify the implemented cyclic window, centered selection, wheel direction, and half-alpha edge rows; no debug-only menu was added for this reset.
- Verify menu-open, category-open, same-category-close, popup-open, and popup-close sounds are distinct and no generic confirm sound was invented.
- Move the mouse across the menu, then close it; verify subtle root/view movement and exact view restoration.
- Open OPTIONS, the vanilla MENU compatibility entry, and LOGOUT; verify ESC/parent returns and disconnect behavior.
- When an evidenced caller opens a Popup, verify band composition, one/two icon placement, expansion, close squeeze, hover colors, and text with one and multiple lines. Logout intentionally does not invoke one.
- Open inventory and validate every vanilla slot, recipe book, drag, shift-click, armor, offhand, and crafting interaction; expect vanilla visuals until original inventory evidence exists.
- Die in normal and Hardcore worlds; verify fixed 280×100 art, two-second black fade, no invented text/buttons, click/Escape action, respawn, and safe world exit.
- Compare HEALTH/FOOD/AIR/LEVEL at short and long player names, absorption, multiple GUI scales, creative, hunger, and underwater states.
- Verify the hotbar is vertical, selection changes instantly, item-pop remains, offhand is at the original location, and no duplicate selected frame appears.
- Verify effects begin at the name-derived X coordinate and overlap by five pixels with no fade/slide.
- Verify mount riding does not show an invented cloned player-health panel.
- Verify jump bar placement and fill, center dot, five right-aligned entity rows, right-to-left entity fill, and 256 logical entity atlas sampling.

No Minecraft client, screenshot automation, GameTest, or new JUnit test is part of this fidelity reset. Runtime visual acceptance remains a manual user step.
