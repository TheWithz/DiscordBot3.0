package events.commands;

import bots.RunBot;
import misc.Database;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageHistory;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class TodoCommand extends Command {
    //Database Methods
    public static final String ADD_TODO_LIST = "addTodoList";
    public static final String ADD_TODO_ENTRY = "addTodoEntry";
    public static final String ADD_TODO_USER = "addTodoUser";
    public static final String EDIT_TODO_ENTRY = "editTodoEntry";
    public static final String GET_TODO_LISTS = "getTodoLists";
    public static final String GET_TODO_ENTRIES = "getTodoEntries";
    public static final String GET_TODO_USERS = "getTodoUsers";
    public static final String SET_TODO_LIST_LOCKED = "setTodoListLocked";
    public static final String SET_TODO_ENTRY_CHECKED = "setTodoEntryChecked";
    public static final String SET_TODO_ENTRIES_CHECKED = "setTodoEntriesChecked";
    public static final String REMOVE_TODO_LIST = "removeTodoList";
    public static final String REMOVE_TODO_ENTRY = "removeTodoEntry";
    public static final String REMOVE_TODO_USER = "removeTodoUser";

    private boolean allowRemove = false;
    private String listToRemove = "";

    private HashMap<String, TodoList> todoLists = new HashMap<>();

    public TodoCommand() {
        try {
            ResultSet sqlTodoLists = Database.getInstance().getStatement(GET_TODO_LISTS).executeQuery();
            while (sqlTodoLists.next()) {
                String label = sqlTodoLists.getString(2);
                TodoList todoList = new TodoList(
                        sqlTodoLists.getInt(1),     //Id
                        label,
                        sqlTodoLists.getString(3),  //OwnerId
                        sqlTodoLists.getBoolean(4)  //Locked
                );
                todoLists.put(label, todoList);

                PreparedStatement getEntries = Database.getInstance().getStatement(GET_TODO_ENTRIES);
                getEntries.setInt(1, todoList.id);
                ResultSet sqlTodoEntries = getEntries.executeQuery();
                while (sqlTodoEntries.next()) {
                    TodoEntry todoEntry = new TodoEntry(
                            sqlTodoEntries.getInt(1),       //Id
                            sqlTodoEntries.getString(2),    //Content
                            sqlTodoEntries.getBoolean(3)    //Checked
                    );
                    todoList.entries.add(todoEntry);
                }
                getEntries.clearParameters();

                PreparedStatement getUsers = Database.getInstance().getStatement(GET_TODO_USERS);
                getUsers.setInt(1, todoList.id);
                ResultSet sqlTodoUsers = getUsers.executeQuery();
                while (sqlTodoUsers.next()) {
                    todoList.allowedUsers.add(sqlTodoUsers.getString(1)); //UserId
                }
                getUsers.clearParameters();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCommand(MessageReceivedEvent e, String[] args) {
        try {
            RunBot.checkArgs(args, 1, ":x: No Action argument was provided. Please use `" + RunBot.PREFIX + "help " + getAliases().get(0) + "` for more information.", e);

            switch (args[1].toLowerCase()) {
                case "show":
                    handleShow(e, args);
                    break;
                case "lists":
                    handleLists(e, args);
                    break;
                case "create":
                    handleCreate(e, args);
                    break;
                case "add":
                    handleAdd(e, args);
                    break;
                case "madd":
                    handleMultipleAdd(e, args);
                case "edit":
                    handleEdit(e, args);
                    break;
                case "mark":
                case "check":
                    handleCheck(e, args, true);
                    break;
                case "unmark":
                case "uncheck":
                    handleCheck(e, args, false);
                    break;
                case "lock":
                    handleLock(e, args, true);
                    break;
                case "unlock":
                    handleLock(e, args, false);
                    break;
                case "users":
                    handleUsers(e, args);
                    break;
                case "clear":
                    handleClear(e, args);
                    break;
                case "remove":
                    handleRemove(e, args);
                    break;
                default:
                    e.getChannel().sendMessage(":x: Unknown Action argument: `" + args[1] + "` was provided. " +
                                                       "Please use `" + RunBot.PREFIX + "help " + getAliases().get(0) + "` for more information.").queue();
            }
            if (Arrays.asList(args).contains("botfeatures") && args[0].equals(RunBot.PREFIX + "todo")) {
                refreshTodoChannel(e, args);
            }
        } catch (SQLException e1) {
            e.getChannel().sendMessage(":x: An SQL error occurred while processing command.\nError Message: " + e1.getMessage()).queue();
            e1.printStackTrace();
        } catch (IllegalArgumentException e2) {
            e.getChannel().sendMessage(e2.getMessage()).queue();
        }
    }

    private void refreshTodoChannel(MessageReceivedEvent e, String[] args) {
        TextChannel tc = RunBot.API.getTextChannelById("193539094410690561");
        tc.getMessageById("212593620618838017").queue(message -> {
            StringBuilder msg = getBotFeaturesShowMessage(tc, args);
            if (message != null) {
                message.editMessage(msg == null ? ":x: The message returned null." : msg.toString()).queue();
            }
        });
        MessageHistory hist = tc.getHistory();
        hist.retrievePast(2).queue(messages -> {
            messages.remove(0);
            messages.remove(0);
        });

    }

    private StringBuilder getBotFeaturesShowMessage(TextChannel tc, String[] args) {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " show [ListName]`", tc);

        String label = args[2].toLowerCase();
        TodoList todoList = todoLists.get(label);
        if (todoList == null)
            return null;

        // Discord messages can only be 2000 characters.
        StringBuilder builder = new StringBuilder();
        builder.append("```fix\nTodo for: ")
               .append(label).append("```")
               .append("```diff\n");
        for (int i = 0; i < todoList.entries.size(); i++) {
            TodoEntry todoEntry = todoList.entries.get(i);
            String todoEntryString = todoEntry.content;
            if (todoEntry.checked) {
                todoEntryString = "+" + (i + 1) + ") " + todoEntryString + "\n\n";
            } else {
                todoEntryString = "-" + (i + 1) + ") " + todoEntryString + "\n\n";
            }
            builder.append(todoEntryString);
        }
        return builder.append("```");
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList(RunBot.PREFIX + "todo");
    }

    @Override
    public String getDescription() {
        return "Used to create todo lists that can be checked off as things are completed.";
    }

    @Override
    public String getName() {
        return "Todo Command";
    }

    @Override
    public List<String> getUsageInstructionsEveryone() {
        return Arrays.asList(
                String.format(
                        "(%1$s)][Action] <Action Arguments>\n" +
                                "<Actions:>\n" +
                                "\n" +
                                "[show][ListName] - Shows all todo entries in the <ListName> TodoList.\n" +
                                "[[Example:](%1$s)][show] <shopping-list> would display all entries in the <shopping-list> list.\n" +
                                "\n" +
                                "[lists][Mentions...] - Displays the todo lists owned by the provided user(s).\n" +
                                "[[Example 1:](%1$s)][lists] Displays lists owned by the User that executed the command.\n" +
                                "[[Example 2:](%1$s)][lists] <@ TheWithz> Displays lists owned by <TheWithz>.\n" +
                                "\n" +
                                "[create][ListName] - Creates a new todo list with name <ListName>\n" +
                                "[[Example:](%1$s)][create] <project5> would create a todo list with the name <project5>\n" +
                                "\n" +
                                "[add][ListName] <Content...> - Adds a todo entry to the <ListName> todo list.\n" +
                                "[Example:](%1$s)][add] <project5> \"Fix bug where Users can delete System32\"\n" +
                                "\n" +
                                "[madd][ListName] <Content> <Content> <Content> etc. - Adds multiple entries to the <ListName> todo list.\n" +
                                "[[Example:](%1$s)][madd] <project5> \"fix house\" \"remove door\" \"eat lunch\"\n" +
                                "\n" +
                                "[edit][ListName] <Entry ID> <Content...> - Edits a todo entry from the <ListName> todo list.\n" +
                                "[[Example:](%1$s)][edit] <project5> <4> \"add more documentation for users of JDA API\"\n" +
                                "\n" +
                                "[mark/unmark][TodoList] <Entry Index> - Marks a todo entry as <complete> or <incomplete>.\n" +
                                "[[Example 1:](%1$s)][mark] <project5> <2> Marks the <second> entry in the <project5> list as <compelted>.\n" +
                                "[[Example 2:](%1$s)][unmark] <project5> <3> Marks the <third> entry in the <project5> list as <incomplete>.\n" +
                                "[[Example 3:](%1$s)][mark] <project5> <*> Marks <all> todo entries in the <project5> list as <completed>.\n" +
                                "<Note:> You can also use <check> and <uncheck>.\n" +
                                "\n",
                        getAliases().get(0)),

                //Second Usage Message
                String.format(
                        "\n[lock/unlock][ListName] - Used to lock a todo list such that only Auth'd users can modify it.\n" +
                                "[[Example 1:](%1$s)][lock] <project5> Locks the <project5> list such that only Auth'd users can use <add>,<mark> and <clear>\n" +
                                "[[Example 2:](%1$s)][unlock] <project5> Unlocks the <project5> list so that all users can modify it.\n" +
                                "________________________________________________________________________________________________________\n\n" +
                                "[[users][SubAction]](ListName) <SubAction Args> Used add, remove and list the Auth'd users for a todo list.\n" +
                                "<SubActions:>\n" +
                                "\n" +
                                "[add][ListName] <@ mentions...> Adds the mentions users to the Auth'd users for <ListName> list.\n" +
                                "[Example:](%1$s)[users](add) <project5> <@ Joe> <@ DudeMan> Adds Joe and DudeMan Auth'd users for the <project5> list.\n\n" +
                                "[remove][ListName] [@ mentions...] Removes the mentioned users from the Auth'd users for <ListName> list.\n" +
                                "[Example:](%1$s)[users](remove) <project5> <@ MrCatMan> Removes <MrCatMan> from the Auth'd users for the <project5> list.\n\n" +
                                "[list][ListName] Lists the Owner and Auth'd users for the <ListName> list.\n" +
                                "[Example:](%1$s)[users](list) <project5> Lists the owner and all Auth'd users for the <project5> list.\n" +
                                "________________________________________________________________________________________________________\n" +
                                "\n" +
                                "[clear][ListName] - Clears all <completed> todo entries from a list.\n" +
                                "[[Example:](%1$s)][clear] <project5> Clears all <completed> todo entries in the <project5> list\n" +
                                "\n" +
                                "[remove][ListName] - Completely deletes the <ListName> list. Only the list owner can do this.\n" +
                                "[[Example:](%1$s)][remove] <project5> Completely deletes the <project5> todo list.\n",
                        getAliases().get(0)));
    }

    @Override
    public List<String> getUsageInstructionsOp() {
        return getUsageInstructionsEveryone();
    }

    @Override
    public List<String> getUsageInstructionsOwner() {
        return getUsageInstructionsOp();
    }

    //alias show [ListName]
    private void handleShow(MessageReceivedEvent e, String[] args) {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " show [ListName]`", e);

        String label = args[2].toLowerCase();
        TodoList todoList = todoLists.get(label);
        if (todoList == null) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` isn't a known todo list.").queue();
            return;
        }

        // Discord messages can only be 2000 characters.
        List<Message> todoMessages = new ArrayList<Message>();
        MessageBuilder builder = new MessageBuilder();
        builder.appendCodeBlock("Todo for: " + label + "\n", "fix").appendString("```diff\n");
        for (int i = 0; i < todoList.entries.size(); i++) {
            TodoEntry todoEntry = todoList.entries.get(i);
            String todoEntryString = todoEntry.content;
            if (todoEntry.checked) {
                todoEntryString = "+" + (i + 1) + ") " + todoEntryString + "\n\n";
            } else {
                todoEntryString = "-" + (i + 1) + ") " + todoEntryString + "\n\n";
            }
            if (builder.length() + todoEntryString.length() > 2000) {
                todoMessages.add(builder.build());
                builder = new MessageBuilder();
            }
            builder.appendString(todoEntryString);
        }

        todoMessages.forEach(message -> e.getChannel().sendMessage(message).queue());
        e.getChannel().sendMessage(builder.appendString("```").build()).queue();
    }

    //alias show [ListName]
    private void handleShow(TextChannel tc, String[] args) {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " show [ListName]`", tc);

        String label = args[2].toLowerCase();
        TodoList todoList = todoLists.get(label);
        if (todoList == null) {
            tc.sendMessage(":x: Sorry, `" + label + "` isn't a known todo list.").queue();
            return;
        }

        // Discord messages can only be 2000 characters.
        List<Message> todoMessages = new ArrayList<Message>();
        MessageBuilder builder = new MessageBuilder();
        builder.appendCodeBlock("Todo for: " + label + "\n", "fix").appendString("```diff\n");
        for (int i = 0; i < todoList.entries.size(); i++) {
            TodoEntry todoEntry = todoList.entries.get(i);
            String todoEntryString = todoEntry.content;
            if (todoEntry.checked) {
                todoEntryString = "+" + (i + 1) + ") " + todoEntryString + "\n\n";
            } else {
                todoEntryString = "-" + (i + 1) + ") " + todoEntryString + "\n\n";
            }
            if (builder.length() + todoEntryString.length() > 2000) {
                todoMessages.add(builder.build());
                builder = new MessageBuilder();
            }
            builder.appendString(todoEntryString);
        }

        todoMessages.forEach(todoMessage -> tc.sendMessage(todoMessage).queue());
        tc.sendMessage(builder.appendString("```").build()).queue();
    }

    //alias lists
    //alias lists [mentions...]
    private void handleLists(MessageReceivedEvent e, String[] args) {
        List<User> mentionedUsers = e.getMessage().getMentionedUsers();
        if (mentionedUsers.size() == 0)
            mentionedUsers = Collections.singletonList(e.getAuthor());

        List<Message> messages = new LinkedList<Message>();
        for (User u : mentionedUsers) {
            MessageBuilder builder = new MessageBuilder();
            List<TodoList> lists = todoLists.values().stream().filter(list -> list.ownerId.equals(u.getId())).collect(Collectors.toList());
            builder.appendString("" + u.getName() + " owns **" + lists.size() + "** todo lists.\n");
            for (TodoList list : lists) {
                String listString = " - " + list.labelName + "\n";
                if (builder.length() + listString.length() > 2000) {
                    messages.add(builder.build());
                    builder = new MessageBuilder();
                }
                builder.appendString(listString);
            }
            messages.add(builder.build());
        }

        messages.forEach(msg -> e.getChannel().sendMessage(msg).queue());
    }

    //alias create [ListName]
    private void handleCreate(MessageReceivedEvent e, String[] args) throws SQLException {
        RunBot.checkArgs(args, 2, ":x: No ListName for the new todo list was provided. Usage: `" + getAliases().get(0) + " create [ListName]`", e);

        String label = args[2].toLowerCase();
        TodoList todoList = todoLists.get(label);

        if (todoList != null) {
            e.getChannel().sendMessage(":x: A todo list already exists with the name `" + label + "`.").queue();
            return;
        }

        PreparedStatement addTodoList = Database.getInstance().getStatement(ADD_TODO_LIST);
        addTodoList.setString(1, label);                //Label
        addTodoList.setString(2, e.getAuthor().getId());//OwnerId
        addTodoList.setBoolean(3, false);               //Locked
        if (addTodoList.executeUpdate() == 0)
            throw new SQLException(ADD_TODO_LIST + " reported no modified rows!");

        todoList = new TodoList(Database.getAutoIncrement(addTodoList, 1), label, e.getAuthor().getId(), false);
        todoLists.put(label, todoList);
        addTodoList.clearParameters();

        e.getChannel().sendMessage(":white_check_mark: Created `" + label + "` todo list. Use `" + getAliases().get(0) + " add " + label + " [content...]` " +
                                           "to add entries to this todo list.").queue();
    }

    //alias add [ListName] [Content ... ]
    private void handleAdd(MessageReceivedEvent e, String[] args) throws SQLException {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " add [ListName] [content...]`", e);
        RunBot.checkArgs(args, 3, ":x: No content was specified. Cannot create an empty todo entry!" +
                "Usage: `" + getAliases().get(0) + " add [ListName] [content...]`", e);

        String label = args[2].toLowerCase();
        String content = StringUtils.join(args, " ", 3, args.length);
        TodoList todoList = todoLists.get(label);

        if (todoList == null) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` isn't a known todo list. " +
                                               "Try using `" + getAliases().get(0) + " create " + label + "` to create a new list by this name.").queue();
            return;
        }

        if (todoList.locked && !todoList.isAuthUser(e.getAuthor())) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` is a locked todo list and you do not have permission to modify it.").queue();
            return;
        }

        PreparedStatement addTodoEntry = Database.getInstance().getStatement(ADD_TODO_ENTRY);
        addTodoEntry.setInt(1, todoList.id);
        addTodoEntry.setString(2, content);
        addTodoEntry.setBoolean(3, false);
        if (addTodoEntry.executeUpdate() == 0)
            throw new SQLException(ADD_TODO_ENTRY + " reported no modified rows!");

        todoList.entries.add(new TodoEntry(Database.getAutoIncrement(addTodoEntry, 1), content, false));
        addTodoEntry.clearParameters();

        e.getChannel().sendMessage(":white_check_mark: Added to `" + label + "` todo list.").queue();
    }

    private void handleMultipleAdd(MessageReceivedEvent e, String[] args) throws SQLException {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " add [ListName] [content...]`", e);
        RunBot.checkArgs(args, 3, ":x: No content was specified. Cannot create an empty todo entry!" +
                "Usage: `" + getAliases().get(0) + " add [ListName] [content...]`", e);

        String label = args[2].toLowerCase();
        TodoList todoList = todoLists.get(label);

        if (todoList == null) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` isn't a known todo list. " +
                                               "Try using `" + getAliases().get(0) + " create " + label + "` to create a new list by this name.").queue();
            return;
        }

        if (todoList.locked && !todoList.isAuthUser(e.getAuthor())) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` is a locked todo list and you do not have permission to modify it.").queue();
            return;
        }

        for (int i = 3; i < args.length; i++) {
            String content = args[i];
            PreparedStatement addTodoEntry = Database.getInstance().getStatement(ADD_TODO_ENTRY);
            addTodoEntry.setInt(1, todoList.id);
            addTodoEntry.setString(2, content);
            addTodoEntry.setBoolean(3, false);
            if (addTodoEntry.executeUpdate() == 0)
                throw new SQLException(ADD_TODO_ENTRY + " reported no modified rows!");

            todoList.entries.add(new TodoEntry(Database.getAutoIncrement(addTodoEntry, 1), content, false));
            addTodoEntry.clearParameters();

            // e.getChannel().sendMessageAsync(":white_check_mark: Added to `" + label + "` todo list.");
        }
        e.getChannel().sendMessage(":white_check_mark: All entries added successfully to todo list.").queue();
    }

    //alias edit [listname] [index of entry] [content]
    private void handleEdit(MessageReceivedEvent e, String[] args) throws SQLException {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " edit [ListName] [index of entry] [content...]`", e);
        RunBot.checkArgs(args, 3, ":x: No entry was specified. Cannot edit an entry that does not exist" +
                "Usage: `" + getAliases().get(0) + " edit [ListName] [index of entry] [content...]`", e);
        RunBot.checkArgs(args, 4, ":x: No content was specified. Cannot create an empty todo entry!" +
                "Usage: `" + getAliases().get(0) + " edit [ListName] [index of entry] [content...]`", e);

        String label = args[2].toLowerCase();
        String content = StringUtils.join(args, " ", 4, args.length);
        TodoList todoList = todoLists.get(label);
        String todoEntryString = args[3];
        int todoEntryIndex;
        try {
            //We subtract 1 from the provided value because entries are listed from 1 and higher.
            // People don't start counting from 0, so when we display the list of entries, we start from 1.
            // This means that the entry index they enter will actually be 1 greater than the actual entry.
            todoEntryIndex = Integer.parseInt(todoEntryString) - 1;
        } catch (NumberFormatException ex) {
            e.getChannel().sendMessage(":x: The provided value as an index to mark was not a number. Value provided: `" + todoEntryString + "`").queue();
            return;
        }
        if (todoEntryIndex < 0 || todoEntryIndex + 1 > todoList.entries.size()) {
            //We add 1 back to the todoEntry because we subtracted 1 from it above. (Basically, we make it human readable again)
            e.getChannel().sendMessage(":x: The provided index to mark does not exist in this Todo list. Value provided: `" + (todoEntryIndex + 1) + "`").queue();
            return;
        }

        if (todoList == null) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` isn't a known todo list. " +
                                               "Try using `" + getAliases().get(0) + " create " + label + "` to create a new list by this name.").queue();
            return;
        }

        if (todoList.locked && !todoList.isAuthUser(e.getAuthor())) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` is a locked todo list and you do not have permission to modify it.").queue();
            return;
        }

        TodoEntry todoEntry = todoList.entries.get(todoEntryIndex);

        PreparedStatement editTodoEntry = Database.getInstance().getStatement(EDIT_TODO_ENTRY);
        editTodoEntry.setString(1, content);
        editTodoEntry.setInt(2, todoList.id);
        editTodoEntry.setInt(3, todoEntry.id);
        if (editTodoEntry.executeUpdate() == 0)
            throw new SQLException(EDIT_TODO_ENTRY + " reported no modified rows!");

        todoEntry.content = content;

        e.getChannel().sendMessage(":white_check_mark: Editted entry " + (todoEntryIndex + 1) + " in `" + label + "` todo list.").queue();
    }

    //alias check [ListName] [EntryIndex]
    //alias mark [ListName] [EntryIndex]
    //alias uncheck [ListName] [EntryIndex]
    //alias unmark [ListName] [EntryIndex]
    private void handleCheck(MessageReceivedEvent e, String[] args, boolean completed) throws SQLException {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " mark/unmark [ListName] [EntryIndex]`", e);
        RunBot.checkArgs(args, 3, ":x: No todo EntryIndex was specified. Usage: `" + getAliases().get(0) + " mark/unmark [ListName] [EntryIndex]`", e);


        String label = args[2].toLowerCase();
        TodoList todoList = todoLists.get(label);
        if (todoList == null) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` isn't a known todo list.").queue();
            return;
        }

        String[] todoEntryStrings = StringUtils.join(args, " ", 3, args.length).split("\\s+");

        if (todoEntryStrings.length == 1 && todoEntryStrings[0].equals("*")) {
            PreparedStatement setTodoEntryChecked = Database.getInstance().getStatement(SET_TODO_ENTRIES_CHECKED);
            setTodoEntryChecked.setBoolean(1, completed);
            setTodoEntryChecked.setInt(2, todoList.id);
            if (setTodoEntryChecked.executeUpdate() == 0)
                throw new SQLException(SET_TODO_ENTRIES_CHECKED + " reported no updated rows!");

            todoList.entries.forEach(todoEntry -> todoEntry.checked = completed);

            e.getChannel().sendMessage(":white_check_mark: Set all entries in the `" + label + "` todo list to **" + (completed ? "complete**" : "incomplete**")).queue();
        } else if (todoEntryStrings.length == 1) {
            int todoEntryIndex;
            try {
                //We subtract 1 from the provided value because entries are listed from 1 and higher.
                // People don't start counting from 0, so when we display the list of entries, we start from.
                // This means that the entry index they enter will actually be 1 greater than the actual entry.
                todoEntryIndex = Integer.parseInt(todoEntryStrings[0]) - 1;
            } catch (NumberFormatException ex) {
                e.getChannel().sendMessage(":x: The provided value as an index to mark was not a number. Value provided: `" + todoEntryStrings[0] + "`").queue();
                return;
            }

            if (todoEntryIndex < 0 || todoEntryIndex + 1 > todoList.entries.size()) {
                //We add 1 back to the todoEntry because we subtracted 1 from it above. (Basically, we make it human readable again)
                e.getChannel().sendMessage(":x: The provided index to mark does not exist in this Todo list. Value provided: `" + (todoEntryIndex + 1) + "`").queue();
                return;
            }

            TodoEntry todoEntry = todoList.entries.get(todoEntryIndex);
            if (todoEntry.checked != completed) {
                PreparedStatement setTodoEntryChecked = Database.getInstance().getStatement(SET_TODO_ENTRY_CHECKED);
                setTodoEntryChecked.setBoolean(1, completed);
                setTodoEntryChecked.setInt(2, todoEntry.id);
                if (setTodoEntryChecked.executeUpdate() == 0)
                    throw new SQLException(SET_TODO_ENTRY_CHECKED + " reported no updated rows!");

                todoEntry.checked = completed;
            }

            e.getChannel()
             .sendMessage(":white_check_mark: Item `" + (todoEntryIndex + 1) + "` in `" + label + "` was marked as **" + (completed ? "completed**" : "incomplete**")).queue();
        } else {
            ArrayList<Integer> todoEntryIndexs = new ArrayList<>();
            //We subtract 1 from the provided value because entries are listed from 1 and higher.
            // People don't start counting from 0, so when we display the list of entries, we start from.
            // This means that the entry index they enter will actually be 1 greater than the actual entry.
            Arrays.asList(todoEntryStrings).forEach(ts -> todoEntryIndexs.add(Integer.parseInt(ts) - 1));

            for (Integer todoEntryIndex : todoEntryIndexs) {
                if (todoEntryIndex < 0 || todoEntryIndex + 1 > todoList.entries.size()) {
                    //We add 1 back to the todoEntry because we subtracted 1 from it above. (Basically, we make it human readable again)
                    e.getChannel().sendMessage(":x: The provided index to mark does not exist in this Todo list. Value provided: `" + (todoEntryIndex + 1) + "`").queue();
                    return;
                }
            }

            for (Integer todoEntryIndex : todoEntryIndexs) {
                TodoEntry todoEntry = todoList.entries.get(todoEntryIndex);
                if (todoEntry.checked != completed) {
                    PreparedStatement setTodoEntryChecked = Database.getInstance().getStatement(SET_TODO_ENTRY_CHECKED);
                    setTodoEntryChecked.setBoolean(1, completed);
                    setTodoEntryChecked.setInt(2, todoEntry.id);
                    if (setTodoEntryChecked.executeUpdate() == 0)
                        throw new SQLException(SET_TODO_ENTRY_CHECKED + " reported no updated rows!");

                    todoEntry.checked = completed;
                }
            }

            e.getChannel()
             .sendMessage(":white_check_mark: Items `" + todoEntryIndexs.toString() + "` in `" + label + "` was marked as **" + (completed ? "completed**" :
                     "incomplete**")).queue();
        }
    }

    //alias lock [ListName]
    private void handleLock(MessageReceivedEvent e, String[] args, boolean locked) throws SQLException {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " lock/unlock [ListName]`", e);

        String label = args[2].toLowerCase();
        TodoList todoList = todoLists.get(label);
        if (todoList == null) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` isn't a known todo list.").queue();
            return;
        }

        if (!todoList.isAuthUser(e.getAuthor())) {
            e.getChannel().sendMessage(":x: Sorry, you do not have permission to lock or unlock the `" + label + "` todo list.").queue();
            return;
        }

        PreparedStatement setTodoListLocked = Database.getInstance().getStatement(SET_TODO_LIST_LOCKED);
        setTodoListLocked.setBoolean(1, locked);
        setTodoListLocked.setInt(2, todoList.id);
        if (setTodoListLocked.executeUpdate() == 0)
            throw new SQLException(SET_TODO_LIST_LOCKED + " reported no updated rows!");
        setTodoListLocked.clearParameters();

        todoList.locked = locked;
        e.getChannel().sendMessage(":white_check_mark: The `" + label + "` todo list was `" + (locked ? "locked`" : "unlocked`")).queue();
    }

    //alias users add [ListName] @mention @mention ...
    //alias users remove [ListName] @mention @mention ...
    //alias users list [ListName]
    private void handleUsers(MessageReceivedEvent e, String[] args) throws SQLException {
        RunBot.checkArgs(args, 2, ":x: No SubAction was specified. Usage: `" + getAliases().get(0) + " users [SubAction] [ListName]`", e);
        RunBot.checkArgs(args, 3, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " users [SubAction] [ListName]`", e);

        String action = args[2].toLowerCase();
        String label = args[3].toLowerCase();
        TodoList todoList = todoLists.get(label);
        if (todoList == null) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` isn't a known todo list.").queue();
            return;
        }

        switch (action) {
            case "add": {
                if (!todoList.ownerId.equals(e.getAuthor().getId())) {
                    e.getChannel().sendMessage(":x: Sorry, but only the Owner of a list has permission add users to a todo list.").queue();
                    return;
                }

                if (e.getMessage().getMentionedUsers().size() == 0) {
                    e.getChannel().sendMessage(":x: No users were specified to add to the `" + label + "` todo list.").queue();
                    return;
                }

                int addedUsers = 0;
                PreparedStatement addTodoUser = Database.getInstance().getStatement(ADD_TODO_USER);
                for (User u : e.getMessage().getMentionedUsers()) {
                    if (!todoList.isAuthUser(u)) {
                        addTodoUser.setInt(1, todoList.id);
                        addTodoUser.setString(2, u.getId());
                        if (addTodoUser.executeUpdate() == 0)
                            throw new SQLException(ADD_TODO_LIST + " reported no updated rows!");
                        addTodoUser.clearParameters();

                        todoList.allowedUsers.add(u.getId());
                        addedUsers++;
                    }
                }

                e.getChannel().sendMessage(":white_check_mark: Added **" + addedUsers + "** users to the `" + label + "` todo list.").queue();
                break;
            }
            case "remove": {
                if (!todoList.ownerId.equals(e.getAuthor().getId())) {
                    e.getChannel().sendMessage(":x: Sorry, but only the Owner of a list has permission remove users from a todo list.").queue();
                    return;
                }

                if (e.getMessage().getMentionedUsers().size() == 0) {
                    e.getChannel().sendMessage(":x: No users were specified to add to the `" + label + "` todo list.").queue();
                    return;
                }

                int removedUsers = 0;
                PreparedStatement removeTodoUser = Database.getInstance().getStatement(REMOVE_TODO_USER);
                for (User u : e.getMessage().getMentionedUsers()) {
                    if (todoList.allowedUsers.stream().anyMatch(id -> u.getId().equals(id))) {
                        removeTodoUser.setInt(1, todoList.id);
                        removeTodoUser.setString(2, u.getId());
                        if (removeTodoUser.executeUpdate() == 0)
                            throw new SQLException(REMOVE_TODO_USER + " reported no updated rows!");
                        removeTodoUser.clearParameters();

                        todoList.allowedUsers.remove(u.getId());
                        removedUsers++;
                    }
                }

                e.getChannel().sendMessage(":white_check_mark: Removed **" + removedUsers + "** users from the `" + label + "` todo list.").queue();
                break;
            }
            case "list": {
                MessageBuilder builder = new MessageBuilder();
                builder.appendString("Owner of `" + label + "`\n");
                User owner = RunBot.API.getUserById(todoList.ownerId);
                if (owner != null)
                    builder.appendString(" - " + owner.getName());
                else
                    builder.appendString(" - Unknown User ID: " + todoList.ownerId);
                builder.appendString("\n");
                builder.appendString("Other Auth'd Users\n");

                for (String id : todoList.allowedUsers) {
                    User u = RunBot.API.getUserById(id);
                    if (u != null)
                        builder.appendString(" - " + u.getName());
                    else
                        builder.appendString(" - Unknown User ID: " + id);
                    builder.appendString("\n");
                }
                if (todoList.allowedUsers.isEmpty())
                    builder.appendString(" - None.");
                e.getChannel().sendMessage(builder.build()).queue();
                break;
            }
            default: {
                e.getChannel().sendMessage(":x: Sorry, the provided sub-action argument for the `users` action is not recognized. " +
                                                   "Provided argument: `" + action + "`").queue();
            }
        }
    }

    //alias clear [ListName]
    public void handleClear(MessageReceivedEvent e, String[] args) throws SQLException {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " clear [ListName]`", e);

        String label = args[2];
        TodoList todoList = todoLists.get(label);
        if (todoList == null) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` isn't a known todo list.").queue();
            return;
        }

        if (todoList.locked && !todoList.isAuthUser(e.getAuthor())) {
            e.getChannel().sendMessage(":x: Sorry, the `" + label + "` todo list is locked and you do not have permission to modify it.").queue();
            return;
        }

        int clearedEntries = 0;
        PreparedStatement removeTodoEntry = Database.getInstance().getStatement(REMOVE_TODO_ENTRY);
        for (Iterator<TodoEntry> it = todoList.entries.iterator(); it.hasNext(); ) {
            TodoEntry todoEntry = it.next();
            if (todoEntry.checked) {
                removeTodoEntry.setInt(1, todoEntry.id);
                if (removeTodoEntry.executeUpdate() == 0)
                    throw new SQLException(REMOVE_TODO_ENTRY + " reported no updated rows!");
                removeTodoEntry.clearParameters();

                it.remove();
                clearedEntries++;
            }
        }
        e.getChannel().sendMessage(":white_check_mark: Cleared **" + clearedEntries + "** completed entries from the `" + label + "` todo list.").queue();
    }

    //alias remove [ListName]
    public void handleRemove(MessageReceivedEvent e, String[] args) throws SQLException {
        RunBot.checkArgs(args, 2, ":x: No todo ListName was specified. Usage: `" + getAliases().get(0) + " remove [ListName]`", e);

        String label = args[2].toLowerCase();
        TodoList todoList = todoLists.get(label);
        if (todoList == null) {
            e.getChannel().sendMessage(":x: Sorry, `" + label + "` isn't a known todo list.").queue();
            allowRemove = false;
            listToRemove = "";
            return;
        }

        if (todoList.locked && !todoList.isAuthUser(e.getAuthor())) {
            e.getChannel().sendMessage(":x: Sorry, the `" + label + "` todo list is locked and you do not have permission to modify it.").queue();
            allowRemove = false;
            listToRemove = "";
            return;
        }

        if (!allowRemove) {
            e.getChannel()
             .sendMessage(":name_badge: :name_badge: :name_badge: Are you sure you want to permanently delete the TodoList `" + label + "` ? If so, run the command again. " +
                                  ":name_badge: :name_badge: :name_badge:").queue();
            allowRemove = true;
            listToRemove = label;
            return;
        }

        if (!listToRemove.equals(label)) {
            e.getChannel()
             .sendMessage(":name_badge: :name_badge: :name_badge: Be careful!! you almost permanently deleted the TodoList `" + label + "` by accident! :name_badge: " +
                                  ":name_badge: :name_badge:").queue();
            allowRemove = false;
            listToRemove = "";
            return;
        }

        PreparedStatement removeTodoList = Database.getInstance().getStatement(REMOVE_TODO_LIST);
        removeTodoList.setInt(1, todoList.id);
        if (removeTodoList.executeUpdate() == 0)
            throw new SQLException(REMOVE_TODO_LIST + " reported no updated rows!");
        removeTodoList.clearParameters();

        todoLists.remove(label);
        e.getChannel().sendMessage(":white_check_mark: Deleted the `" + label + "` todo list.").queue();
        allowRemove = false;
    }

    private static class TodoList {
        int id;
        String labelName;
        String ownerId;
        boolean locked;
        List<TodoEntry> entries;
        List<String> allowedUsers;

        TodoList(int id, String labelName, String ownerId, boolean locked) {
            this.id = id;
            this.labelName = labelName;
            this.ownerId = ownerId;
            this.locked = locked;
            this.entries = new ArrayList<>();
            this.allowedUsers = new ArrayList<>();
        }

        public boolean isAuthUser(User user) {
            return ownerId.equals(user.getId()) || allowedUsers.stream().anyMatch(id -> id.equals(user.getId()));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TodoList))
                return false;

            TodoList tl = (TodoList) o;
            return tl.id == this.id && tl.labelName.equals(this.labelName) && tl.locked == this.locked;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public String toString() {
            return "TodoLabel: Id: " + id + " Name: " + labelName + " Size: " + entries.size() + " Locked: " + locked;
        }
    }

    private static class TodoEntry {
        int id;
        String content;
        boolean checked;

        TodoEntry(int id, String content, boolean checked) {
            this.id = id;
            this.content = content;
            this.checked = checked;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TodoEntry))
                return false;

            TodoEntry te = (TodoEntry) o;
            return te.id == this.id && te.content.equals(this.content) && te.checked == this.checked;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public String toString() {
            return "TodoEntry: Id: " + id + " Checked: " + checked + " Content: " + content;
        }
    }
}