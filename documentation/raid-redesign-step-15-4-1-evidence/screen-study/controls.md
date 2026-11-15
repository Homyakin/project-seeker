# Часть15.4.1: раздельные здоровье и атака, первая фиксированная разведка

Новый опыт отдельно от истории15.4. Рабочий код,target, настройки, БД, бот и
исторические материалы не менялись. Maven не запускался. Помощник компилирован
в собственном временном каталоге и вызывает настоящий Battle.process с V3
и ускоренным темпом1.75. Ровно50 условий×30 пар×2 порядка×2 модели=6000 боёв.
Все условия заранее согласованы; проигрышные строки и обе модели сохранены.

SAME:
H*=HrefNPC*(sqrt(qL)/sqrt(q25))*(Hplayers/HrefPlayers),
D*=DrefNPC*(sqrt(qL)/sqrt(q25))*(Dplayers/DrefPlayers).
CROSS меняет последние отношения местами. qL=1+((L−10)*.06+.14), прежний бонус
уровня. Нейтральные H/D используют прежние составляющие, но суммы коэффициентов
полученного урона и командные суммы сортируются для точной инвариантности типов.
Цели считаются через отношения к опорному набору, не через иначе округляемое
k*sqrt(q)*H. Концептуальные k сохранены в manifest.

Опора — фактические15.2 MAIN MID3L25: множитель4.414285714285715,
защита707 у Стража/309 у Пращника. Она постоянна для всех уровней и составов.
Численность M=max(2, N), Стражей floor(M/2), Пращников остальные. Естественные
HP420/280, атака70/130. Никаких весов M/(2F/S), общего множителя HP/ATK/DEF,
g/h/a/r или подгонки по исходам. Отдельные множители — targetH/naturalH(M)
и targetD/naturalD(M). Применяется одна min1ceil к базовому HP/атаке;
фактические H/D и верхние границы ошибки округления сохранены в каждой строке.

## Фабрики и случайность

Настоящие private guardian(double, V3)/slinger(double, V3) уже используют weight1.
Готовый BattlePersonage не хранит исходный Item. Поэтому помощник получает
настоящий шаблон фабрики при опорной защите и естественный шаблон при m1,
собирает неизменяемый синтетический Item и меняет только здоровье/части атак.
Повтор конструктора на том же изолированном unit-seed сохраняет UUID/gauge.
В final-поля BattlePersonage ничего не записывается. Один легендарный MAIN_HAND
даёт ровно4 очка, не8; крит1.5/1.7, скорость, угроза, типы, диапазоны и V3 умения
сохраняются. Привязка к текущей фабрике защищена проверками и source/classhash.

SHA48: первые6 байтов SHA256(namespace+'/'+groupId+'/'+pair),unsigned big-endian.
Namespaces:
MYCONID-15.4.1-SPLIT-SCREEN-GEN-20261108-v1,
MYCONID-15.4.1-SPLIT-SCREEN-BATTLE-20261108-v1.
Модель/вариант в ключ не входят. GEN создаёт игроков, затем выдаёт48 бит на
каждого противника. На unit-seed шаблон и пересборка изолированно повторяются;
все unit-seeds сохранены, проверяются диапазон/дубликаты/UUID-коллизии.
SAME/CROSS и контртипы имеют одинаковые идентификаторы/шкалы. Для APPEND
сохраняется префикс игроков; идентичность новых противников не обещается.

## Фиксация и проверки

Авторитетная подготовка: `frozen-preparation/`, до первого просмотра исходов.
Там50 входов,780 пар начальных чисел, снимок параметров и frozenJava.
`prebattle-freeze.sha256` фиксирует их контрольные суммы. Более ранняя
`preparation/` — только первая проверка построения до добавления проверки
монотонности; формулы/входы/коэффициенты не менялись, боёв в ней не было.

Проверки построения:30 полных сравнений настоящих weight1 фабрик и пересборки
на5 множителях×3seed×2 роли; независимые оси; SAME=CROSS exact на опоре;
4 контртипа PIERCE/SLASH×ARCANE/LEATHER с теми же числами дают одинаковые
H/D/цели/NPC для обеих моделей;20 префиксов игроков; настоящие эффекты через
PveAdaptationStudy.prepare; изолированный хвост генератора;400 проверок построения
для уровней1..50 при фиксированном снаряжении подтверждают неубываниеHP/ATK
и неизменную DEF. Они не являются дополнительными боями.

После6000 боёв Python независимо проверяет нейтральные составляющие по настоящим
начальным снимкам, цели, два закрытых множителя, пределы округления, HP/ATKбезвесов,
DEF707/309, V3 умения4 очка, эффекты, префиксы, типы/шкалы/UUID/seed, исходы поHP
и все100 строк сводки. Сырые gzip сохраняются побайтно, без xz/перепаковки.
Полные действия не хранятся: есть их хеши, начальные снимки и индивидуальные итоги.
Межпроцессный повтор боёв отдельно не выполнялся и без нового разрешения
не запускается. Эти30 пар — разведка, не принятие любого соотношения побед.

## Точные команды

cwd `/home/user/projects/project-seeker/game`. Каталоги вывода должны быть новыми.

```sh
split_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$split_classpath" -d /tmp/seeker-step15-4-separate-scaling.cZmkPh/classes /tmp/seeker-step15-4-separate-scaling.cZmkPh/Step1541SeparateScalingProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-separate-scaling.cZmkPh/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1541SeparateScalingProbe prepare /tmp/seeker-step15-4-separate-scaling.cZmkPh/frozen-preparation > /tmp/seeker-step15-4-separate-scaling.cZmkPh/frozen-preparation.log 2>&1
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dsplit.pairs=30 -cp "/tmp/seeker-step15-4-separate-scaling.cZmkPh/classes:target/classes:target/test-classes:$split_classpath" ru.homyakin.seeker.game.battle.Step1541SeparateScalingProbe screen /tmp/seeker-step15-4-separate-scaling.cZmkPh/screen > /tmp/seeker-step15-4-separate-scaling.cZmkPh/screen.log 2>&1
python3 /tmp/seeker-step15-4-separate-scaling.cZmkPh/validate.py /tmp/seeker-step15-4-separate-scaling.cZmkPh/screen > /tmp/seeker-step15-4-separate-scaling.cZmkPh/validation.log
python3 /tmp/seeker-step15-4-separate-scaling.cZmkPh/analyze.py /tmp/seeker-step15-4-separate-scaling.cZmkPh/screen
```

Перед окончательной подготовкой была такая же первая компиляция/prepare в
`preparation/`, без последних проверок монотонности. Новых боёв это не создавало.
Один повтор чтения hash-листов из game дал ошибку относительных путей;
проверка из корня репозитория до/после опыта прошла, никакие файлы не менялись:

```sh
# cwd /home/user/projects/project-seeker
sha256sum -c /tmp/seeker-step15-4-separate-scaling.cZmkPh/source-basis.sha256
sha256sum -c /tmp/seeker-step15-4-separate-scaling.cZmkPh/class-basis.sha256
sha256sum -c /tmp/seeker-step15-4-separate-scaling.cZmkPh/prebattle-freeze.sha256
```
