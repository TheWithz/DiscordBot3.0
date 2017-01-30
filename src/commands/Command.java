package commands;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;

/**
 * Created by TheWithz on 2/21/16.
 */
public abstract class Command extends ListenerAdapter {

    public abstract void onCommand(MessageReceivedEvent e, String[] args);

    public abstract List<String> getAliases();

    public abstract String getDescription();

    public abstract String getName();

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (e.getMessage().getContent().length() > 0 && containsCommand(e.getMessage())) {
            if (e.isFromType(ChannelType.PRIVATE)) {
                onCommand(e, commandArgs(e.getMessage()));
                return;
            }
            onCommand(e, commandArgs(e.getMessage()));
        }
    }

    private boolean containsCommand(Message message) {
        return getAliases().contains(commandArgs(message)[0].toLowerCase());
    }

    private String[] commandArgs(Message message) {
        return commandArgs(message.getContent());
    }

    private String[] commandArgs(String string) {
        commands.ArgParse parser = new commands.ArgParse();
        return parser.parse(string);
    }

}