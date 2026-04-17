package io.github.agebhar1.snippets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {

    public static void main(String[] args) throws Exception {

        final String sql = "SELECT ? FROM DUAL";

        try (final Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521/demo", "jdbc", "jdbc");
             final PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, "oO");

            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final String value = resultSet.getString(1);
                    System.out.format("value: %s\n", value);
                }
            }
        }
    }

}
