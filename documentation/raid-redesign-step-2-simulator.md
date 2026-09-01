# Шаг 2. Воспроизводимый combat simulator

Дата завершения: 2026-09-01

## Итог

Добавлен воспроизводимый симулятор боёв с управляемым корневым `seed`. Одинаковая версия кода, одинаковые входные параметры и одинаковый `seed` дают побайтово одинаковый Markdown-отчёт. Тяжёлый калибровочный прогон не входит в обычный набор Surefire и запускается только явно.

Шаг не меняет game-data, числовые формулы предметов, параметры рейдов или награды. Он создаёт измерительный контур для следующих этапов редизайна; порядок battle-коллекций сделан явным и стабильным ради воспроизводимости.

## Как обеспечивается воспроизводимость

- `RandomUtils.withSeed(seed, action)` создаёт ограниченный текущим потоком RNG-контекст и восстанавливает предыдущий контекст после завершения или исключения.
- В seed-контекст включена вся подготовка одной итерации: создание обеих команд, legacy raid generation и сам бой.
- Через управляемый RNG проходят initiative start, shuffle, выбор цели, crit, dodge, damage variance, вероятностные skills, нормальное распределение характеристик и случайный тип атаки Zombie Horde.
- UUID участников боя внутри seed-контекста генерируются детерминированно как UUID v4. В обычном production-пути по-прежнему используется `UUID.randomUUID()`.
- Коллекции, чей порядок влияет на бой, переведены на стабильный порядок: линии и живые команды используют `LinkedHashMap`, attack types — `EnumSet`, active skills — `EnumMap`, веса целей — `LinkedHashMap`.
- Каждая итерация получает отдельный seed, вычисленный из корневого seed и номера итерации смешиванием SplitMix64. Изменение или сбой одной итерации не сдвигает RNG-поток следующих итераций.
- Вне `withSeed` сохранено прежнее случайное production-поведение.

Seed гарантирует повтор результата на одной и той же версии кода. Изменение порядка или количества RNG-вызовов в будущих коммитах закономерно может изменить результат при том же seed.

## Артефакты

- `RandomUtils` — seed scope и детерминированные UUID.
- `CombatSimulator` — цикл независимых итераций, агрегация и статистика.
- `CombatSimulationRequest` и `CombatSimulationTeams` — описание ячейки, round cap и фабрика свежих команд для каждого боя.
- `CombatSimulationReport` — типизированный результат и стабильный Markdown-формат.
- `RaidSimulationFixtures` — reference loadouts из аудита шага 1.
- `RaidCalibrationSimulator` — запускаемый вручную property-gated runner.
- `RandomUtilsSeedTest`, `BattleDeterminismTest` и `CombatSimulatorTest` — быстрые регрессионные тесты.

Все классы самого калибратора находятся в test source set: они доступны Maven-тестам и ручным симуляциям, но не попадают в production JAR.

## Метрики и их смысл

Для каждой ячейки отчёт содержит:

- raid type, loadout, точный composition, Difficulty, размер группы, seed и число боёв;
- максимальное число раундов одного боя;
- win rate первой, то есть оцениваемой, команды и двусторонний 95% Wilson interval;
- p50 длительности как медиану и p95 методом nearest rank;
- среднее число выживших;
- средний остаток HP и средний процент остатка HP относительно стартового максимума команды;
- средние фактические damage dealt/taken после mitigation, variance и ограничения overkill;
- среднее суммарное число ходов команды.

`Rounds` — тики battle loop, а `Turns` — реально выполненные действия персонажей. Для damage taken используется новый read-only счётчик фактически потерянного HP. Legacy `BattlePersonageStats.damageTaken()` намеренно не менялся: его текущая семантика участвует в существующих наградах и должна быть исправлена только отдельным решением.

Damage dealt команды выводится из фактического damage taken противоположной команды. Поэтому в метрику входят delayed/effect damage, которые старый `skillDamageDealt` не всегда приписывает источнику.

## Reference loadouts

| Имя | Позиция | Назначение |
|---|---|---|
| `VIRTUAL_DEFAULT` | FRONT | виртуальная стартовая экипировка |
| `PLATE_TANK` | FRONT | тяжёлая защита, mace + tower shield |
| `LEATHER_PIERCE` | FRONT | spear + dirk |
| `LEATHER_SLASH` | FRONT | sword + shortsword |
| `ARCANE_MAGE` | BACK | arcane cloth set, staff + orb |
| `CLOTH_MAGE` | BACK | обычный cloth set, staff + orb |

