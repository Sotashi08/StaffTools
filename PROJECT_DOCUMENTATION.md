# StaffTools — Документация проекта

**StaffTools** — клиентский мод для Minecraft (Fabric, 1.21.1), набор утилит для модерации:
макросы с плейсхолдерами, попап действий над игроками в чате, умная детекция ников,
история наказаний и toast-уведомления. Локализация RU/EN.

- **Mod ID:** `stafftools`
- **Версия:** `1.0-SNAP`
- **Автор:** uwdie
- **Minecraft:** 1.21.1 (Yarn mappings)
- **Fabric Loader:** ≥ 0.19.3, Fabric API
- **Java:** 21
- **Environment:** client (на сервере присутствие не требуется)

---

## Архитектура проекта

```
com.uwdie.stafftools
├── Stafftools                  — main entrypoint (пустой)
└── client
    ├── StafftoolsClient        — client entrypoint, регистрация хоткея, менеджеров
    ├── chat                    — обработка текста чата
    │   ├── ChatTextProcessor   — многослойная детекция ников + кликабельность
    │   ├── PlayerMentionDetector — поиск имён (прямой / §-очистка / алиасы)
    │   ├── NickAliases         — обучаемая карта «кастомный ник → игрок»
    │   └── PlayerMention       — record: упоминание (игрок + позиция)
    ├── config                  — конфигурация
    │   ├── ConfigManager       — загрузка/сохранение JSON
    │   ├── StaffToolsConfig    — модель конфига (+ toastsEnabled)
    │   └── ActionEntry         — пункт попапа действий
    ├── i18n
    │   └── Lang                — локализация RU/EN + ключи строк
    ├── macro                   — система макросов
    │   ├── Macro               — модель макроса
    │   ├── MacroManager        — CRUD макросов
    │   ├── MacroExecutor       — исполнение команд
    │   ├── MacroContext        — контекст переменных для плейсхолдеров
    │   ├── Placeholder         — record: плейсхолдер
    │   ├── PlaceholderRegistry — реестр плейсхолдеров
    │   └── PlaceholderEngine   — подстановка <placeholder>
    ├── mixin                   — интеграция с Minecraft
    │   ├── ChatHudMixin
    │   ├── ScreenMixin
    │   ├── ChatScreenMixin
    │   ├── ParentElementMixin
    │   ├── ClientPlayNetworkHandlerMixin
    │   └── InGameHudMixin      — рендер тостов на HUD
    ├── player
    │   ├── PlayerContext       — record: имя + UUID
    │   └── PlayerResolver      — поиск игроков (+ resolveByDisplayName)
    ├── punishment              — история наказаний
    │   ├── PunishmentHistory   — хранилище (singleton) + автодетект + фильтр чужих
    │   ├── PunishmentRecord    — запись о наказании
    │   ├── PunishmentType      — enum типов (ban/mute/kick/warn/freeze/other)
    │   ├── PunishmentStatus    — enum статусов
    │   └── CommandAnalyzer     — разбор команды → тип наказания + extractIssuer
    └── ui                      — интерфейс
        ├── Ui                  — цвета, анимации, примитивы рисования
        ├── StScreen            — базовый экран с анимацией входа
        ├── StafftoolsScreen    — главное меню (анимированный логотип, 4 варианта)
        ├── Toasts              — toast-уведомления (singleton)
        ├── MacroListScreen     — список макросов
        ├── MacroEditorScreen   — редактор макроса
        ├── ActionsConfigScreen — настройка действий игрока
        ├── ActionEntryEditScreen — редактор действия
        ├── ActionHistoryScreen — история наказаний
        ├── PlayerActionOverlay — попап действий в чате (singleton)
        ├── EmojiPicker         — выбор эмодзи
        └── widget
            ├── StButton        — стилизованная кнопка
            ├── StToggle        — кнопка-переключатель с цветовым индикатором
            └── StTextField     — поле ввода
```

### Поток данных (как всё связано)

1. **Чат**: `ChatHudMixin` перехватывает каждое сообщение → `ChatTextProcessor` детектит ники (6+ слоёв) и делает их кликабельными; параллельно `PunishmentHistory.onMessageReceived()` ищет ответы сервера на отправленные наказания.
2. **Клик по имени**: `ScreenMixin` перехватывает `handleTextClick`:
   - свой префикс `stafftools:player:` → Shift+клик копирует имя, обычный клик открывает `PlayerActionOverlay`;
   - чужой click-event (`/msg <ник>` и т.п.) → резолвится игрок и тоже открывается попап (вместо вставки команды в поле ввода).
