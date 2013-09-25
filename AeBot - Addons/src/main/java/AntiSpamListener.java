/*
 * Copyright (C) 2013 Lord_Ralex
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import net.ae97.aebot.AeBot;
import net.ae97.aebot.api.EventType;
import net.ae97.aebot.api.Listener;
import net.ae97.aebot.api.Priority;
import net.ae97.aebot.api.channels.Channel;
import net.ae97.aebot.api.events.ActionEvent;
import net.ae97.aebot.api.events.MessageEvent;
import net.ae97.aebot.api.users.BotUser;
import net.ae97.aebot.api.users.User;
import net.ae97.aebot.settings.Settings;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AntiSpamListener implements Listener {

    private final Map<String, Posts> logs = new HashMap<>();
    private final int MAX_MESSAGES;
    private final int SPAM_RATE;
    private final int DUPE_RATE;
    private final List<String> channels = new ArrayList<>();

    public AntiSpamListener() {
        Settings settings = new Settings(new File("settings", "antispam.yml"));
        MAX_MESSAGES = settings.getInt("message-count");
        SPAM_RATE = settings.getInt("spam-rate");
        DUPE_RATE = settings.getInt("dupe-rate");
        channels.clear();
        channels.addAll(settings.getStringList("channels"));
        logs.clear();
    }

    @EventType(priority = Priority.LOW)
    public void runEvent(MessageEvent event) {
        synchronized (logs) {
            Channel channel = event.getChannel();
            if (!channels.contains(channel.getName().toLowerCase())) {
                return;
            }
            User sender = event.getUser();
            String message = event.getMessage();
            if (sender.hasOP(channel.getName()) || sender.hasVoice(channel.getName()) || sender.getNick().equalsIgnoreCase(BotUser.getBotUser().getNick()) || sender.hasPermission(channel.getName(), "antispam.ignore")) {
                return;
            }
            message = message.toString().toLowerCase();
            Posts posts = logs.remove(sender.getNick());
            if (posts == null) {
                posts = new Posts();
            }
            if (posts.addPost(message)) {
                if (AeBot.getDebugMode()) {
                    BotUser.getBotUser().sendMessage(Settings.getGlobalSettings().getString("debug-channel"),
                            "Would have kicked " + event.getUser().getNick() + " with last line of " + posts.posts.get(posts.posts.size() - 1));
                } else {
                    BotUser.getBotUser().kick(sender.getNick(), channel.getName(), "Triggered Spam Guard (IP=" + sender.getIP() + ")");
                }
                event.setCancelled(true);
            } else {
                logs.put(sender.getNick(), posts);
            }
        }
    }

    @EventType(priority = Priority.LOW)
    public void runEvent(ActionEvent event) {
        synchronized (logs) {
            if (event.isCancelled()) {
                return;
            }
            Channel channel = event.getChannel();
            User sender = event.getUser();
            String message = event.getAction();
            if (sender.hasOP(channel.getName()) || sender.hasVoice(channel.getName()) || sender.getNick().equalsIgnoreCase(BotUser.getBotUser().getNick())) {
                return;
            }
            message = message.toString().toLowerCase();
            Posts posts = logs.remove(sender.getNick());
            if (posts == null) {
                posts = new Posts();
            }
            if (posts.addPost(message)) {
                if (AeBot.getDebugMode()) {
                    BotUser.getBotUser().sendMessage(Settings.getGlobalSettings().getString("debug-channel"),
                            "Would have kicked " + event.getUser().getNick() + " with last line of " + posts.posts.get(posts.posts.size() - 1));
                } else {
                    BotUser.getBotUser().kick(sender.getNick(), channel.getName(), "Triggered Spam Guard (IP=" + sender.getIP() + ")");
                }
                event.setCancelled(true);
            } else {
                logs.put(sender.getNick(), posts);
            }
        }
    }

    private class Posts {

        List<Post> posts = new ArrayList<>();

        public boolean addPost(String lastPost) {
            posts.add(new Post(System.currentTimeMillis(), lastPost));
            if (posts.size() == MAX_MESSAGES) {
                boolean areSame = true;
                for (int i = 1; i < posts.size() && areSame; i++) {
                    if (!posts.get(i - 1).message.equalsIgnoreCase(posts.get(i).message)) {
                        areSame = false;
                    }
                }
                if (areSame) {
                    if (posts.get(posts.size() - 1).getTime() - posts.get(0).getTime() < DUPE_RATE) {
                        return true;
                    }
                }
                if (posts.get(posts.size() - 1).getTime() - posts.get(0).getTime() < SPAM_RATE) {
                    return true;
                }
                posts.remove(0);
            }
            return false;
        }
    }

    private class Post {

        long timePosted;
        String message;

        public Post(long Time, String Message) {
            timePosted = Time;
            message = Message;
        }

        public String getMessage() {
            return message;
        }

        public long getTime() {
            return timePosted;
        }
    }
}