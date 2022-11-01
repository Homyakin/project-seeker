# Боёвка

## Боевой персонаж

Характеристики:
- Здоровье (hp)
- Атака (atk)
- Защита (def)
- Сила (strength)
- Ловкость (agility)
- Мудрость (wisdom)

## Бой

### Попадание игрока1 по игроку2
Шанс попадания игрока 1 по игроку 2 равен 50 процентам плюс разность между ловкостями игроков.
Но не менее 10% и не более 90% <br>

`agility1` - ловкость игрока1 <br>
`agility2` - ловкость игрока2 <br>

`chance = 50 + (agility1 - agility2)`

`hitChance = chance < 10 ? 10 : chance > 90 ? 90 : chance`

### Шанс критического урона
Шанс критического урона игрока 1 по игроку 2 равен 50 процентам плюс разность между мудростями игроков.
Но не менее 10% и не более 90% <br>
`wisdom1` - мудрость игрока1 <br>
`wisdom2` - мудрость игрока2 <br>

`chance = 50 + (wisdom1 - wisdom2)`

`critChance = chance < 10 ? 10 : chance > 90 ? 90 : chance`

### Один ход боя

```
rand = random(1, 100)

если rand > hitChance:
    закончить ход (промах)
    
rand = random(1, 100)
crit = false
если rand <= critChance:
    crit = true
critMulti = 1,5 + (wisdom - wisdom(противника)) / 100
critMulti = critMulti < 1,5 ? 1,5 : critMulti

rand = random_double(-10 + strength, 10 + strength)
baseAtk = atk*(1 + rand/100)
damage = crit ? baseAtk : baseAtk * critMulti

rand = random_double(-10 + strength(противник), 10 + strength(противник))
finalDef(противник) = def(противник)*(1 + rand/100)

finalDamage = damange - finalDef(противник) < damage*0,1 ? damage*0,1 : damange - finalDef(противник)

hp(противник) -= finalDamage

если hp(противник) < 0:
    поражение противник
```




