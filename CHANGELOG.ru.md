# Changelog

## 0.1.2+26.2 — порт на Minecraft 26.2 (25.10.2026)

**RU**

Линия 0.1.x (участки, разрезы, виды карты, оформление Aell UI / Vanilla / Vanilla+, понятное окно на старом Fabric Loader)
под Minecraft 26.2 (Litematica 0.28.8, malilib 0.29.6). Код — дерево 26.3 (= подготовленная 0.1.2), описание и переводы — из jar 0.1.2.
Код — дерево 26.3 без изменений: API 26.2 в местах, которые трогает Zone Focus, совпадает с 26.3; `aell-ui` берёт
замены из `../aell-ui-26.2`.

Material HUD для 26.2 пока отдаёт API 5: превью участка из нескольких схем, ручки разрезов в 3D и ползунок среза
(API 6) не включаются — Zone Focus сам это проверяет (`MaterialHudCompat.hasSectionPreview()`) и показывает
«3D — для участка из одной схемы», как до 0.1.0. Компилируется против jar MHUD 1.0.21+26.3 (сигнатуры API 6); что все
остальные ссылки есть в MHUD 26.2, проверено скриптом `~/projects/_port-1.21.11-26.2/mhudrefs.py`.

Проверено (25.10.2026), харнесс Material HUD 26.2 (копия стенда `Beta FIX/_port-mhud-stand-26.2`, тесты ZF перенесены из стенда 26.3): `zonelib`, `zfviews`, `zfverify`,
`zonesshot`, `zonecard` — OK, `zoneplace` — «всё прошло», `zfsections` — всё, кроме трёх проверок шага 10 (ручки
разрезов в 3D, ползунок среза — это API 6).

**EN**

The 0.1.x line (sections, cuts, map views, Aell UI / Vanilla / Vanilla+ look) for Minecraft 26.2. The multi-schematic
section preview, 3D cut handles and the height slider need Material HUD API 6, which Material HUD for 26.2 doesn't have yet.

## 0.1.2+26.3 — 23.10.2026

**RU**

**fix — понятное окно вместо вылета на старом Fabric Loader.** Zone Focus собран на Fabric Loader 0.19.5, а в `fabric.mod.json` нижней границей стояло `>=0.18.2`. На 0.19.3 (там MixinExtras 0.5.4) миксин на рендер схем Litematica падал с `ClassCastException`, и игра вылетала при входе в мир — в отчёте виноватым выглядел Voxy или Litematica. Теперь нижние версии в `depends` берутся из `gradle.properties` при сборке (Fabric Loader, Fabric API, malilib, Litematica — те, на которых мод собран), и на старом загрузчике Fabric до запуска игры показывает окно «Replace mod 'Fabric Loader' 0.19.3 with version 0.19.5 or later». Проверено на голых стендах `_smoke-zf-26.2` / `_smoke-zf-26.3`: старая версия на 0.19.3 — тот же вылет, новая на 0.19.3 — окно Fabric, новая на 0.19.5 — вход в мир и все экраны Zone Focus без ошибок.

**EN**

**fix — a clear message instead of a crash on an old Fabric Loader.** Zone Focus is built on Fabric Loader 0.19.5 but declared `>=0.18.2`. On 0.19.3 (MixinExtras 0.5.4) the mixin into Litematica's schematic renderer failed with a `ClassCastException` and the game crashed on world join, with Voxy or Litematica blamed in the report. The minimum versions in `depends` (Fabric Loader, Fabric API, malilib, Litematica) now come from the versions the mod is built with, so an old loader gets Fabric's "Replace mod 'Fabric Loader' 0.19.3 with version 0.19.5 or later" window before the game starts.

## 0.1.1+26.3 — 22.10.2026

**RU**

**new — оформление: Aell UI, Vanilla или Vanilla+.** Настройки Zone Focus → «Подсчёт» → «Стиль интерфейса». Vanilla и Vanilla+ переводят окна Zone Focus на вид Minecraft: экран зон, управление зонами с картой, разбиение участка, кнопки инструментов, подсказки клавиш — ванильные кнопки, поля, рамки и щелчок кнопки Minecraft; экран настроек — плитками в две колонки (Vanilla) или строками, как у Litematica (Vanilla+). При «Aell UI» и установленном Material HUD 1.0.19+ берётся его оформление — Zone Focus рядом с ним в той же теме.

**EN**

**new — Look: Aell UI, Vanilla or Vanilla+.** Zone Focus settings → Counting → Interface style. Vanilla and Vanilla+ switch the Zone Focus windows to Minecraft's own look: the zones screen, zone manager with its map, section split, tool buttons and key hints get vanilla buttons, fields, frames and the Minecraft click sound; the settings screen is tiles in two columns (Vanilla) or rows like Litematica's (Vanilla+). With "Aell UI" and Material HUD 1.0.19+ installed, Zone Focus uses Material HUD's look.

## 0.1.0+26.3 — не выпущено (20.10.2026)

**RU**

**new — 3D для любого участка.** С Material HUD 1.0.18 (API 6) 3D-превью «Управления зонами» показывает участок из любого числа схем — модель снимается с мира схем Litematica, где схемы уже сложены с поворотами и отражениями. Раньше 3D было только у участка из одной схемы. Со старым Material HUD — как раньше.

**new — резка мышью с шагом в блок, в 2D и в 3D.** На карте линии разрезов тянутся мышью, в 3D у каждой плоскости разреза — ручка; плоскость ходит ровно по блоку, между соседними разрезами. ПКМ по карте или щелчок ПКМ по модели — «Разрезать здесь: X = …» по блоку под курсором, у наведённого разреза — «Убрать разрез». Участок переходит на правило «разрезы» (сетка с произвольными плоскостями); число зон то же — активная зона и скрытия остаются. Резать можно применённую сетку одной схемы, «Общего габарита» или уже разрезы.

**new — карта 2D сверху, спереди и сбоку.** Кнопки «Сверху / Спереди / Сбоку» над картой; срезы карты идут по оси глубины (Y, Z или X), разрезы двигаются в любой плоскости. В 3D к «Изометрии / Сверху / Спереди» добавился вид «Сбоку».

**new — срез по высоте в 3D.** Ползунок «Срез» внизу превью, как в библиотеке Material HUD: блоки выше выбранного слоя не рисуются — видно, что внутри.

**new — мир Voxy под голограммой.** Если стоит Voxy (отдельно или во VoxyMap Bobby), в 3D под моделью участка рисуется живой мир той же камерой — без кнопок и настроек. Нет Voxy, Vulkan или свой шейдерпак Iris — превью как раньше.

**new — участки: несколько схем, у каждой своё разбиение.** Раскладка мира теперь состоит из участков; участок — это одна или несколько схем плюс правило разбиения на зоны (сетка, подрегионы или зоны вручную). Замок можно разбить сеткой 3×2×3, мост — по подрегионам, башню — одной ручной коробкой, и всё это живёт рядом. Старая «одна раскладка на мир» осталась как частный случай — участок один.

**new — группа схем с «общим габаритом».** В окне участка при двух и более отмеченных схемах переключатель «Каждая отдельно / Общий габарит»: четыре схемы, поставленные встык, можно резать одной сеткой по общему габариту, как одну постройку. «Каждая отдельно» режет каждую схему по её собственному габариту (зоны идут схема за схемой). Общий габарит едет вместе с первой схемой участка.

**new — окно «Новый участок» (и «Разбить заново»).** Слева — включённые схемы Litematica с флажками (имя, размер, файл; у занятых пометка «в участке №N» — отметка переносит схему из того участка, строка под списком об этом предупреждает), справа — способ («Число зон» / «Размер зоны»), сетка по осям, «Подрегионы = зоны», «Из выделения Litematica», имя участка. Если отмеченные схемы наложены друг на друга, жёлтая строка предупреждает: в пересечении блоки обеих схем считаются и видны сквозь скрытие. «Разбить заново…» в меню участка открывает то же окно и перезаписывает его правило и состав.

**new — полоса участков в «Управлении зонами».** Над левой панелью — список участков: номер, имя, число схем, правило, прогресс («[2] Замок · 2 схемы · сетка 3×2×3 · 64%»; серым — схемы не найдены, «не хватает схем: N» — найдены не все). Щелчок — сделать участок активным; ПКМ — «Переименовать», «Скрыть участок / Показать участок», «Разбить заново…», «Удалить участок», «Очистить всё»; кнопка «+ Участок» открывает окно создания. Пока участок один, список сворачивается в строку-заголовок. Карта, список зон и 3D работают с активным участком; 3D — когда в участке ровно одна схема. «Отменить разбиение» теперь называется «Отменить разбиение участка» и убирает только активный участок; «Очистить всё» (второй щелчок подтверждает) — всю раскладку.

**new — остальные участки не мешают в мире.** Новые настройки (вкладка «Зоны», раздел «Участки»): «Скрывать остальные участки» (`hideOtherSections`, по умолчанию вкл) — голограммы схем из других участков не рисуются, выключено — показываются целиком; «Скрывать схемы вне раскладки» (`hideOutsideSchematics`, по умолчанию выкл) — схемы, не входящие ни в один участок, остаются как в обычной Litematica, пока не включишь. Скрытый участок (меню участка) не рисуется вовсе, даже если он активен.

**new — иерархия «участок → зона».** В HUD подпись зоны при нескольких участках — «У2 · З7/24» (У — участок, З — зона, П — подрегион; «У2 · пуст» и «У2 · скрыт» для пустого или скрытого активного участка); при одном участке — как раньше, «Зона 7/24». В мире подпись активной зоны — «Участок 2 · Зона 7/24». Если активный участок пуст или скрыт, активность сама переходит на ближайший живой.

**new — клавиши «Следующий участок» и «Предыдущий участок»** (`nextSection`, `prevSection`, по умолчанию не заданы) — по не скрытым участкам. Настройка «Следующая зона — через участки» (`cycleAcrossSections`, по умолчанию выкл): «Следующая зона» после последней зоны участка переходит к первой зоне следующего.

**new — «Рамки других участков»** (`showOtherSections`, вкладка «Отрисовка», по умолчанию выкл): тонкие рамки зон остальных участков.

**new — на главном экране** (`Ctrl + J`) чипы «Участки: 3 · активен №2» (имена — в подсказке) и «Схемы: N»; кнопки «Подрегионы = зоны» и «Из выделения» работают с выбранной в Litematica схемой и называют её участок. «Сбросить зону» убирает активный участок, «Сбросить всё» — всю раскладку (оба — вторым щелчком).

**Сообщения в чат по умолчанию выключены.** Новая опция «Сообщения в чат» (`chatMessages`, вкладка «Подсчёт», раздел «Модуль»), по умолчанию выкл. Отклик на действия — инструмент разметки вкл/выкл, «инструмент Axiom активен», итог проверки зоны, клавиша проверки — при выключенной опции идёт строкой над хотбаром (`ZoneChat.notice`), при включённой — в чат, как раньше. До этого опции не было и сообщения шли в чат всегда, поэтому переноса старых настроек не нужно. Строки опции — на всех 12 языках. (Была записана как «Не выпущено 0.0.9»; 0.0.9 отдельно не выйдет — пункт вошёл сюда.)

**Что изменилось внутри.**

