package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;

@Component
public class AddModifierExecutor extends CommandExecutor<AddModifier> {
    private final ConfirmEnhanceExecutor confirmEnhanceExecutor;

    public AddModifierExecutor(ConfirmEnhanceExecutor confirmEnhanceExecutor) {
        this.confirmEnhanceExecutor = confirmEnhanceExecutor;
    }

    @Override
    public void execute(AddModifier command) {
        confirmEnhanceExecutor.execute(new ConfirmEnhance(command.userId(), command.itemId()));
    }
}
