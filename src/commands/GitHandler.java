package commands;

//import com.jcabi.github.Coordinates;
//import com.jcabi.github.Github;
//import com.jcabi.github.Repo;
//import com.jcabi.github.RtGithub;

import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.io.IOException;
import java.util.*;

/**
 * Created by thewithz on 7/6/16.
 */
public class GitHandler extends ListenerAdapter {
    // private static Github github;
    private static Timer timer = new Timer();
    private static Date lastCommit;
    // private static Repo discordRepo;
    private static Map<String, String> commits;

    public GitHandler(String gitApiToken) {
        commits = new HashMap<>();
        // github = new RtGithub(gitApiToken);
        //  discordRepo = github.repos().get(new Coordinates.Simple("TheWithz", "DiscordBot3.0"));
//            lastCommit =
        //   discordRepo.commits().iterate(commits).forEach(System.out::println);

    }

    /*
    DV8FromTheWorld/JDA (development)
aa9fd8b Fixed issues when JDA received a create event for a new Private channel. [Austin Keener]
     */

    public static void startTimer() {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    throw new IOException();
                    // discordRepo = github.getMyself().getRepository("DiscordBot3.0");
                    // checkCommit();
                    //  checkPullRequest();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 10 * 1000);
    }

    private static void checkPullRequest() {

    }

//    private static void checkCommit() throws IOException {
//        if (!discordRepo.getPushedAt().equals(lastCommit)) {
//            GHCommit commit = discordRepo.listCommits().asList().get(0);
//            GHBranch branch = discordRepo.getBranches().get(discordRepo.getDefaultBranch());
//            for (String br : discordRepo.getBranches().keySet()) {
//                if (discordRepo.getBranches().get(br).getSHA1().equals(commit.getSHA1())) {
//                    branch = discordRepo.getBranches().get(br);
//                }
//            }
//            TextChannel textChannel = RunBot.API.getTextChannelById("147169039049949184");
//            textChannel.sendMessage(String.format("***%1$s*** / **%2$s** (%3$s) <%4$s>\n`%5$s` %6$s [%7$s]",
//                                                  commit.getAuthor().getLogin(),
//                                                  discordRepo.getName(),
//                                                  branch.getName(),
//                                                  discordRepo.getHtmlUrl(),
//                                                  commit.getSHA1(),
//                                                  commit.getCommitShortInfo().getMessage(),
//                                                  github.getMyself().getName())).queue();
//            lastCommit = discordRepo.getPushedAt();
//        }
//    }

}
