# Материалы шага 15.6

Проверка реализации [согласованного проекта](../raid-redesign-step-15-5-minimal-adaptation.md); [отчёт о реализации](../raid-redesign-step-15-6-equipment-power-implementation.md).

Целевые технические проверки и полная чистая сборка игрового модуля завершены. Массовые боевые прогоны и приёмка баланса относятся к отдельному шагу 15.7.

## Результаты

- [targeted-test-summary.json](targeted-test-summary.json): 279 целевых проверок в 14 наборах, ошибок и пропусков нет.
- [test-summary.json](test-summary.json): полная чистая сборка, 1763 проверки, 1734 успешных, 29 прежних/условных пропусков, ошибок и нарушений Checkstyle нет. Все затронутые проверки выполнены. Приложение, бот и БД не запускались; выпуск не включён.
- [collect-test-summary.cjs](collect-test-summary.cjs): повторяемый сборщик только имён/чисел из отчётов Surefire, сравнивающий итог с журналом Maven. Полные отчёты со свойствами JVM и полные журналы не копируются в материалы; контрольные суммы журналов включены в сводки.
- [original-power-comparison.json](original-power-comparison.json): отдельный запуск прежнего вычислителя и побитное совпадение эталона с новым ядром и независимым проверочным расчётом. Новая первоначальная тестовая константа была на один младший бит неточной; проверена по старому классу и исправлена без ослабления допуска.
- [inputs.sha256](inputs.sha256), [selected-inputs.txt](selected-inputs.txt): 211 выбранных открытых входов, без локальной конфигурации с секретами и без пользовательского теста блокировки.
- [source-change-audit.json](source-change-audit.json): сравнение с 203 открытыми входами 15.2 — семь ожидаемых изменений, 196 неизменённых файлов; пользовательский `InMemoryLockServiceTest` отдельно проверен и сохранён.
- [artifacts.sha256](artifacts.sha256): контрольные суммы файлов этого набора; проверять из корня репозитория.

## Повторение проверок

В использованной среде команды запускаются из `game` — относительный путь подавлений Checkstyle разрешается относительно этого каталога. Ничего не отключать ради запуска из корня репозитория.

```sh
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 \
  JAVA_TOOL_OPTIONS=-javaagent:/home/user/.m2/repository/org/mockito/mockito-core/5.18.0/mockito-core-5.18.0.jar \
  /opt/apache-maven-3.9.12/bin/mvn -o -B -ntp clean test \
  -Dtest=EquipmentPowerCalculatorTest,ReferencePowerCalculatorTest,PowerDisplayServiceTest,BattlePersonageEquipmentPowerTest,BattlePersonagePersonageEffectsTest,PowerDependencyGuardTest,MyconidTeamScalingTest,MyconidColonyAdaptationTest,RaidProcessingCombatVersionTest,RaidNpcCombatVersionTest,PveCharacteristicScalingTest,PveScalingDiagnosticsTest,PveTeamAssessmentTest,PveAdaptationStudyTest

env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 \
  JAVA_TOOL_OPTIONS=-javaagent:/home/user/.m2/repository/org/mockito/mockito-core/5.18.0/mockito-core-5.18.0.jar \
  /opt/apache-maven-3.9.12/bin/mvn -o -B -ntp clean package
```

Для сводки целевого запуска запускать сборщик **до** следующего `clean`, пока отчёты соответствуют именно этому запуску. Из корня репозитория:

```sh
node documentation/raid-redesign-step-15-6-evidence/collect-test-summary.cjs \
  game/target/surefire-reports /absolute/path/to/maven.log \
  /absolute/path/to/new-summary.json TARGETED_CLEAN_TEST
```

Для окончательной сборки указать `FINAL_CLEAN_PACKAGE`. Проверка списка исходных файлов из корня репозитория: `sha256sum -c documentation/raid-redesign-step-15-6-evidence/inputs.sha256`.

Архивы и исходные результаты прежних исследований не переписываются. Их контрольные суммы проверены. Файлы локальной конфигурации с секретами не входят в материалы проверки; готовый архив приложения и полные журналы также не публикуются.
