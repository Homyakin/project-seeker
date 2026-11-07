# Проверки внедрения темпа ×1,75

[Результат внедрения](../raid-redesign-step-12-3-battle-tempo.md). Эти данные относятся к производственной реализации, а не к первоначальной временной подмене двух обработчиков урона.

- [equivalence.json](equivalence.json) — независимое точное сравнение 90 200 боёв с исходным опытом, фактические места загрузки классов, контрольные суммы и сохранение безопасных настроек.
- [test-summary.json](test-summary.json) — результат полной сборки и количества тестов без сохранения частных свойств процесса из XML.
- [sources.tar.gz](sources.tar.gz) — только явно выбранные публичные исходники, проверочный код, сохранённые классы и сценарий сравнения. Частные профили и полный каталог `src` не упаковывались.
- [measurements.tar.gz](measurements.tar.gz) — все новые исходные наблюдения и определения команд, а не только выбранные медианы.
- [manifest.json](manifest.json) — контрольные суммы файлов и выбранных исходников.

Начальные значения и составы взяты из [первоначального опыта](../raid-redesign-step-12-3-duration-evidence/README.md). Ускоренная группа повторяет числитель 7, базовая — числитель 4. Обе группы намеренно повторяют исходную случайную последовательность для проверки эквивалентности, а не объявляются новым независимым статистическим подтверждением меты.

Временный `Step123BattleRunner` отличается от прежнего только явной передачей темпа в производственный `BattleContext`. В каталоге проверочных классов отсутствуют подмены `Battle`, `BattleContext`, `BattlePersonage` и `BattleTempoVersion`. `DurationDamageScale` оставлен только для неизменности подписей и сведений старого измерительного кода, его счётчики не участвуют в рабочем расчёте. Единственные исключения при сравнении — изменившиеся сведения о хешируемых исходниках и этот неиспользуемый старый счётчик. Боевые показатели не исключались.

## Проверка сохранённых наблюдений

Распаковать исходники и измерения интеграции в одну новую папку, а оба архива исходного опыта — в другую:

```sh
study_docs=/home/user/projects/project-seeker/documentation
study_integrated=$(mktemp -d /tmp/seeker-tempo-integrated.XXXXXX)
study_original=$(mktemp -d /tmp/seeker-tempo-original.XXXXXX)
tar -xzf "$study_docs/raid-redesign-step-12-3-tempo-evidence/sources.tar.gz" -C "$study_integrated"
tar -xzf "$study_docs/raid-redesign-step-12-3-tempo-evidence/measurements.tar.gz" -C "$study_integrated"
tar -xzf "$study_docs/raid-redesign-step-12-3-duration-evidence/sources.tar.gz" -C "$study_original"
tar -xzf "$study_docs/raid-redesign-step-12-3-duration-evidence/measurements.tar.gz" -C "$study_original"
node "$study_integrated/validator.cjs" "$study_integrated" "$study_original" /path/not-used-with-frozen-inputs
```

Требуется Node с сохранением исходного текста числа в третьем аргументе обработчика `JSON.parse`, в опыте — 24.22.1. 64-битные начальные значения не переводятся в неточный `Number`. Сохранённые исходники и классы позволяют проверить этот архив без обращения к текущему рабочему дереву; они не образуют автономную сборку всех зависимостей игры.

## Команда полной сборки

Из `game/`, с установленными зависимостями и JDK 25.0.1:

```sh
JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 \
/opt/apache-maven-3.9.12/bin/mvn \
  '-DargLine=-javaagent:/home/user/.m2/repository/org/mockito/mockito-core/5.18.0/mockito-core-5.18.0.jar -javaagent:/home/user/.m2/repository/org/jacoco/org.jacoco.agent/0.8.13/org.jacoco.agent-0.8.13-runtime.jar=destfile=target/jacoco.exec' \
  clean package
```

Результат: 1 361 успешный тест из 1 383, 22 пропуска, Checkstyle пройден. Три PostgreSQL-проверки не заменяются этим числом и отдельно в данном изменении не запускались. Частный локальный профиль приложения и собранный JAR не входят в публикуемые архивы.
