# Как добавить новую локализацию?

1. Необходимо зайти в [папку с файлами локализации](../../src/main/resources/localization).
2. Добавить папку с кодом своего языка.
3. Перевести все `.toml` файлы из других папок
4. Зайти в [папку с игровыми данными](../../src/main/resources/game-data)
5. Добавить к каждой записи объект со своим переводом. 
Пример для events.toml:
```toml
[event.raid.locales.RU]
intro = "Появилось соломенное чучело!"
description = "Посреди площади появилось уродливое чучело из соломы. Уничтожьте его!"

[event.raid.locales.КОД_ЯЗЫКА]
intro = "ПЕРЕВОД"
description = "ПЕРЕВОД"
```
6. Сделать пулл реквест с добавлением новой локализации

# How to add new localization?
1. Go to [localization files folder](../../src/main/resources/localization).
2. Add a folder with your language code.
3. Translate all `.toml` files from other folders
4. Go to [folder with game data](../../src/main/resources/game-data)
5. Add your translation to each object. Example for events.toml:
```toml
[event.raid.locales.RU]
intro = "Появилось соломенное чучело!"
description = "Посреди площади появилось уродливое чучело из соломы. Уничтожьте его!"

[event.raid.locales.LANGUAGE_CODE]
intro = "YOUR_TRANSLATE"
description = "YOUR_TRANSLATE"
```
6. Make pull request with the addition of new localization
