# Шаг 1. Полный аудит текущих экипировки и рейдов

[← К поэтапному плану](raid-redesign-plan.md)

Статус: **завершён 2026-08-31**. Игровой код и game-data на этом шаге не менялись.

Аудит выполнен по production-коду, TOML-каталогам, Liquibase migrations и калибровочным тестам. Упакованные в репозиторий значения считаются baseline; комментарии и названия тестов, которые им противоречат, источником истины не являются. Игровой код и данные на этом шаге не менялись.

## Каталог `ItemObject`

Источник — `game/src/main/resources/game-data/item_objects_catalog.toml`. В каталоге 40 уникальных объектов: 16 атакующих и 24 защитных, hybrid-объектов нет. Распределение типов атаки: PIERCE 6, SLASH 4, BLUNT 4, MAGICAL 2. Защитных объектов ровно по 6 для CLOTH, LEATHER, PLATE и ARCANE.

| Группа | Кол-во | Объекты |
|---|---:|---|
| Только MAIN_HAND | 5 | `sword`, `rapier`, `mace`, `spear`, `staff` |
| MAIN_HAND + OFF_HAND | 6 | `bow`, `longbow`, `crossbow`, `two_handed_sword`, `sledgehammer`, `halberd` |
| OFF_HAND, attack | 5 | `shortsword`, `club`, `dagger`, `dirk`, `orb` |
| OFF_HAND, defense | 4 | `buckler`, `shield`, `tower_shield`, `tome` |
| BODY | 4 | `robe`, `cuirass`, `breastplate`, `wizard_robe` |
| PANTS | 4 | `cloth_chausses`, `leather_chausses`, `greaves`, `arcane_chausses` |
| SHOES | 4 | `cloth_boots`, `boots`, `sabatons`, `arcane_boots` |
| HELMET | 4 | `hood`, `leather_helm`, `great_helm`, `circlet` |
| GLOVES | 4 | `cloth_gloves`, `leather_gloves`, `gauntlets`, `arcane_gloves` |

Полный числовой baseline приведён ниже. `Crit×` — добавка к базовому crit multiplier, `A@R` — attack на максимальной дистанции R.

