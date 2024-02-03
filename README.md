# Project Seeker - социальная RPG в Telegram

## Локализация / Localization

[Как добавить новую локализацию (How to add new localization)](documentation/localization/README.md)

## Оформление коммитов

### Префиксы

- **BF** - (bug fix) исправление бага
- **RF** - (refactoring) структурные изменения, затрагивающие 2 класса и более
- **NF** - (new feature) новая функциональность
- **CF** - (change feature) изменения логики существующей функциональности
- **OPT** - (optimization) различные локальные оптимизации
- **BK** - (breaks something) используется для колечащих изменений в api
- **TF** - (tests feature) используется только если изменения затрагивают только тесты. если изменения вносятся вместе с рефакторингом или новой фичей, то используются эти типы
- **DOC** - (documentation) изменения документация
- **DB-CH** - (database change) коммит требует изменений в базе данных

### Сообщение коммита

`[CHANGE_TYPE] short descriptive message in English`<br>
**Пример:**<br>
`[NF] add get profile command`

## Технологии

- Java 21
- Docker
- PostgreSQL 14
- Python

## Самостоятельный запуск

Самостоятельно запустить можно с помощью [bash скрипта](deploy.sh) (необходимо иметь установленные maven и docker)

## Сопутствующая документация

- Документация по игре - [здесь](documentation/README.md)

## Разработчики

- [Homyakin](https://github.com/Homyakin) - автор идеи, программист, геймдизайнер;
- [Дима](https://github.com/Accdaeffi) - технический специалист;
- Александр Антюшин - нарративный дизайнер
- Вика Тимкова - нарративный дизайнер
