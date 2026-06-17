package ru.homyakin.seeker.utils;

import java.util.Map;

public class StringNamedTemplate {
    private static final String START_TOKEN = "${";
    private static final String END_TOKEN = "}";

    // https://gist.github.com/Homyakin/812028a0ed7aef18af2fbd62fbeac0c1
    public static String format(String template, Map<String, Object> parameters) {
        if (template == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();

        int startIndex = 0;
        while (startIndex < template.length()) {
            int openIndex = template.indexOf(START_TOKEN, startIndex);
            if (openIndex < 0) {
                result.append(template.substring(startIndex));
                break;
            }

            int closeIndex = template.indexOf(END_TOKEN, openIndex);
            if (closeIndex < 0) {
                result.append(template.substring(startIndex));
                break;
            }

            String key = template.substring(openIndex + START_TOKEN.length(), closeIndex);
            if (!parameters.containsKey(key)) {
                result.append(template, startIndex, closeIndex + END_TOKEN.length());
            } else {
                result.append(template, startIndex, openIndex);
                result.append(parameters.get(key));
            }

            startIndex = closeIndex + END_TOKEN.length();
        }

        return result.toString();
    }
}