| Code | Slots | Attack | Defense | HP | Crit | Dodge | Crit× | Speed | Threat |
|---|---|---|---|---:|---:|---:|---:|---:|---:|
| `sword` | MAIN | SLASH 300@1 | — | 0 | 2 | 2 | .05 | 15 | 8 |
| `rapier` | MAIN | PIERCE 285@1 | — | 0 | 4 | 5 | .08 | 20 | 4 |
| `mace` | MAIN | BLUNT 298@1 | — | 0 | 2 | 1 | .05 | 15 | 12 |
| `spear` | MAIN | PIERCE 310@2 | — | 0 | 3 | 2 | .05 | 10 | 5 |
| `staff` | MAIN | MAGICAL 310@3 | — | 0 | 3 | 1 | .08 | 10 | 5 |
| `bow` | MAIN + OFF | PIERCE 360@2 | — | 0 | 4 | 2 | .05 | 35 | 4 |
| `longbow` | MAIN + OFF | PIERCE 380@3 | — | 0 | 3 | 1 | .05 | 31 | 5 |
| `crossbow` | MAIN + OFF | BLUNT 370@4 | — | 0 | 5 | 1 | .08 | 35 | 8 |
| `two_handed_sword` | MAIN + OFF | SLASH 400@1 | — | 0 | 4 | 1 | .10 | 25 | 10 |
| `sledgehammer` | MAIN + OFF | BLUNT 415@1 | — | 0 | 2 | 0 | .05 | 20 | 12 |
| `halberd` | MAIN + OFF | PIERCE 398@2 | — | 0 | 3 | 0 | .05 | 25 | 10 |
| `shortsword` | OFF | SLASH 50@1 | — | 0 | 2 | 2 | .05 | 25 | 2 |
| `club` | OFF | BLUNT 50@1 | — | 0 | 1 | 1 | .03 | 25 | 8 |
| `dagger` | OFF | SLASH 42@1 | — | 0 | 5 | 1 | .10 | 18 | 1 |
| `dirk` | OFF | PIERCE 52@1 | — | 0 | 2 | 5 | .05 | 28 | 4 |
| `orb` | OFF | MAGICAL 50@2 | — | 0 | 2 | 1 | .05 | 22 | 2 |
| `buckler` | OFF | — | CLOTH 10 | 160 | 0 | 4 | 0 | 28 | 0 |
| `shield` | OFF | — | LEATHER 16 | 280 | 0 | 2 | 0 | 20 | 0 |
| `tower_shield` | OFF | — | PLATE 32 | 340 | 0 | 0 | 0 | 12 | 8 |
| `tome` | OFF | — | ARCANE 22 | 250 | 0 | 0 | 0 | 26 | 0 |
| `robe` | BODY | — | CLOTH 32 | 500 | 1 | 1 | .05 | 38 | 1 |
| `cuirass` | BODY | — | PLATE 64 | 680 | 0 | 0 | 0 | 16 | 12 |
| `breastplate` | BODY | — | LEATHER 48 | 600 | 1 | 2 | .05 | 26 | 2 |
| `wizard_robe` | BODY | — | ARCANE 58 | 510 | 1 | 1 | .05 | 32 | 1 |
| `cloth_chausses` | PANTS | — | CLOTH 12 | 230 | 1 | 1 | .05 | 32 | 0 |
| `leather_chausses` | PANTS | — | LEATHER 18 | 340 | 1 | 1 | .05 | 24 | 1 |
| `greaves` | PANTS | — | PLATE 36 | 420 | 0 | 0 | 0 | 14 | 8 |
| `arcane_chausses` | PANTS | — | ARCANE 22 | 280 | 1 | 1 | .05 | 28 | 0 |
| `cloth_boots` | SHOES | — | CLOTH 7 | 120 | 1 | 1 | .05 | 26 | 0 |
| `boots` | SHOES | — | LEATHER 10 | 220 | 1 | 2 | .05 | 18 | 1 |
| `sabatons` | SHOES | — | PLATE 22 | 280 | 0 | 0 | 0 | 10 | 6 |
| `arcane_boots` | SHOES | — | ARCANE 14 | 160 | 1 | 1 | .05 | 22 | 0 |
| `hood` | HELMET | — | CLOTH 7 | 120 | 1 | 1 | .05 | 24 | 1 |
| `leather_helm` | HELMET | — | LEATHER 10 | 220 | 1 | 0 | .05 | 16 | 1 |
| `great_helm` | HELMET | — | PLATE 22 | 280 | 0 | 0 | 0 | 10 | 6 |
| `circlet` | HELMET | — | ARCANE 14 | 160 | 4 | 1 | .10 | 20 | 1 |
| `cloth_gloves` | GLOVES | — | CLOTH 7 | 120 | 1 | 1 | .05 | 26 | 1 |
| `leather_gloves` | GLOVES | — | LEATHER 10 | 220 | 1 | 0 | .05 | 16 | 1 |
| `gauntlets` | GLOVES | — | PLATE 22 | 280 | 0 | 0 | 0 | 10 | 4 |
| `arcane_gloves` | GLOVES | — | ARCANE 14 | 160 | 1 | 1 | .05 | 22 | 0 |

Диапазоны по slot eligibility; двухслотовые предметы входят одновременно в MAIN и OFF:

| Slot | N (attack/defense) | Attack | Defense | Range | HP | Crit | Dodge | Crit× | Speed | Threat |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| MAIN_HAND | 11 (11/0) | 285–415 | — | 1–4 | 0 | 2–5 | 0–5 | .05–.10 | 10–35 | 4–12 |
| OFF_HAND | 15 (11/4) | 42–415 | 10–32 | 1–4 | 0–340 | 0–5 | 0–5 | 0–.10 | 12–35 | 0–12 |
| BODY | 4 (0/4) | — | 32–64 | — | 500–680 | 0–1 | 0–2 | 0–.05 | 16–38 | 1–12 |
| PANTS | 4 (0/4) | — | 12–36 | — | 230–420 | 0–1 | 0–1 | 0–.05 | 14–32 | 0–8 |
| SHOES | 4 (0/4) | — | 7–22 | — | 120–280 | 0–1 | 0–2 | 0–.05 | 10–26 | 0–6 |
| HELMET | 4 (0/4) | — | 7–22 | — | 120–280 | 0–4 | 0–1 | 0–.10 | 10–24 | 1–6 |
| GLOVES | 4 (0/4) | — | 7–22 | — | 120–280 | 0–1 | 0–1 | 0–.05 | 10–26 | 0–4 |