- **Новый формат `config/zonefocus_zones.json` (версия 2).** Запись мира — `{version: 2, layouts: [...], active_layout}`; участок хранит `id`, имя, схемы (по `id` размещения Litematica, имя и файл запасные), правило, активную и скрытые зоны, `bounds` и общий габарит. Старый файл читается как раскладка из одного участка: сетка и подрегионы — с привязкой к схеме по имени (`id` дописывается при первом совпадении), ручная зона — со своей базой и сдвигом. Перед первой перезаписью файла со старой записью мод один раз кладёт копию `zonefocus_zones.json.bak-0.0.9`. Откат на 0.0.9 участков не увидит, а его сохранение может стереть запись мира — копия на этот случай.
- **Объём зоны считается в одном месте.** Счёт для Material HUD, прогресс зон и собственная проверка берут объём из одного класса (`ZoneVolume`): пересечения коробки зоны с подрегионами схем участка; подрегионы одной схемы, наложенные друг на друга, не считаются дважды.
- **Подписи для Material HUD видят вторую схему.** Подпись объёма (`getSignature`) теперь включает все схемы участка (id, положение, поворот, отражение), скрыт ли участок и пуст ли объём — Material HUD пересчитывает счёт при включении, сдвиге и выключении схем участка, при скрытии и показе активного участка. Сдвиг схемы другого участка подпись не меняет.
- **Мост с Material HUD: `bridgeApi()` = 2.** Все прежние методы `ZoneFocusBridge` и `ZoneActions` сохранили имена и сигнатуры. «Сбросить зону» в Material HUD убирает активный участок (при одном участке — как раньше, всю раскладку). Новые методы: `sectionCount`, `activeSection`, `sectionNames`, `sectionSummary(i)`, `sectionZoneBoxes(i)`, `sectionZoneProgress(i)`, `setActiveSection(i)`, `setActiveZone(section, zone)`, `fileInGroup(path)`. Файловые методы библиотеки Material HUD (`fileZoneGrid`, `fileZoneBoxes`, …) работают с одиночным участком схемы-файла; схема в групповом участке для библиотеки — «не управляется» (`null`), попытка разбить её даёт сообщение «Схема уже в групповом участке №N».
- **Обрезка схемы — по участкам.** Хук `LayerRange.getClampedRenderBoundingBox` получает и исходный бокс части схемы, и обрезанный слоями; по исходному боксу часть опознаётся геометрически (какой схеме принадлежит), и к ней применяется правило её участка. Часть, которой нет в плане (схему сдвинули между постановкой и сборкой), не рисуется и ставится на пересборку один раз. Переход внахлёст — только между соседними зонами одного участка.
- **Раздача сетки файла.** «Разбить файл в библиотеке» сетка по-прежнему раздаётся схеме сама, только если нет ни одного участка с непустым объёмом (скрытые участки тоже считаются): вторая схема не пропадёт «вдруг».
- **Юнит-тесты.** `./gradlew test` — JUnit 5 (без `fabric-loader-junit`: его слушатель не стартует под loom 26.3): 13 тестов модели и миграции формата.

**Ограничение: счёт в HUD Material HUD.** Material HUD считает материалы по всем включённым схемам внутри коробки активной зоны. Если вторая схема стоит рядом или внутри коробки зоны первой, её блоки попадут в HUD — изоляция участков действует в отрисовке и в собственной проверке Zone Focus, но не в счёте Material HUD. Устранить можно только правкой Material HUD (отдельная задача, после 0.1.0).

**fix — счёт Material HUD не обновлялся при включении второй схемы.** Подпись объёма учитывала только активную зону и одну схему; включение второй схемы в ту же коробку подпись не меняло, и Material HUD мог продолжать показывать прежние числа. Теперь подпись включает все схемы участка, и счёт пересчитывается при их включении, сдвиге и выключении.

**fix — ручная зона и сетка считали объём по-разному.** Счёт для Material HUD брал для сетки и подрегионов одну схему, а для ручной зоны — все включённые; прогресс зон и проверка всегда брали все включённые. При двух схемах числа расходились. Теперь все три берут объём из одного места.

**fix — номер активной зоны для карточки Material HUD отдавался ненормализованным.** Мост отдавал сырое число, а сам менеджер приводил номер к числу зон, поэтому при выходе номера за границы карточка и менеджер расходились. Теперь номер приводится к числу зон и там, и там.

**fix — автопереключение по положению дребезжало между наложенными зонами.** Когда игрок стоял в пересечении зон, активность могла перескакивать между ними. Теперь, если активная зона уже содержит игрока, ничего не меняется; иначе сначала смотрятся зоны активного участка, затем остальных.

**EN**

**new — 3D for any section** (Material HUD 1.0.18, API 6): the model is taken from Litematica's schematic world, so a section of several schematics shows in 3D. **new — cut with the mouse, one block per step**, on the 2D map (drag lines) and in 3D (drag plane handles); RMB — «Cut here» / «Remove cut». **new — 2D map from the top, front and side**; 3D gets a «Side» view. **new — height cut slider in 3D.** **new — Voxy world under the hologram** when Voxy is installed, no settings.

**new — sections: several schematics, each with its own split.** The world layout is now a list of sections; a section is one or more schematics plus a rule for cutting them into zones (grid, sub-regions, or manual boxes). A castle can be a 3×2×3 grid, a bridge split by sub-regions, a tower one manual box, all at once. The old single layout is just the case of one section.

**new — a group of schematics with "Combined bounds".** In the section window, with two or more schematics ticked, an "Each separately / Combined bounds" switch: four schematics placed end to end can be cut by one grid over their joint bounds, like one build. "Each separately" cuts every schematic by its own bounds. The combined bounds follow the first schematic of the section.

**new — "New section" window (and "Split again").** Enabled Litematica placements with checkboxes on the left (name, size, file; taken ones say "in section #N" — ticking one moves it from that section, with a warning line below), on the right the method, per-axis grid, "Sub-regions = zones", "From Litematica selection", the section name. When ticked schematics overlap, a yellow line warns that in the overlap the blocks of both are counted and shown through the hiding. "Split again…" in the section menu opens the same window and rewrites the rule and the members of that section.

**new — a section strip in "Manage zones".** Above the left panel: the section list with number, name, schematic count, rule and progress. Click — make it active; right click — Rename, Hide/Show section, Split again…, Delete section, Clear all; "+ Section" opens the creation window. With one section the list collapses into a header line. The map, the zone list and 3D work on the active section; 3D — when the section has exactly one schematic. "Undo split" is now "Undo section split" and removes only the active section; "Clear all" (second click confirms) removes the whole layout.

**new — other sections stay out of the way in the world.** New options (Zones tab, "Sections" group): "Hide other sections" (`hideOtherSections`, on by default) — holograms of schematics from other sections are not drawn, off — shown whole; "Hide schematics outside the layout" (`hideOutsideSchematics`, off by default) — schematics in no section stay as in plain Litematica until you turn it on. A hidden section (section menu) is not drawn at all, even if active.

**new — "section → zone" hierarchy.** With several sections the HUD label is "S2 · Z7/24" in English texts ("У2 · З7/24" in Russian) (section · zone, "empty" / "hidden" for an empty or hidden active section); with one section it stays "Zone 7/24". In the world the active zone label is "Section 2 · Zone 7/24". If the active section is empty or hidden the activity moves to the nearest live one by itself.

**new — "Next section" and "Previous section" keys** (`nextSection`, `prevSection`, unbound by default) — over visible sections. New option "Next zone crosses sections" (`cycleAcrossSections`, off by default): "Next zone" after the last zone of a section moves to the first zone of the next section.

**new — "Other sections' frames"** (`showOtherSections`, Rendering tab, off by default): thin frames of the zones of the other sections.

**new — on the main screen** (`Ctrl + J`): chips "Sections: 3 · active #2" and "Schematics: N"; "Sub-regions = zones" and "From selection" act on the Litematica-selected schematic and name its section. "Clear the zone" removes the active section, "Reset all" the whole layout (both by a second click).

**Chat messages are off by default.** New option "Chat messages" (`chatMessages`, Counting tab, "Mod" group), off by default. Feedback on actions (marking tool on/off, "Axiom tool is active", the zone check result, the check key) goes to the line above the hotbar (`ZoneChat.notice`) with the option off and to chat as before with it on. Strings in all 12 languages. (Was listed as "Unreleased 0.0.9"; 0.0.9 will not ship separately, so the item moved here.)