Один идентификатор composition повторяется до размера группы. Смешанный состав задаётся ровно `N` значениями через запятую.

## Запуск

Пример исследовательской ячейки на 1 000 боёв:

```bash
cd game
mvn test \
  -Dtest=RaidCalibrationSimulator \
  -Draid.calibration.enabled=true \
  -Draid.calibration.raidType=MYCONID_COLONY \
  -Draid.calibration.difficulty=10 \
  -Draid.calibration.partySize=3 \
  -Draid.calibration.iterations=1000 \
  -Draid.calibration.maxRounds=10000 \
  -Draid.calibration.seed=20260901 \
  -Draid.calibration.composition=LEATHER_PIERCE,ARCANE_MAGE,PLATE_TANK \
  -Draid.calibration.output=target/raid-calibration-report.md
```

Для приёмки ключевой ячейки меняется только `raid.calibration.iterations=10000`. Ключевые ячейки будут определены после утверждения архетипов и raid fixtures; на этом шаге 10 000 боёв не используются как балансировочный критерий.

Параметры по умолчанию: `MYCONID_COLONY`, D10, N=3, 1 000 итераций, round cap 10 000, seed `20260901`, `VIRTUAL_DEFAULT`, отчёт `target/raid-calibration-report.md`. Difficulty и round cap должны быть положительными.

## Проверки

### Автоматические тесты

- `RandomUtilsSeedTest`: все seed-aware источники повторяются; UUID имеет version 4 и RFC variant; вложенный scope восстанавливается после успеха и исключения.
- `BattleDeterminismTest`: два полных Zombie Horde боя повторяют init state, NPC variants, action log, outcome, rounds и personage stats.
- `CombatSimulatorTest`: повторяются record и Markdown; проверены Wilson interval, медиана, nearest-rank p95, iteration seed, валидация, равенство dealt/taken между командами и fail-fast на stalemate после заданного round cap.
- Полный `mvn clean package`: `BUILD SUCCESS`, 365 тестов, 0 failures, 0 errors, 19 skipped, Checkstyle без нарушений.
- В обычном Surefire-прогоне отсутствует `RaidCalibrationSimulator`; legacy `RaidWinrateTest` остаётся отключённым.

### Побайтовая воспроизводимость

Одна и та же отдельная ячейка была запущена в двух независимых Maven JVM:

- Zombie Horde, D10, N=3;
- `LEATHER_PIERCE+ARCANE_MAGE+PLATE_TANK`;
- 10 боёв, seed `20260901`.

Оба отчёта получили одинаковый SHA-256:

```text
9bbcf9d517878588f411e49746bdf27a184d8c2bd4426c651610d641684ac611
```

### Рабочий smoke-run на 1 000 боёв

Для проверки рабочего объёма прогнана исследовательская ячейка Myconid Colony D10, N=3, seed `20260901`, состав `LEATHER_PIERCE+ARCANE_MAGE+PLATE_TANK`:

| Метрика | Значение |
|---|---:|
| Win rate | 77.70% |
| 95% Wilson interval | [75.02%, 80.17%] |
| Rounds p50 / p95 | 57 / 70 |
| Выжившие / остаток HP оцениваемой команды | 0.99 / 769.73 (14.95%) |
| Damage dealt / taken оцениваемой команды | 2888.87 / 4380.27 |
| Turns оцениваемой команды | 15.74 |

Это проверка инструмента, а не вывод о будущем балансе: используются текущие предметы и legacy power-dependent raid generator.

## Ограничения и риски

- Seed scope синхронный и привязан к потоку. Он безопасен для независимых параллельных ячеек, но не переносится в async-задачи, запущенные внутри scope.
- Симуляционные fixtures обязаны передавать фиксированное время при появлении time-dependent эффектов. Текущий raid event использует фиксированную дату.
- Legacy raid generator создаёт служебных персонажей во время расчётов и расходует RNG; это воспроизводимо, но его рефакторинг изменит последовательность результатов.
- Калибратор прерывает ячейку с явной ошибкой, если бой достигает round cap; частичный результат при stalemate не выдаётся. Существующий production-вызов `Battle.process` сохраняет прежнее поведение без нового лимита.
- Результаты этого шага нельзя использовать как окончательные balance targets до утверждения архетипов, stat economy и замороженных fixtures.

## Критерий завершения

Выполнен: одинаковый seed даёт одинаковый типизированный и Markdown-отчёт, включая полный battle RNG и случайные NPC variants; обычный Maven build запускает только быстрые тесты и не запускает калибровочные серии на 1 000/10 000 боёв.
