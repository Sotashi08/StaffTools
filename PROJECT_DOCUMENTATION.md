# StaffTools — Документация проекта

**StaffTools** — клиентский мод для Minecraft (Fabric, 1.21.1), набор утилит для модерации:
макросы с плейсхолдерами и алиасами, попап действий над игроками в чате, умная детекция ников
(включая поле ввода чата), история наказаний, toast-уведомления и темы оформления.
Локализация RU/EN.

- **Mod ID:** `stafftools`
- **Версия:** `1.1.1`
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
    │   ├── StaffToolsConfig    — модель конфига (+ themeIndex, chatInputDetectEnabled)
    │   └── ActionEntry         — пункт попапа действий (+ aliases)
    ├── i18n
    │   └── Lang                — локализация RU/EN + ключи строк (вкл. ph.* подсказки)
    ├── macro                   — система макросов
    │   ├── Macro               — модель макроса (+ aliases)
    │   ├── MacroManager        — CRUD макросов
    │   ├── MacroExecutor       — исполнение команд (+ requiresAlias / executeWithAlias)
    │   ├── MacroContext        — контекст переменных для плейсхолдеров
    │   ├── Placeholder         — record: плейсхолдер
    │   ├── PlaceholderRegistry — реестр плейсхолдеров (описания = ключи Lang ph.*)
    │   └── PlaceholderEngine   — подстановка <placeholder>
    ├── mixin                   — интеграция с Minecraft
    │   ├── ChatHudMixin
    │   ├── ScreenMixin
    │   ├── ChatScreenMixin     — + клик-детект ника из поля ввода
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
        ├── Ui                  — живая палитра (меняется темами), градиенты, звук ховера
        ├── Theme               — пресеты палитр (7 штук, вкл. градиентные) → apply() в Ui
        ├── StScreen            — базовый экран (статичный фон, анимация входа)
        ├── StafftoolsScreen    — главное меню (анимированный логотип, 4 варианта)
        ├── ThemesScreen        — сетка карточек выбора темы
        ├── Toasts              — toast-уведомления с прогрессом времени (singleton)
        ├── MacroListScreen     — список макросов (описание с переносом строк)
        ├── MacroEditorScreen   — редактор макроса (+ панель алиасов слева)
        ├── ActionsConfigScreen — настройка действий игрока
        ├── ActionEntryEditScreen — редактор действия (+ панель алиасов слева)
        ├── ActionHistoryScreen — история наказаний
        ├── PlayerActionOverlay — попап действий в чате (singleton, модалка алиасов)
        ├── EmojiPicker         — выбор эмодзи
        └── widget
            ├── StButton        — стилизованная кнопка (+ hover-звук)
            ├── StToggle        — кнопка-переключатель с цветовым индикатором (+ hover-звук)
            └── StTextField     — поле ввода (центрированный текст, setOnFocusGain)