3. **Попап**: показывает настроенные действия (`ActionEntry`) и включённые макросы; при выборе `MacroExecutor` подставляет плейсхолдеры через `PlaceholderEngine`, отправляет команды и показывает toast.
4. **Команды**: `ClientPlayNetworkHandlerMixin` ловит каждую отправленную команду → `CommandAnalyzer` определяет наказание → запись в `PunishmentHistory`.
5. **UI**: хоткей Right Shift (`StafftoolsClient`) открывает `StafftoolsScreen`; `Toasts` рендерится на экранах (`ScreenMixin`/`ChatScreenMixin`) и на HUD (`InGameHudMixin`).

---

## 1. Главные классы

### 1.1 `Stafftools` (main entrypoint)

Точка входа мода на общей стороне. Пустой — вся логика клиентская. Константа `MOD_ID = "stafftools"`.

### 1.2 `StafftoolsClient` (client entrypoint)

| Метод / поле | Описание |
|---|---|
| `onInitializeClient()` | Создаёт `ConfigManager`; выставляет язык; инициализирует `PunishmentHistory`; создаёт `MacroManager` (+ дефолтные макросы); регистрирует хоткей **Right Shift**; в `END_CLIENT_TICK` открывает новый `StafftoolsScreen()`. |
| `getMacroManager()` / `getConfig()` / `saveConfig()` | Глобальный доступ к менеджерам. |

---

## 2. Пакет `config`

### 2.1 `ConfigManager`
Gson (pretty printing), файл `config/stafftools.json`. `load()` / `save()` / `getConfig()`; при ошибке загрузки — откат к дефолту.

### 2.2 `StaffToolsConfig`
Поля: `chatMentionsEnabled`, `clickToCopyEnabled`, `playerActionsEnabled`, `dangerousMacroConfirmation`, **`toastsEnabled`** (новое), `language` (RU/EN), `macros`, `actionEntries`. Все геттеры null-safe.

### 2.3 `ActionEntry`
Пункт попапа: `icon`, `label`, `command`, `copyName`, `enabled`, `dangerous`, `confirmationRequired`.
`dangerous` и `confirmationRequired` в UI объединены в один флаг «Подтверждение» (синхронно записываются оба поля). `static defaults()` — Mute/Ban/Warn/Kick/Copy name.

---

## 3. Пакет `i18n`

### 3.1 `Lang`
Локализация без JSON: карта `Map<Language, Map<String,String>>` в `static {}`.
`Language { RU, EN }` с `@SerializedName`; `Key` — константы ключей (app, btn, toggle, label, msg, overlay, hover, history/pun, toast).
Методы: `setLanguage`, `getLanguage`, `t(key[, args])`, `text(key[, args])`.

---

## 4. Пакет `macro`

### 4.1 `Macro`
`id` (UUID), `name`, `description`, `commands`, `aliases`, `enabled`, `dangerous`, `confirmationRequired`. Списки выдаются как `List.copyOf`.

### 4.2 `MacroManager`
`initialize()` создаёт дефолты (Day, Night, Teleport, Mute) при пустом списке; `register` (upsert по id), `remove(UUID)`, `getMacros()`. Любое изменение сразу сохраняется.

### 4.3 `MacroExecutor`
`execute(Macro, target)` / `executeCommands(List<String>, target)`: `MacroContext.create(target)` → на каждую команду `PlaceholderEngine.resolve` → trim → срез `/` → `sendChatCommand`.

### 4.4 `MacroContext`
Переменные (актуальный набор): `player`, `staff`, **`x`, `y`, `z`** (блочные координаты игрока), **`ping`** (латентность цели из tab-list), **`health`** (HP игрока), **`server`** (адрес текущего сервера). Устаревшие `uuid/world/time/day` удалены.
`resolvePing(client, target)` (private static) ищет цель в player list.

### 4.5–4.7 `Placeholder` / `PlaceholderRegistry` / `PlaceholderEngine`
Реестр (`LinkedHashMap`, порядок = порядок чипов в UI): `<player>`, `<staff>`, `<x>`, `<y>`, `<z>`, `<ping>`, `<health>`, `<server>`.
`register/get/getAll` публичны. `PlaceholderEngine.resolve` — regex `<([a-zA-Z0-9_]+)>`, неизвестные шаблоны не трогаются, замена через `quoteReplacement`.