**Under the hood.** New `config/zonefocus_zones.json` format (version 2) with a one-time `zonefocus_zones.json.bak-0.0.9` copy before the first rewrite of an old-format file; old zones are read as one section (rolling back to 0.0.9 does not see sections, and its save may erase a world's record — the copy covers that); the zone volume is computed in one place for the Material HUD count, the progress scan and the own check; the volume signature for Material HUD now includes every schematic of the section, so the count refreshes when a second schematic is enabled, moved or disabled; the bridge to Material HUD is `bridgeApi()` = 2 with new methods `sectionCount`, `activeSection`, `sectionNames`, `sectionSummary`, `sectionZoneBoxes`, `sectionZoneProgress`, `setActiveSection`, `setActiveZone(section, zone)`, `fileInGroup` (all old methods keep names and signatures); schematic clipping works by section (hook `LayerRange.getClampedRenderBoundingBox` now sees the original part box); unit tests on plain JUnit 5.

**Limitation: the Material HUD count.** Material HUD counts materials over every enabled schematic inside the active zone box. If a second schematic stands next to or inside the box of the first one's zone, its blocks reach the HUD — section isolation works in rendering and in Zone Focus's own check, not in the Material HUD count. Fixing it takes a change in Material HUD (separate task, after 0.1.0).

**fix — the Material HUD count did not refresh when a second schematic was enabled.** The volume signature covered only the active zone and one schematic, so enabling a second schematic in the same box did not change it and Material HUD could keep showing the old numbers. It now covers every schematic of the section, so the count refreshes when they are enabled, moved or disabled.

**fix — a manual zone and a grid counted the volume differently.** The Material HUD count took one schematic for a grid or sub-regions and every enabled schematic for a manual zone, while the progress scan and the check always took every enabled one, so with two schematics the numbers disagreed. All three now take the volume from one place.

**fix — the active zone number for the Material HUD card was passed unnormalised.** The bridge passed the raw number while the manager wrapped it into the zone count, so when the number went out of range the card and the manager disagreed. It is now wrapped on both sides.

**fix — auto-switching by position jittered between overlapping zones.** With the player standing in the overlap of zones the activity could jump between them. Now, if the active zone already contains the player, nothing changes; otherwise the zones of the active section are tried first, then the rest.

## 0.0.8+26.3 / 0.0.8+26.2 — 10.10.2026

**RU**

**Версия под Minecraft 26.2.** Тот же 0.0.8 собран и под 26.2 (Litematica 0.28.8, malilib 0.29.6) — `litematica-zone-focus-26.2`, исходники Zone Focus без изменений; 3D и зоны в библиотеке — с Material HUD 1.0.6+26.2. Проверено харнессом Material HUD 26.2 (zftab, zfverify, zonelib, zfviews) и отдельно без Material HUD (экраны и все вкладки настроек).

**new — «Управление зонами» — один экран вместо трёх.** Разбиение, карта и список зон теперь на одном экране (раньше — хаб, «Разбить схему на зоны» и «Список зон»). Слева: способ («Число зон» / «Размер зоны»), сетка по осям — поля с вводом по центру, −/+, колесо (Shift — по 10), Enter; итог чипами (зон, размер зоны, блоков), «Разбить на N зон» (акцент), «Подрегионы = зоны», «Отменить разбиение» (красная, второй щелчок подтверждает). Справа: карта сверху с номером на каждой клетке (клик — активная зона; для нескольких зон по Y — срезы), список зон с прогрессом, сортировкой и скрытием. Тумблер «Показывать прогресс» в шапке (по умолчанию выключен; включили — скан запускается сам). Всё на ките aell-ui — те же кнопки и поля, что в Material HUD. Клавиша «Список зон», вкладка Zone Focus в Material HUD и старые методы моста (`openSplit`, `openList`) ведут сюда.

**new — 3D в «Управлении зонами» (с Material HUD 1.0.5).** В шапке «2D | 3D»: в 3D вместо карты — превью схемы из библиотеки Material HUD, изометрия, каркасы и номера зон в цветах мира; ЛКМ — вращать, ПКМ — двигать, колесо — масштаб, двойной щелчок — вернуть вид, щелчок по зоне — активная. С «Показывать прогресс» каркасы красятся по готовности и у номера процент. «Зоны» в углу убирают каркасы — смотреть на постройку без полос. Зоны мира переводятся в координаты схемы с учётом поворота и отражения размещения — номера совпадают с миром.

**new — виды камеры в 3D: «Изометрия», «Сверху», «Спереди».** Кнопки — над превью, справа в строке «Схема в 3D», как над превью библиотеки Material HUD; двойной щелчок по-прежнему возвращает выбранный вид.

**new — в 2D под клетками карты — снимок схемы строго сверху.** Мышью не вращается и не двигается, повёрнут и отражён, как размещение в мире, — клетки зон ложатся ровно на постройку; при нескольких срезах по высоте снимок срезан по текущему: видно, что внутри среза. Модель держит только видимое превью — 2D или 3D, второе освобождает видеопамять. С Material HUD 1.0.5.

**Качество 3D-превью — в шапке «Управления зонами», рядом с 2D / 3D** («Качество / Оптимизация», только в 3D; та же настройка, что у превью библиотеки Material HUD). В настройках «Инструмент» и «Интерфейс инструмента» — один раздел; заголовки разделов — с цветовым акцентом (aell-ui).

**Автопереключение зон по умолчанию выключено — обе опции, «По положению игрока» и «К следующей после достройки».** Зашёл в другую зону или достроил текущую — активная остаётся, пока не переключишь сам. Раньше «К следующей после достройки» была включена по умолчанию и записывалась в настройки, поэтому при обновлении обе опции выключаются один раз и у тех, у кого были включены; включить снова — раздел «Автопереключение».

**new — прогресс «блок в блок» в 3D.** С «Показывать прогресс» модель в 3D красится по сверке с миром: построенное как есть, непостроенное — голубым «призраком», чужие блоки — красным; над картой «Схема в 3D · построено N%». Для карточки «Управление зонами» во вкладке Material HUD мост отдаёт сводку: `zonePlacementName`, `zoneSummary`, `zoneBoxes`, `zoneProgress`, `setActiveZone`.

**new — разбиение схемы-файла.** Схему можно разбить в библиотеке Material HUD без размещения: сетка хранится для файла (`config/zonefocus_file_zones.json`) и встаёт на размещение этой схемы сама, если зон ещё нет; разбиение размещения сеткой запоминается и для его файла. «Отменить разбиение» убирает и то и другое. Новые методы моста `ZoneFocusBridge`: `fileZoneGrid`, `fileZoneBoxes`, `fileActiveZone`, `setFileActiveZone`, `fileZoneProgress`, `setFileZoneGrid`, `clearFileZones`, `filePlaced`, `openManagerForFile`; старые не менялись.

**new — поверх Material HUD проверка блоков — его Ctrl+B.** С Material HUD 1.0.5 (API 5) вкладки «Проверка» в настройках Zone Focus нет — она есть в Material HUD; клавиша «Проверка» (Ctrl+P) включает и выключает проверку Material HUD (по активной зоне, когда включён счёт по зоне), своя проверка Zone Focus не запускается. В «Клавишах» строка подписана «Проверка (Material HUD)». Без Material HUD или со старым (API ниже 5) всё как раньше. Конфиг проверки Zone Focus не трогается.

**fix — в настройках не назначались клавиши: щелчок по полю сам записывался как клавиша.** В Minecraft 26.3 мышь идёт через SDL, и левая кнопка — 1, а не 0, как было в GLFW; встроенный интерфейс (aell-ui) ждал 0 и принимал щелчок по полю за нажатую кнопку мыши. Пересобрано с текущим aell-ui (`InputConstants.MOUSE_BUTTON_LEFT`), как в Material HUD и Easy Place Fix+.

**«Призраки» в текстах названы голограммами, как в Litematica.** В русском переводе самой Litematica блоки схемы называются голограммами («Размещённые голограммы» в её главном меню); так теперь и у Zone Focus — в подсказке «Дальность луча», в описании на Modrinth, в документации и журнале изменений. Ключи языка и опции не менялись — настройки не сбрасываются.

**EN**

**new — «Manage zones» — one screen instead of three**: split method, per-axis fields, a top-down map with a number on every cell (click — active zone), the zone list with progress; built on the shared aell-ui kit.

**new — 3D in «Manage zones» (with Material HUD 1.0.5)**: the schematic preview with zone frames and numbers, click a zone to make it active, progress colors, a «Zones» button to hide the frames.

**new — camera presets in 3D: Isometric, Top, Front**, as above the Material HUD library preview.

**new — the 2D map shows a straight-down shot of the schematic under the zone cells**: it does not rotate or pan, it is turned and mirrored like the placement so the cells sit on the build, and with several height slices it is cut at the current one.

**Zone auto-switching is off by default — both «By player position» and «Next one when finished».** Updating turns both off once, even if they were on; turn them back on under «Auto switching».

**new — block-by-block progress in 3D**: built blocks as they are, missing ones as a blue «ghost», wrong ones red, «built N%» above the map.

**new — splitting a schematic file** without placing it (from the Material HUD library); the grid is applied to its placement automatically.

**new — on top of Material HUD 1.0.5, block verification is its Ctrl+B**: the "Verification" tab is gone and Ctrl+P toggles Material HUD's check.

**fix — keys could not be bound in the settings: the click on the field was recorded as the key.** On Minecraft 26.3 the left mouse button is 1 (SDL), not 0; the bundled UI now compares with `InputConstants.MOUSE_BUTTON_LEFT`.

**Russian texts call schematic ghost blocks "голограммы" (holograms), as Litematica does.** Litematica's own Russian translation names them so ("Размещённые голограммы" in its main menu); Zone Focus now matches in the "Ray reach" tooltip, the Modrinth description, the documentation and the changelog. English texts, language keys and options are unchanged, so settings are kept.

## 0.0.7+26.3

**RU**

**Экран настроек — общая библиотека aell-ui.** Esc во время ввода клавиши заканчивает ввод тем, что нажато; ничего не нажато — клавиша остаётся «не задано», как в malilib. Прежнее значение возвращает повторный клик по полю, значение по умолчанию — кнопка сброса в строке; подсказка об этом — во всплывающем тексте строки. Панель настроек растёт под ряд вкладок до двух третей окна (было до половины).

**fix — значок поиска в заголовке настроек.** Глиф «⌕» игрового шрифта сидел ниже строки подсказки; теперь лупа рисуется пиксельной маской 7×7 вровень с текстом и золотым цветом.

**fix — подсказка «Поиск настройки» не пропадала при клике по полю.** В фокусе подсказки больше нет, вместо неё мигающий курсор: видно, что поле приняло клик.

**EN**

**Settings screen — shared aell-ui library.** Esc while capturing a key finishes the capture with whatever was pressed; nothing pressed — the key stays "not set", as in malilib. A second click on the field brings the previous value back, the reset button in the row — the default; the row tooltip says so. The settings panel grows to fit its tab row, up to two thirds of the window (was a half).

**fix — search icon in the settings header.** The font glyph "⌕" sat below the hint line; the magnifier is now a 7×7 pixel mask aligned with the text and painted gold.

**fix — the "Search settings" hint stayed after a click on the field.** In focus the hint is gone and a blinking caret shows instead: the field visibly took the click.

## 0.0.6+26.3

**26.3.** Сборка под Minecraft 26.3 (Litematica 0.29.0, MaLiLib 0.30.1). Функционально то же, что и 0.0.6+26.2; экран настроек переведён с GLFW на `InputConstants` (в 26.3 ввод через SDL).

**26.3.** Build for Minecraft 26.3 (Litematica 0.29.0, MaLiLib 0.30.1). Functionally the same as 0.0.6+26.2; the settings screen moved from GLFW to `InputConstants` (26.3 uses SDL input).

## 0.0.6+26.2

**RU**

**fix — Схема больше не рисуется за пределами активной зоны после прыжка между далёкими зонами.** При смене зоны прежняя ненадолго остаётся видимой, пока пересобираются чанки новой, — иначе мелькает пустота. Хук отрисовки Litematica принимает один бокс, поэтому две зоны можно было выразить только объемлющим боксом обеих, а у далёких друг от друга зон он накрывает всё между ними, до двух секунд. Теперь придержка работает только между соседними зонами, а при прыжке новая зона появляется сразу.

Если установлен и Material HUD, обновите его до 0.0.17: его встроенная копия Zone Focus забирала хуки отрисовки и, спящая, ничего не фильтровала.

**EN**

**fix — The schematic no longer renders outside the active zone after a jump between distant zones.** On a zone switch the previous zone is held visible for a moment while the new zone's chunks are rebuilt, so you do not get a blank flash. Litematica's render hook accepts a single box, so two zones could only be expressed as the box enclosing both — and for zones far apart that box covers everything between them, for up to two seconds. The hold-over now runs only between adjacent zones; on a jump the new zone appears straight away.

If Material HUD is installed as well, update it to 0.0.17: its built-in copy of Zone Focus claimed the render hooks and, dormant, filtered nothing.

## 0.0.5+26.2

**RU**

**fix — кнопка «разметить зону» в инвентаре больше не прячется под панелью эффектов и не
наезжает на книгу рецептов.** Место считалось один раз при открытии экрана и всегда справа от
окна. Но справа же ванильный инвентарь рисует панель активных эффектов — с 26.x она именно
справа, а не слева, как было раньше, — и кнопка оказывалась под ней; раскрытая книга рецептов
сдвигает окно вправо, и кнопка уезжала к краю экрана; появившийся по ходу дела эффект, закрытая
книга или смена масштаба интерфейса ничего не пересчитывали. Теперь место выбирается каждый кадр
по свободному пространству: обычно справа вровень с верхом окна, при активных эффектах — слева,
а если слева раскрыта книга рецептов — справа под панелью эффектов. Панель считается по правилам
самой игры (32 px в узком варианте, иначе по ширине свободного места, шаг строки 33). На тесном
экране кнопка уходит под окно или прижимается к краю, оставаясь нажимаемой; в креативе вариант
«над окном» не рассматривается — там ряд вкладок. Чужие виджеты, добавленные на экран другими
модами, тоже учитываются: на них кнопка не встаёт.

**EN**

**fix — the "mark the zone" button in the inventory no longer hides under the effects panel or
collides with the recipe book.** Its position was decided once, when the screen opened, and always
sat to the right of the window. But the vanilla inventory draws the active effects panel there —
since 26.x it is on the right, not on the left as it used to be — so the button ended up beneath
it; an open recipe book shifts the window right and pushed the button to the screen edge; and an
effect appearing mid-session, the book being closed or the GUI scale changing recalculated
nothing. The spot is now picked every frame from the free space: normally to the right, level with
the top of the window; with active effects, to the left; and when the recipe book occupies the
left, to the right below the effects panel. The panel is measured by the game's own rules (32 px
in the narrow variant, otherwise the width of the free space, 33 px per row). On a cramped screen
the button moves below the window or hugs the edge, so it stays clickable; in creative the "above
the window" option is skipped because the tab row lives there. Widgets other mods add to the
screen are taken into account too — the button will not sit on top of them.

