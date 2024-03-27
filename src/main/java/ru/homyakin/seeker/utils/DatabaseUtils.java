package ru.homyakin.seeker.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtils {
    public static int getIntOrDefault(ResultSet resultSet, String columnName, int defaultValue) throws SQLException {
        int i = resultSet.getInt(columnName);
        if (resultSet.wasNull()) {
            return defaultValue;
        }
        return i;
    }
}
