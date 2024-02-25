# Правила оформления кода

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

## Соответствие checkstyle

Для автоматизированной проверки правил кода используется checkstyle. Правила описаны в файлах:
- [checkstyle.xml](../../checkstyle.xml)
- [checkstyle-suppression.xml](../../checkstyle-suppression.xml)

## База данных
Доступ к базе данных осуществляется с помощью прямых SQL запросов. ORM не используется.

Всё взаимодействие с базой данных находится в классах *Dao.

## Объекты
Объекты отражают реальные сущности из доменной области. Всё, что изменяет состояние объекта, находится внутри класса.
Пример:
```java
public record Personage(
    String name
) {
    public Either<NameError, Personage> changeName(String name) {
        return validateName(name)
            .map(this::copyWithName);
    }

    public static Either<NameError, String> validateName(String name) {
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            return Either.left(new NameError.InvalidLength(MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return Either.left(new NameError.NotAllowedSymbols());
        }
        return Either.right(name);
    }

    private static final int MIN_NAME_LENGTH = 10;
    private static final int MAX_NAME_LENGTH = 20;
    private static final Pattern NAME_PATTERN = Pattern.compile("....");
}
```
Здесь класс Personage отражает сущность игрового персонажа, и имя является его параметром. 
Поэтому все манипуляции с именем находятся внутри класса. 

Однако, за изменения состояния объекта в базе данных отвечает сервис:
```java
public class PersonageService {
    //...
    public Either<NameError, Personage> changeName(Personage personage, String name) {
        return personage.changeName(name).peek(personageDao::update);
    }
}
```

## Обработка ошибок
Для ошибок бизнес логики используется return-based подход с использованием Either из библиотеки vavr.io
```java
public record Personage(
    String name
) {
    public Either<NameError, Personage> changeName(String name) {
        return validateName(name).map(this::copyWithName);
    }
}
```

Исключения используются для ситуаций, которые невозможно обработать корректно.