Выбросы, которые требуют явного решения на шаге 5: `crossbow` — единственный range 4 и имеет BLUNT; `circlet` имеет crit 4 и Crit× .10 против crit 0–1 и Crit× 0–.05 у остальных helmets; MAGICAL представлен только двумя объектами. Virtual default items из `DefaultItems` не входят в TOML и loot pool, но автоматически заполняют свободные slots.

## Modifiers, rarity и enhancement

Источник modifiers — `game/src/main/resources/game-data/item_modifiers_catalog.toml`; строк 11.

| Code | Skill | Type | Допустимые slots |
|---|---|---|---|
| `counter` | COUNTER_ATTACK | DEFENSE | BODY, PANTS, HELMET, GLOVES |
| `thorny` | THORNS | DEFENSE | BODY, PANTS, GLOVES |
| `double` | DOUBLE_ATTACK | ATTACK | MAIN_HAND, OFF_HAND |
| `furious` | BERSERK | ANY | MAIN_HAND, OFF_HAND, BODY |
| `swift` | HIT_AND_RUN | DEFENSE | SHOES, HELMET, GLOVES |
| `sharp` | BLEEDING | ATTACK | MAIN_HAND, OFF_HAND |
| `knocking` | KNOCKBACK | ANY | MAIN_HAND, OFF_HAND, SHOES, GLOVES |
| `healing` | SELF_HEAL | DEFENSE | BODY, PANTS, SHOES, HELMET |
| `precise` | PRECISE_STRIKE | ATTACK | MAIN_HAND, OFF_HAND |
| `retreating` | RETREAT | DEFENSE | PANTS, SHOES, HELMET |
| `cunning` | FEINT | ANY | MAIN_HAND, OFF_HAND, SHOES, GLOVES |

- Rarity не меняет статы. Она даёт modifier points: COMMON 0, UNCOMMON 1, RARE 2, EPIC 3, LEGENDARY 4.
- Число points умножается на количество занятых slots; поэтому двухслотовый предмет получает 0/2/4/6/8 points. Пороги skill rank — 1/2/4/6/8 points: UNCOMMON двухслотовый предмет уже даёт rank 2, LEGENDARY — rank 5; однослотовый LEGENDARY даёт только rank 3.
- Points одинакового `ActiveEnum` суммируются по всей экипировке. COMMON modifier в бою не активен. При COMMON → UNCOMMON modifier выбирается один раз, следующие rarity upgrades его сохраняют.
- Compatibility определяется типом объекта и одним slot, переданным генератору; для multi-slot предмета не проверяется, что modifier поддерживает все занятые slots.
- Обычный rarity-upgrade двухслотового предмета не дороже однослотового, несмотря на удвоенные skill points.
- Storm enhancement линейно масштабирует только attack, defense и HP: `round(base × (1 + level × 5%))`. Вторичные статы, range и modifier rank не меняются. Цена — `round(10 × 1.25^level × slotCount)`; с уровня 4 возможен rollback, верхнего cap и DB constraint для level нет. На L10 множитель статов равен 1.5, цена — 93/186 shards для одного/двух slots, исходы success/failure/rollback — 10/63/27%.

## Получение предметов и реальный loot pool

Все production-вызовы генерации предмета сведены к пяти путям:

| Путь | Условие и chance | Slot | Rarity |
|---|---|---|---|
| Обычный raid | Только win и non-exhausted; pity `r%` при `r≤5`, затем `floor((r-3)^1.8)%`, buffs добавляют п.п. | Персональный raid slot deck | От `raidLevel` |
| World raid | Только win; `70 + 10r%`, гарантирован с третьего dry result | Uniform 1/7 | weights 5/15/50/20/10 |
| Shop fixed/random | Покупка по выбранному shop type | Отдельный slot deck для каждого из шести типов | Fixed либо 44/24/14/11/7 |
| Shop exact object | Выбранный `objectId` | Slot объекта | Всегда COMMON без modifier |
| Contraband reward | Успешное открытие; item является одним из возможных rewards tier | Uniform 1/7 | Зависит от tier контрабанды |

Обычный raid сначала проверяет contraband с шансом `0 + 10 × winsSinceLastContraband` процентов; активная незавершённая контрабанда блокирует новую. Item pity и contraband pity независимы и не сбрасывают друг друга. Тир определяется тем же `raidLevel`:

