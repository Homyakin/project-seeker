package ru.homyakin.seeker.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

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
}
