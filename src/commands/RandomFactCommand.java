package commands;

import bot.RunBot;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Created by TheWithz on 2/21/16.
 */
public class RandomFactCommand extends Command {

    @Override
    public void onCommand(MessageReceivedEvent e, String[] args) {
        generateRandomFact(e);
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList(RunBot.PREFIX + "fact");
    }

    @Override
    public String getDescription() {
        return "Command that generates a random fact!";
    }

    @Override
    public String getName() {
        return "Random Fact Command";
    }

    private void generateRandomFact(MessageReceivedEvent e) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("randomFacts.txt"));
        } catch (FileNotFoundException m) {
            m.printStackTrace();
        }
        ArrayList<String> a = new ArrayList<>();
        if (scanner != null) {
            while (scanner.hasNextLine()) {
                a.add(scanner.nextLine());
            }
            int rnum = (int) (a.size() * Math.random());
            e.getChannel().sendMessage(a.get(rnum)).queue();
            scanner.close();
        }
    }

}

