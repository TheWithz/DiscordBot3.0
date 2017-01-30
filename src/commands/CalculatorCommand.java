package commands;

import bot.RunBot;
import commands.calculator.EvalPostfix;
import commands.calculator.InfixToPostfix;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Created by TheWithz on 2/15/16.
 */
public class CalculatorCommand extends Command {

    @Override
    public void onCommand(MessageReceivedEvent e, String[] args) {
        e.getChannel().sendMessage(calculate(args)).queue();
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList(RunBot.PREFIX + "calc", RunBot.PREFIX + "calculate");
    }

    @Override
    public String getDescription() {
        return "Calculates a given expression.";
    }

    @Override
    public String getName() {
        return "Calculator";
    }

    private String calculate(String[] commandArguments) {
        StringBuilder inFixExpression = new StringBuilder();
        for (int i = 1; i < commandArguments.length; i++) {
            inFixExpression.append(commandArguments[i]);
            inFixExpression.append(" ");
        }
        return ":white_check_mark: The answer to your expression is: "
                + EvalPostfix.evalPostFix(InfixToPostfix.convertToPostfix(inFixExpression.toString()));
    }

}
