package pl.edu.agh.fis.vtaskmaster.core;

import org.sqlite.SQLiteConfig;
import pl.edu.agh.fis.vtaskmaster.core.model.ExecutedTask;
import pl.edu.agh.fis.vtaskmaster.core.model.Task;

import java.sql.*;
import java.util.ArrayList;


public class CoreDB {
    /**
     * sqlite driver for JDBC
     */
    public static final String DRIVER = "org.sqlite.JDBC";

    /**
     * address of the database
     */
    public static final String DB_URL = "jdbc:sqlite:vtaskmaster.db";

    /**
     * connection with database
     */
    private Connection connection;

    /**
     * statement to perform requests to database
     */
    private Statement statement;

    /**
     * Default constructor creates standard VTM db with name CoreDB.DB_URL.
     */
    public CoreDB() {
        try {
            Class.forName(CoreDB.DRIVER);
        }
        catch (ClassNotFoundException e) {
            System.err.println("Nie znaleziono sterownika JDBC");
            e.printStackTrace();
        }

        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            connection = DriverManager.getConnection(CoreDB.DB_URL, config.toProperties());
            statement = connection.createStatement();
        }
        catch (SQLException e) {
            System.err.println("Błąd połączenia z bazą");
            e.printStackTrace();
        }

        initDB();
    }

    /**
     * Constructor creates VTM db with name given in parameter.
     * @param dbName name of the database
     */
    public CoreDB(String dbName) {
        try {
            Class.forName(CoreDB.DRIVER);
        }
        catch (ClassNotFoundException e) {
            System.err.println("Nie znaleziono sterownika JDBC");
            e.printStackTrace();
        }

        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbName, config.toProperties());
            statement = connection.createStatement();
        }
        catch (SQLException e) {
            System.err.println("Błąd połączenia z bazą");
            e.printStackTrace();
        }

        initDB();
    }

    /**
     * creates tables in the database
     * @return true if the operation went positive, false otherwise
     */
    public boolean initDB() {
        String createTasks = "CREATE TABLE IF NOT EXISTS tasks(" +
                "name           varchar(45)     PRIMARY KEY NOT NULL, " +
                "description    TEXT, " +
                "priority       SMALLINT        NOT NULL, " +
                "expectedTime   BIGINT         NOT NULL, " +
                "favourite      BOOLEAN         DEFAULT FALSE, " +
                "todo           BOOLEAN         DEFAULT TRUE, " +
                "UNIQUE(name) ON CONFLICT ABORT)";

        String createExecutedTasks = "CREATE TABLE IF NOT EXISTS executedTasks(" +
                "id             INTEGER         PRIMARY KEY AUTOINCREMENT, " +
                "taskName       varchar(45), " +
                "startTime      BIGINT          NOT NULL, " +
                "endTime        BIGINT          NOT NULL, " +
                "duration       BIGINT          NOT NULL, " +
                "done           BOOLEAN         NOT NULL, " +
                "FOREIGN KEY(taskName) REFERENCES tasks(name) " +
                "ON DELETE CASCADE)";

        try {
            statement.execute(createTasks);
            statement.execute(createExecutedTasks);
        }
        catch (SQLException e) {
            System.err.println("Błąd w trakcie tworzenia tabel");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Clears the database.
     * @return true if the operation went positive, false otherwise
     */
    public boolean clearDB() {
        String dropTasks = "DROP TABLE IF EXISTS tasks";
        String dropExecutedTasks = "DROP TABLE IF EXISTS executedTasks";

        try {
            statement.execute(dropExecutedTasks);
            statement.execute(dropTasks);
        }
        catch(SQLException e) {
            System.err.println("Blad w trakcie czyszczenia tabel");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Adds a task to the database.
     * @param task task which you want to add
     * @return true if the operation went positive, false otherwise
     * @throws SQLException
     */
    public boolean addTask(Task task) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO tasks VALUES (?, ?, ?, ?, ?, ?)"
        );
        preparedStatement.setString(1, task.getName());
        preparedStatement.setString(2, task.getDescription());
        preparedStatement.setInt(3, task.getPriority());
        preparedStatement.setLong(4, task.getExpectedTime());
        preparedStatement.setBoolean(5, task.isFavourite());
        preparedStatement.setBoolean(6, task.isTodo());
        preparedStatement.execute();

        return true;
    }

    /**
     * Removes task with given name from the database.
     * @param taskName name of the task you want to remove
     * @return true if the operation went positive, false otherwise
     * @throws SQLException
     */
    public boolean removeTaskByName(String taskName) throws SQLException {
        return statement.execute("DELETE FROM tasks WHERE name='" + taskName + "'");
    }

    /**
     * Updates a task given as parameter.
     *
     * Remember that task which you got from the database remembers its name, even if it was modified in runtime.
     * You can change the name of the task and when performing this method, the right one in db will be modified.
     *
     * @param task task you want to update
     * @return true if the operation went positive, false otherwise
     * @throws SQLException
     */
    public boolean updateTask(Task task) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE tasks SET name = ?, description = ?, priority = ?, expectedTime = ?," +
                        "favourite = ?, todo = ? WHERE name = ?"
        );

        statement.setString(1, task.getName());
        statement.setString(2, task.getDescription());
        statement.setInt(3, task.getPriority());
        statement.setLong(4, task.getExpectedTime());
        statement.setBoolean(5, task.isFavourite());
        statement.setBoolean(6, task.isTodo());
        statement.setString(7, task.getOldName());

        statement.executeUpdate();
        statement.close();
        return true;
    }

    /**
     * Gets task from database with given query.
     *
     * Example query: "SELECT * FROM tasks WHERE todo"
     *
     * @param query query string
     * @return list of tasks
     * @throws SQLException
     */
    private ArrayList<Task> getTasksWithQuery(String query)
            throws SQLException
    {
        ArrayList<Task> tasks = new ArrayList<>();
        ResultSet result = statement.executeQuery(query);
        int priority;
        boolean favourite, todo;
        String name, description;
        long expectedTime;
        while(result.next()) {
            name = result.getString("name");
            description = result.getString("description");
            priority = result.getInt("priority");
            expectedTime = result.getLong("expectedTime");
            favourite = result.getBoolean("favourite");
            todo = result.getBoolean("todo");

            tasks.add(
                    new Task(name, description, priority, expectedTime, favourite, todo)
            );
        }
        return tasks;
    }

    /**
     * Gets tasks with positive todo flag.
     * @return list of tasks
     * @throws SQLException
     */
    public ArrayList<Task> getTodo() throws SQLException {
        return getTasksWithQuery("SELECT * FROM tasks WHERE todo");
    }

    /**
     * Gets tasks with positive favourite flag.
     * @return list of tasks
     * @throws SQLException
     */
    public ArrayList<Task> getFavourites() throws SQLException {
        return getTasksWithQuery("SELECT * FROM tasks WHERE favourite");
    }

    /**
     * Gets tasks with negative favourite flag.
     * @return list of tasks
     * @throws SQLException
     */
    public ArrayList<Task> getNonFavourites() throws SQLException {
        return getTasksWithQuery("SELECT * FROM tasks WHERE NOT favourite");
    }

    /**
     * Gets all tasks from the database.
     * @return list of tasks
     * @throws SQLException
     */
    public ArrayList<Task> getAllTasks() throws SQLException {
        return getTasksWithQuery("SELECT * FROM tasks");
    }

    /**
     * Gets tasks which have been already done.
     * @return list of tasks
     * @throws SQLException
     */
    public ArrayList<Task> getHistory() throws SQLException {
        return getTasksWithQuery(
                "SELECT name, description, priority, expectedTime, favourite, todo " +
                        "FROM tasks AS T JOIN executedTasks AS E " +
                        "ON E.taskName=T.name"
        );
    }

    /**
     * Gets a task with a given name.
     * @param name name of the task you want to get
     * @return task with given name
     * @throws SQLException
     */
    Task getTaskByName(String name) throws SQLException {
        int priority;
        boolean favourite, todo;
        String description;
        long expectedTime;
        Task task = null;

        ResultSet result = statement.executeQuery("SELECT * FROM tasks WHERE name='"
                + name + "'");

        if (result.next()) {
            priority = result.getInt("priority");
            description = result.getString("description");
            expectedTime = result.getLong("expectedTime");
            favourite = result.getBoolean("favourite");
            todo = result.getBoolean("todo");

            task = new Task(name, description, priority, expectedTime, favourite, todo);
        }
        else
        {
            // there's no such task
            return null;
        }
        return task;
    }

    /**
     * Checks if there is a task with a given name in the database.
     * @param taskName name to check
     * @return true if task with given name exists, false otherwise
     * @throws SQLException
     */
    public boolean isTaskWithName(String taskName) throws SQLException {
        ResultSet result = statement.executeQuery(
                "SELECT 1 FROM tasks WHERE name = '" + taskName + "'"
        );

        return result.next();
    }

    /**
     * Adds an executed task to the database.
     *
     * First parameter is an ordinary Task and the ExecutedTask is created with connection with it.
     *
     * @param task task which will be connected with created ExecutedTask
     * @param startTime time in ms since Jan 1, 1970
     * @return id of new executedTask
     * @throws SQLException
     */
    public int addExecutedTask(Task task, long startTime) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO executedTasks VALUES (NULL, ?, ?, ?, ?, ?)"
        );

        preparedStatement.setString(1, task.getName());
        preparedStatement.setLong(2, startTime);
        preparedStatement.setLong(3, 0);
        preparedStatement.setLong(4, 0);
        preparedStatement.setBoolean(5, false);
        preparedStatement.execute();

       return preparedStatement.getGeneratedKeys().getInt(1);
    }

    /**
     * Updates given executed task. The same mechanism as in {@link #updateTask(Task)}
     * @param task executed task you want to update
     * @return true if the operation went positive, false otherwise
     * @throws SQLException
     */
    public boolean updateExecutedTask(ExecutedTask task) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE executedTasks SET duration = ?, done = ?" +
                        "WHERE id = ?"
        );

        statement.setLong(1, task.getElapsedTime());
        statement.setBoolean(2, task.isDone());
        statement.setInt(3, task.getId());

        statement.executeUpdate();
        statement.close();
        return true;
    }

    /**
     * Removes given executedTask from the database.
     * @param task executedTask you want to remove from db
     * @return true if the operation went positive, false otherwise
     * @throws SQLException
     */
    public boolean removeExecutedTask(ExecutedTask task) throws SQLException{
        return statement.execute("DELETE FROM executedTasks WHERE id='" + task.getId() + "'");
    }

    /**
     * Gets executedTasks from database with given query.
     *
     * Example query: "SELECT * FROM executedTasks"
     *
     * @param query query string
     * @return list of executed tasks
     * @throws SQLException
     */
    private ArrayList<ExecutedTask> getExecutedTasksWithQuery(String query)
            throws SQLException
    {
        ArrayList<ExecutedTask> tasks = new ArrayList<>();

        ResultSet result = statement.executeQuery(query);
        String taskName;
        int id;
        boolean done;
        long startTime, endTime, elapsedTime;
        while(result.next()) {
            id = result.getInt("id");
            taskName = result.getString("taskName");
            done = result.getBoolean("done");
            startTime = result.getLong("startTime");
            endTime = result.getLong("endTime");
            elapsedTime = result.getLong("duration");

            tasks.add(
                    new ExecutedTask(id, taskName, startTime, endTime, elapsedTime, done)
            );
        }

        return tasks;
    }

    /**
     * Gets all executed tasks.
     * @return list of all excuted tasks
     * @throws SQLException
     */
    public ArrayList<ExecutedTask> getAllExecutedTasks() throws SQLException {
        return getExecutedTasksWithQuery("SELECT * FROM executedTasks");
    }

    /**
     * Gets all executed tasks for task with a given name.
     * @param name name of the task
     * @return list of executed tasks
     * @throws SQLException
     */
    public ArrayList<ExecutedTask> getAllExecutedTasksForTaskWithName(String name)
        throws SQLException
    {
        return getExecutedTasksWithQuery(
                "SELECT * FROM executedTasks WHERE taskName = '" + name + "'"
        );
    }

    /**
     * Gets all executed tasks before a given date.
     * @param date date which is upper bound
     * @return list of executed tasks
     * @throws SQLException
     */
    public ArrayList<ExecutedTask> getExecutedTasksBeforeDate(long date)
        throws SQLException
    {
        return getExecutedTasksWithQuery(
                "SELECT * FROM executedTasks WHERE startTime <= '" + date + "'"
        );
    }

    /**
     * Get all executed tasks after a given date.
     * @param date date which is lower bound
     * @return list of executed tasks
     * @throws SQLException
     */
    public ArrayList<ExecutedTask> getExecutedTasksAfterDate(long date)
            throws SQLException
    {
        return getExecutedTasksWithQuery(
                "SELECT * FROM executedTasks WHERE startTime >= '" + date + "'"
        );
    }

    /**
     * Gets all executed tasks with positive done flag.
     * @return list of executed tasks
     * @throws SQLException
     */
    public ArrayList<ExecutedTask> getExecutedTasksDone()
            throws SQLException
    {
        return getExecutedTasksWithQuery(
                "SELECT * FROM executedTasks WHERE done"
        );
    }

    /**
     * Gets an executed task with a given id.
     *
     * It's possible to get the id from {@link pl.edu.agh.fis.vtaskmaster.core.model.ExecutedTask}
     * @param id id of executedTask
     * @return executed task with given id
     * @throws SQLException
     */
    public ExecutedTask getExecutedTaskWithId(int id)
            throws SQLException
    {
        ResultSet result = statement.executeQuery("SELECT * FROM executedTasks WHERE id = '" + id + "'");

        String taskName;
        boolean done;
        long startTime, endTime, elapsedTime;
        if(result.next()) {
            id = result.getInt("id");
            taskName = result.getString("taskName");
            done = result.getBoolean("done");
            startTime = result.getLong("startTime");
            endTime = result.getLong("endTime");
            elapsedTime = result.getLong("duration");

            return new ExecutedTask(id, taskName, startTime, endTime, elapsedTime, done);
        }
        else {
            return null;
        }
    }

    /**
     * Closes connection with the database.
     */
    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.err.println("Problem z zamknieciem polaczenia");
            e.printStackTrace();
        }
    }
}
