# 15.4.3: свежая проверка `BLEND_T`

Это отдельный исследовательский запуск, не внедрение в игру. Исходный пилот не изменяется и его наблюдения не объединяются со свежей выборкой. Формула, коэффициент ½, фабрики противников, веса рядов, исходные предметы и боевые правила сохранены.

Ровно 34 исходных случая, 37 сочетаний случая и правила, 40 000 зеркальных пар, 80 000 исполнений. Для групп 1–4 и 16 выделено по 1500 пар; для групп 5–7 — по 1000, для группы 13 — 500. Всего 11 000 записей с начальными значениями генераторов случайных чисел. Дополнительный поток `NPC` записан, но не используется: в этой проверке нет `FIXED_NPC`.

Корневая политика сохранена до исходов в `frozen-preparation/confirmation-policy.json`, SHA-256 `3876690b6ceb542c1bf2a3112d2b6e194ae922c028f706bd2b08686c11e515cf`. Основное семейство состоит из 11 сравнений, второе — из 20; они не образуют совместной 95-процентной гарантии. Контраст улучшения — прямой результат `BLEND_T` минус `BASELINE` у того же слабого добавления. Исходное одиночное состояние совпадает точно, поэтому диапазон этого контраста — `[-1,1]`, а не `[-2,2]`.

## Подготовка

Команды выполняются из `/home/user/projects/project-seeker/game`. Они используют готовые классы только для чтения; Maven, приложение, база данных и бот не запускаются. Каталог вывода не должен существовать заранее.

```sh
split_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$split_classpath" -d /tmp/seeker-step15-4-3-confirm.orJtCu/classes /tmp/seeker-step15-4-3-confirm.orJtCu/Step1543HeterogeneityProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-3-confirm.orJtCu/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1543HeterogeneityProbe prepare /tmp/seeker-step15-4-3-confirm.orJtCu/preparation > /tmp/seeker-step15-4-3-confirm.orJtCu/preparation.log 2>&1
```

До боёв сохранены точные копии пяти файлов подготовки, Java-исходника и политики в `frozen-preparation`, исходные входы в `source-inputs`, список контрольных сумм `prebattle-freeze.sha256`, снимки параметров источников и классов. Собственная проверка: `python3 preflight.py ROOT`. Независимый предварительный допуск должен быть получен до следующей команды.

## Единственный боевой запуск

```sh
split_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
date -Iseconds > /tmp/seeker-step15-4-3-confirm.orJtCu/screen-started-at.txt
/usr/bin/time -p env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-3-confirm.orJtCu/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1543HeterogeneityProbe screen /tmp/seeker-step15-4-3-confirm.orJtCu/screen > /tmp/seeker-step15-4-3-confirm.orJtCu/screen.log 2> /tmp/seeker-step15-4-3-confirm.orJtCu/screen-time.log
date -Iseconds > /tmp/seeker-step15-4-3-confirm.orJtCu/screen-finished-at.txt
```

Сырой поток `screen/battles.jsonl.gz` сохраняется в исходных байтах, без повторного сжатия. Каждая строка содержит исходные параметры игроков и противников, `Q/B/T/F`, скидку, точную целевую величину и калибровку, начальную геометрию, конечные индивидуальные показатели, исход, длительность, счётчики и контрольные суммы событий. Для пары 0 каждого сочетания сохранены также полные `actionLog` и отдельные `traceEvents`.

После исходов допускаются только проверка и анализ уже полученных данных. Дополнительные бои, продление выборки, замена начальных значений, изменение формулы или коэффициентов не разрешены.
