package ru.homyakin.seeker.game.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.chat.models.Chat;
import ru.homyakin.seeker.telegram.chat.ChatService;
import ru.homyakin.seeker.game.event.config.EventConfig;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.Keyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class EventManager {
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);
    private final EventConfig eventConfig;
    private final ChatService chatService;
    private final EventService eventService;
    private final TelegramSender telegramSender;
    private final LaunchedEventService launchedEventService;
    private final EventProcessing eventProcessing;

    public EventManager(
        EventConfig eventConfig,
        ChatService chatService,
        EventService eventService,
        TelegramSender telegramSender,
        LaunchedEventService launchedEventService,
        EventProcessing eventProcessing
    ) {
        this.eventConfig = eventConfig;
        this.chatService = chatService;
        this.eventService = eventService;
        this.telegramSender = telegramSender;
        this.launchedEventService = launchedEventService;
        this.eventProcessing = eventProcessing;
    }

    public void launchEventsInChats() {
        // Здесь может возникнуть какая-нибудь многопоточная гонка, потом можно добавить локи на чаты
        chatService
            .getGetChatsWithLessNextEventDate(TimeUtils.moscowTime())
            .forEach(chat -> {
                logger.debug("Creating event for chat " + chat.id());
                final var event = eventService.getRandomEvent();
                launchEventInChat(chat, event);
            });
    }

    public void stopEvents() {
        launchedEventService
            .getExpiredActiveEvents()
            .forEach(this::stopLaunchedEvent);
    }

    private void stopLaunchedEvent(LaunchedEvent launchedEvent) {
        logger.debug("Stopping event " + launchedEvent.id());
        final var result = eventProcessing.processEvent(launchedEvent);
        launchedEventService.updateActive(launchedEvent, false);
        launchedEventService.getChatEvents(launchedEvent)
            .forEach(chatEvent -> {
                final var chat = chatService.getOrCreate(chatEvent.chatId());
                final var event = eventService.getEventById(launchedEvent.eventId())
                    .orElseThrow(() -> new IllegalStateException("Can't end nonexistent event"));
                telegramSender.send(TelegramMethods.createEditMessageText(
                    chatEvent.chatId(),
                    chatEvent.messageId(),
                    event.toStartMessage(chat.language())
                ));
                telegramSender.send(TelegramMethods.createSendMessage(
                    chatEvent.chatId(),
                    event.endMessage(chat.language(), result),
                    chatEvent.messageId()
                ));
            });

    }

    private void launchEventInChat(Chat chat, Event event) {
        final var launchedEvent = launchedEventService.createLaunchedEvent(event);
        var result = telegramSender.send(
            TelegramMethods.createSendMessage(
                chat.id(),
                event.toStartMessage(chat.language(), launchedEvent.endDate()),
                //TODO выбирать клавиатуру в зависимости от типа события
                Keyboards.joinBossEventKeyboard(chat.language(), launchedEvent.id())
            )
        );
        if (result.isLeft()) {
            launchedEventService.updateActive(launchedEvent, false);
            return;
        }
        launchedEventService.addChatMessage(launchedEvent, chat, result.get().getMessageId());
        chatService.updateNextEventDate(
            chat,
            TimeUtils.moscowTime()
                .plus(
                    RandomUtils.getRandomDuration(
                        eventConfig.minimalInterval(), eventConfig.maximumInterval()
                    )
                )
        );
    }
}
