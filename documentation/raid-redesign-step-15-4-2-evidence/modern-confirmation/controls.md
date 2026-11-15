# 15.4.2: свежее подтверждение текущего C9

Это **отдельный**, ограниченный после пилота запуск: только настоящий текущий C9, общая проекция T3 и T3+обычный BRUISER, уровень 25, 500 зеркальных пар на каждое условие. Здесь ровно **2000 боёв**. Исторический агент отдельно исполнил те же два условия в H, ещё 2000; общая свежая проверка — 4000. Все условия, параметры и объём взяты из политики, сохранённой до исходов.

Политика `inference-policy.json` — точная копия `documentation/raid-redesign-step-15-4-2-evidence/confirmation-policy.json`, SHA-256 `b4c8ca144c8b454e6a123405390ea985f2f041ed9669a77a441233a51e714ebf`. Общий fixture совпадает с пилотом побитово, SHA-256 `b4aa68845c6498a93b81a6ff70051f970721ba4bd0ddcdcdf08c41289f161058`. Никакие коэффициенты, снаряжение, умения, позиции, тактики, темп или дальность не менялись. Никакой другой фазы, уровня, состава или продолжения выборки здесь нет.

## Команды

Java-команды выполнялись из `/home/user/projects/project-seeker/game`; новые классы создавались только в этом новом `/tmp`:

```sh
split_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$split_classpath" -d /tmp/seeker-step15-4-2-confirm.TpC9K3/classes /tmp/seeker-step15-4-2-confirm.TpC9K3/HeadLegacyMyconidOracle.java /tmp/seeker-step15-4-2-confirm.TpC9K3/LegacyPhaseMyconidController.java /tmp/seeker-step15-4-2-confirm.TpC9K3/Step1542CausalProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-2-confirm.TpC9K3/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1542CausalProbe prepare /tmp/seeker-step15-4-2-confirm.TpC9K3/frozen-preparation > /tmp/seeker-step15-4-2-confirm.TpC9K3/frozen-preparation.log 2>&1
python3 /tmp/seeker-step15-4-2-confirm.TpC9K3/preflight.py /tmp/seeker-step15-4-2-confirm.TpC9K3
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-2-confirm.TpC9K3/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1542CausalProbe screen /tmp/seeker-step15-4-2-confirm.TpC9K3/screen > /tmp/seeker-step15-4-2-confirm.TpC9K3/screen.log 2>&1
python3 /tmp/seeker-step15-4-2-confirm.TpC9K3/validate.py /tmp/seeker-step15-4-2-confirm.TpC9K3/screen
python3 /tmp/seeker-step15-4-2-confirm.TpC9K3/joint-confidence.py /tmp/seeker-step15-4-2-confirm.TpC9K3/screen /tmp/seeker-step1542-historical-confirm.0V5RWa/screen
```

Помощник скопирован из замороженного пилота в новый каталог. Изменены только фиксированная матрица C9/25/1–2, число пар, пространства имён, соответствующая сводная арифметика/метаданные и необязательная выгрузка trace для первых двух пар. Числовой конструкционный мост против C9 пилота проверил одинаковые calibration, diagnostics и характеристики игроков/противников, кроме ожидаемых новых UUID/начальной инициативы. Игра, `target`, настройки, бот и база данных не менялись, Maven не запускался. После заморозки помощник не менялся, `screen` исполнялся ровно один раз.

## Семена и статистика

Здесь 500 записей, groupId251, pair0..499. Пространства имён `MYCONID-15.4.2-CAUSAL-CONFIRM-GEN-20261108-v1` и `...-BATTLE-...`; первые 48 бит SHA-256 от `namespace/groupId/pair`, big endian. Идентификатор варианта/фазы не входит в хеш. Начальные значения общие с исторической свежей проверкой и обоими составами, но это не обещание одинаковых потоков событий разных движков.

Предобъявленное семейство состоит из трёх парных сравнений: `deltaH`, `deltaC9`, `DID=deltaC9-deltaH`. У первых двух разности в[-1,1], половина ширины одновременного 95%-ного интервала Хёффдинга с поправкой Бонферрони — `sqrt(2*ln(120)/500)`; у DID поддержка[-2,2] и половина ширины вдвое больше. Пилот в подтверждение не включается. Зеркальные порядки не считаются независимыми наблюдениями; единица — 500 парных индексов.

У первых двух пар обоих условий и обоих порядков (8 боёв) отдельно сохранены `actionLog.events` и верхнеуровневый массив `traceEvents`; их хеши проверены. Это исправление выгрузки в новом помощнике, а не переписывание пилотных raw. У всех 2000 боёв сохранены входы, версии/флаги, параметры подбора, первоначальные характеристики/состояние поля, конечная индивидуальная статистика и показатели журнала. Исходный gzip не перепаковывался. Отдельных повторений боёв в другой JVM и новых боёв после этого запуска нет.
