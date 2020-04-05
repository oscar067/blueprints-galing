package ocp.bluprints.executor;

import ocp.bluprints.echo.Event;

import java.sql.*;
import java.time.Instant;

public class Persistence {

    private static String theSql = "INSERT INTO gatlingPocCheck (traceId, personId, created_on) VALUES (?,?,?)";
    private static String createTableTest = "CREATE TABLE gatlingPocCheck (\n" +
            "   traceId VARCHAR(200) UNIQUE NOT NULL,\n" +
            "   personId VARCHAR(200),\n" +
            "   created_on TIMESTAMP NOT NULL\n" +
            ")";
    private Connection connection = null;

    public Persistence() throws SQLException {
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres");
        createTable();
    }

    public void insertElement(Event consumedEvent) {
        try {
            PreparedStatement ps = connection.prepareStatement(theSql);
            ps.setString(1, consumedEvent.getTraceId().toString());
            ps.setString(2, consumedEvent.getPersonId().toString());
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.execute();
        } catch (SQLException e) {
            System.out.println("Ignored.." + e.getLocalizedMessage());
        }

    }

    private void createTable() {
        Statement ps = null;
        try {
            ps = connection.createStatement();
            ps.execute(createTableTest);
        } catch (SQLException e) {
            //Ignored
        }

    }

}