## 0.0.4+26.2

**RU**

**new — общий выключатель мода.** Тумблер «Мод Zone Focus» на вкладке «Подсчёт» (и он же —
на вкладке «Zone Focus» в Litematica Material HUD, это одна настройка) выключает мод целиком:
засыпают подсчёт по зоне, обрезка схемы, рамки зон, проверка зоны, разметка мышью и клавиши
действий. Экран настроек открывается всегда — иначе включить обратно было бы нечем. По умолчанию
включён, то есть поведение прежнее.

**change — со стоящим Material HUD настройки зон живут его вкладкой «Zone Focus».** Своего экрана
зон в этом случае больше нет: те же действия — деление на зоны, список, разметка мышью, сброс —
стоят на его вкладке и работают с этим модом, движок по-прежнему ведём мы. Туда же ведут все
входы: «Настройки» в Mod Menu, кнопка мода на экранах Litematica и клавиша настроек. Кнопка
«Зоны» в шапке настроек Material HUD убрана — она открывала тот самый экран. Проверка зоны и
клавиши этого мода никуда не делись: они в собственном экране настроек, а он открывается строкой
«Настройки Zone Focus…» на той же вкладке. Без Material HUD всё как было — свой экран зон на
месте, входы ведут на него.

**fix — кнопка мода на экране слоёв Litematica не наезжает на новую вкладку «All».** В
Litematica 0.28.6 ряд вкладок на экране «Слои отрисовки» стал собираться циклом по всем вкладкам
настроек, и в нём появилась седьмая — «All»; прежде шесть кнопок создавались поимённо. Наш
распределитель слотов считал длину ряда по шести вкладкам, поэтому кнопка аддона вставала ровно
на ширину «All» левее и накрывала её. Теперь наличие вкладки спрашивается у самого экрана, а не
задаётся константой: с 0.28.6 ряд считается по семи вкладкам, на прежних версиях — по шести, и
раскладка верна в обоих случаях. Экран настроек Litematica спрашивал экран и раньше — расходился
только экран слоёв. Правка общая для всех наших аддонов Litematica, так что кнопки не наезжают ни
на вкладки Litematica, ни друг на друга.

**EN**

**new — a master switch for the mod.** The "Zone Focus mod" toggle on the "Counting" tab (and
the same one on Litematica Material HUD's "Zone Focus" tab — it is one setting) turns the whole mod
off: zone counting, schematic clipping, zone boxes, zone verification, mouse marking and the action
hotkeys all go to sleep. The settings screen always opens — otherwise there would be nothing left
to switch it back on with. On by default, so nothing changes for anyone who leaves it alone.

**change — with Material HUD installed, zone settings live on its "Zone Focus" tab.** The mod's own
zones screen is gone in that case: the same actions — splitting into zones, the zone list, mouse
marking, clearing — sit on that tab and work with this mod, which still drives the engine. Every
entry point leads there: "Settings" in Mod Menu, the mod's button on Litematica's screens and the
settings hotkey. The "Zones" button in Material HUD's settings header is gone — it opened that very
screen. This mod's zone verification and hotkeys did not go anywhere: they are on its own settings
screen, reached by the "Zone Focus settings..." row on the same tab. Without Material HUD nothing
changes — the zones screen is where it was, and the entry points still lead to it.

**fix — the mod's button on Litematica's render layers screen no longer overlaps the new "All"
tab.** In Litematica 0.28.6 the tab row on the render layers screen is built by looping over every
config tab, and a seventh one — "All" — joined it; before that six buttons were created by name.
Our slot allocator measured the row as six tabs, so the addon button landed exactly one "All"
width too far left and covered it. The screen is now asked whether it has that tab instead of the
answer being hardcoded: on 0.28.6 the row counts seven tabs, on earlier versions six, and the
layout is right either way. Litematica's config screen was already asked this way — only the
render layers screen disagreed. The fix is shared by all our Litematica addons, so the buttons
overlap neither Litematica's tabs nor each other.

## 0.0.3+26.2 — публичный релиз / public release

**RU**

**new — экран «Разбить схему на зоны»: ввод с клавиатуры и диаграмма разреза.** Число
зон или размер зоны теперь можно вписать руками: клик по числу открывает набор (цифры,
Backspace, Enter — принять, Esc — отменить, Tab — следующая ось, стрелки ↑/↓ — плюс-минус
один), значение зажимается в допустимый диапазон. Под предпросмотром — двумерная диаграмма
вида сверху, север сверху: форма схемы собирается из её подрегионов (синим — как слой и
прогресс в остальном интерфейсе), линии разреза жёлтым — цветом предпросмотра зоны в мире,
первая зона подсвечена цветом активной зоны из мира и помечена «1». Разрезы по Y сверху не
видны — в углу диаграммы напоминание «Y ×n». Диаграмма перерисовывается на каждое изменение
значений и честно повторяет неравные доли деления.

**Общие настройки со встроенным модулем Zone Focus в Material HUD.** У Litematica
Material HUD появился встроенный модуль Zone Focus; при обоих установленных модах движком
по-прежнему владеет этот мод, а модуль соседа спит. Новый класс-мост
`compat.ZoneFocusBridge` (тот же контракт, что у Easy Place: `option(String)` + `saveOwn()`)
отдаёт соседу опции движка зон — вкладка Zone Focus в настройках Material HUD правит живые
объекты этого мода, значения одни в обоих экранах. При сохранении настроек значения
дополнительно проталкиваются соседу, чтобы его файл не отставал. Клавиши не общие — как
всегда в семье модов. При первом запуске без своего `zonefocus.json` настройки молча
переезжают из секции `"ZoneFocus"` файла `layerlist.json`, если человек пользовался
встроенным модулем.

**EN**

**new — "Split the schematic into zones" screen: keyboard input and a cut diagram.** The
zone count or zone size can now be typed in: clicking a number starts input (digits,
Backspace, Enter to accept, Esc to cancel, Tab for the next axis, ↑/↓ arrows for plus and
minus one), and the value is clamped to the allowed range. Below the preview sits a 2D
top-down diagram, north up: the schematic's shape is assembled from its sub-regions (in
blue — same as layers and progress elsewhere in the UI), the cut lines are yellow — the
world's zone-preview colour, and the first zone is highlighted with the world's active-zone
colour and marked "1". Y cuts are not visible from above — a "Y ×n" reminder sits in the
diagram corner. The diagram redraws on every value change and honestly shows uneven splits.

**Shared settings with the built-in Zone Focus module in Material HUD.** Litematica
Material HUD now carries a built-in Zone Focus module; with both mods installed the engine
is still run by this mod, and the neighbour's module sleeps. The new bridge class
`compat.ZoneFocusBridge` (the same contract as Easy Place: `option(String)` + `saveOwn()`)
hands the zone engine options to the neighbour — the Zone Focus tab in Material HUD's
settings edits this mod's live objects, so the values are one and the same on both screens.
Saving the settings additionally pushes the values to the neighbour so its file does not
fall behind. Keybinds are not shared — as always in the mod family. On first run without
its own `zonefocus.json` the settings migrate silently from the `"ZoneFocus"` section of
`layerlist.json` if the person had been using the built-in module.

## 0.0.8+26.2

**RU**

**Привязка клавиш показывает нажатое сразу.** Кнопка привязки, пока идёт захват, рисовала
только надпись «нажми клавиши…» — набранное сочетание появлялось лишь после Escape.
Теперь текст кнопки обновляется на каждой нажатой клавише, а синяя рамка по-прежнему
показывает, что захват продолжается.

**Escape не теряет прежнюю привязку.** Клик по привязке очищает её, чтобы набрать новую;
если после этого нажать Escape ничего не набрав, старое сочетание возвращается. То же при
переключении вкладки и при закрытии экрана.

**EN**

**Key binding shows the pressed keys immediately.** While capturing, the binding button drew
only the "press keys…" placeholder — the combination appeared only after Escape. The button
text now updates on every key press, while the blue border still shows that capture is
still running.

**Escape no longer loses the previous binding.** Clicking a binding clears it so a new one
can be typed; pressing Escape without typing anything now restores the old combination. The
same applies when switching tabs and when closing the screen.

## 0.0.7+26.2

**RU**

**Плитка настроек сама расширяется под вкладки.** Раньше ширина была жёстко 360 px, и
семь вкладок Material HUD в неё не влезали — «Ghost Alpha» и «Easy Place» обрезались
многоточием. Теперь панель берёт столько ширины, сколько нужно полосе вкладок с
нормальными отступами (для семи вкладок это около 407 px), но растёт не дальше половины
экрана — и никогда не уже прежних 360 px. Подписи выводятся целиком; обрезка с
многоточием и подсказкой осталась только там, где расширяться уже некуда.

**EN**

**The settings tile now widens itself to fit the tabs.** The width used to be a hard
360 px, which Material HUD's seven tabs did not fit — "Ghost Alpha" and "Easy Place" were
ellipsised. The panel now takes as much width as the tab strip needs at normal padding
(about 407 px for seven tabs), but never grows past half the screen — and never below the
old 360 px. Labels are shown in full; ellipsis plus tooltip is left only where there is no
room left to grow into.

## 0.0.6+26.2

**RU**

**Вкладки настроек больше никогда не переносятся на вторую строку** — общий
экран настроек стал держать их в одной горизонтальной полосе: отступы
сжимаются, подписи укорачиваются с многоточием (полная — в подсказке), а при
очень большом числе вкладок полоса прокручивается колесом и стрелками «‹ ›».

**Примечания разделов раскрываются подсказкой.** Пояснение под заголовком
раздела больше не рисуется в теле списка и не обрезается: у заголовка знак «?»,
текст — целиком в подсказке при наведении.

**Убран «Format Error»** в подписях и подсказках: переводы без подстановок
больше не прогоняются через `String.format`, поэтому одиночный `%` в тексте не
ломает строку.

**Исправлено в подтверждении сброса.** Плитка «Сбросить всё?» делилась пополам, а
слова рисовались слева направо, поэтому нажатие на «Нет» стирало все настройки.
Теперь координаты слов считает та же пара методов, которой пользуется отрисовка, и
полоса подтверждения при нехватке места растёт влево, а не обрезает слово.

**Кнопки аддонов в шапке больше не исчезают** в узком окне: когда места нет даже под
одну кнопку, остаётся «⋯» со списком, а не пустое место — заодно перестала
пересчитываться вёрстка каждый кадр. Кнопка «Назад» больше не сжимается в ноль.

**Перехват клавиш** больше не срывается после первой нажатой клавиши: строки списка
пересобираются при любом изменении, и мод теперь заново находит нужную строку по самой
настройке. **Пробел в поле поиска** больше не переключает выделенную настройку.
**Поиск** не затирает запомненную прокрутку вкладки.

