package me.doppey.tjbot.events.utility;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;
import org.bson.Document;

public class StagingAreaEvent extends ListenerAdapter {
    private final MongoCollection<Document> botSuspicionCollection;
    private GuildController gc = null;
    private Role stagingRole = null;

    public StagingAreaEvent(MongoDatabase database) {
        this.botSuspicionCollection = database.getCollection("botSuspicion");
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (gc == null) {
            this.gc = new GuildController(event.getGuild());
        }

        if (stagingRole == null) {
            stagingRole = gc.getGuild().getRolesByName("stagingrole", true).get(0);
        }

        giveMemberStagingRole(event);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getGuild().getId().equalsIgnoreCase("272761734820003841")) {
            if (gc == null) {
                this.gc = new GuildController(event.getGuild());
            }

            if (stagingRole == null) {
                stagingRole = gc.getGuild().getRolesByName("stagingrole", true).get(0);
            }

            final boolean isWelcomeChannel = event.getChannel().getId().equalsIgnoreCase("513551097449807883");
            final boolean hasAcceptedRules = event.getMessage().getContentRaw().equalsIgnoreCase("accept");

            if (isWelcomeChannel) {
                if (hasAcceptedRules) {
                    removeStagingRoleFromUser(event);
                } else {
                    User user = event.getAuthor();
                    event.getGuild().getTextChannelById("546416238922956845").sendMessage(new EmbedBuilder()
                            .setTitle("Message Sent in Welcome")
                            .addField("Message", event.getMessage().getContentRaw(), true)
                            .addField("User", user.getAsMention() + " (`" + user.getId() + "`)", true)
                            .build()).queue(messageSent -> {
                        messageSent.addReaction("\uD83D\uDD25").queue(reactionAdded -> {
                            Document report = new Document();
                            report.put("messageId", messageSent.getId());
                            report.put("userId", user.getId());

                            botSuspicionCollection.insertOne(report);
                        });
                    });
                }
                event.getMessage().delete().queue();
            }
        }
    }

    private void giveMemberStagingRole(GuildMemberJoinEvent event) {
        gc.addSingleRoleToMember(event.getMember(), gc.getGuild().getRolesByName("stagingrole", true).get(0)).queue();
    }

    private void removeStagingRoleFromUser(GuildMessageReceivedEvent event) {
        gc.removeSingleRoleFromMember(event.getMember(), event.getMember().getRoles().get(0)).queue();
    }
}
