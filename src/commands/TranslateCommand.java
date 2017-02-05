package commands;

import bot.RunBot;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by TheWithz on 2/15/16.
 */
public class TranslateCommand extends Command {
    private final String MICROSOFT_CLIENT_ID = "DiscordBotForTheWithz";
    private final String MICROSOFT_CLIENT_SECRET = "SDy+DFjPKIzmwkC59aA1E4tyIoTn4nAoWKhCEEfOksk=";

    @Override
    public void onCommand(MessageReceivedEvent e, String[] args) {
        if (RunBot.OpRequired(e))
            return;
        generateTranslatedText(e, args);
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList(RunBot.PREFIX + "tran", RunBot.PREFIX + "translate");
    }

    @Override
    public String getDescription() {
        return "Command that translates a message!";
    }

    @Override
    public String getName() {
        return "Translate Command";
    }

    private void generateTranslatedText(MessageReceivedEvent event, String[] commandArguments) {
        RunBot.checkArgs(commandArguments, 1, ":x: No language was specified to translate from.", event);
        RunBot.checkArgs(commandArguments, 2, ":x: No language was specified to translate to.", event);
        RunBot.checkArgs(commandArguments, 3, ":x: No Content was specified to translate.", event);

        //Set your Windows Azure Marketplace client info - See http:msdn.microsoft.com/en-us/library/hh454950.aspx
        Translate.setClientId(MICROSOFT_CLIENT_ID);
        Translate.setClientSecret(MICROSOFT_CLIENT_SECRET);
        String translatedText = null;
        try {
            translatedText = Translate.execute(StringUtils.join(commandArguments, " ", 3, commandArguments.length), Language.valueOf(commandArguments[1].toUpperCase()), Language
                    .valueOf(commandArguments[2].toUpperCase()));
        } catch (Exception e) {
            event.getChannel().sendMessage(":x: " + e.getMessage()).queue();
        }
        if (translatedText != null)
            event.getChannel().sendMessage(":white_check_mark: `" + translatedText + "`").queue();
    }

}