| Raid level | Contraband tier | Цена продажи | Raw weights rarity предмета |
|---:|---|---:|---|
| ≤15 | COMMON | 10 | Common 30, Uncommon 70 |
| 16–30 | UNCOMMON | 50 | Common 10, Uncommon 60, Rare 30 |
| 31–45 | RARE | 100 | Uncommon 15, Rare 60, Epic 35 |
| >45 | EPIC | 200 | Rare 15, Epic 60, Legendary 35 |

В последних двух строках сумма raw weights равна 110, поэтому это веса для нормализации, а не готовые проценты.

`SlotRandomPool` — очередь из двух перемешанных копий семи slots, то есть 14 выдач и ровно две выдачи каждого slot. Он не содержит codes объектов. После выбора slot выполняется `ORDER BY random()` по всем строкам `item_object`, у которых массив slots содержит этот slot; признака `enabled/lootable` нет. Если БД совпадает с TOML, long-run chance отдельного armor object равен 3.571%, двухслотового weapon — 2.251%, MAIN-only — 1.299%, OFF-only — 0.952%.

Фактическое распределение rarity обычного raid item противоречит комментариям `RaidLevelItemConfig`: RARE имеет ненулевой base weight уже на D1.

| Raid level | Common | Uncommon | Rare | Epic | Legendary |
|---:|---:|---:|---:|---:|---:|
| D1–D10 | 36.36% | 36.36% | 27.27% | 0 | 0 |
| D11 | 29.06% | 32.48% | 38.46% | 0 | 0 |
| D20 | 0.50% | 9.95% | 89.55% | 0 | 0 |
| D30 | 0.29% | 0.29% | 96.77% | 2.64% | 0 |
| D40 | 0.20% | 0.20% | 94.12% | 3.33% | 2.16% |
| D50 | 0.15% | 0.15% | 92.92% | 3.69% | 3.10% |

## Сохранение и совместимость предметов

- На старте приложения TOML-каталоги upsert-ятся по `code`. Экземпляр `item` хранит ссылки на object/modifier, rarity ordinal, owner, equipped и enhance level; статы и slots не snapshot-ятся.
- Изменение существующего object code ретроактивно меняет все его экземпляры, включая экипированные. Изменение slots может создать уже экипированный конфликт и меняет modifier points.
- Удаление или переименование code в TOML не удаляет старую строку БД. Она остаётся в random loot и exact-object shop; физическому удалению мешают FK от существующих items и raid results.
- Rarity хранится как ordinal, attack/defense/skill enums — строками. Перестановка rarity enum или rename других enums несовместимы с сохранёнными данными.
- Продажа и donation снимают owner, но не удаляют item. При полной сумке генерация также успевает создать orphan item; raid/world raid фиксируют его как generated item и сбрасывают pity, shop возвращает деньги, contraband превращает результат в `Gold(0)`.
- Upsert всего каталога и выдача raid rewards не образуют единой транзакции. Частичный сбой может оставить смешанное состояние.

## Reference loadouts и перекосы выборки

`ItemObjectsCombinationTest` перечисляет 52 224 допустимых полных COMMON-сборки. Диапазон текущего `power()` — 80 319.91–104 507.39, среднее 95 879.49, p50 96 089.00, p95 100 812.21. Минимум: `mace + dagger + robe + cloth_chausses + cloth_boots + hood + cloth_gloves`; максимум: `staff + tome + wizard_robe + leather_chausses + boots + circlet + arcane_gloves`.

| Reference loadout | HP | Attack | Defense | Speed | Dodge | Current power |
|---|---:|---|---|---:|---:|---:|
| Mace + full plate | 2280 | 298 BLUNT R1 | 198 PLATE | 87 | 1 | 82 689.66 |
| Spear/dirk + leather | 1600 | 362 PIERCE R2 | 96 LEATHER | 138 | 12 | 100 421.69 |
| Sword/shortsword + leather | 1600 | 350 SLASH R1 | 96 LEATHER | 140 | 9 | 97 986.43 |
| Staff/orb + arcane | 1270 | 360 MAGICAL R3 | 122 ARCANE | 156 | 7 | 95 858.76 |
| Staff/orb + cloth | 1090 | 360 MAGICAL R3 | 65 CLOTH | 178 | 7 | 83 465.05 |
| Virtual default gear | 720 | 200 BLUNT R1 | 64 CLOTH | 140 | 18 | 24 264.13 |

