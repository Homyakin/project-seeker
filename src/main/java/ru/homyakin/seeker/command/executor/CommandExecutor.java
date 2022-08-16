package ru.homyakin.seeker.command.executor;

import java.lang.reflect.ParameterizedType;
import ru.homyakin.seeker.command.models.Command;

public abstract class CommandExecutor<T extends Command> {

    public abstract void execute(T command);

    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
