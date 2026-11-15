# Последний фиксированный запуск15.4: воспроизведение и границы

17условий,8групп,500зеркальныхпар в каждом,17000настоящих боёв. Полный список
и результаты в RESULTS.md. Коэффициенты B_NORM неизменны: C=.47754531235323505,
a=.8766292004637334; r(M) — ранее утверждённая таблица из actualNPC(M,mref),
где mref=4.414285714285715. Никаких новых формул или подгонки по результатам.
После этого запуска только валидация/анализ: новых боёв не было и не планируется.
Код игры,target,настройки,БД и бот не изменялись; Maven не запускался.

## Построение и случайность

До боёв сохранены `preparation/inputs.json`, `manifest.json`, `seeds.json`:
17точных составов,4000записей group/pair с тремя начальными числами. Начальные
числа — первые6байтов SHA256(namespace+'/'+groupId+'/'+pair), unsigned big-endian.
Модель и вариант не входят в ключ, зеркальные порядки используют ту же пару.

-GEN:`MYCONID-15.4-COMPOSITION-CONFIRM-GEN-20261108-v1`
-BATTLE:`MYCONID-15.4-COMPOSITION-CONFIRM-BATTLE-20261108-v1`
-NPC:`MYCONID-15.4-COMPOSITION-CONFIRM-NPC-20261108-v1`

Независимый Python совпал с Java во всех12000значениях; внутри каждого потока
коллизий нет. Это исследовательские начальные числа, реализация игрового RNG
не менялась. В обычных группах NPC по-прежнему создаются продолжением GEN
после игроков; отдельный NPC-поток для них не используется.

Только в диагностических группах7/8 игроки создаются из GEN, калибровка берётся
по неизменному BASE, а финальные NPC — из отдельного NPC-потока. После добавления
союзника сохраняются count,m,UUID,gauge,все характеристики,умения и относительные
ряды этих NPC. В каждой строке явно записаны diagnosticFixedNpc,
npcSeparateSeedUsed,calibrationParticipantScore,calibrationEnemyCount. Поэтому
диагностическая цель не выдается за пересчёт по расширенному составу. Повторно
используемых мутируемых бойцов нет: каждый бой строит свежие экземпляры.

Проверки подготовки: оба NPC-якоря точны;6сверок моста текущего генератора Q;
6сверок настоящих подготовленных игроков с эффектами;2условия M2/3 совпадают
с ненормированным B;5диагностических вариантов совпадают с BASE по построенным
NPC и префиксу игроков. Это проверки построения, не дополнительные бои.

## Сырая проверка и обнаруженная оговорка геометрии

Сохранены все17000строк с полными предметами/модификаторами/редкостью/заточкой,
индивидуальными итогами,начальными снимками,реальными позициями,оценками,
калибровкой,эффектами и хешами действий/трассы. Ничьих/превышений лимита нет.
Сводка не получена исключением неудобных поражений.

Первый строгий вариант проверки сравнивал полный снимок NPC включая абсолютный
lineIndex и остановился на case17,group8,pair0,PLAYERS_FIRST: только индексы
1/3→3/5 отличались. Причина подтверждена read-only чтением BattleContext:
linesFromTeam не создаёт пустые передние линии одиночного BACK-бойца, но новый
FRONT-союзник добавляет их. Код игры и замороженный Java-помощник не менялись.

Проверка уточнена: сравниваются ВСЕ поля NPC кроме абсолютного lineIndex,
одновременно отдельно проверяется точное совпадение фактических FRONT/BACK,
а каждый сдвиг индекса учитывается в отчёте. Повторная полная проверка17000строк
нашла ровно500таких строк,только case17 PLAYERS_FIRST. Остальные NPC-поля
совпали точно. Геометрия нового союзника — часть диагностического вмешательства,
а не замаскированное изменение калибровки.

Окончательная валидация:3000fixedNPC-сравнений,8000проверок префикса игроков,
3000живых weak840,все начальные числа/нейтральные оценки/цели/взвешенные
характеристики NPC/V3умения/эффекты/исходы по итоговомуHP/парная сводка совпали.
Это не независимая реализация движка; повторного межпроцессного запуска боёв
для этой исследовательской модели не делали. Доверительные интервалы отдельно
заданы в inference-policy.json и analysis.json;17условий не объединяются в одну
вероятность победы. Обычный и диагностический составы разных групп не спарены
между собой. Все9сравнений заранее входят в одну поправку Бонферрони.

## Точные команды

cwd:`/home/user/projects/project-seeker/game`. Каталоги результата должны быть
новыми. Для повторения нужно менять только конечный каталог вывода; такой
повтор не увеличит независимую выборку.500пар и namespaces помощник фиксирует.

```sh
formula_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$formula_classpath" -d /tmp/seeker-step15-4-composition-confirm.O9o40T/classes /tmp/seeker-step15-4-composition-confirm.O9o40T/Step154FormulaProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-composition-confirm.O9o40T/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe prepare /tmp/seeker-step15-4-composition-confirm.O9o40T/preparation > /tmp/seeker-step15-4-composition-confirm.O9o40T/preparation.log 2>&1
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dformula.pairs=500 -cp "/tmp/seeker-step15-4-composition-confirm.O9o40T/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe screen /tmp/seeker-step15-4-composition-confirm.O9o40T/screen > /tmp/seeker-step15-4-composition-confirm.O9o40T/screen.log 2>&1
python3 /tmp/seeker-step15-4-composition-confirm.O9o40T/validate.py /tmp/seeker-step15-4-composition-confirm.O9o40T/screen > /tmp/seeker-step15-4-composition-confirm.O9o40T/validation.log
python3 /tmp/seeker-step15-4-composition-confirm.O9o40T/summarize.py /tmp/seeker-step15-4-composition-confirm.O9o40T/screen
```

Первая из двух попыток валидации имела ту же команду и описанную выше слишком
строгую проверку абсолютных индексов. Обе попытки только читали тот же поток;
бои не перезапускались, сырой файл и сводка не переписывались.

```sh
# cwd /home/user/projects/project-seeker
sha256sum -c /tmp/seeker-step15-4-composition-confirm.O9o40T/source-basis.sha256
sha256sum -c /tmp/seeker-step15-4-composition-confirm.O9o40T/class-basis.sha256
```
