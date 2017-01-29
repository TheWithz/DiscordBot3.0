package misc;

import events.commands.TagCommand;
import events.commands.TodoCommand;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;

/**
 * Created by TheWithz on 2/21/16.
 */
public class Database {
    private static Database instance;

    private Connection conn;
    private HashMap<String, PreparedStatement> preparedStatements;

    public static Database getInstance() {
        if (instance == null)
            instance = new Database();
        return instance;
    }

    private Database() {
        try {
            JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get("Config.json"))));
            preparedStatements = new HashMap<>();
            com.mysql.jdbc.Driver.class.newInstance();
            conn = DriverManager.getConnection("jdbc:mysql://thewithz-raspi3:3306/thewithzBot?useSSL=false", obj.getString("mysqlUserName"), obj.getString("mysqlPassword"));
            Statement statement = conn.createStatement();
            statement.setQueryTimeout(30);
            statement.execute("SET FOREIGN_KEY_CHECKS=0;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Ops(id VARCHAR(18))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " +
                                            "TodoLists(" +
                                            "id INTEGER NOT NULL AUTO_INCREMENT," +
                                            "label VARCHAR(50) NOT NULL," +
                                            "owner VARCHAR(18) NOT NULL," +
                                            "locked BOOLEAN," +
                                            "PRIMARY KEY (id)" +
                                            ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " +
                                            "TodoEntries(" +
                                            "id INTEGER NOT NULL AUTO_INCREMENT," +
                                            "listId INTEGER," +
                                            "content TEXT NOT NULL," +
                                            "checked BOOLEAN," +
                                            "PRIMARY KEY (id)," +
                                            "FOREIGN KEY (listId) REFERENCES TodoLists(id) ON DELETE CASCADE" +
                                            ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " +
                                            "TodoUsers(" +
                                            "listId INT," +
                                            "userId VARCHAR(18) NOT NULL," +
                                            "PRIMARY KEY (listId, userId)," +
                                            "FOREIGN KEY (listId) REFERENCES TodoLists(id) ON DELETE CASCADE" +
                                            ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " +
                                            "Tags(" +
                                            "id INTEGER NOT NULL AUTO_INCREMENT," +
                                            "label TEXT NOT NULL," +
                                            "content TEXT NOT NULL," +
                                            "PRIMARY KEY (id)" +
                                            ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " +
                                            "Messages(" +
                                            "id INTEGER NOT NULL AUTO_INCREMENT," +
                                            "messages INTEGER," +
                                            "PRIMARY KEY (id)" +
                                            ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " +
                                            "Guilds(" +
                                            "id INTEGER NOT NULL AUTO_INCREMENT," +
                                            "guilds INTEGER," +
                                            "PRIMARY KEY (id)" +
                                            ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " +
                                            "Uptime(" +
                                            "id INTEGER NOT NULL AUTO_INCREMENT," +
                                            "uptime VARCHAR(64)," +
                                            "PRIMARY KEY (id)" +
                                            ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " +
                                            "Commands(" +
                                            "id INTEGER NOT NULL AUTO_INCREMENT," +
                                            "commands INTEGER," +
                                            "PRIMARY KEY (id)" +
                                            ")");

            //Permissions
            preparedStatements.put(Permissions.ADD_OP, conn.prepareStatement("REPLACE INTO Ops (id) VALUES (?)", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(Permissions.GET_OPS, conn.prepareStatement("SELECT id FROM Ops", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(Permissions.REMOVE_OPS, conn.prepareStatement("DELETE FROM Ops WHERE id = ?", Statement.RETURN_GENERATED_KEYS));

            //TodoCommand
            preparedStatements.put(TodoCommand.ADD_TODO_LIST, conn.prepareStatement("INSERT INTO TodoLists (label, owner, locked) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.ADD_TODO_ENTRY, conn.prepareStatement("INSERT INTO TodoEntries (listId, content, checked) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.ADD_TODO_USER, conn.prepareStatement("INSERT INTO TodoUsers (listId, userId) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.EDIT_TODO_ENTRY, conn.prepareStatement("UPDATE TodoEntries SET content = ? WHERE listId = ? AND id = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.GET_TODO_LISTS, conn.prepareStatement("SELECT id, label, owner, locked FROM TodoLists", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.GET_TODO_ENTRIES, conn.prepareStatement("SELECT id, content, checked FROM TodoEntries WHERE listId = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.GET_TODO_USERS, conn.prepareStatement("SELECT userId FROM TodoUsers WHERE listId = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.SET_TODO_LIST_LOCKED, conn.prepareStatement("UPDATE TodoListS SET locked = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.SET_TODO_ENTRY_CHECKED, conn.prepareStatement("UPDATE TodoEntries SET checked = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.SET_TODO_ENTRIES_CHECKED, conn.prepareStatement("UPDATE TodoEntries SET checked = ? WHERE listId = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.REMOVE_TODO_LIST, conn.prepareStatement("DELETE FROM TodoLists WHERE id = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.REMOVE_TODO_ENTRY, conn.prepareStatement("DELETE FROM TodoEntries WHERE id = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TodoCommand.REMOVE_TODO_USER, conn.prepareStatement("DELETE FROM TodoUsers WHERE listId = ? AND userId = ?", Statement.RETURN_GENERATED_KEYS));

            //TagCommand
            preparedStatements.put(TagCommand.ADD_TAG, conn.prepareStatement("INSERT INTO Tags (label, content) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TagCommand.EDIT_TAG_LABEL, conn.prepareStatement("UPDATE Tags SET label = ? WHERE id = ? AND label = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TagCommand.EDIT_TAG_CONTENT, conn.prepareStatement("UPDATE Tags SET content = ? WHERE id = ? AND label = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TagCommand.GET_TAG, conn.prepareStatement("SELECT content FROM Tags WHERE id = ? AND label = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TagCommand.GET_TAGS, conn.prepareStatement("SELECT id, label, content FROM Tags", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(TagCommand.REMOVE_TAG, conn.prepareStatement("DELETE FROM Tags WHERE id = ?", Statement.RETURN_GENERATED_KEYS));

            //Statistics
            preparedStatements.put(Statistics.GET_MESSAGES_RECEIVED, conn.prepareStatement("SELECT messages FROM Messages", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(Statistics.GET_COMMANDS_RUN, conn.prepareStatement("SELECT commands FROM Commands", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(Statistics.EDIT_MESSAGES_RECEIVED, conn.prepareStatement("UPDATE Messages SET messages = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(Statistics.EDIT_COMMANDS_RUN, conn.prepareStatement("UPDATE Commands SET commands = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(Statistics.EDIT_GUILDS_JOINED, conn.prepareStatement("UPDATE Guilds SET guilds = ?", Statement.RETURN_GENERATED_KEYS));
            preparedStatements.put(Statistics.EDIT_UPTIME, conn.prepareStatement("UPDATE Uptime SET uptime = ?", Statement.RETURN_GENERATED_KEYS));
        } catch (IOException | InstantiationException | IllegalAccessException | SQLException e) {
            e.printStackTrace();
        }
    }

    public PreparedStatement getStatement(String statementName) {
        if (!preparedStatements.containsKey(statementName))
            throw new RuntimeException("The statement: '" + statementName + "' does not exist.");
        return preparedStatements.get(statementName);
    }

    public static int getAutoIncrement(PreparedStatement executedStatement, int col) throws SQLException {
        ResultSet autoIncrements = executedStatement.getGeneratedKeys();
        autoIncrements.next();
        return autoIncrements.getInt(col);
    }
}