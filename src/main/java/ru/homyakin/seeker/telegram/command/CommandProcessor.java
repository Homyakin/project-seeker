package ru.homyakin.seeker.telegram.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CommandProcessor.class);
    /*
    Немного магии на дженериках. В конструкторе собираются все исполнители. Если указать <Command>,
     то будут только явные имплементации с <Command>. Проставлены @SuppressWarnings, потому что линтёр
     не может проверить типы.
     */
    @SuppressWarnings("rawtypes")
    private final Map<Class, CommandExecutor> executorMap;

    @SuppressWarnings("rawtypes")
    public CommandProcessor(List<CommandExecutor> executorList) {
        executorMap = new HashMap<>();

        for (final CommandExecutor executor: executorList) {
            executorMap.put(executor.getCommandType(), executor);
        }
    }

    @SuppressWarnings("unchecked")
    public void process(Command command) {
        logger.info("Executing " + command.toString());
        Optional.ofNullable(executorMap.get(command.getClass()))
            .ifPresentOrElse(
                commandExecutor -> commandExecutor.execute(command),
                () -> logger.error("No executor for command " + command.getClass())
            );
    }
}