---

## 5. Пакет `player`

### 5.1 `PlayerContext`
```java
record PlayerContext(String name, UUID uuid)
```

### 5.2 `PlayerResolver`
| Метод | Описание |
|---|---|
| `resolve(name)` | Точное совпадение (ignore case). |
| `resolveByUuid(uuid)` | Поиск по UUID. |
| **`resolveByDisplayName(text)`** | Ищет игрока, чей **display-name из tab-list'а** содержится в тексте (для серверов с кастомными никами). Минимальная длина DN — 3 символа. |
| `findPlayers(text)` | Все игроки с целым вхождением имени в текст; сортировка по длине убыванию. |

---

## 6. Пакет `chat`

### 6.1 `PlayerMention`
```java
record PlayerMention(PlayerContext player, int start, int end)
```

### 6.2 `NickAliases` (новый)
Обучаемая карта «кастомный ник → игрок» (in-memory, до 500 записей).
- `learn(visibleText, player)` — запоминает токены из коротких (≤24 симв.) компонентов, когда реальный игрок известен через другой канал; токены, совпадающие с реальными никами онлайн-игроков, пропускаются;
- `detect(message)` — находит все вхождения выученных алиасов (границы слова).

### 6.3 `PlayerMentionDetector`
Слои детекта:
1. Прямой матчинг имён tab-list (все вхождения, границы слова);
2. Матчинг после **очистки §-кодов** (с обратным маппингом индексов для подсветки);
3. **Алиасы** из `NickAliases`.

### 6.4 `ChatTextProcessor`
Ядро подсветки. Порядок разрешения игрока для узла текста:
1. `resolveFromForeignClick(clickValue)` — команда вида `/msg|tell|whisper|w|m|t <ник>` (regex `COMMAND_PLAYER`) или любой ник tab-list внутри значения;
2. HoverEvent SHOW_TEXT → детект ника в тексте тултипа → display-name в тултипе;
3. Обычный текст → §-очистка → алиасы;
4. Display-name в видимом тексте узла.

При успешном резолве через события/display-name вызывается `NickAliases.learn`, узел копируется и получает `mentionStyle` (жёлтый, ClickEvent `stafftools:player:<name>:<uuid>`, hover-подсказки).
Ключевые методы: `process`, `processNode`, `resolveFromEvents`, `resolveFromForeignClick` (public — используется ScreenMixin), `buildHighlighted`, `styleName`, `mentionStyle`.

---

## 7. Пакет `punishment`

### 7.1 `PunishmentStatus`
`PENDING` / `DONE` / `NO_RESPONSE`.

### 7.2 `PunishmentType`
Enum: BAN 🔨, MUTE 🔇, KICK 👢, WARN ⚠, FREEZE 🧊, OTHER ✏. Несёт `commandAliases` и RU/EN `responseKeywords`. Методы: `getLabel`, `getIcon`, `matchesCommand`, `matchesResponse`, `match(head)`.

### 7.3 `PunishmentRecord`
`playerName`, `type`, `command`, `timestamp`, `status`, `response`. `RESPONSE_TIMEOUT_MS = 30_000`; `getEffectiveStatus()` возвращает NO_RESPONSE по таймауту.

### 7.4 `CommandAnalyzer`
- `analyze(rawCommand)` — команда → `PunishmentRecord` (тип по первому слову, игрок — первый аргумент без `-`);
- `isFailure(lowerText)` — признаки ошибки сервера;
- **`extractIssuer(lowerText)`** — regex `(?:by|от|модератором|администратором|модером|админом)\s+<ник>`: извлекает модератора из broadcast'ов;
- `normalize(text)`.

### 7.5 `PunishmentHistory`
Singleton, JSON `config/stafftools_history.json`, максимум 300 записей.
- `onCommandSent(command)` — фиксация своих наказаний (PENDING);
- `onMessageReceived(message)` — сопоставление ответов: сообщение должно содержать имя игрока записи И (keyword типа ИЛИ failure). **Фильтр чужих наказаний:** если `extractIssuer` нашёл модератора и это не локальный игрок (`client.player.getName()`), запись НЕ разрешается — чужие баны не съедают ваши PENDING-записи;
- `getRecords()`, `clear()`, `trim/load/save`.

---

## 8. Пакет `ui`

