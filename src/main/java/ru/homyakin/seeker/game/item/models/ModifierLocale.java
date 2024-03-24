package ru.homyakin.seeker.game.item.models;

import java.util.Map;
import ru.homyakin.seeker.locale.WordForm;

public record ModifierLocale(
    Map<WordForm, String> form
) {
    /**
     * @param wordForm Запрашиваемая форма слова
     * @return Нужная форма слова или <code>WordForm.WITHOUT</code>. Считается, что для языка всегда существуют
     *         нужные формы или <code>WordForm.WITHOUT</code>
     */
    public String getFormOrWithout(WordForm wordForm) {
        final var requestedForm = form.get(wordForm);
        if (requestedForm == null) {
            return form.get(WordForm.WITHOUT);
        }
        return requestedForm;
    }
}