**EN**

**Settings tabs never wrap to a second row again** — the shared settings screen
keeps them on one horizontal strip: padding shrinks, labels are ellipsised (the
full one in a tooltip), and with very many tabs the strip scrolls by wheel and
by the "‹ ›" chevrons.

**Section notes open as a tooltip.** The explanation under a section title is no
longer drawn in the list body and no longer truncated: the title carries a "?"
and the full text shows on hover.

**Reset confirmation fixed.** The "Reset all?" tile was split down the middle while the
words were drawn left to right, so clicking "No" wiped every setting. The word positions
now come from the same pair of methods the painter uses, and the confirmation strip grows
leftwards when it does not fit instead of clipping a word.

**Add-on buttons in the header no longer vanish** in a narrow window: when there is no
room for even one button a "⋯" list stays behind instead of empty space — and the layout
stopped being recomputed every frame. The "Back" button no longer shrinks to zero.

**Keybind capture** no longer breaks after the first key: the row list is rebuilt on every
change and the mod now re-finds the row by its setting. **Space in the search box** no
longer toggles the focused setting. **Searching** no longer overwrites the tab's
remembered scroll offset.

**"Format Error" is gone** from labels and tooltips: translations without
arguments no longer go through `String.format`, so a single `%` in the text does
not break the string.

## 0.0.5+26.2

**RU**

**Своя иконка 16×16** у кнопки на экранах Litematica и в заголовке экрана
настроек — как у Material HUD и Ghost Alpha.

**Кнопка больше не налезает на строку поиска.** Когда ряд кнопок аддонов не
помещается после вкладок Litematica, кнопки становятся квадратными иконками с
подсказкой и остаются в строке вкладок вместо второго ряда поверх поля поиска.

**EN**

**Own 16x16 icon** on the button in Litematica's screens and in the settings
screen header, matching Material HUD and Ghost Alpha.

**The button no longer covers the search bar.** When the add-on button row does
not fit after Litematica's tabs, the buttons turn into icon squares with a hover
label and stay in the tab row instead of a second row over the search field.

## 0.0.4+26.2

**RU**

Ряд кнопок аддонов на экранах Litematica знает про третий слот — новый аддон
Litematica Ghost Alpha (прозрачность голограмм). На узком экране кнопки
всех трёх модов теперь резервируют место друг под друга и не наезжают. Больше в
этой версии ничего не менялось.

**EN**

The add-on button row on Litematica's screens now knows about a third slot — the
new Litematica Ghost Alpha add-on (ghost block transparency). On a narrow screen
the buttons of all three mods reserve room for each other and no longer overlap.
Nothing else changed in this version.

## 0.0.3+26.2

**EN**

- **Fixed: verification markers no longer go stale after a chunk reloads.** Blocks placed while the zone was beyond render distance kept their "missing" boxes when you came back — the check re-examined a position only on an explicit block update. Now every chunk that comes back into render distance is re-checked automatically (within the scan budget), and the boxes match reality the moment the zone loads.

**RU**

- **Исправлено: рамки проверки больше не устаревают после перепрогрузки чанка.** Блоки, поставленные пока зона была за пределами прорисовки, сохраняли рамку «пропущено» при возвращении — проверка перечитывала позицию только по явному обновлению блока. Теперь каждый чанк, вернувшийся в прорисовку, перепроверяется автоматически (в рамках бюджета скана), и рамки совпадают с реальностью в момент прогрузки зоны.

## 0.0.2+26.2 — публичный релиз / public release

**EN**

Everything below this block is the internal history under the old numbering (0.0.2…0.0.12); all of it ships in this public 0.0.2. Highlights since the public 0.0.1:

- **Verification is a live mode.** Layer-by-layer scanning with live, slice-scoped counters; the highlight survives layer moves, zone switches and placement nudges; markers update the moment a block changes; layers with unloaded chunks retry on their own.
- **Zone switching cross-fades** instead of blanking the schematic, and no longer requeues every schematic chunk — only the old and new zone are touched. Schematic ghosts refresh a full round-trip earlier.
- **Easy Place works into the zone from outside it.** Litematica's schematic targeting (Easy Place, placement restriction) now follows the visible zone — invisible ghosts outside the clip no longer swallow clicks.
- **The shared settings screen**: always-visible values, Ctrl + F search across tabs, sections, three-level reset with in-place confirmation, changed-value marks, keybind conflict display, full keyboard control, MaLiLib mod switcher, and a "Zone Focus" button in Litematica's own tab row on both of its config screens.
- **One entry point**: Ctrl + J opens the zones screen, settings are one green button away. Reworked default binds; rebinding takes effect immediately; mouse buttons can be bound (all but LMB).
- Neutral dark style matching Litematica, color kept for meaning; keybind rows color-coded; per-row reset buttons; 512×512 icon.
- Stability: the mod's render clipping is scoped strictly to Litematica's own layer range (no effect on MiniHUD/tweakeroo and other malilib mods); the Axiom guard disables itself cleanly after a reflection failure; screen messages no longer overlap the footer buttons; the "no schematic" status checks any enabled placement.

**RU**

Всё, что ниже этого блока, — внутренняя история со старой нумерацией (0.0.2…0.0.12); целиком вошла в этот публичный 0.0.2. Главное со времён публичного 0.0.1:

- **Проверка — живой режим.** Скан по слоям с живыми счётчиками в границах среза; подсветка переживает смену слоя, зоны и сдвиг размещения; маркеры обновляются в момент изменения блока; слои с непрогруженными чанками допроверяются сами.
- **Переключение зоны — с перекрёстным затуханием** вместо исчезновения схематика, и без перестройки всех чанков — трогаются только старая и новая зона. Голограммы схематика обновляются на полный круг раньше.
- **Easy Place работает в зону снаружи.** Прицеливание литематики по схематику (Easy Place, ограничение установки) следует видимой зоне — невидимые голограммы за обрезкой больше не глотают клики.
- **Общий экран настроек**: значения видны всегда, поиск Ctrl + F по всем вкладкам, секции, трёхуровневый сброс с подтверждением на месте, метки изменённого, показ конфликтов биндов, полное управление с клавиатуры, переключатель модов MaLiLib и кнопка «Zone Focus» в родном ряду вкладок Litematica на обоих её экранах.
- **Один вход**: Ctrl + J открывает экран зон, настройки — в одной зелёной кнопке. Переработанные бинды по умолчанию; переназначение действует сразу; в бинд пишутся кнопки мыши (кроме левой).
- Нейтральный тёмный стиль под Litematica, цвет — только для смысла; строки биндов с цветовой кодировкой; сброс у каждой строки; иконка 512×512.
- Стабильность: обрезка рендера строго заскоуплена на слой-диапазон самой Litematica (не задевает MiniHUD/tweakeroo и другие malilib-моды); защита от инструмента Axiom корректно отключается после сбоя рефлексии; сообщения экранов больше не ложатся на кнопки подвала; статус «нет схемы» учитывает любое включённое размещение.


## 0.0.12+26.2

**EN**

- **Fixed: Easy Place now works into the zone from outside it.** Litematica's schematic ray trace still saw the ghost blocks hidden by the zone clip, so aiming into the zone from outside hit an invisible ghost in front of it — the click went to a block you could not see, and placing into the zone seemed dead. The trace now skips everything outside the active zone while clipping is on, matching what is on screen: Easy Place, placement restriction and the schematic-targeting tools all follow the visible zone. With clipping off (or no zones) nothing changes.

**RU**

- **Исправлено: Easy Place работает в зону снаружи.** Луч литематики по схематик-миру видел и голограммы, скрытые обрезкой по зоне: прицелившись в зону снаружи, ты попадал в невидимую голограмму перед ней — клик уходил в блок, которого не видно, и ставить в зону было нельзя. Теперь при включённой обрезке трейс пропускает всё вне активной зоны, как на экране: Easy Place, ограничение установки и инструменты по схематику следуют видимой зоне. Без обрезки (или без зон) поведение прежнее.

## 0.0.11+26.2

**EN**

- **Fixed: rebinding hotkeys now takes effect immediately.** The mod's settings screen did not refresh malilib's key-to-hotkey map after a bind changed, so a newly assigned key stayed dead until restart — only combinations that still contained one of the old keys would fire.
- **New: mouse buttons can be bound.** While capturing a bind, press any mouse button except the left one — right, middle and side buttons are recorded just like keyboard keys.

**RU**

- **Исправлено: переназначение клавиш действует сразу.** Экран настроек не обновлял карту «клавиша → хоткей» в malilib после смены бинда, поэтому новая клавиша молчала до перезапуска — срабатывали только комбинации, где оставалась одна из старых клавиш.
- **Новое: в бинд можно записать кнопки мыши.** Во время захвата нажмите любую кнопку мыши, кроме левой — правая, средняя и боковые записываются так же, как клавиши клавиатуры.

## 0.0.10+26.2

**EN**

- **Fixed: 0.0.9 crashed the game when Litematica's config screen opened.** The helper class holding the addon slot order sat in the mixin package, and Mixin refuses to transform a class there that is not a registered mixin. The constants moved back into the mixins themselves. Found by opening the screen in the game — 0.0.9 must not be used.
- **Fixed: the addon buttons no longer cover Litematica's own tabs.** When the native tab row already fills the window, the addon group used to slide left on top of "Hotkeys" and "Render Layers"; now it drops to a second row, right-aligned, and nothing overlaps. On a wide window nothing changes.
- **The "Hotkeys" tab is on the same line as the others.** Six labels were 5 px over the panel width, so the last one wrapped to a second row. The tab padding now shrinks from 8 to 4 px as needed and only wraps if even that does not fit.
- **Fixed: text in every control sat one pixel too high.** Vertical centring assumed a 9 px line where the font paints 8, so the gap above and below was uneven — most visible in the keybind boxes.
- **Fixed: the reset icon was a broken glyph.** `⟲` is not in the game font; it is now the same 7×7 pixel mask both mods have always used, centred in its box.
- **Keybind rows are colour-coded.** Each row carries the colour of what it does — screens blue, mouse marking green, zone switching the colour of the active zone frame, verification yellow — as a square next to the name and as the border and a faint fill of the key box itself. An unbound key keeps the colour but dimmed. Conflicts still override the colour with red or orange.
- **Ctrl + J opens the zones screen, not the settings.** One entry point into the mod; the settings are one green button away, and the way back is the green button in the footer.
- **The "Zones screen" hotkey is gone** — Ctrl + J does that now.
- **The zone list has no hotkey by default.**
- **The two arrows next to "Progress of all zones" are gone.** They flipped pages, which the mouse wheel already does over the list, and with a single page they did nothing at all.
- **"Bring back all zones" is greyed out when nothing is hidden** instead of looking clickable.
- "Addon settings…" on the zones screen is green.

**RU**

