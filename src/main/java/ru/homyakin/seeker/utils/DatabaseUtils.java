package ru.homyakin.seeker.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtils {
    public static Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int i = resultSet.getInt(columnName);
        if (resultSet.wasNull()) {
            return null;
        }
        return i;
    }
}