### 8.1 `Ui`
Палитра: `ACCENT` #3F8AE0, `ACCENT_SOFT/DARK`, `TEXT`, `TEXT_DIM/MUTED`, `DANGER(_SOFT)`, `SUCCESS`, `WARNING`, `PANEL_BG(_SOFT)`, `PANEL_BORDER`.
Методы: `argb(color, alpha)`, `clamp01`, `easeOutCubic`, `mix(a,b,t)`, `drawRoundRect`, `drawRoundBorder`, `drawPanel`, `drawHeader`.
⚠️ Известное ограничение: `drawRoundRect` деградирует при `radius == size/2` (нулевые заливки) — маленькие круги рисовать вручную (см. индикатор StToggle).

### 8.2 `StScreen` (abstract)
Анимация входа (260 мс, сдвиг снизу + fade); staggered-виджеты (28 мс/слот).
`openTime` — **не final**: переопределяемые экраны могут сбросить вход.
Хуки: `renderTheme` / `renderOverlay`. Фабрики: `button(...)`, `toggle(...)`. Хелперы: `drawTitle`, `drawHint`, `formWidth/formLeft` (max 380px).

### 8.3 Виджеты

#### `StButton`
Фон `0xFF171B22 → 0xFF23334B` (hover lerp delta*13), рамка `0xFF2C3B55 → accent`, радиус 3, акцентная полоса сверху, staggered появление.

#### `StToggle`
Выглядит как обычная кнопка (та же рамка/фон/ховер), справа — **круглый цветовой индикатор** вместо ползунка: зелёно-акцентный (вкл) / серый (выкл), рисуется вручную (fill-полосы + угловые пиксели, т.к. drawRoundRect ломается на кругах). Подпись слева.

#### `StTextField`
Прозрачный фон, focus-подсветка (`focusAmt` lerp), внутренний сдвиг контента `translate(4, 5)` для центрирования текста в рамке, колбэк `setOnFocusGain(Runnable)` (используется редакторами для вставки эмодзи/плейсхолдеров в последнее активное поле).

### 8.4 `StafftoolsScreen` — главное меню
Одна центрированная колонка 220px: анимированный логотип → тэглайн → разделители с бегущим бликом → 4 кнопки навигации → 3 тумблера (Попап действий / Подтверждение опасных / Уведомления) → Закрыть.
- **Логотип** — текстовый, 4 случайных варианта при открытии (`logoVariant`): wave / comet (ping-pong пробег) / float / ripple; субпиксельная плавность через матрицы;
- **Шиммер-линии** — ping-pong сегмент по разделителям, в противофазе; появляются синхронно с прогрессом входа (`alpha`), а не по wall-clock;
- ⚠️ Все временные циклы используют `% N` в **long**-пространстве (float теряет точность на epoch-таймстампах);
- Возврат из дочерних экранов — всегда через `new StafftoolsScreen()` (свежая анимация).

### 8.5 `MacroListScreen`
Виртуальный список (rebuild по needsRebuild). Строки: имя, бейдж «⚠ confirm» (объединённый), описание. Кнопки строки: Вкл/Выкл, Изменить, Удалить. «Назад» → `new StafftoolsScreen()`.

### 8.6 `MacroEditorScreen extends StScreen`
Центрированный диалог max 330px (`panelX/panelW`). Сетка от якорей `nameY=50 → descY+38 → cmdY+38`: поля Название/Описание/Команда (текст центрирован в рамках), кнопка 😀, тумблеры `Включено | Подтверждение` (единый флаг → setDangerous+setConfirmationRequired), чипы плейсхолдеров **динамической ширины**, Сохранить/Отмена. `lastFocused` — цель вставки эмодзи/плейсхолдеров.

### 8.7 `ActionsConfigScreen`
Список действий: ▲ ▼ (перестановка), Изменить, Удалить; «Добавить действие»/«Назад» (→ новый главный экран).

### 8.8 `ActionEntryEditScreen extends StScreen`
Как MacroEditorScreen, но поля Иконка(60)/Действие/Команда; тумблеры `Включено | Подтверждение` (copyName из UI убран — поле модели сохраняется как есть). Отмена: если parent — главный экран, создаётся новый экземпляр.

### 8.9 `ActionHistoryScreen extends StScreen`
Таблица Время/Действие/Игрок, зебра, staggered-строки, скролл. Кнопки Назад (→ новый главный экран) и Очистить (danger-accent).

