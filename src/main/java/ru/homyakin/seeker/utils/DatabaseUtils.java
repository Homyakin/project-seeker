package ru.homyakin.seeker.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class DatabaseUtils {
    public static Integer getIntOrDefault(ResultSet resultSet, String columnName, Integer defaultValue) throws SQLException {
        int i = resultSet.getInt(columnName);
        if (resultSet.wasNull()) {
            return defaultValue;
        }
        return i;
    }

    public static Integer getIntOrNull(ResultSet resultSet, String columnName) throws SQLException {
        return getIntOrDefault(resultSet, columnName, null);
    }

    public static Optional<Integer> getIntOrEmpty(ResultSet resultSet, String columnName) throws SQLException {
        return Optional.ofNullable(getIntOrNull(resultSet, columnName));
    }

    public static Long getLongOrDefault(ResultSet resultSet, String columnName, Long defaultValue) throws SQLException {
        final var i = resultSet.getLong(columnName);
        if (resultSet.wasNull()) {
            return defaultValue;
        }
        return i;
    }

    public static Long getLongOrNull(ResultSet resultSet, String columnName) throws SQLException {
        return getLongOrDefault(resultSet, columnName, null);
    }
}
