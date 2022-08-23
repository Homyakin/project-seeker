package ru.homyakin.seeker.command;

import java.lang.reflect.ParameterizedType;

public abstract class CommandExecutor<T extends Command> {

    public abstract void execute(T command);

    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