- **Исправлено: 0.0.9 крашила игру при открытии настроек Litematica.** Класс с порядком слотов кнопок лежал в mixin-пакете, а Mixin отказывается трансформировать класс оттуда, если он не зарегистрирован как миксин. Константы вернулись в сами миксины. Поймано открытием экрана в игре — 0.0.9 ставить нельзя.
- **Исправлено: кнопки аддонов больше не наезжают на вкладки Litematica.** Когда нативный ряд уже занял всё окно, группа аддонов сдвигалась влево поверх «Hotkeys» и «Render Layers»; теперь она уходит на вторую строку, прижатая вправо, и ничего не перекрывается. На широком окне ничего не меняется.
- **Вкладка «Клавиши» встала в один ряд с остальными.** Шесть подписей не влезали в ширину панели на 5 px, и последняя переносилась вниз. Теперь отступ вкладок сжимается с 8 до 4 px по необходимости, а перенос остаётся только если не хватило и этого.
- **Исправлено: текст во всех контролах стоял на пиксель выше центра.** Центрирование считало строку в 9 px, а шрифт рисует 8, поэтому зазор сверху и снизу был разным — заметнее всего в полях привязок.
- **Исправлено: иконка сброса была битым глифом.** `⟲` в шрифте игры нет; теперь это та же маска 7×7, что всегда была в обоих модах, по центру своей рамки.
- **Строки привязок раскрашены по смыслу.** У каждой строки цвет того, чем она управляет: экраны — синий, разметка мышью — зелёный, переключение зон — цвет рамки активной зоны, проверка — жёлтый. Цвет идёт и квадратом у названия, и обводкой с лёгкой заливкой самого поля клавиши. У неназначенной клавиши цвет тот же, но приглушённый. Конфликт по-прежнему перекрывает цвет красным или оранжевым.
- **Ctrl + J открывает экран зон, а не настройки.** Один вход в мод; настройки — в одной зелёной кнопке оттуда, обратно — зелёная кнопка в футере.
- **Привязки «Экран зон» больше нет** — эту работу делает Ctrl + J.
- **У списка зон по умолчанию нет клавиши.**
- **Две стрелочки рядом с «Прогресс всех зон» убраны.** Они листали страницы, что и так делает колесо над списком, а на одной странице не делали вообще ничего.
- **«Вернуть все зоны» гаснет, когда скрытых зон нет**, вместо вида нажимаемой кнопки.
- Кнопка «Настройки аддона…» на экране зон — зелёная.

## 0.0.9+26.2

**EN**

- **Fixed: the Zone Focus button no longer disappears on Litematica's "Render Layers" screen.** That tab is a separate screen class (`GuiRenderLayer`) that draws Litematica's tab row itself, and the addon button was only added to `GuiConfigs` — so it vanished the moment you switched to that tab. The button is now added to both screens, in the same slot, with the same width.
- **New default hotkeys.** Zone list moves to `Ctrl+L` (was `Alt+L`), verification toggle gets `Ctrl+P` (was unbound), and mouse marking is left unbound by default — the inventory button is the intended way in. The zones screen keeps `Alt+J`, settings keep `Ctrl+J`.
- **"Search the whole zone" is off by default.** Storage search follows the visible HUD page again unless you turn it on.
- **The settings hotkey leads to the zones screen.** `Ctrl+J` now opens the settings with the zones screen behind it, so the footer button back to it is one click away — and that button is highlighted in green.
- **Zone list footer: the sort button gets the width it needs.** "Count progress of every zone" used to eat the whole row, so the sort label was cut down to just its value. The count button is now as wide as its own label (and the label itself is shorter — "Progress of all zones"), and the rest of the row goes to the filter.
- **Every keybind row has a reset icon.** It used to appear only after you changed the binding; since clicking a keybind clears it first, there was no visible way back to the default.
- Icon replaced with the 512×512 one.
- Settings changed by previous versions are migrated: this release forces the four settings above back to their new defaults once, then leaves your choices alone.

**RU**

- **Исправлено: кнопка Zone Focus больше не пропадает на экране «Отображение слоёв» Litematica.** Эта вкладка — отдельный класс экрана (`GuiRenderLayer`), который сам рисует ряд вкладок Litematica, а кнопка аддона добавлялась только в `GuiConfigs` — поэтому исчезала, стоило переключиться на эту вкладку. Теперь кнопка добавляется на оба экрана, в тот же слот и той же ширины.
- **Новые привязки по умолчанию.** Список зон — `Ctrl+L` (было `Alt+L`), проверка вкл/выкл — `Ctrl+P` (не была назначена), разметка мышью по умолчанию не назначена вовсе: вход в неё — кнопка в инвентаре. Экран зон остался на `Alt+J`, настройки — на `Ctrl+J`.
- **«Поиск по всей зоне» выключен по умолчанию.** Поиск по сундукам снова идёт по видимой странице HUD, пока сам не включишь.
- **Бинд настроек ведёт на экран зон.** По `Ctrl+J` настройки открываются с экраном зон за ними — кнопка возврата на него в одном клике, и она подсвечена зелёным.
- **Футер списка зон: фильтру хватает ширины.** «Посчитать прогресс всех зон» занимала всю строку, и подпись сортировки урезалась до одного значения. Теперь кнопка подсчёта шириной по своей подписи (а сама подпись короче — «Прогресс всех зон»), остаток строки уходит фильтру.
- **У каждой строки с клавишей есть иконка сброса.** Раньше она появлялась только после изменения бинда, а клик по биндy его сначала стирает — вернуться к дефолту было нечем.
- Иконка заменена на 512×512.
- Настройки, сохранённые прошлыми версиями, переносятся: этот выпуск один раз возвращает четыре пункта выше к новым значениям по умолчанию и больше в твой выбор не лезет.

## 0.0.8+26.2

**EN**

- **The zones screen is pinned to the left edge and much narrower.** Its width now follows the longest action label instead of taking a fixed slab of the window, so it sits exactly like the settings screen.
- **Status chips wrap onto a second line instead of vanishing.** A long schematic name used to silently drop its whole chip when it did not fit on one row.
- **The settings screen got the look of the zones screen**: rounded rows and controls, an animated toggle knob, a status plate with a coloured dot, a fading hover. One design across both screens and both mods, which is what the documentation always claimed.
- **The Zone Focus button now sits in Litematica's own tab row**, sized and styled like the native tabs, right after them — not as a separate group below and to the right. On a narrow window the addon buttons shift left instead of running off the screen.
- **The mod switcher from MaLiLib is available on our screens too**: from the zones screen and from the settings screen you can jump straight to the settings of Litematica, MaLiLib or any other mod, exactly as from Litematica's own screen. Zone Focus itself now appears in that list.
- The middle footer button reads "Reset tab (N)" in full; the Back button gives up width first when the footer is tight.

**RU**

- **Экран зон прижат к левому краю и стал заметно уже.** Ширина теперь идёт от самой длинной подписи действия, а не занимает фиксированную долю окна, — он встаёт ровно так же, как экран настроек.
- **Плашки статуса переносятся на вторую строку, а не пропадают.** Раньше длинное имя схемы молча выкидывало свою плашку целиком, если она не влезала в строку.
- **Экран настроек получил вид экрана зон**: скруглённые строки и элементы, анимированный бегунок тумблера, плашка состояния с цветной точкой, плавная подсветка под курсором. Один дизайн на оба экрана и оба мода — как и было обещано в документации.
- **Кнопка Zone Focus встала в родной ряд вкладок Litematica**, того же размера и стиля, сразу за нативными вкладками, — а не отдельной группой справа снизу. На узком окне кнопки аддонов сдвигаются влево, а не уезжают за край.
- **Переключатель модов из MaLiLib теперь есть и на наших экранах**: с экрана зон и из настроек можно уйти сразу в настройки Litematica, MaLiLib или любого другого мода — так же, как с родного экрана Litematica. Сам Zone Focus тоже появился в этом списке.
- Средняя кнопка футера читается полностью — «Сбросить вкладку (N)»; при нехватке места ширину первой отдаёт кнопка «Назад».

## 0.0.7+26.2

**EN**

- **A brand new settings screen, shared with Litematica Material HUD.** Same grid, same palette, same behaviour in both mods — open one and you already know the other.
- **The value of every option is always visible**: On/Off next to the toggle, the number in its own field next to the slider, the key combo spelled out in full. Nothing hides behind a hover or a click.
- **Search across all tabs.** Ctrl+F, type a word, and matching rows from every tab are listed flat with their tab name in front; the matching fragment is highlighted.
- **Sections inside a tab** with headers, so a long tab reads as groups rather than one list.
- **Options that depend on a switched-off parent are greyed out** instead of silently doing nothing, and the tooltip says why. Options that need Material HUD are greyed out in their own colour, so "you turned this off" never looks like "another mod owns this".
- **Reset on three levels**: one row, the whole tab (with a count), or the whole config. "Reset all" asks for confirmation in place, no popup window.
- **Changed values are visible**: a stripe on the left of the row, a dot next to the tab name.
- **A status line** under the tabs appears only when there is something to say: no schematic loaded, settings that contradict each other, or an integration problem. Clicking it jumps to the option in question.
- **Keybind conflicts are shown**: a hard conflict inside the mod in red, a combo already taken by another mod in orange, with the mod and action named.
- **Full keyboard control**: Tab, arrows, Space/Enter, Page Up/Down, Home/End, Esc.
- The layout no longer breaks on narrow or short windows: the control column, the row reset button and the section headers step down in a fixed order.
- The button in Litematica's own settings now keeps a fixed slot order regardless of which mod loaded first.

**RU**

- **Совершенно новый экран настроек, общий с Litematica Material HUD.** Одна сетка, одна палитра, одно поведение в обоих модах — открыв один, второй уже знаешь.
- **Значение каждой настройки видно всегда**: «Вкл/Выкл» рядом с тумблером, число в своём поле у ползунка, сочетание клавиш прописано целиком. Ничего не прячется за наведением или щелчком.
- **Поиск сразу по всем вкладкам.** Ctrl+F, слово — и подходящие строки со всех вкладок идут плоским списком с именем вкладки впереди; совпавший кусок подсвечен.
- **Секции внутри вкладки** с заголовками: длинная вкладка читается группами, а не одним списком.
- **Настройки, зависящие от выключенного родителя, гаснут**, а не молча ничего не делают, и подсказка объясняет причину. То, чему нужен Material HUD, гаснет своим цветом — «ты сам выключил» больше не выглядит как «этим управляет другой мод».
- **Сброс на трёх уровнях**: строка, вся вкладка (с количеством) или весь конфиг. «Сбросить всё» переспрашивает прямо в кнопке, без всплывающего окна.
- **Изменённое видно**: полоска слева от строки, точка рядом с названием вкладки.
- **Строка состояния** под вкладками появляется, только когда есть что сказать: схема не загружена, настройки противоречат друг другу, интеграция не сложилась. Клик уводит к нужной настройке.
- **Конфликты привязок показываются**: жёсткий внутри мода — красным, занятое соседним модом сочетание — оранжевым, с именем мода и действия.
- **Полное управление с клавиатуры**: Tab, стрелки, Space/Enter, Page Up/Down, Home/End, Esc.
- Вёрстка больше не ломается на узком и низком окне: колонка контрола, кнопка сброса строки и заголовки секций убираются в заданном порядке.
- Кнопка в настройках самой Litematica теперь занимает постоянный слот независимо от того, какой мод загрузился первым.

## 0.0.6+26.2

**EN**

- **A reset button on every settings row.** It appears next to the control only while the value differs from the default, and one click puts that single option back.
- **The settings screen got noticeably tighter**: a narrow column pinned to the left edge, shorter tabs and rows, a smaller Back button.
- **More colour marks in the settings**: every zone box and label carries the blue of its box in the world, and verification keeps the colours of its categories — missing red, wrong block orange, wrong state yellow, extra cyan.
- **A Zone Focus button in Litematica's own settings screen**, under the tab row on the right. When Material HUD is installed too, it takes the slot next to it instead of overlapping.

