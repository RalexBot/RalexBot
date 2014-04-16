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
package org.hoenn.pokebot.extensions.faq;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.hoenn.pokebot.PokeBot;
import org.hoenn.pokebot.api.CommandExecutor;
import org.hoenn.pokebot.api.events.CommandEvent;
import org.hoenn.pokebot.extension.Extension;
import org.hoenn.pokebot.extension.ExtensionReloadFailedException;
import org.hoenn.pokebot.extensions.faq.commands.FaqSubCommand;
import org.hoenn.pokebot.extensions.faq.commands.MessageChannelCommand;
import org.hoenn.pokebot.extensions.faq.commands.MessageSelfCommand;
import org.hoenn.pokebot.extensions.faq.commands.MessageUserCommand;
import org.hoenn.pokebot.extensions.faq.database.Database;
import org.hoenn.pokebot.extensions.faq.database.MySQLDatabase;

/**
 * @author Lord_Ralex
 */
public class FaqExtension extends Extension implements CommandExecutor {

    private final Map<String, Database> databases = new ConcurrentHashMap<>();
    private final Map<String, FaqSubCommand> subCommands = new ConcurrentHashMap<>();
    private int delay;
    private String messageFormat, messageFormat_nouser;
    private final List<Character> delimiters = new ArrayList<>();

    @Override
    public String getName() {
        return "Faq Extension";
    }

    @Override
    public void load() {
        delay = getConfig().getInt("delay");
        messageFormat = getConfig().getString("format");
        messageFormat_nouser = getConfig().getString("format-nouser");
        List<String> delimits = getConfig().getStringList("delimiters");
        for (String str : delimits) {
            if (str.length() == 1) {
                delimiters.add(str.charAt(0));
            }
        }
        loadCommands();
        loadDatabases();
        PokeBot.getExtensionManager().addCommandExecutor(this);
    }

    @Override
    public void reload() throws ExtensionReloadFailedException {
        super.reload();
        delay = getConfig().getInt("delay");
        messageFormat = getConfig().getString("format");
        messageFormat_nouser = getConfig().getString("format-nouser");
        List<String> delimits = getConfig().getStringList("delimiters");
        for (String str : delimits) {
            if (str.length() == 1) {
                delimiters.add(str.charAt(0));
            }
        }
        loadCommands();
        loadDatabases();
    }

    @Override
    public void runEvent(CommandEvent event) {
        if (event.getCommand().equalsIgnoreCase("faqreload")) {
            load();
            event.getUser().sendNotice("Updated local storage of all databases");
        } else {
            for (Entry<String, FaqSubCommand> entry : subCommands.entrySet()) {
                if (event.getCommand().equalsIgnoreCase(entry.getKey())) {
                    entry.getValue().execute(this, event);
                    return;
                }
            }
        }
    }

    @Override
    public String[] getAliases() {
        ArrayList<String> aliases = new ArrayList<>();
        aliases.addAll(subCommands.keySet());
        String[] it;
        synchronized (databases) {
            it = databases.keySet().toArray(new String[0]);
        }
        for (String key : it) {
            for (String sub : subCommands.keySet()) {
                aliases.add(key + sub);
            }
        }
        aliases.add("faqreload");
        return aliases.toArray(new String[aliases.size()]);
    }

    public Map<String, Database> getDatabases() {
        return databases;
    }

    public int getDelay() {
        return delay;
    }

    public String getMessageFormat() {
        return messageFormat;
    }

    public String getMessageFormatNoUser() {
        return messageFormat_nouser;
    }

    public List<Character> getDelimiters() {
        return delimiters;
    }

    public void loadDatabases() {
        List<String> databasesToLoad = getConfig().getStringList("databases");
        for (String load : databasesToLoad) {
            String databaseType = getConfig().getString(load + ".type");
            try {
                Database newDatabase = null;
                String name = load;
                HashMap<String, Object> details = new HashMap<>();
                switch (databaseType.toUpperCase()) {
                    case "MYSQL": {
                        details.put("host", getConfig().getString(name + ".host"));
                        details.put("port", getConfig().getInt(name + ".port"));
                        details.put("user", getConfig().getString(name + ".user"));
                        details.put("pass", getConfig().getString(name + ".pass"));
                        details.put("database", getConfig().getString(name + ".database"));
                        details.put("get", getConfig().getString(name + ".get"));
                        details.put("search", getConfig().getString(name + ".search"));
                        newDatabase = new MySQLDatabase(name, details);
                    }
                    break;
                }
                if (newDatabase != null) {
                    PokeBot.getLogger().log(Level.INFO, "[FAQ] Loading database: " + newDatabase.getName());
                    newDatabase.load();
                    databases.put(newDatabase.getName().toLowerCase(), newDatabase);
                } else {
                    PokeBot.getLogger().log(Level.WARNING, name + " could not be created into a database, no " + databaseType.toUpperCase() + " found");
                }
            } catch (SQLException ex) {
                PokeBot.getLogger().log(Level.SEVERE, "    There was an error with this setting: " + load, ex);
            }
        }
    }

    public void loadCommands() {
        List<FaqSubCommand> subs = new ArrayList<>();
        subs.add(new MessageChannelCommand());
        subs.add(new MessageSelfCommand());
        subs.add(new MessageUserCommand());
        for (FaqSubCommand cmd : subs) {
            for (String key : cmd.getPrefix()) {
                subCommands.put(key, cmd);
            }
        }
    }

    public Database getDatabase(CommandEvent event) {
        Database index = null;
        String[] dbNames = databases.keySet().toArray(new String[databases.size()]);
        for (String name : dbNames) {
            if (event.getCommand().contains(name)) {
                index = databases.get(name);
                break;
            } else if (event.getChannel() != null) {
                if (event.getChannel().getName().contains(name)) {
                    index = databases.get(name);
                    break;
                }
            }
        }
        if (databases.size() == 1) {
            index = databases.get(dbNames[0]);
        }
        return index;
    }

    public String[] splitFactoid(String line) {
        for (Character ch : delimiters) {
            if (line.contains(ch.toString())) {
                return line.split(ch.toString());
            }
        }
        return new String[]{line};
    }
}
