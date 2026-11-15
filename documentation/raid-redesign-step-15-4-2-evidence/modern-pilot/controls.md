# 15.4.2: причинный пилот текущего движка

Выполнен ровно один разрешённый пилот: C0–C9 × 4 сценария × уровни 10/25 × 30 пар × 2 порядка = 4800 боёв; C10 × 2 точные современные сборки × те же уровни/пары/порядки = 240 боёв. Всего здесь **5040**, отдельно исторический агент исполнил 480. База данных, бот, настройки приложения, рабочие исходники и `target` не менялись. Maven не запускался.

## Входы и проверка до исходов

Общие определения снаряжения согласованы с независимым историческим агентом до боя: `historical-fixture-ack.json`, исходный общий снимок `frozen-preparation/fixture-manifest.json`, SHA-256 `b4aa68845c6498a93b81a6ff70051f970721ba4bd0ddcdcdf08c41289f161058`. Историческая версия — `6150d562546cca22bed139c4d0fa23456e4d801e`; точные TOML скопированы в `old-data`, а нынешний класс `DefaultItems` побитово совпадает с историческим (SHA-256 `80568430645dfbf3f4e5c40a76e362907c20e800c8d50c4d7b11cd0ff2d35ec8`). Старые начальные вещи дают слабому дополнению 720 HP, новые — 840 HP; в обоих случаях заняты все семь слотов.

G сохраняет RARE tower_shield, удалён только модификатор GUARD; R использует longbow UNCOMMON DOUBLE_ATTACK2; M — staff RARE DOUBLE_ATTACK2 + обычную orb. У T3 и добавленного обычного BRUISER все предметы +3; у сильного одиночного BRUISER все +6, sledgehammer RARE BERSERK4; у слабого всё COMMON +0 без модификаторов. G/BRUISER/weak начинают FRONT, R/M — MID. Тактики: G/weak THREAT, R/M EXPLOIT_WEAKNESS, BRUISER WOUNDED_HUNTER. Точная современная сборка с GUARD и battle_staff исполняется только в C10.

Контроллер C0–C8 повторяет исходный HEAD `bd142e61a0aa2fc0e1b2117164ab4bc6fac29469`: `sum(actualLegacyPower)*q*g*.77`, исходные основы противников, поиск с 32 делениями и прежним расходом генератора случайных чисел пробными фабриками. Oracle отличается от буквального HEAD только именем класса и удалением изменившегося интерфейса/`@Override`. Перед боями 24 проверки сравнили полные характеристики, UUID, начальную инициативу и следующий случайный draw с этим oracle; все PASS. Независимый исторический агент сравнил восемь начальных C0-состояний с целостным старым движком и подтвердил общие параметры/UUID/инициативу. Это не превращает C0 в исторический движок: C0 исполняется нынешним кодом.

C2 переключает весь новый режим исполнения хода/боевых правил, не только порядок инициативы. C3 и C4 меняют версии умений при уже включённом явном force. `legacyPower()` теряет старые оценки соответствующих умений после переключения на V3; поэтому сохранены фактическая оценка и конструкционный Legacy-прокси для тех же предметов. Это не скрыто исправленная новая формула. C4/C5/C6/C8 образуют полную проверку темпа × дальности; C7 меняет только g. C9 и C10 вызывают настоящий текущий генератор 15.2 Q и являются агрегатными контрольными условиями, не однофакторными переходами.

## Команды

Все Java-команды выполнялись из `/home/user/projects/project-seeker/game`, классы помощника — только в новом `/tmp`:

```sh
split_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$split_classpath" -d /tmp/seeker-step15-4-2-causal.1HOgPK/classes /tmp/seeker-step15-4-2-causal.1HOgPK/HeadLegacyMyconidOracle.java
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "/tmp/seeker-step15-4-2-causal.1HOgPK/classes:target/classes:target/test-classes:$split_classpath" -d /tmp/seeker-step15-4-2-causal.1HOgPK/classes /tmp/seeker-step15-4-2-causal.1HOgPK/Step1542CausalProbe.java /tmp/seeker-step15-4-2-causal.1HOgPK/LegacyPhaseMyconidController.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-2-causal.1HOgPK/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1542CausalProbe prepare /tmp/seeker-step15-4-2-causal.1HOgPK/frozen-preparation > /tmp/seeker-step15-4-2-causal.1HOgPK/frozen-preparation.log 2>&1
python3 /tmp/seeker-step15-4-2-causal.1HOgPK/preflight.py /tmp/seeker-step15-4-2-causal.1HOgPK/frozen-preparation
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-2-causal.1HOgPK/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1542CausalProbe screen /tmp/seeker-step15-4-2-causal.1HOgPK/screen > /tmp/seeker-step15-4-2-causal.1HOgPK/screen.log 2>&1
python3 /tmp/seeker-step15-4-2-causal.1HOgPK/validate.py /tmp/seeker-step15-4-2-causal.1HOgPK/screen
```

Предварительная конструкционная подготовка ранее выполнялась также в `preparation`; затем добавлены единообразные верхнеуровневые поля, поля calibration для Q и описание телеметрии, помощник перекомпилирован, а полная окончательная подготовка выполнена в `frozen-preparation`. Обе подготовки успешны и не исполняли бой. Единственная боевая команда — `screen`, ровно один раз. После заморозки исходник не менялся.

Пять ключевых исходников и восемь скомпилированных классов проверяются из корня репозитория: пути в списках начинаются с `game/`. Полные 203 файла общего архива 15.2 отдельно сверены побитово; результат до пилота — `source-archive-preflight.json`.

## Семена, показатели и ограничения

В `seeds.json` 120 записей: группы 101/102/251/252 и пары 0..29. Группа — `10*уровень + (T3=1 или S1=2)`; варианты добавления и фазы не входят в начальное значение. Берутся первые шесть байтов SHA-256 от `namespace/groupId/pair`, big endian. Пространства имён `MYCONID-15.4.2-CAUSAL-PILOT-GEN-20261108-v1` и `...-BATTLE-...`. Поток GEN последовательно строит игроков, пробные фабрики и окончательных противников; поток BATTLE отдельный. Зеркальный порядок строится заново с теми же значениями. Прокси-оценки вычисляются во вложенном отдельном потоке и не меняют GEN.

Исходные строки содержат полные входы/версии/флаги, параметры подбора, характеристики до поля боя, реальное начальное состояние, индивидуальную конечную статистику, число событий, попытки обычных атак, перемещения, хеши и исход. `actionCount` — число событий журнала, не строго число действий; `turnsCount` — записанный счётчик, не гарантия реально завершённого действия. У Legacy число trace-попыток может отсутствовать как механика инструментария и не означает отсутствие обычных атак.

У первых двух пар каждого условия (336 боёв) дополнительно сохранены все `actionLog.events`. **Отдельный trace-массив в пилоте не сохранён**: настоящий `BattleActionLog.traceEvents()` помечен `@JsonIgnore`. Исходная фраза `including traceEvents` в замороженном manifest ошибочна; честная поправка — `telemetry-audit.json`. Ни исходник, ни замороженный manifest, ни summary, ни raw после исходов не переписывались. Хеш trace и общее число попыток сохранены для всех боёв. Повторения боёв ради необязательной телеметрии не было.

Выборка 30 пар — описательная диагностика. Успехи/провалы 0% или 100% не доказывают отсутствие проблемы; эффекты переходов условны к порядку переключений. Нельзя требовать каждому сочетанию 50/50 или объявлять найденное решение по этому пилоту. Полный original gzip сохраняется без перепаковки.
