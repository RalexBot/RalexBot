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
import net.ae97.aebot.api.events.ActionEvent;
import net.ae97.aebot.api.events.JoinEvent;
import net.ae97.aebot.api.events.MessageEvent;
import net.ae97.aebot.api.events.NickChangeEvent;
import net.ae97.aebot.api.users.BotUser;
import net.ae97.aebot.settings.Settings;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Joshua
 */
public class CensorListener implements Listener {

    private final List<String> censor = new ArrayList<>();
    private final List<String> channels = new ArrayList<>();
    private final Set<String> warned = new HashSet<>();
    private final Settings settings;

    public CensorListener() {
        settings = new Settings(new File("settings", "censor.yml"));
        censor.clear();
        censor.addAll(settings.getStringList("words"));
        channels.clear();
        channels.addAll(settings.getStringList("channels"));
    }

    @EventType
    public void runEvent(MessageEvent event) {
        if (!channels.contains(event.getChannel().getName().toLowerCase())) {
            return;
        }
        if (event.getUser().getNick().equalsIgnoreCase(BotUser.getBotUser().getNick())) {
            return;
        }
        if (event.getUser().hasOP(event.getChannel().getName())) {
            return;
        }
        if (event.getUser().hasPermission(event.getChannel().getName(), "censor.ignore")) {
            return;
        }
        String message = event.getMessage().toLowerCase();
        if (scanMessage(message)) {
            if (!AeBot.getDebugMode()) {
                if (warned.contains(event.getUser().getNick()) || warned.contains(event.getUser().getIP())) {
                    BotUser.getBotUser().kick(event.getUser().getNick(), event.getChannel().getName(), "Please keep it civil");
                } else {
                    warned.add(event.getUser().getNick());
                    warned.add(event.getUser().getIP());
                    event.getChannel().sendMessage("Please keep it civil, " + event.getUser().getNick());
                }
            }
            if (AeBot.getDebugMode()) {
                BotUser.getBotUser().sendMessage(Settings.getGlobalSettings().getString("debug-channel"), event.getUser().getNick() + " triggered the censor with his line: " + event.getMessage());
            }
        }
    }

    @EventType
    public void runEvent(ActionEvent event) {
        if (!channels.contains(event.getChannel().getName().toLowerCase())) {
            return;
        }
        if (event.getUser().getNick().equalsIgnoreCase(BotUser.getBotUser().getNick())) {
            return;
        }
        String message = event.getAction().toLowerCase();
        if (scanMessage(message)) {
            if (!AeBot.getDebugMode()) {
                if (warned.contains(event.getUser().getNick()) || warned.contains(event.getUser().getIP())) {
                    BotUser.getBotUser().kick(event.getUser().getNick(), event.getChannel().getName(), "Please keep it civil");
                } else {
                    warned.add(event.getUser().getNick());
                    warned.add(event.getUser().getIP());
                    event.getChannel().sendMessage("Please keep it civil, " + event.getUser().getNick());
                }
            }
            if (AeBot.getDebugMode()) {
                BotUser.getBotUser().sendMessage(Settings.getGlobalSettings().getString("debug-channel"), event.getUser().getNick() + " triggered the censor with his line: " + event.getAction());
            }
        }
    }

    @EventType
    public void runEvent(NickChangeEvent event) {
        String message = event.getNewNick().toLowerCase();
        if (scanMessage(message)) {
            for (String chan : BotUser.getBotUser().getChannels()) {
                if (!AeBot.getDebugMode()) {
                    BotUser.getBotUser().kick(event.getNewNick(), chan, "Please keep it civil");
                }
            }
        }
    }

    @EventType
    public void runEvent(JoinEvent event) {
        if (!channels.contains(event.getChannel().getName().toLowerCase())) {
            return;
        }
        String message = event.getUser().getNick().toLowerCase();
        if (scanMessage(message)) {
            if (!AeBot.getDebugMode()) {
                BotUser.getBotUser().kick(event.getUser().getNick(), event.getChannel().getName(), "Please keep it civil");
            }
        }
    }

    private boolean scanMessage(String message) {
        for (String word : censor) {
            String[] parts = message.split(" ");
            for (String p : parts) {
                if (p.equalsIgnoreCase(word)) {
                    return true;
                }
            }
        }
        return false;
    }
}