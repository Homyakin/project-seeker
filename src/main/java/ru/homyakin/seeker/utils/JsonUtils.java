package ru.homyakin.seeker.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.sql.SQLException;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsonUtils {
    private static final ObjectMapper objectMapper = JsonMapper.builder()
        .addModule(new Jdk8Module())
        .build();
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public <T> T fromString(String string, Class<T> clazz) {
        try {
            return objectMapper.readValue(string, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Can't parse " + clazz.getSimpleName(), e);
            throw new IllegalStateException(e);
        }
    }

    public <T> PGobject mapToPostgresJson(T object) {
        try {
            PGobject jsonObject = new PGobject();
            final var jsonStr = objectMapper.writeValueAsString(object);
            jsonObject.setType("json");
            jsonObject.setValue(jsonStr);
            return jsonObject;
        } catch (JsonProcessingException | SQLException e) {
            logger.error("Can't deserialize " + object.getClass().getSimpleName(), e);
            throw new IllegalStateException(e);
        }
    }
}
