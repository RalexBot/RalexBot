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

import com.lordralex.ralexbot.RalexBot;
import com.lordralex.ralexbot.api.EventField;
import com.lordralex.ralexbot.api.EventType;
import com.lordralex.ralexbot.api.Listener;
import com.lordralex.ralexbot.api.events.CommandEvent;
import com.lordralex.ralexbot.api.events.JoinEvent;
import com.lordralex.ralexbot.api.events.PartEvent;
import com.lordralex.ralexbot.api.sender.Sender;
import com.lordralex.ralexbot.api.users.BotUser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.pircbotx.Colors;

/**
 * @author Lord_Ralex
 * @version 1.0
 */
public class VoxelHeadCommands extends Listener {

    private final File db = new File("voxelhead", "vh.db");
    private static final URL dbLink;
    private final Map<String, String[]> index = new HashMap<>();

    static {
        URL temp = null;
        try {
            temp = new URL("http://home.ghoti.me:8080/~faqbot/faqdatabase");
        } catch (MalformedURLException ex) {
            RalexBot.getLogger().log(Level.SEVERE, "An error happened", ex);
        }
        dbLink = temp;
    }

    @Override
    public void setup() {
        index.clear();
        db.getParentFile().mkdirs();
        db.delete();
        try {
            InputStream reader = dbLink.openStream();
            FileOutputStream writer = new FileOutputStream(db);
            copyInputStream(reader, writer);
            BufferedReader filereader = new BufferedReader(new FileReader(db));
            String line;
            while ((line = filereader.readLine()) != null) {
                String key = line.split("\\|")[0];
                String value = line.split("\\|", 2)[1];
                index.put(key.toLowerCase(), value.split(";;"));
            }
        } catch (IOException ex) {
            RalexBot.getLogger().log(Level.SEVERE, "There was an error", ex);
        }
    }

    @Override
    @EventType(event = EventField.Command)
    public void runEvent(CommandEvent event) {
        if (event.getCommand().equalsIgnoreCase("refresh")) {
            setup();
            event.getSender().sendMessage("Updated local storage");
            return;
        } else {
            Sender target = event.getChannel();
            if (target == null) {
                target = event.getSender();
                if (target == null) {
                    return;
                }
            }
            boolean allowExec = true;
            List<String> users = event.getChannel().getUsers();
            if (users.contains("VoxelHead")) {
                allowExec = false;
            }
            if (!allowExec) {
                return;
            }
            if (event.getArgs().length == 1) {
                String[] lines = index.get(event.getArgs()[0].toLowerCase());
                if (lines == null || lines.length == 0) {
                    event.getSender().sendNotice("No key called " + event.getArgs()[0].toLowerCase());
                    return;
                }
                for (String line : lines) {
                    if (event.getCommand().equals(">") || event.getCommand().isEmpty()) {
                        target.sendMessage(Colors.BOLD + event.getArgs()[0].toLowerCase() + ": " + Colors.NORMAL + line);
                    } else {
                        event.getSender().sendNotice(Colors.BOLD + event.getArgs()[0].toLowerCase() + ": " + Colors.NORMAL + line);
                    }
                }
            } else if (event.getArgs().length == 2 && (event.getCommand().equals(">") || event.getCommand().equals("<<"))) {
                String[] lines = index.get(event.getArgs()[1].toLowerCase());
                if (lines == null || lines.length == 0) {
                    return;
                }
                String sendTo = event.getArgs()[0];
                for (String line : lines) {
                    if (event.getCommand().equals(">")) {
                        target.sendMessage(Colors.BOLD + sendTo + ": " + Colors.NORMAL + "(" + event.getArgs()[1].toLowerCase() + ") " + Colors.NORMAL + line);
                    } else {
                        BotUser.getBotUser().sendNotice(sendTo, Colors.BOLD + event.getArgs()[1].toLowerCase() + ": " + Colors.NORMAL + line);
                    }
                }
                if (event.getCommand().equalsIgnoreCase("<<")) {
                    event.getSender().sendNotice("I have told " + event.getArgs()[0] + " about " + event.getArgs()[1]);
                }
            }
        }
    }

    @Override
    public String[] getAliases() {
        return new String[]{
            "vh",
            ">",
            "<",
            "<<",
            "",
            "refresh"
        };
    }

    @Override
    @EventType(event = EventField.Join)
    public void runEvent(JoinEvent event) {
        if (event.getSender().getNick().equalsIgnoreCase("voxelhead")) {
            event.getChannel().sendMessage("Voxelhead has returned. Returning to my cave.");
        }
    }

    @Override
    @EventType(event = EventField.Part)
    public void runEvent(PartEvent event) {
        if (event.getSender().getNick().equalsIgnoreCase("voxelhead")) {
            event.getChannel().sendMessage("Voxelhead has left the building. Taking over.");
        }
    }

    private void copyInputStream(InputStream in, FileOutputStream out) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(in);
        out.getChannel().transferFrom(rbc, 0, 1 << 24);
    }
}