Random sampler в `RaidWinrateTest` не равномерен по этим 52 224 сборкам: сначала равновероятно выбирается один из 11 MAIN objects, поэтому двуручные составляют 54.55% выборки вместо 11.76% при равномерной выборке полных комбинаций. Конкретная двухслотовая сборка встречается в 9 раз чаще конкретной `1H + OFF`; шанс полностью однородного armor material при независимой выборке пяти slots — только 0.390625%. Все тестовые предметы COMMON без modifiers, а все игроки принудительно FRONT.

## Ограничения `BattlePersonage.power()` и spatial-модели

- Dodge в `power()` фактически игнорируется: `Math.max(1, 1 - dodgeProbability)` всегда возвращает 1 для обычных значений. Реальный бой dodge применяет; поэтому starter gear с 18% dodge выглядит слабым для raid scaler, но существенно сильнее в бою.
- Формула не учитывает range, start position, attack type matchup, threat/targeting и action economy группы. KNOCKBACK, RETREAT и HIT_AND_RUN дают нулевой `skillPowerRating`.
- Defense усредняется с одинаковым весом по четырём attack types. Crit в power не clamp-ится к 100%; speed растёт линейно даже за пределами реальной частоты одного действия за tick.
- Attack предмета полностью действует на каждой дистанции `1..range`, без falloff. Если при инициализации персонаж не достаёт до ближайшего врага, `BattleContext` молча переносит его вперёд; если цель позже недоступна, ход расходуется на один шаг.
- Метрика `damageBlocked` записывает raw incoming damage, а `damageTaken()` возвращает `damageBlocked + damageDodged`, не фактически потерянное HP. Новый simulator не должен переиспользовать это поле как received damage.
- RNG распределён между default generator, `Collections.shuffle()`, UUID/порядком map и множеством боевых rolls. Один seed только в `RandomUtils` не сделает симуляцию воспроизводимой.

Матрица умножает defense перед формулой `damageMultiplier = 500 / (500 + effectiveDefense)`; большее значение означает лучший resist:

| Defense \ incoming attack | SLASH | BLUNT | PIERCE | MAGICAL |
|---|---:|---:|---:|---:|
| CLOTH | .75 | 1.25 | .90 | 1.10 |
| LEATHER | .90 | 1.10 | 1.25 | .75 |
| PLATE | 1.25 | .75 | 1.10 | .90 |
| ARCANE | 1.10 | .90 | .75 | 1.25 |

## Каталог и генерация обычных рейдов

Обычные `RAID` и `WORLD_RAID` — независимые подсистемы. Ниже описан только обычный raid; world raid не использует `RaidParams` или `pgroup.raid_level`. В production включены ровно четыре типа из `game-data/prod/raids.toml`:

| Тип | Состав при N участниках | Базовые особенности | Correction |
|---|---|---|---:|
| `wolfpack` | N wolves + alpha; все FRONT | Wolves: SLASH/LEATHER, HP400/A100/D50, BLEEDING. Alpha: HP1500/A300/D100, BERSERK | 1.00 |
| `zombie_horde` | N zombies + boss; все FRONT | Zombies: случайно BLUNT/SLASH, PLATE, HP500/A80/D200, THORNS. Boss: HP2000/A180/D350, SELF_HEAL | 1.00 |
| `myconid_colony` | `M=max(2,N)`, половина Guardians FRONT, остальные Slingers BACK | Guardian: MAGICAL/ARCANE, HP420/A70/D160, KNOCKBACK. Slinger: HP280/A130/D70, PRECISE_STRIKE | 0.77 |
| `maggeese_flock` | `F=max(2,N)`, половина Chargers FRONT, остальные Mageese MID | Charger: SLASH/CLOTH, HP320/A95/D70, DOUBLE_ATTACK. Mageese: MAGICAL/CLOTH, HP270/A120/D50, HIT_AND_RUN | 0.75 |

Все восемь NPC используют attack range 1. Их фиксированные secondary stats до scaling:

