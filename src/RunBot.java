package bots;

import events.commands.*;
import events.commands.generator.*;
import events.commands.music.*;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
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

    public RunBot() {
        try {
            JSONObject configObj = new JSONObject(new String(Files.readAllBytes(Paths.get("Config.json"))));

            API = new JDABuilder(AccountType.BOT).setToken(configObj.getString("releaseBotToken"))
                                                 .setBulkDeleteSplittingEnabled(false)
                                                 .buildAsync();
        } catch (RateLimitedException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("The config was not populated. Please provide a token.");
        } catch (LoginException e) {
            System.out.println("The provided botToken was incorrect. Please provide valid details.");
        } /*catch (InterruptedException e) {
            e.printStackTrace();
        }*/ catch (JSONException e) {
            System.err.println("Encountered a JSON error. Most likely caused due to an outdated or ill-formatted config.\n" +
                                       "Please delete the config so that it can be regenerated. JSON Error:\n");
            e.printStackTrace();
        } catch (IOException e) {
            JSONObject obj = new JSONObject();
            obj.put("botToken", "");
            try {
                Files.write(Paths.get("Config.json"), obj.toString(4).getBytes());
                System.out.println("No config file was found. Config.json has been generated, please populate it!");
            } catch (IOException e1) {
                System.out.println("No config file was found and we failed to generate one.");
                e1.printStackTrace();
            }
        }
    }

    public static boolean printAsFile(TextChannel channel, StringBuilder b, String fileName) {
        channel.sendTyping();
        BashCommand.runLinuxCommand("touch " + fileName + ".txt");
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
