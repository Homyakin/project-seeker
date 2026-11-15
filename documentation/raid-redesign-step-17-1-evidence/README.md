# Материалы исправления Орды зомби, шаг 17.1

Исходные материалы шагов 14 и 16–18 не изменяются. Условия нового исследования заранее записаны в [протоколе](protocol.md); выводы — в [отчёте](../raid-redesign-step-17-1-zombie-calibration.md).

## Воспроизведение

Команды выполняются из корня репозитория. Нужны собранные `game/target/classes`, `game/target/test-classes` и зависимости тестов в `CP`. Используется `Step171ZombieCalibrationStudy` из тестового исходного кода; это не автоматический JUnit-тест и не запуск игрового бота.

```sh
java -Dzombie.study.productionCorrection=1 -cp "$CP" ru.homyakin.seeker.game.battle.Step171ZombieCalibrationStudy plan-screen NEW_SCREEN
java -Dzombie.study.productionCorrection=1 -cp "$CP" ru.homyakin.seeker.game.battle.Step171ZombieCalibrationStudy run-screen NEW_SCREEN
node documentation/raid-redesign-step-17-1-evidence/validate-screen.mjs NEW_SCREEN NEW_SCREEN_REPORT

java -Dzombie.study.productionCorrection=0.8 -cp "$CP" ru.homyakin.seeker.game.battle.Step171ZombieCalibrationStudy plan-final NEW_FINAL
java -Dzombie.study.productionCorrection=0.8 -cp "$CP" ru.homyakin.seeker.game.battle.Step171ZombieCalibrationStudy run-final NEW_FINAL
java -Dzombie.study.productionCorrection=0.8 -cp "$CP" ru.homyakin.seeker.game.battle.Step171ZombieCalibrationStudy replay-final NEW_FINAL NEW_REPLAY
node documentation/raid-redesign-step-17-1-evidence/validate-final.mjs NEW_FINAL NEW_REPLAY NEW_FINAL_REPORT
```

Каждый вывод создаётся только по новому пути. Исследовательская фаза выполнялась на производственном `C=1`; три исходных файла до правки сохранены в `screen-sources.tar.gz`. Для её воспроизведения после исправления нужны эти исходники и отдельная сборка, а не молчаливое использование новой формулы с прежним протоколом. Начальные значения и входные комплекты уже находятся в соответствующем `protocol.json`.

`screen-lock.sha256` фиксирует исходники и условия до исследовательских боёв; после намеренной производственной правки суммы трёх изменённых файлов ожидаемо отличаются. Архив позволяет проверить прежние суммы. Аналогично отдельный `final-lock.sha256` фиксирует окончательную формулу и условия до проверочных боёв. Итоговый `artifacts.sha256` относится к сохранённым материалам, а `inputs.sha256` — к актуальным публичным входам.

Полные журналы и трассы сохраняются для заранее выбранных 32 повторяемых боёв; для остальных — контрольные суммы. Сжатые исходные строки следует читать потоком, не загружать распакованную выборку целиком. Повторение сверяется целиком, без удаления полей; различие порядка ключей отображений несущественно. Проверяющий скрипт отдельно пересчитывает новую мощь, цель подбора, итоговые характеристики противников, исходы, пары, сравнения и условие подтверждения.

Историческая совместимость проверяется неизменённым `RaidHistoricalCompatibilityStudy` против пяти классов из прежнего `baseline-classes.tar.gz`: это сравнение генераторов при общей нынешней модели боя, не доказательство полного воспроизведения старого игрового выпуска. Запуски не обращаются к игровой БД, не начисляют наград и не включают выпуск.

## Результат

Предварительные 3120 боёв и их отдельная сверка выбрали `0,8`. На свежих 8300 боях правило подтверждено; все 32 полных повтора совпали. Полная чистая сборка: 1898 успешных тестов, 29 прежних/условных пропусков, ошибок нет; 858 сравнений совместимости успешны. Первая неудачная попытка сборки сохранена отдельно, не заменена успешным журналом.

[Технический разбор](independent-review-technical.md) и [разбор баланса](independent-review-balance.md) завершены после последней игровой правки. Ограничения — дешёвый дробящий состав на уровне 10, большая длительность боёв этого уровня и резкая сложность к 15 — раскрыты в основном отчёте. Исходные материалы 12 предыдущих наборов проверены с учётом их собственного корня путей, см. `previous-artifacts-validation.json`.