```

### Поток данных (как всё связано)

1. **Чат**: `ChatHudMixin` перехватывает каждое сообщение → `ChatTextProcessor` детектит ники (многослойно) и делает их кликабельными; параллельно `PunishmentHistory.onMessageReceived()` ищет ответы сервера на отправленные наказания.
2. **Клик по имени**: `ScreenMixin` перехватывает `handleTextClick`:
   - свой префикс `stafftools:player:` → Shift+клик копирует имя, обычный клик открывает `PlayerActionOverlay`;
   - чужой click-event (`/msg <ник>` и т.п.) → резолвится игрок и тоже открывается попап (вместо вставки команды в поле ввода).
3. **Поле ввода чата**: `ChatScreenMixin` при клике по тексту, который вы пишете вручную, распознаёт ник онлайн-игрока (точное или однозначное частичное совпадение) и открывает попап действий.
4. **Попап**: показывает настроенные действия (`ActionEntry`) и включённые макросы; для макросов с `<alias>` открывается модалка выбора пункта; `MacroExecutor` подставляет плейсхолдеры через `PlaceholderEngine`, отправляет команды, подсвечивает строку и показывает toast.
5. **Команды**: `ClientPlayNetworkHandlerMixin` ловит каждую отправленную команду → `CommandAnalyzer` определяет наказание → запись в `PunishmentHistory`.
6. **UI**: хоткей Right Shift открывает `StafftoolsScreen`; `Toasts` рендерится на экранах и на HUD (`InGameHudMixin`); тема применяется из конфига на старте и меняется мгновенно через `ThemesScreen`.

---

## 1. Главные классы

### 1.1 `Stafftools` (main entrypoint)

Пустой — вся логика клиентская. Константа `MOD_ID = "stafftools"`.

### 1.2 `StafftoolsClient` (client entrypoint)

| Метод / поле | Описание |
|---|---|
| `onInitializeClient()` | Конфиг → язык → **`Theme.apply(themeIndex)`** → `PunishmentHistory` → `MacroManager` → хоткей Right Shift → тик-хендлер открытия главного меню. |
| `getMacroManager()` / `getConfig()` / `saveConfig()` | Глобальный доступ к менеджерам. |

---

## 2. Пакет `config`

### 2.1 `ConfigManager`
Gson (pretty printing), файл `config/stafftools.json`. `load()/save()/getConfig()`; при ошибке — откат к дефолту.

### 2.2 `StaffToolsConfig`
Поля: `chatMentionsEnabled`, `clickToCopyEnabled`, `playerActionsEnabled`, `dangerousMacroConfirmation`, `toastsEnabled`, **`themeIndex`**, **`chatInputDetectEnabled`**, `language`, `macros`, `actionEntries`. Все геттеры null-safe.

⚠️ Тумблеры «Копировать имя» и «Упоминания в чате» убраны из UI — функции работают всегда, поля конфига сохранены для совместимости.

### 2.3 `ActionEntry`
Пункт попапа: `icon`, `label`, `command`, `copyName`, `enabled`, `dangerous`, `confirmationRequired`, **`aliases`** (список вариантов для `<alias>`).
`dangerous` + `confirmationRequired` в UI объединены в один флаг «Подтверждение». `static defaults()` — Mute/Ban/Warn/Kick/Copy name.

---

## 3. Пакет `i18n`

### 3.1 `Lang`
Карта `Map<Language, Map<String,String>>` в `static {}`. Группы ключей: app, btn, toggle (вкл. `toggle.chatInput`), label (`label.aliases`), msg (`msg.aliasHelp` — многострочная справка `<alias>`), overlay (`overlay.aliasTitle`), hover, history/pun, toast, **`ph.*`** — подробные RU/EN описания каждого плейсхолдера (2 строки: назначение + пример).
Методы: `setLanguage`, `getLanguage`, `t(key[, args])`, `text(key[, args])`.

---

## 4. Пакет `macro`

### 4.1 `Macro`
`id`, `name`, `description`, `commands`, **`aliases`** (варианты для `<alias>`), `enabled`, `dangerous`, `confirmationRequired`.

### 4.2 `MacroManager`
Дефолты при пустом списке; `register` (upsert по id), `remove(UUID)`, `getMacros()`. Изменения сохраняются сразу.

### 4.3 `MacroExecutor`
- `execute(Macro, target)` / `executeCommands(List<String>, target)` — базовый путь;
- **`requiresAlias(Macro)`** — есть ли `<alias>` в командах;
- **`executeWithAlias(Macro, target, alias)`** — заменяет `<alias>` ДО резолва остальных плейсхолдеров, схлопывает двойные пробелы, пустой алиас допустим.

### 4.4 `MacroContext`
Переменные: `player`, `staff`, `x`, `y`, `z`, `ping`, `server`. (`health` удалён.)

### 4.5–4.7 `Placeholder` / `PlaceholderRegistry` / `PlaceholderEngine`
Реестр: `<player>`, `<staff>`, `<x>`, `<y>`, `<z>`, `<ping>`, `<server>` (порядок = порядок чипов). Описания — **ключи Lang `ph.*`** (локализуемые, двухстрочные).
`<alias>` — не в реестре: это служебный плейсхолдер макросов; чип добавляется в редакторе сразу после `<player>`.

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
| `resolveByDisplayName(text)` | Игрок, чей display-name из tab-list'а содержится в тексте. |
| `findPlayers(text)` | Игроки с целым вхождением имени; сортировка по длине убыванию. |

---

## 6. Пакет `chat`

### 6.1 `PlayerMention`
```java
record PlayerMention(PlayerContext player, int start, int end)
```

### 6.2 `NickAliases`
Обучаемая карта «кастомный ник → игрок» (до 500 записей): `learn(visibleText, player)` / `detect(message)`.

### 6.3 `PlayerMentionDetector`
Слои: прямой матчинг → §-очистка (с обратным маппингом индексов) → алиасы `NickAliases`.

### 6.4 `ChatTextProcessor`
Многослойный резолв игрока для узла текста: foreign click-event (`/msg <ник>` или любой ник в команде) → hover-текст → hover display-name → видимый текст (§-очистка → алиасы) → display-name в видимом тексте. При резолве через события/display-name вызывается `NickAliases.learn`. Успешный узел получает `mentionStyle` (жёлтый + ClickEvent `stafftools:player:<name>:<uuid>` + hover-подсказки).

---

## 7. Пакет `punishment`

### 7.1 `PunishmentStatus`
`PENDING` / `DONE` / `NO_RESPONSE`.

### 7.2 `PunishmentType`
BAN 🔨, MUTE 🔇, KICK 👢, WARN ⚠, FREEZE 🧊, OTHER ✏ — алиасы команд + RU/EN ключевые слова ответов.

### 7.3 `PunishmentRecord`
`playerName/type/command/timestamp/status/response`; таймаут ответа 30 c → NO_RESPONSE.

### 7.4 `CommandAnalyzer`
`analyze`, `isFailure`, **`extractIssuer`** (кто выдал наказание — из broadcast'ов), `normalize`.

### 7.5 `PunishmentHistory`
Singleton, до 300 записей. `onCommandSent` фиксирует свои наказания; `onMessageReceived` разрешает PENDING только если сообщение содержит имя игрока И (keyword типа ИЛИ failure) И **issuer не другой модератор** (`extractIssuer` + сравнение со своим ником).

---

## 8. Пакет `ui`

### 8.1 `Ui` — живая палитра
Цветовые поля **не final**: `Theme.apply(...)` их перезаписывает, все виджеты читают при рендере — смена темы мгновенная. Поля: `ACCENT`, **`ACCENT_2`** (второй цвет градиента), `ACCENT_SOFT/DARK`, `TEXT`, `TEXT_DIM/MUTED`, `DANGER(_SOFT)`, `SUCCESS`, `WARNING`, `PANEL_BG(_SOFT)`, `PANEL_BORDER`.
Методы: `argb`, `clamp01`, `easeOutCubic`, `mix`, `drawRoundRect`, `drawRoundBorder`, `drawPanel` (нижняя акцентная полоса — градиент при ACCENT_2≠ACCENT), `drawHeader`, **`drawGradientH(x,y,w,h,c1,c2,alpha)`** (полосками), **`playHover()`** (тихий тик BLOCK_NOTE_BLOCK_HAT).
⚠️ `drawRoundRect` ломается при `radius == size/2` — маленькие круги рисовать вручную.

### 8.2 `StScreen` (abstract)
- Анимация входа (260 мс, сдвиг снизу + fade); staggered-виджеты (28 мс/слот);
- **Статичный фон**: переопределён `renderBackground` (public в 1.21!) — свой тёмный градиент без ванильного анимированного блюра; флаг `suppressBackground` глушит повторную отрисовку фона внутри `super.render()`;
- Хуки `renderTheme/renderOverlay`; фабрики `button/toggle`; хелперы `drawTitle/drawHint/formWidth/formLeft`;
- ⚠️ Виджеты создавать **только в `init()`** — создание в render-методах накапливает копии каждый кадр.

### 8.3 Виджеты

#### `StButton`
Фон/рамка как у всей темы, hover lerp, акцентная полоса сверху, staggered появление, hover-звук.

#### `StToggle`
Выглядит как обычная кнопка; справа круглый цветовой индикатор (вкл = зелёно-акцентный, выкл = серый), рисуется вручную. Подпись слева. Hover-звук.

#### `StTextField`
Прозрачный фон, focus-подсветка, контент сдвинут `translate(4, 5)` (центрирование в рамке), колбэк `setOnFocusGain(Runnable)`.

### 8.4 `StafftoolsScreen` — главное меню
Центрированная колонка 220px: логотип → тэглайн → разделители с бегущим бликом → навигация (Макросы/Создать/Действия/История) → тумблеры (**Попап действий / Подтверждение опасных / Уведомления / Ник из поля ввода**) → Закрыть. Кнопка «🎨 Темы» — правый нижний угол **экрана**.
- Логотип: 4 случайных варианта (wave/comet/float/ripple), субпиксельно;
- Шиммер-линии: ping-pong, противофаза, синхронны с прогрессом входа;
- `% N` — только в long-пространстве.

### 8.5 `ThemesScreen extends StScreen`
Сетка карточек 4×N (карточка ~90×52, адаптивная ширина): градиентный превью-блок (реальные цвета пресета через `drawGradientH`), название, ✓ у активной, рамка-акцент, hover-lift −1px. Клики применяют тему + saveConfig + пересборка экрана. «Назад» внизу по центру. Виджеты создаются только в `init()`.

### 8.6 `MacroListScreen`
Виртуальный список; описание с **автопереносом до 2 строк** (word-wrap + обрезка длинных слов + «...»); бейдж «⚠ confirm».

### 8.7 `MacroEditorScreen extends StScreen`
Три панели: **алиасы слева** (высота ограничена контентом; ввод + «+», список с ✕ и hover-подсветкой, скролл), основной диалог по центру (max 330px), **инфо-панель справа**. Сетка от якорей (nameY=50, шаг 38). Чипы динамической ширины, `<alias>` после `<player>`. Тумблеры `Включено | Подтверждение`.

**Инфо-панель плейсхолдеров**: при ховере чипа справа появляется панель — тег акцентом + полное описание с примерами (перенос по словам). Заменила плавающие тултипы (те накладывались на контент).

### 8.8 `ActionsConfigScreen`
Список действий: ▲ ▼, Изменить, Удалить; «Добавить действие»/«Назад».

### 8.9 `ActionEntryEditScreen extends StScreen`
Как MacroEditorScreen (Иконка/Действие/Команда) + **та же панель алиасов слева** (алиасы сохраняются в `ActionEntry`) + инфо-панель справа. copyName из UI убран.

### 8.10 `ActionHistoryScreen extends StScreen`
Таблица Время/Действие/Игрок, зебра, staggered-строки, скролл.

### 8.11 `PlayerActionOverlay` (singleton)
Попап действий над чатом:
- **Жизненный цикл закрытия**: `requestClose(delayMs)` запускает fade+подъём+сжатие (160 мс), затем `finishClose()`; `delayMs=300` даёт время показать вспышку выполненной строки; во время закрытия ввод поглощается;
- **Плавный скролл**: колесо двигает `scrollTarget`, `scroll` догоняет его (delta*14);
- **Подтверждение**: двойной клик, 3 c, угасание красной подложки + таймер-полоска на строке (отдельной текстовой подсказки больше нет);
- **Модалка алиасов**: по центру, без затемнения, акцентная полоса шапки, опции-кнопки с обводками и каскадным выдвижением слева-направо; ESC → отмена; клик вне → отмена; пока открыта — весь ввод уходит в неё;
- **Grip** «⋯» справа сверху: пульсация, hover, перетаскивание;
- Шапка: ник + `NP: <всего>` (статичное число из истории);
- Вспышка SUCCESS на выполненной строке; hover-звук при смене строки;
- Закрытие при смене экрана (`ownerScreen`).

### 8.12 `Toasts` (singleton)
Правый нижний угол, стек до 4. Типы info/success/warn/danger; slide-in → hold (с прогресс-линией времени) → fade-out. Гейтится `config.isToastsEnabled()`.

### 8.13 `EmojiPicker`
Без изменений: сетка 40 эмодзи, авто-позиционирование.

---

## 9. Пакет `mixin`

### 9.1 `ChatHudMixin`
Подсветка сообщений (`@ModifyVariable`) + сопоставление ответов истории (`@Inject`).

### 9.2 `ScreenMixin`
`handleTextClick`(HEAD): свои click-events → копирование/попап; чужие (`/msg <ник>`) → `resolveFromForeignClick` → попап. `render`(TAIL): попап + тосты на всех экранах кроме ChatScreen.

### 9.3 `ChatScreenMixin`
- `render`(TAIL): попап + тосты поверх чата;
- `mouseClicked`(HEAD): **сначала клик-детект ника в поле ввода** (`tryChatInputDetect` — каретка по ширине префиксов, границы слова, точный/однозначный частичный матчинг), затем ввод в попап;
- `mouseScrolled`(HEAD) — скролл попапа;
- `keyPressed`(HEAD) — ESC через `handleEscape()`.
- `@Shadow input` — доступ к полю ввода чата.

### 9.4 `ParentElementMixin`
Drag/release попапа (default-методы ParentElement), только ChatScreen + открытый попап.

### 9.5 `ClientPlayNetworkHandlerMixin`
`sendChatCommand`(HEAD) → история наказаний.

### 9.6 `InGameHudMixin`
`render`(TAIL, `RenderTickCounter`) → тосты на HUD. ⚠️ Сигнатура именно с RenderTickCounter (float = краш миксина).

---

## 10. Ресурсы и данные

- `fabric.mod.json` — манифест; `environment: client`.
- `stafftools.client.mixins.json` — 6 клиентских миксинов.
- Хоткей: Right Shift.
- Данные: `config/stafftools.json`, `config/stafftools_history.json`.

## 11. Технические заметки

- `% N` для epoch-таймстампов — только в long (float теряет точность);
- Маленькие круги — рисовать вручную, не `drawRoundRect`;
- Анимации входа — от прогресса `entrance()`, не wall-clock;
- Возврат в главное меню — новый экземпляр экрана;
- Виджеты — создавать только в `init()`;
- `renderBackground` в 1.21.1 — **public** (protected нельзя override);
- `super.render()` рисует фон сам — глушится флагом `suppressBackground`;
- `super.render()` внутри матрицы анимирует ванильный фон — фон рисовать вне матрицы;
- `drawRoundRect` + радиус = половина стороны → «4 точки» вместо круга.