| NPC | Crit | Dodge | Crit× | Speed | Threat |
|---|---:|---:|---:|---:|---:|
| Wolf | 5 | 5 | +.50 | 300 | 30 |
| Alpha wolf | 15 | 15 | +1.00 | 200 | 10 |
| Zombie | 3 | 2 | +.30 | 200 | 25 |
| Zombie boss | 8 | 3 | +.50 | 150 | 50 |
| Guardian | 4 | 3 | +.30 | 190 | 35 |
| Slinger | 8 | 5 | +.50 | 160 | 15 |
| Charger | 6 | 8 | +.40 | 280 | 30 |
| Mageese | 10 | 8 | +.60 | 220 | 20 |

Skill у каждого NPC задаётся одним LEGENDARY однослотовым предметом: 4 modifier points, то есть rank 3.

Slinger объявлен ranged/BACK, но имеет range 1 и потому при старте переносится вперёд. JavaDoc Mageese обещает FEINT, тогда как код выдаёт HIT_AND_RUN. Старый `legacy_raids.toml` с 18 типами текущим loader не читается; test profile загружает только `wolfpack`, но upsert-loader не деактивирует ранее сохранённые production rows.

Общая цель каждого генератора:

```text
targetPower = Σ player.power()
              × (0.54 + 0.06 × raidLevel)
              × groupSizeScaling
              × typeCorrection
```

Уровневый множитель равен 0.60/0.84/1.14/1.74/2.34/3.54 на L1/L5/L10/L20/L30/L50. Binary search меняет у NPC только attack, defense и HP; crit, dodge, speed, threat, range, position и skill фиксированы.

Piecewise group-size функции имеют немонотонные разрывы:

| Переход | До | После | Причина |
|---|---:|---:|---|
| Myconid N7 → N8 | 1.1234 | 1.0750 | Смена `log₂․₂` на `log₄` |
| Zombie N10 → N11 | 1.1460 | 1.0865 | Смена `log₂․₂` на `log₄` |
| Maggeese N10 → N11 | 1.1460 | 1.0865 | Смена `log₂․₂` на `log₄` |
| Wolf N13 → N14 | 1.1464 | 1.1000 | Смена на `1.1 + log₁₀(N-13)×.045` |
| Zombie N13 → N14 | 1.0925 | 1.0635 | Смена `log₄` на `log₈` |

## Карта зависимостей raid progression

```text
pgroup.raid_level
  → snapshot RaidParams(raidLevel, 0) в launched_event.event_params
  ├→ Telegram: отображаемый уровень
  ├→ targetPower генератора
  ├→ raid points: win 2L / loss L
  ├→ rarity обычного raid item
  └→ tier контрабанды
      ↓ после завершённого боя
pgroup.raid_level += 1 при win / -= 2 при loss, clamp 1..50
```

| Узел | Текущее поведение | Зависимые данные и риск изменения |
|---|---|---|
| `pgroup.raid_level` | Новая группа стартует с L10; хранит mutable current level, отдельного record нет | Генерация следующего raid и `/topg_raid_level` |
| `RaidParams` | При старте фиксирует level и `raidPoints=0` в JSONB; старое событие без params читается как L10/0 | История событий, UI, battle generation, reward economy, SQL tops |
| Event launch | Планировщик выбирает случайный enabled raid; окно участия 30 минут | Ошибка Telegram создаёт `CREATION_ERROR`; next date не сдвигается |
| Join/loadout | Вход стоит 20 energy; без energy игрок вступает exhausted за 0; cancel до конца возвращает `max(spent-1,0)` | Exhausted участвует в бою и влияет на level/points, но не получает money/item. Gear, position, tactic и effects читаются заново при завершении, не snapshot-ятся при join |
| Completion | Win +1, loss −2, clamp 1..50; EXPIRED без участников ничего не меняет | Один level одновременно означает difficulty, progress и reward tier |
| Money | Loss 10; win `round(10 + max(0, log₁․₁((dealt+taken)/10)-43))`, затем bonuses | Нет прямой зависимости от L или N; зависит от спорной battle stats semantics |
| Items | Только win/non-exhausted; contraband roll выполняется раньше обычного item | Оба pity независимы; full bag может сбросить item pity без полученного предмета |
| Raid points | Win `2L`, loss `L`; записываются обратно в event params | Изменение JSON key обнулит SQL-derived weekly tops |
| Season stats | После processing аддитивный upsert group/personage success/fail и points | Нет event-id/idempotency; exhausted также учитываются в success/fail |
| Weekly tops | Пересчитываются из `event_params.raidPoints`; группа получает points один раз, каждый non-exhausted участник — полную сумму | Семантика отличается от season aggregate и current-level top |
| Battle history | `event_battle_log` + персональные result rows; backend/visualizer читают log | Обновление не атомарно; retry после частичного сбоя может упереться в PK log и оставить частично выданные rewards |
| Telegram/report | Старт/результат показывают level, но не начисленные points; есть weekly и level tops | Group report может выбрать более поздний EXPIRED/CREATION_ERROR вместо последнего боя; raid-localization неполна вне RU |

