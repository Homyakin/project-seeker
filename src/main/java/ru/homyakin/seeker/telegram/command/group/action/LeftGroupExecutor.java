package ru.homyakin.seeker.telegram.command.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;

@Component
class LeftGroupExecutor extends CommandExecutor<LeftGroup> {
    private final GroupTgService groupTgService;

    public LeftGroupExecutor(GroupTgService groupTgService) {
        this.groupTgService = groupTgService;
    }

    @Override
    public void execute(LeftGroup command) {
        groupTgService.setNotActive(command.groupId());
    }
}
