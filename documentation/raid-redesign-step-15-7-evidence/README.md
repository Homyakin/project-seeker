# Материалы проверки 15.7

[Отчёт и ограничения](../raid-redesign-step-15-7-workability.md), [условия до запуска](protocol.md), [краткий проверочный итог](verification-summary.json).

## Сохранённые данные

- [run/protocol.json](run/protocol.json) — зафиксированные до боёв 359 условий, постоянные предметы, 281 группа начальных значений, 8620 боёв и 213 контрольных сумм открытых исходников. Объём 14 МБ — полные явные предметы, без скрытого подбора по исходам.
- [pre-run-lock.sha256](pre-run-lock.sha256) — контрольные суммы человеческого/машинного протоколов, исполнителя и его теста, записанные до боёв. Файлы после запуска не менялись.
- [run/battles.jsonl.gz](run/battles.jsonl.gz) — все 8620 исходных строк, включая предметы, новую мощь, подбор, состояния, исходы и контрольные суммы действий/трассировки. Поля старой мощи явно помечены как историческая диагностика.
- [run/summary.json](run/summary.json) — все 359 сводок и 78 связанных сравнений. Интервалы — по независимым парам внутри условия, не по глобально независимым 8620 боям.
- [validate-study.cjs](validate-study.cjs) — независимая потоковая проверка матрицы, строк, мощи NPC, зеркал, сводок и сравнений. Ничего не перезаписывает.
- [replay/battles.jsonl.gz](replay/battles.jsonl.gz), [replay-compare.cjs](replay-compare.cjs), [replay-verification.json](replay-verification.json) — 32 заранее выбранных повторных боя новым процессом и строгое совпадение всех полей с основной выборкой. Упорядочиваются только ключи словарей; массивы, исходы и контрольные суммы действий совпадают точно.
- [test-summary.json](test-summary.json) — полная чистая сборка: 1768 проверок, 1739 успешных, 29 прежних/условных пропусков, ошибок и нарушений Checkstyle нет. Пять новых проверок структуры матрицы выполняются без генерации NPC и боёв. Включены прежние 279 целевых проверок 15.6.
- [artifacts.sha256](artifacts.sha256) — контрольные суммы всех материалов этого набора; проверять из корня репозитория.

Основной исполнитель и его тест находятся в `game/src/test/java/ru/homyakin/seeker/game/battle/Step157MyconidValidationStudy.java` и `Step157MyconidValidationStudyTest.java`. Игровые файлы 15.6 не менялись. Полные журналы Maven и свойства JVM не публикуются; контрольная сумма журнала включена в сводку, содержащую только имена/числа тестов.

## Проверка сохранённого результата

Из корня репозитория:

```sh
sha256sum -c documentation/raid-redesign-step-15-7-evidence/pre-run-lock.sha256
sha256sum -c documentation/raid-redesign-step-15-6-evidence/inputs.sha256
node documentation/raid-redesign-step-15-7-evidence/validate-study.cjs documentation/raid-redesign-step-15-7-evidence/run
node documentation/raid-redesign-step-15-7-evidence/replay-compare.cjs documentation/raid-redesign-step-15-7-evidence/run documentation/raid-redesign-step-15-7-evidence/replay
```

Проверка требует исходников выбранной версии. Будущие игровые правки могут закономерно изменить контрольные суммы; не восстанавливать старый снимок поверх чужих изменений.

## Повторение исполнения

Сначала собрать выбранную неизменённую версию. В использованной среде — из `game`:

```sh
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 \
  JAVA_TOOL_OPTIONS=-javaagent:/home/user/.m2/repository/org/mockito/mockito-core/5.18.0/mockito-core-5.18.0.jar \
  /opt/apache-maven-3.9.12/bin/mvn -o -B -ntp clean package
```

Пути библиотек взять из единственного свойства `java.class.path` отчёта `game/target/surefire-reports/TEST-ru.homyakin.seeker.game.battle.Step157MyconidValidationStudyTest.xml`. Не публиковать остальные свойства JVM или конфигурацию. Из корня репозитория, с этим значением в переменной `seeker_step157_classpath`:

```sh
/home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx768m -cp "$seeker_step157_classpath" \
  ru.homyakin.seeker.game.battle.Step157MyconidValidationStudy plan /tmp/new-step157-study
/home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx768m -cp "$seeker_step157_classpath" \
  ru.homyakin.seeker.game.battle.Step157MyconidValidationStudy run /tmp/new-step157-study
/home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx768m -cp "$seeker_step157_classpath" \
  ru.homyakin.seeker.game.battle.Step157MyconidValidationStudy replay /tmp/new-step157-study /tmp/new-step157-replay
```

Выходные каталоги должны быть новыми; перезапись запрещена. Сначала проверить `--plan-only`, затем после исполнения — полный проверяющий и сравнение повторения на новых каталогах. Повторные 32 боя не увеличивают статистическую выборку.

**Ограничение стенда:** между процессами JVM неупорядоченное `ItemObject.slots` может записаться в JSON как другой порядок двух элементов. Строгая защита тогда останавливает процесс до боёв, не разрешая перезапись. Именно это произошло при первой попытке повторения; независимая сверка нашла только 1722 перестановки двух слотов двуручного оружия, иных отличий нет. Следующий обычный процесс прошёл ту же неизменённую защиту и дал строго совпавшие 32 боя. Это раскрытая особенность сериализации, не причина переписывать протокол или подбирать новые исходы.

## Границы

Выпуск, приложение, бот и БД не запускались; начисление настоящих наград не моделировалось как транзакция. Верхние уровни 40–50 почти не проходятся этой выборкой — это явно раскрытый результат, не скрытое доказательство прежнего баланса. Все данные относятся к ограниченной работоспособности выбранного нового подбора.