Упакованные outpost defaults добавляют Shadow Shop `+5% gold/level` и Storm Scanner `+3` процентных пункта item chance/level. Для гостя используются passives его собственной member group, а не группы-хоста рейда.

Важная транзакционная граница: battle log, индивидуальные money/items/results, group level, terminal event params, season stats и Telegram обновляются последовательно без общей транзакции и idempotency key. Редизайн progression не должен расширять этот риск незаметно.

## Информационный Myconid baseline

До появления seeded simulator прогнаны 360 000 боёв: 3 профиля × N1–20 × L5–10 × 1 000. Это диагностический, а не acceptance baseline; при 1 000 боях максимальная 95% погрешность одной доли около ±3.1 п.п.

| N на L10 | Random catalog | Virtual default | Counter-filter |
|---:|---:|---:|---:|
| 1 | 1.9% | 10.9% | 4.9% |
| 2 | 63.3% | 83.1% | 86.3% |
| 3 | 59.2% | 84.5% | 80.5% |
| 7 | 47.0% | 86.4% | 82.2% |
| 8 | 64.7% | 92.8% | 93.7% |
| 15 | 56.1% | 96.3% | 95.0% |
| 20 | 55.1% | 97.1% | 96.5% |

Скачок N1 → N2 вызван прежде всего одинаковыми двумя NPC при `max(2,N)` и неучтённой action economy; N7 → N8 совпадает с падением scale на 4.31%. Каждый профиль порождает новый encounter, масштабированный от собственного ошибочного `power()`, поэтому таблица не сравнивает сборки на одном fixed encounter.

## Зафиксированные выводы и маршрутизация рисков

- До шага 7 существующие codes нельзя удалять или переименовывать. Retire требует явного `lootable/enabled`, фильтрации random/exact shop и проверки фактических строк БД.
- На шагах 4–6 нужно явно решить, остаются ли изменения статов по существующему code ретроактивными; текущая модель всегда ретроактивна.
- На шаге 2 seeded RNG должен охватить initiative, shuffle, UUID/порядок обхода, target selection, crit/dodge/damage, NPC variants и sampler loadouts. Нужны новые корректные damage dealt/taken counters.
- На шаге 4 нельзя использовать текущий `power()` как item budget без отдельной замены/валидации. На шагах 9–12 fixed-difficulty raid не должен зависеть от player power.
- На шагах 5 и 9 отдельно подтвердить BLUNT у `crossbow`, реальную роль Slinger, HIT_AND_RUN у Mageese и baseline D1–D10 с 27.27% Rare.
- На шагах 7 и 15 проверить full-bag/pity, атомарность и идемпотентность reward processing, stale item/raid rows и backward-compatible `event_params`.
- Фактические production inventories, stale catalog rows, распределение уровней и внешние overrides нельзя получить из репозитория. Локальный PostgreSQL на шаге был недоступен; сверка persistent DB является обязательной входной проверкой шага 7. Все code paths, которые могут изменить существующий item или progression, при этом найдены и перечислены выше.

## Проверки шага

- Targeted item/loot suite: `ItemObjectsCombinationTest`, `ItemModifierSlotStatsTest`, `EquippedCharacteristicsTest`, `ItemServiceGenerateItemTest`, `ItemStormEnhanceTest`, `RaidLevelItemConfigIntegrationTest`, `RaidItemGeneratorTest` — **23/23 passed**.
- Mitigation и targeting: `BattlePersonageMitigatedDamageTest`, `TargetingTacticWeightTest` — **34/34 passed**; в текущем JDK для Mockito требовался явный Java agent.
- Myconid diagnostic matrix — **3/3 tests passed**, 360 000 боёв, 123.3 s; RNG не seeded.
- Full `mvn clean package` не запускался: шаг не менял production-код, а долгие disabled simulations не входят в обычный test suite.