**RU**

- **Кнопка сброса у каждой строки настроек.** Появляется рядом с элементом управления, только когда значение отличается от стандартного, и одним щелчком возвращает именно эту настройку.
- **Экран настроек стал заметно плотнее**: узкая колонка у левого края, ниже вкладки и строки, меньше кнопка «Назад».
- **Больше цветных меток в настройках**: рамки и подписи зон получили синий цвет своих коробок в мире, а проверка сохранила цвета категорий — не хватает красный, не тот блок оранжевый, не то состояние жёлтый, лишнее голубой.
- **Кнопка Zone Focus в настройках самой Litematica**, под строкой вкладок справа. Если стоит и Material HUD, кнопка встаёт в соседний слот, а не поверх него.

## 0.0.5+26.2

**EN**

- **The settings screen works like the old one again.** The panel sits at the left edge instead of being centred, tabs line up from the left at the width of their own labels, and option rows show just the name — the description is a tooltip that appears when the pointer is over the row. The tooltip is drawn in the mod's own style, not in the vanilla frame with its purple border.
- **Hotkeys for the mod's screens**: the zones screen is bound to `Alt+J` by default (it had no binding at all), and the zone list gets a new binding on `Alt+L`. Settings stay on `Ctrl+J` — mod screens are on Alt, settings on Ctrl.

**RU**

- **Экран настроек снова работает как старый.** Панель стоит у левого края, а не по центру, вкладки идут слева по ширине своих подписей, в строке настройки — только название, а пояснение всплывает подсказкой при наведении на строку. Подсказка рисуется в оформлении мода, а не ванильной плашкой с фиолетовой рамкой.
- **Бинды на экраны мода**: экран зон теперь по умолчанию на `Alt+J` (раньше не был назначен вовсе), а список зон получил свой бинд `Alt+L`. Настройки остались на `Ctrl+J` — экраны мода на Alt, настройки на Ctrl.

## 0.0.4+26.2

**EN**

- **New mod icon** in the same neutral tone: dark plate, the three cubes and the green frame corners unchanged.
- **The purple look is gone.** Screens no longer paint their own background: a flat dark dim over the game and neutral dark-grey panels, the same neutral tone Litematica's own screens use — the zone toolbar and its key hints included. Sections in the settings stay as they are, and colour still marks meaning: green for a toggle that is on or an active zone, blue for the current selection, and the verify categories keep the colours of their boxes in the world.

**RU**

- **Новая иконка мода** в том же нейтральном тоне: тёмная плашка, три куба и зелёные уголки кадра на месте.
- **Фиолетового оформления больше нет.** Экраны не красят фон в свой цвет: ровное затемнение поверх игры и нейтральные тёмно-серые панели в тон экранам самой Litematica — панель инструментов зон и подсказки клавиш тоже. Разбивка настроек по разделам осталась, цвет по-прежнему обозначает смысл: зелёный — включённый тумблер и активная зона, синий — текущий выбор, а категории проверки сохранили цвета своих рамок в мире.

## 0.0.3+26.2

**EN**

Zone switching goes through an overlap: the old zone stays drawn until the new one is actually on screen.

- **The schematic never disappears while a zone switch is being computed.** A schematic chunk mesh is built clipped to the zone, so switching zones is a rebuild: the old zone's chunks lose their content, the new zone's chunks gain it. Emptying a mesh is cheap, filling one is not, which is why it read as "the schematic vanished and came back a second later" — the old one was wiped at once while the new one trickled in at Litematica's 8 ms per frame.
  - The old zone is now left alone: its chunks are not touched at all. Only the new zone's chunks are queued, and while they build, the clip lets both boxes through — a chunk covered by both gets the volume that spans both parts, so neither side flickers.
  - The moment the last chunk of the new zone reports its mesh is ready (`MixinChunkRendererSchematicVbo` on `rebuildChunk`, a signal from the mesh thread), the old box is released and its chunks are queued — an empty, fast rebuild. There is not a single frame with a hole in it.
  - A timeout covers chunks nobody will ever rebuild: outside the render distance or the frustum no mesh is built and no report arrives.
  - Starting another switch mid-transition closes the previous one first, so a zone from two switches ago can never stay on screen.

**RU**

Переключение зоны идёт внахлёст: старая зона остаётся нарисованной, пока новая не появится на экране.

- **Схема больше не исчезает на время просчёта.** Меш чанка схемы строится обрезанным по зоне, поэтому смена зоны — это пересборка: чанки старой зоны теряют содержимое, чанки новой набирают. Опустошить меш дёшево, наполнить дорого — отсюда и «схема пропала и появилась через секунду»: старое стиралось разом, новое доезжало по 8 мс на кадр.
  - Старую зону теперь не трогают вообще: её чанки остаются как есть. На пересборку встают только чанки новой зоны, и пока они строятся, обрезка пропускает обе коробки — чанку, попавшему в обе, отдаётся объём, накрывающий оба куска, так что не мигает ни та ни другая сторона.
  - Как только последний чанк новой зоны отчитался о готовом меше (`MixinChunkRendererSchematicVbo` на `rebuildChunk`, сигнал из потока сборки), старая коробка отпускается и её чанки уходят на пересборку — уже пустую и быструю. Кадра с дырой нет ни одного.
  - Страховка по времени — на чанки, которые пересобирать никто не станет: за пределами прорисовки или вне фрустума меш не строится и отчёта не будет.
  - Новое переключение посреди перехода сначала закрывает предыдущий, поэтому зона от позапрошлого переключения на экране не залипает.

## 0.0.2+26.2

**EN**

Verify-the-zone rework: the mismatch highlight became a live mode instead of a one-shot scan, and the schematic keeps up with what you build.

- **The schematic no longer takes a second to catch up with what you build.** Three separate delays, all removed:
  - Litematica learns about a world block change in `MixinClientLevel.setServerVerifiedBlockState` — the server packet. A block you place is in your world instantly (client prediction), but its ghost hangs there for a full client→server→client round trip before the chunk is even queued for a rebuild. Zone Focus already sees the prediction, so it marks the schematic chunk for re-render right there, a round trip earlier — and only where the schematic actually has a block, since Litematica rebuilds nearby chunks synchronously on the main thread and an extra column per block placed outside the schematic would be a stutter for nothing.
  - Nothing has to vanish while that rebuild runs: a schematic chunk keeps its previous mesh until the new one is ready (`ChunkRendererSchematicVbo.updateChunkRenderData` swaps the data only at the very end). The schematic only goes blank when every chunk renderer is recreated (`WorldRendererSchematic.loadRenderers`), and the only route there is a full render reload by the game itself — the mod no longer has a path to it.
  - Switching zones used to call `SchematicWorldRefresher.updateAll()`, which queues *every* loaded schematic chunk. Litematica gives chunk rebuilds 8 ms per frame (`LitematicaRenderer.calculateFinishTime`), so on a large schematic that is the second where the schematic vanishes and crawls back in pieces. Only the chunks of the old and the new zone are touched now; a full refresh is left for turning zone clipping on or off, where everything really does change.
  - The verify pass no longer walks the whole zone. Its unit of work is one layer of the zone along the axis of Litematica's layer mode, scanned layers are remembered, and moving between them costs nothing.
- **The verify highlight is a mode now, not a one-shot result.** It used to be a snapshot pinned to a signature of zone + placement + layer bounds, and any change to that signature called `stop()`: moving a layer, switching zones or nudging the schematic wiped the highlight and you had to press the bind again. Now it is only ever turned off by you — the bind, the button or the option. Everything else it follows.
  - Layers move instantly: a layer that has been scanned is never scanned again, and a new one is a single plane of the zone. Turning `ignoreLayersInZone` on highlights the whole zone, exactly as it renders.
  - Switching zones or moving the placement no longer blanks the screen. Layers are refreshed in place, position by position, and positions outside the new volume are dropped immediately — there is nothing to clear and refill.
  - Counts in the GUI are live and scoped to the slice you see, instead of being frozen at the moment of the scan.
  - A layer that ran into unloaded chunks is not marked as done and is revisited a few seconds later — that layer alone, not the whole zone.
- **Fixed the verify highlight not clearing on blocks you place yourself.** The update hook sat on `ClientLevel.setServerVerifiedBlockState`, but a block you place is applied by the client's own prediction before the server packet arrives: by the time the packet came in, the world already held the new state, "before" equalled "after" and no update happened — the outline stayed lit. The hook moved to `sendBlockUpdated`, where every block change passes with its before/after pair, prediction included. The outline itself is now patched at the changed position instead of waiting for the render list to be rebuilt on a tick, so it goes out in the same frame. Zone progress lagged the same way and now follows too.

**RU**

Переделка проверки зоны: подсветка несовпадений стала живым режимом вместо разового прохода, а схема успевает за стройкой.

- **Схема больше не догоняет стройку через секунду.** Три независимые задержки, все убраны:
  - Об изменении блока в мире Litematica узнаёт из `MixinClientLevel.setServerVerifiedBlockState` — то есть из пакета сервера. Поставленный тобой блок лежит в мире сразу (предсказание клиента), а его голограмма висит ещё целый круг «клиент → сервер → клиент», и только потом чанк вообще встаёт в очередь на пересборку. Zone Focus предсказание уже видит, поэтому помечает чанк схемы к пересборке прямо там — на круг раньше. Помечает не всё подряд, а только там, где в схеме есть блок: ближние чанки Litematica пересобирает синхронно, в основном потоке, и лишний столб на каждый блок мимо схемы был бы подтормаживанием на ровном месте.
  - Пропадать при пересборке при этом нечему: чанк схемы держит прежний меш, пока не готов новый (`ChunkRendererSchematicVbo.updateChunkRenderData` подменяет данные только в самом конце). Насовсем схема гаснет лишь при пересоздании всех рендереров (`WorldRendererSchematic.loadRenderers`), а туда ведёт только полная перезагрузка рендера самой игры — из мода такого пути больше нет.
  - Переключение зоны вызывало `SchematicWorldRefresher.updateAll()`, а он ставит в очередь **каждый** загруженный чанк схемы. На пересборку Litematica отводит 8 мс на кадр (`LitematicaRenderer.calculateFinishTime`) — вот та самая секунда, за которую схема пропадает и потом набирается кусками. Теперь трогаются только чанки старой и новой зоны, а полная пересборка осталась там, где она честно нужна: при включении и выключении обрезки по зоне.
  - Проход проверки больше не обходит зону целиком. Единица работы — один слой зоны по оси текущего режима слоёв Litematica, посчитанные слои запоминаются, возврат на них не стоит ничего.
- **Подсветка несовпадений стала режимом, а не разовым результатом.** Раньше это был снимок, привязанный к подписи «зона + плейсмент + границы слоя», и любое расхождение подписи вызывало `stop()`: сдвинул слой, переключил зону или подвинул схему — подсветка пропадала, и надо было жать бинд заново. Теперь её выключает только пользователь: бинд, кнопка или опция. Всё остальное она сопровождает.
  - Слои двигаются мгновенно: слой, который уже считали, не пересчитывается, а новый — это одна плоскость зоны. С включённым `ignoreLayersInZone` подсвечивается вся зона, ровно как она и рисуется.
  - Смена зоны и сдвиг плейсмента больше не гасят экран. Слой обновляется на месте, позиция за позицией, а лишнее вне нового объёма отсекается сразу — стирать и набирать заново нечего.
  - Числа в интерфейсе живые и считаются по видимому срезу, а не заморожены на момент прохода.
  - Слой, которому попались незагруженные чанки, не засчитывается и перепроверяется через несколько секунд — он один, а не вся зона.