### 8.10 `PlayerActionOverlay` (singleton)
Попап действий над чатом. Ключевые особенности:
- Закрытие при смене экрана: `ownerScreen` проверяется в `render()` (фикс «зависшего» попапа после отправки сообщения);
- Строки: зазор `ROW_GAP=4`, текст вертикально центрирован, обводки всех строк (тусклые/акцент при hover/DANGER при подтверждении);
- **Подтверждение с таймером**: `CONFIRM_MS=3000`, фейд подложки по остатку времени, прогресс-полоска на строке, отсчёт секунд в подсказке; подсказка живёт в расширенной футер-зоне (`FOOTER_H=26`) и не накладывается на строки;
- **Grip** «⋯» в правом верхнем углу: пульсирующая рамка, hover-подсветка, перетаскивание панели;
- Шапка: ник игрока + `NP: <total>` — общее число наказаний цели из истории;
- Тосты на копирование имени и выполнение действия;
- Анимация scale+fade открытия; закрытие по ESC/клику вне.

Методы: `open/close/isOpen`, `buildItems`, `layout`, hit-testing (`insidePanel/inGrip/inCopyRow/hitRow`), `render` + `drawCopyRow/drawBody/drawFooter/drawGrip`, ввод (`mouseClicked/Dragged/Released/Scrolled`), `punishmentSummary`, `handleRow`, `execute`, внутренние `RowItem`/`Row`.

### 8.11 `Toasts` (singleton, новый)
Уведомления в правом нижнем углу, стек до 4 штук.
- Типы: `info/success/warn/danger` (цветная боковая полоса);
- Анимация: slide-in справа (ease-out, 200 мс) → hold 2.6 с → fade-out с дрейфом;
- Рендерится везде: экраны (`ScreenMixin`, `ChatScreenMixin`) + HUD (`InGameHudMixin`);
- `push()` проверяет `config.isToastsEnabled()`;
- Триггеры: копирование ника, выполнение действия/макроса.

### 8.12 `EmojiPicker`
Сетка 40 эмодзи (8×18px), авто-позиционирование, `open(x,y,onPick)/close/mouseClicked/render`.

---

## 9. Пакет `mixin`

### 9.1 `ChatHudMixin`
| Инъекция | Описание |
|---|---|
| `@ModifyVariable stafftools$styleMessage` | Сообщение → `ChatTextProcessor.process`. |
| `@Inject stafftools$captureMessage` | Текст → `PunishmentHistory.onMessageReceived`. |

### 9.2 `ScreenMixin`
| Инъекция | Описание |
|---|---|
| `stafftools$onTextClick` (HEAD) | Свой префикс → парс `name:uuid`, резолв (UUID → имя → fallback), Shift+клик = копия, клик = попап. Чужие клики → `resolveFromForeignClick` → попап вместо вставки команды. |
| `stafftools$renderOverlay` (TAIL) | Попап + `Toasts.render` на любом экране кроме ChatScreen. |

### 9.3 `ChatScreenMixin`
`render`(TAIL): попап + тосты поверх чата; `mouseClicked`/`mouseScrolled`(HEAD) — ввод в попап; `keyPressed`(HEAD) — ESC закрывает только попап.

### 9.4 `ParentElementMixin`
Drag/release перехват на `ParentElement` (default-методы в 1.21.x), только для ChatScreen при открытом попапе.

### 9.5 `ClientPlayNetworkHandlerMixin`
`sendChatCommand`(HEAD) → `PunishmentHistory.onCommandSent`.

### 9.6 `InGameHudMixin` (новый)
`render`(TAIL, сигнатура с `RenderTickCounter`) → `Toasts.render` — уведомления видны в игре без экранов.

---

## 10. Ресурсы и данные

- `fabric.mod.json` — манифест; `environment: client`; описание мода.
- `stafftools.client.mixins.json` — 6 клиентских миксинов.
- Хоткей: `key.stafftools.open` = **Right Shift**.
- Данные: `config/stafftools.json`, `config/stafftools_history.json`.

## 11. Известные технические заметки

- Модули времени: никогда не использовать `% N` во float для epoch-таймстампов (потеря точности ~131 с);
- Маленькие круги не рисовать через `Ui.drawRoundRect` (radius == size/2 ломает заливку);
- Анимации появления привязывать к прогрессу `entrance()`, а не к wall-clock;
- Возврат в главное меню — только через новый экземпляр экрана.
