package ru.homyakin.seeker.telegram.command.user.contraband;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.contraband.action.ContrabandService;
import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.contraband.entity.ContrabandConfig;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.contraband.ContrabandLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.ContrabandKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ViewContrabandExecutor extends CommandExecutor<ViewContraband> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ContrabandService contrabandService;
    private final ContrabandConfig config;
    private final PersonageService personageService;

    public ViewContrabandExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ContrabandService contrabandService,
        ContrabandConfig config,
        PersonageService personageService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.contrabandService = contrabandService;
        this.config = config;
        this.personageService = personageService;
    }

    @Override
    public void execute(ViewContraband command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var language = user.language();

        final var contrabandOpt = contrabandService.getActiveContraband(user.personageId());
        if (contrabandOpt.isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(ContrabandLocalization.noActiveContraband(language))
                .build()
            );
            return;
        }

        final var contraband = contrabandOpt.get();
        if (contraband.canBeProcessedByFinder()) {
            sendFinderView(user, language, contraband);
        } else {
            sendReceiverView(user, language, contraband);
        }
    }

    private void sendFinderView(User user, Language language, Contraband contraband) {
        final var finderSuccessChance = config.finderSuccessChancePercent();
        final var sellPrice = config.sellPrice(contraband.tier());
        final var text = ContrabandLocalization.contrabandFoundPrivateMessage(
            language, contraband, finderSuccessChance, sellPrice
        );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ContrabandKeyboards.finderChoiceKeyboard(language, contraband, sellPrice))
            .build()
        );
    }

    private void sendReceiverView(User user, Language language, Contraband contraband) {
        final var finder = personageService.getByIdForce(contraband.finderPersonageId());
        final var receiverSuccessChance = config.receiverSuccessChancePercent();
        final var text = ContrabandLocalization.receiverNotification(
            language, contraband, finder, receiverSuccessChance
        );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ContrabandKeyboards.receiverOpenKeyboard(language, contraband))
            .build()
        );
    }
}
