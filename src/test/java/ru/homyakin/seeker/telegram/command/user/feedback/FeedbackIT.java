package ru.homyakin.seeker.telegram.command.user.feedback;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.homyakin.seeker.telegram.user.state.FeedbackState;
import ru.homyakin.seeker.telegram.user.state.UserStateDao;
import ru.homyakin.seeker.test_config.BaseIntegrationTest;


public class FeedbackIT extends BaseIntegrationTest {
    @Autowired
    private InitFeedbackExecutor initFeedbackExecutor;

    @Autowired
    private UserStateDao userStateDao;

    @Test
    public void Given_UserWithClearState_When_InitFeedback_UserStateIsSetToInitFeedback() {
        final var userId = nextUserId();

        initFeedbackExecutor.execute(new InitFeedback(userId));

        final var state = userStateDao.getUserStateById(userId);

        Assertions.assertTrue(state.isPresent());
        Assertions.assertInstanceOf(FeedbackState.ChooseFeedbackThemeState.class, state.get());
    }
}
