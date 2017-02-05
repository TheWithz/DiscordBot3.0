package bot;

import commands.*;
import commands.util.Permissions;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RunBot {
    public static final String PREFIX = "$$$";
    public static JDA API = null;
    public static User BOT = null;
    private static final String OP_REQUIRED = ":x: Sorry, this command is OP only!";
    public static String OWNER_REQUIRED = null;

    public RunBot() {
        try {
            JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get("Config.json"))));
            API = new JDABuilder(AccountType.BOT).setToken(obj.getString("releaseBotToken"))
                                                 //.addListener(new GitHandler(obj.getString("gitApiToken")))
                                                 .addListener(new LoginHandler())
                                                 .addListener(new TranslateCommand())
                                                 .addListener(new CalculatorCommand())
                                                 .addListener(new SearchCommand())
                                                 .addListener(new PermissionsCommand())
                                                 .addListener(new TodoCommand())
                                                 .addListener(new RandomNumberCommand())
                                                 .addListener(new RandomFactCommand())
                                                 .addListener(new BashCommand())
                                                 .addListener(new EvalCommand())
                                                 .addListener(new JsonCommand())
                                                 .addListener(new TagCommand())
                                                 .setBulkDeleteSplittingEnabled(false)
                                                 .buildAsync();
        } catch (RateLimitedException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.out.println("The config was not populated. Please provide a token.");
        } catch (LoginException e) {
            e.printStackTrace();
            System.out.println("The provided botToken was incorrect. Please provide valid details.");
        } /*catch (InterruptedException e) {
            e.printStackTrace();
        }*/ catch (JSONException e) {
            System.err.println("Encountered a JSON error. Most likely caused due to an outdated or ill-formatted config.\n" +
                                       "Please delete the config so that it can be regenerated. JSON Error:\n");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            JSONObject obj = new JSONObject();
            obj.put("releaseBotToken", "");
            try {
                Files.write(Paths.get("Config.json"), obj.toString(4).getBytes());
                System.out.println("No config file was found. Config.json has been generated, please populate it!");
            } catch (IOException e1) {
                System.out.println("No config file was found and we failed to generate one.");
                e1.printStackTrace();
            }
        }
    }

    public static void checkArgs(String[] args, int index, String failMessage, MessageReceivedEvent e) {
        if (args.length < (index + 1)) {
            e.getChannel().sendMessage(failMessage).queue();
            throw new IllegalArgumentException(failMessage);
        }
    }

    public static void checkArgs(String[] args, int index, String failMessage, TextChannel tc) {
        if (args.length < (index + 1)) {
            tc.sendMessage(failMessage).queue();
            throw new IllegalArgumentException(failMessage);
        }
    }

    public static boolean OpRequired(MessageReceivedEvent e) {
        System.out.println(Permissions.getPermissions() == null);
        if (!Permissions.getPermissions().isOp(e.getAuthor())) {
            e.getChannel().sendMessage(RunBot.OP_REQUIRED).queue();
            return true;
        }
        return false;
    }

    public static boolean OwnerRequired(MessageReceivedEvent e) {
        if (!e.getAuthor().getId().equals("122764399961309184")) {
            e.getChannel().sendMessage(RunBot.OWNER_REQUIRED).queue();
            return true;
        }
        return false;
    }

    public static boolean printAsFile(TextChannel channel, StringBuilder b, String fileName) {
        channel.sendTyping();
        commands.BashCommand.runLinuxCommand("touch " + fileName + ".txt");
        File file = new File(fileName + ".txt");
        Path path = Paths.get(fileName + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(b.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            channel.sendFile(file, new MessageBuilder().appendCodeBlock(fileName, "java").build()).queue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.delete();
    }

    public static void main(String[] args) {
        RunBot bot = new RunBot();
    }

}
