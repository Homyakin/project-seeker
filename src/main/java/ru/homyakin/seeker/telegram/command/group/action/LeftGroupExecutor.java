package ru.homyakin.seeker.telegram.command.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;

@Component
class LeftGroupExecutor extends CommandExecutor<LeftGroup> {
    private final GroupService groupService;

    public LeftGroupExecutor(GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    public void execute(LeftGroup command) {
        groupService.setNotActive(command.groupId());
    }
}
