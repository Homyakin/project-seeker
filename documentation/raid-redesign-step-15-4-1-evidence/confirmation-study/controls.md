# 15.4.1: последнее подтверждение контртипов

Это один заранее утверждённый запуск: 12 условий × 500 пар × 2 порядка × SAME/CROSS = 24 000 исполнений. После него разрешены только проверки сохранённых данных, анализ и независимая рецензия. Новые бои, изменение формул и коэффициентов запрещены.

Порядок условий и два отдельных семейства сравнений заданы до исходов в `inference-policy.json` (точная копия политики родительского агента). Группа 1 содержит все восемь сочетаний типа/добавления, группа 2 — исходную тройку и обычного четвёртого участника, группа 3 — одиночного сильного Ритуалиста и живого слабого участника. Начальные значения генераторов случайных чисел одинаковы между вариантами и моделями внутри группы/пары, но не между группами.

Константы и формулы полностью сохранены от разведки 6000 исполнений. Полный код, 12 входов и 1500 записей начальных значений заморожены в `frozen-preparation` до запуска боёв. Ранний каталог `preparation` — только предварительная проверка конструкции до уточнения текстового поля `effectBridgeScope`; боёв там нет. Контроль эффектов в окончательной подготовке — только конструкционный, вне 12 боевых условий без эффектов.

## Команды

Из корня проекта до и после запуска выполнена проверка пяти ключевых исходников и восьми скомпилированных классов; списки используют пути с `game/`. После запуска также побитово сверены все 203 файла общего исходного архива 15.2:

```sh
sha256sum --check --status /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/source-basis.sha256
sha256sum --check --status /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/class-basis.sha256
```

Дальнейшие Java-команды выполняются из `/home/user/projects/project-seeker/game`. Переменная `split_classpath` читается из восстановленного списка зависимостей, а не из приложения:

```sh
split_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$split_classpath" -d /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/classes /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/Step1541SeparateScalingProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-1-counter-confirm.j9vtbv/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1541SeparateScalingProbe prepare /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/frozen-preparation > /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/frozen-preparation.log 2>&1
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dsplit.pairs=500 -cp "/tmp/seeker-step15-4-1-counter-confirm.j9vtbv/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1541SeparateScalingProbe screen /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/screen > /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/screen.log 2>&1
```

Две первые команды подготовки ранее выполнялись также в каталог `preparation`; затем исходник дополнен только явным описанием области контроля эффектов, перекомпилирован и повторно подготовлен в `frozen-preparation`. Обе подготовки закончились успешно, ни одна не исполняла бой. Единственная боевая команда — `screen`, ровно один раз. Исходники игры, `target`, настройки и база данных не изменялись; Maven, бот и приложение не запускались.

Проверки сохранённых результатов (не новые бои):

```sh
python3 /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/validate.py /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/screen
python3 /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/analyze.py /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/screen
python3 /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/check-analysis.py /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/screen
python3 /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/check-source-archive.py /home/user/projects/project-seeker /tmp/seeker-step15-4-1-counter-confirm.j9vtbv/source-archive-validation.json
```

## Воспроизводимость и ограничения

Новые пространства имён — `MYCONID-15.4.1-SPLIT-CONFIRM-GEN-20261108-v1` и `MYCONID-15.4.1-SPLIT-CONFIRM-BATTLE-20261108-v1`. Для каждой группы/пары берутся первые 48 бит SHA-256 от `namespace/groupId/pair`, порядок big endian. Идентификатор модели/варианта не входит в хеш. Внешний поток генерации создаёт игроков и затем по одному 48-битному начальному значению на противника; настоящая фабрика и пересборка воспроизводятся внутри этого изолированного потока. Отдельный поток боя не используется для построения бойцов.

Два семейства по 12 парных сравнений имеют каждое собственную одновременную 95%-ную границу Бонферрони—Хёффдинга: `sqrt(2*ln(480)/500)`. Это не совместная 95%-ная гарантия сразу для обоих семейств. Единица наблюдения — пара двух зеркальных порядков. Совпадение SAME/CROSS на тройках с опорными отношениями не создаёт независимые дополнительные наблюдения. Проверяется не приближение каждого боя к 50/50, а сохранение контртипов и характер штрафа за добавление участника.

Полные 24 000 строк сохраняются в исходном `screen/battles.jsonl.gz`, без перепаковки и удаления неудобных исходов. Этот последний запуск не включает отдельного повторения боёв в другой JVM.
