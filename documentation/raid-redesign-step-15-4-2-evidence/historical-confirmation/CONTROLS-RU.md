# Воспроизведение и сохранённые материалы

Исторические источники и библиотеки находятся в `/tmp/seeker-step1542-historical.77NJth`. Точный разрешённый список — `whitelist.txt`, SHA исходников — `source-basis.json`, замороженные классы и исходники — `runtime-freeze.json`. Общие составы проверены в `common-preparation-validation.json`. Старый предварительный набор из 480 боёв — `screen/battles.jsonl.gz`, его полная проверка — `screen/validation.json`. Эти файлы не менялись при подтверждении.

Отдельное подтверждение находится в текущем каталоге: `HistoricalCausalConfirmProbe.java`, `classes/HistoricalCausalConfirmProbe.class`, `frozen-policy.json`, `frozen-common-fixture.json`, `seeds.json`, `preparation/manifest.json`, `runtime-freeze.json`. Это полный состав новых входов до первого нового исхода. Дополнительные игровые настройки не читаются.

Компиляция отдельного подтверждения, выполненная до заморозки:

```sh
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 \
  -cp "/tmp/seeker-step1542-historical.77NJth/classes-6150:$(</tmp/seeker-step1542-historical.77NJth/dependencies.classpath)" \
  -d /tmp/seeker-step1542-historical-confirm.0V5RWa/classes \
  /tmp/seeker-step1542-historical-confirm.0V5RWa/HistoricalCausalConfirmProbe.java
```

Подготовка без боёв была выполнена командой с `prepare`, после чего `freeze-confirmation.cjs` проверил SHA правила опыта, совпадение составов, неизменность старой сборки и непересечение новых корней случайности с предварительной проверкой. Фактическая единственная команда запуска подтверждения:

```sh
/home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m \
  -cp "/tmp/seeker-step1542-historical-confirm.0V5RWa/classes:/tmp/seeker-step1542-historical.77NJth/classes-6150:/tmp/seeker-step1542-historical.77NJth/basis-6150/game/src/main/resources:$(</tmp/seeker-step1542-historical.77NJth/dependencies.classpath)" \
  HistoricalCausalConfirmProbe run \
  /tmp/seeker-step1542-historical-confirm.0V5RWa/screen \
  /home/user/projects/project-seeker/documentation/raid-redesign-step-15-4-2-evidence/confirmation-policy.json
```

Повторять бои для проверки сохранённых данных не нужно. Проверка только чтением:

```sh
/home/user/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node \
  /tmp/seeker-step1542-historical-confirm.0V5RWa/validate-confirmation.cjs
```

Эта последняя команда заново проверяет сохранённый архив и записывает только производный `screen/validation.json`. Она не вызывает Java, не пересчитывает бои, не меняет исходный архив или производственные файлы. Сохранённое исходное правило ограничивает подтверждение 2000 историческими и 2000 современными боями; дальнейшие прогоны не разрешены.