- **Подсветка гаснет на блоке, который поставил сам.** Обновление висело на `ClientLevel.setServerVerifiedBlockState`, но поставленный тобой блок клиент ставит в мир своим предсказанием, ещё до пакета сервера: к приходу пакета в мире уже лежало новое состояние, «было» совпадало со «стало», и обновления не происходило — рамка продолжала светиться. Точка перенесена на `sendBlockUpdated`, через которую проходит любое изменение блока с парой «было → стало», включая предсказание. Сама рамка теперь правится точечно по изменившейся позиции, а не ждёт пересборки списка отрисовки на тике, — гаснет в том же кадре. Прогресс зоны отставал по той же причине и теперь тоже идёт следом.

## 0.0.1+26.2

**EN**

First release of Litematica Zone Focus as a standalone mod (Fabric, MC 26.2) — needs Litematica, MaLiLib and Fabric API. Works alongside any version of Litematica Material HUD: with a compatible one the material list, chest search and container slot highlights follow the active zone, without it (or with an old one) the mod silently runs standalone.

- **The schematic is rendered only inside the active zone** (option, on): blocks, the missing-block overlay and the schematic's chests and signs outside the zone are not drawn at all.
- Split a placed schematic into a grid of zones through the GUI (by zone count or by zone size), draw a zone with the mouse (button in the inventory), take it from Litematica's area selection, or make one zone per schematic sub-region.
- Zone list with per-zone progress, percent, blocks left and distance; hidden-zone filter; sorting by number / progress / blocks left / distance.
- **Verify the schematic inside the zone**: missing / wrong block / wrong state / extra, outlines through walls, color-coded, with draw-range and marker caps.
- Zones combine with Litematica's render layers, follow the placement when it moves and are saved per server and dimension.
- **Settings open with Ctrl + J** (rebindable; `openSettings` and `toggleTool` are the only binds taken by default); the settings screen has a "Back" button to the zones screen.
- **Mouse marking is turned on from the inventory.** A slot-styled button sits beside the inventory panel (survival and creative alike) whenever a placement is enabled; clicking it starts marking and closes the screen so you can drag straight away, clicking it again stops. The icon has two states: a padlock while the zone is pinned and world clicks work as usual, a selection frame while marking is on.
  - This replaces the extra slot at the edge of the hotbar. That gesture — scrolling past the hotbar edge — is shared with Axiom, and the two never fit: Axiom's default tool is Move, and with any active selection `MoveBuilderTool.scroll()` takes the wheel for itself and never releases its slot, so the marking slot was unreachable in that direction and the wheel appeared to stop dead on Axiom's icon. The wheel is now entirely Axiom's; the inventory belongs to nobody.
  - Everything the hotbar slot needed from Axiom is gone with it — only one check remains, and marking still will not turn on over an active Axiom tool, which listens for held attack just like we do.
- **Axiom-style control hints while marking.** A panel in the bottom-right corner lists the controls for as long as marking is on: hold LMB and drag, release LMB to save, scroll to expand the zone. The scroll line carries a mouse glyph with the wheel picked out in red. Hides with the rest of the HUD on F1; can be turned off with `toolKeyHints`.
- **The button no longer hides itself when no placement is enabled.** It used to only appear inside an enabled schematic placement, which read as "the mod is gone": you open the inventory, there is no button and no hint as to why. Now it is always there while mouse marking is on, greyed out without a placement, and its tooltip says what is missing.
- The button also appears on **Axiom's creative screens**. Once you visit Axiom's colour or gradient tab, `E` stops opening the vanilla `CreativeModeInventoryScreen` — Axiom remembers the last tab and swaps the screen on every press — which is why the button was missing in creative. Its screens are `AbstractContainerScreen` with the same 195×136 panel, so the button sits in exactly the same place.
- **Fixed Esc dropping you into the world from the zones screen opened via Material HUD.** The "Zones" button registered its screen with no parent, so MaLiLib's `GuiBase` had nowhere to close back to and Material HUD's settings just vanished — with no way back, since the zones screen has no "Back" button of its own. The screen now takes whatever screen the button was clicked from as its parent.
- **Fixed zones auto-advancing on their own next to Material HUD.** "Next zone when done" fired on `blocksMissing == 0`, but Material HUD also publishes a zero result when there was nothing to count — most easily when Litematica's render layer does not intersect the zone, so every box is clamped away and not a single chunk is counted. Zone Focus read that as "zone finished" and switched zones every two seconds, rebuilding the schematic meshes each time. The auto-advance now also requires a non-zero block total.
- Registration order in the Material HUD bridge is reversed (button first, count scope second): if the button throws `LinkageError` on a mismatched API, the zone is no longer left attached as the counting scope of a mod that already considers the integration off.
- `REQUIRED_API_VERSION` raised from 0 to 1 — the level the mod actually calls (`registerConfigButton`, `AddonScreen`, `getFooterLines`, `searchWholeScope`). At level 0 it would "integrate" with a build missing half of those and rely on catching `LinkageError`.
- Fixed a crash on "Verify the zone" without Litematica Material HUD installed: `ZoneVerifier` reached the optional mod's API through `ZoneScope` and threw `NoClassDefFoundError: com/aell/layerlist/api/CountScope`. The verifier now reads its own option directly, and a build-time check fails the build if anything outside the compat bridge references Material HUD classes.
- Localized into 12 languages: English, Russian, Ukrainian, German, French, Spanish, Italian, Polish, Brazilian Portuguese, Simplified Chinese, Japanese, Korean.

**RU**

Первый релиз Litematica Zone Focus как самостоятельного мода (Fabric, MC 26.2) — нужны Litematica, MaLiLib и Fabric API. Работает рядом с любой версией Litematica Material HUD: с подходящей список материалов, поиск по сундукам и подсветка слотов считаются по активной зоне, без неё (или со старой) мод молча работает автономно.

- **Схема отрисовывается только внутри активной зоны** (опция, вкл): блоки, оверлей «не поставлено» и сундуки с табличками схемы за пределами зоны не рисуются вовсе.
- Разбиение поставленной схемы на сетку зон через интерфейс (количеством зон или размером зоны), разметка зоны мышью (кнопка в инвентаре), зона из выделения Litematica или «каждый подрегион = зона».
- Список зон с прогрессом, процентом, «осталось N» и расстоянием; фильтр скрытых; сортировка по номеру / прогрессу / остатку / расстоянию.
- **Проверка схемы в пределах зоны**: пропущено / не тот блок / не то состояние / лишние — рамки сквозь стены, цвет по категории, с лимитами дальности и числа рамок.
- Зоны сочетаются со слоями Litematica, едут вместе со схемой и сохраняются по серверу и измерению.
- **Настройки открываются по Ctrl + J** (переназначается; по умолчанию заняты только `openSettings` и `toggleTool`); в экране настроек есть кнопка «Назад» в экран зон.
- **Разметка мышью включается из инвентаря.** Кнопка-слот стоит сбоку от панели инвентаря (и в выживании, и в креативе), пока включена поставленная схема: клик запускает разметку и закрывает экран, чтобы сразу тянуть рамку, повторный клик её выключает. У значка два состояния: замок — зона закреплена и клики по миру работают как обычно, рамка выделения — идёт разметка.
  - Это замена дополнительному слоту у края хотбара. Тот жест — прокрутка за край — общий с Axiom, и ужиться они не могли: инструмент Axiom по умолчанию Move, при любом активном выделении `MoveBuilderTool.scroll()` забирает колесо себе и слот не отдаёт, поэтому в ту сторону слот разметки был недостижим, а колесо будто упиралось в иконку Axiom. Теперь колесо целиком его, а инвентарь ничей.
  - Вместе со слотом ушло и всё, что мы просили у Axiom: осталась одна проверка, и разметка по-прежнему не включается поверх активного инструмента Axiom — он тоже слушает зажатую атаку.
- **Подсказки по управлению в стиле Axiom.** Плашка в правом нижнем углу висит, пока идёт разметка: зажми ЛКМ и тяни, отпусти ЛКМ — зона сохранена, прокрутка расширяет зону. В строке про прокрутку стоит значок мыши с колесом, подсвеченным красным. По F1 прячется вместе с остальным интерфейсом, отключается опцией `toolKeyHints`.
- **Кнопка больше не прячется, когда нет включённой схемы.** Раньше она появлялась только внутри включённого размещения, и это читалось как «мода нет»: заходишь в инвентарь, кнопки нет и непонятно почему. Теперь она на месте всегда, пока разметка мышью включена, без схемы просто гаснет, а подсказка объясняет, чего не хватает.
- Кнопка появляется и на **креативных экранах Axiom**. Стоит один раз зайти в его вкладку палитры или градиента, и «E» перестаёт открывать ванильный `CreativeModeInventoryScreen`: Axiom помнит последнюю вкладку и подменяет экран на каждое нажатие — из-за этого кнопки в креативе и не было. Его экраны — тоже `AbstractContainerScreen` с той же панелью 195×136, так что кнопка встаёт ровно на то же место.
- **Esc в экране зон, открытом из Material HUD, больше не выбрасывает в мир.** Кнопка «Зоны» регистрировала экран без родителя, и `GuiBase` из MaLiLib закрывался в пустоту: настройки Material HUD просто исчезали, а вернуться было нечем — своей кнопки «Назад» у экрана зон нет. Теперь родителем становится тот экран, с которого нажали кнопку.
- **Зоны больше не переключаются сами при установленном Material HUD.** «Следующая зона по готовности» срабатывала по `blocksMissing == 0`, но Material HUD публикует нулевой результат и там, где считать было нечего — проще всего это ловится, когда слой Litematica не пересекается с зоной: боксы отсекаются поштучно и до подсчёта не доходит ни один чанк. Zone Focus читал это как «зона достроена» и переключал зону каждые две секунды, каждый раз пересобирая меши схемы. Теперь для перехода нужен ещё и ненулевой итог блоков.
- Порядок регистрации в мостике Material HUD перевёрнут (сначала кнопка, потом область подсчёта): если кнопка упадёт с `LinkageError` на чужой версии API, зона больше не останется привязанной как область подсчёта к моду, который уже считает интеграцию выключенной.
- `REQUIRED_API_VERSION` поднят с 0 до 1 — до уровня, который мод реально вызывает (`registerConfigButton`, `AddonScreen`, `getFooterLines`, `searchWholeScope`). На уровне 0 он «интегрировался» бы со сборкой, где половины этого нет, и держался бы только на отлове `LinkageError`.
- Исправлен краш «Проверить схему в зоне» без установленного Litematica Material HUD: `ZoneVerifier` дотягивался до API опционального мода через `ZoneScope` и падал с `NoClassDefFoundError: com/aell/layerlist/api/CountScope`. Проверка теперь читает свою опцию напрямую, а сборка падает, если что-то вне мостика совместимости снова сошлётся на классы Material HUD.
- Локализация на 12 языков: английский, русский, украинский, немецкий, французский, испанский, итальянский, польский, бразильский португальский, упрощённый китайский, японский, корейский.
