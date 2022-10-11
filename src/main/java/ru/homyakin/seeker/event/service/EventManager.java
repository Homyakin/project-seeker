package ru.homyakin.seeker.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.chat.Chat;
import ru.homyakin.seeker.telegram.chat.ChatService;
import ru.homyakin.seeker.event.config.EventConfig;
import ru.homyakin.seeker.event.models.Event;
import ru.homyakin.seeker.event.models.LaunchedEvent;
import ru.homyakin.seeker.locale.Localization;
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

    public EventManager(
        EventConfig eventConfig,
        ChatService chatService,
        EventService eventService,
        TelegramSender telegramSender,
        LaunchedEventService launchedEventService
    ) {
        this.eventConfig = eventConfig;
        this.chatService = chatService;
        this.eventService = eventService;
        this.telegramSender = telegramSender;
        this.launchedEventService = launchedEventService;
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
        launchedEventService.updateActive(launchedEvent, false);
        launchedEventService.getChatEvents(launchedEvent)
            .forEach(chatEvent -> {
                //TODO редактировать исходное сообщение
                final var chat = chatService.getOrCreate(chatEvent.chatId());
                final var event = eventService.getEventById(launchedEvent.eventId())
                    .orElseThrow(() -> new IllegalStateException("Can't end nonexistent event"));
                telegramSender.send(TelegramMethods.createEditMessageText(
                    chatEvent.chatId(),
                    chatEvent.messageId(),
                    event.getLocaleByLanguageOrDefault(chat.language()).toEndMessage()
                ));
                telegramSender.send(TelegramMethods.createSendMessage(
                    chatEvent.chatId(),
                    Localization.get(chat.language()).expiredEvent(),
                    chatEvent.messageId()
                ));
            });

    }

    private void launchEventInChat(Chat chat, Event event) {
        final var launchedEvent = launchedEventService.createLaunchedEvent(event);
        var result = telegramSender.send(
            TelegramMethods.createSendMessage(
                chat.id(),
                event.getLocaleByLanguageOrDefault(chat.language()).toStartMessage(),
                Keyboards.joinEventKeyboard(chat.language(), launchedEvent.id())
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
