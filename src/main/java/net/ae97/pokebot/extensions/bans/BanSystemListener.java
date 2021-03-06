/*
 * Copyright (C) 2015 Joshua
 *
 * This file is a part of pokebot-extensions
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
package net.ae97.pokebot.extensions.bans;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.ae97.pircboty.Channel;
import net.ae97.pircboty.User;
import net.ae97.pircboty.api.events.CommandEvent;
import net.ae97.pircboty.api.events.JoinEvent;
import net.ae97.pircboty.api.events.SetChannelBanEvent;
import net.ae97.pircboty.api.events.UserAuthEvent;
import net.ae97.pokebot.PokeBot;
import net.ae97.pokebot.api.CommandExecutor;
import net.ae97.pokebot.api.EventExecutor;
import net.ae97.pokebot.api.Listener;
import sun.misc.Regexp;

/**
 * @author Joshua
 */
public class BanSystemListener implements Listener {

    private final BanSystem core;
    private final Map<String, List<String>> permBanTemp = new ConcurrentHashMap<>();

    public BanSystemListener(BanSystem system) {
        core = system;
    }

    @EventExecutor
    public void onJoin(JoinEvent event) {
        processEvent(event.getUser(), event.getChannel());
    }

    @EventExecutor
    public void onAuth(UserAuthEvent event) {
        Set<Channel> channels = event.getUser().getChannels();
        for (Channel chan : channels) {
            processEvent(event.getUser(), chan);
        }
    }

    @EventExecutor
    public void onBan(SetChannelBanEvent event) {
        if (!getChannels().contains(event.getChannel().getName().toLowerCase())) {
            return;
        }
        addBanToHistory(event.getHostmask(), event.getUser(), event.getChannel());

        PokeBot.getScheduler().scheduleTask(new UnbanRunnable(event.getChannel().getName(), event.getHostmask()), core.getConfig().getInt("unban-delay", 3), TimeUnit.HOURS);
        if (!isIgnored(event)) {
            addBanToSystem(event.getHostmask(), event.getUser(), event.getChannel());
        }
    }

    private void processEvent(User user, Channel channel) {
        if (!getChannels().contains(channel.getName().toLowerCase())) {
            return;
        }
        if (user.getBot().getUserBot().getNick().equals(user.getNick())) {
            return;
        }
        try (Connection connection = openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT id,content,kickMessage FROM bans"
                    + " INNER JOIN banchannels ON bans.id = banchannels.banId"
                    + " WHERE channel IN (?, \"all\") AND (expireDate > CURRENT_TIMESTAMP OR expireDate IS NULL) AND (? LIKE content OR content=?)")) {
                statement.setString(1, channel.getName());
                statement.setString(2, user.getNick() + "!" + user.getLogin() + "@" + user.getHostmask());
                statement.setString(3, "$a:" + user.getLogin());
                ResultSet set = statement.executeQuery();
                if (set.first()) {
                    String content = set.getString("content").replace("%", "*");
                    String message = set.getString("kickMessage");
                    channel.send().ban(content);
                    channel.send().kick(user, message + " (#" + set.getInt("id") + ")");
                }
            }
        } catch (SQLException e) {
            core.getLogger().log(Level.SEVERE, "Error on checking for bans", e);
        }
    }

    private void addBanToHistory(String mask, User banner, Channel channel) {
        try (Connection connection = openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO banhistory VALUES (null, ?, ?, ?, ?, null)")) {
                statement.setString(1, mask.replace("*", "%"));
                statement.setString(2, channel.getName());
                statement.setString(3, banner.getNick());
                statement.setString(4, banner.getLogin());
                statement.execute();
            }
        } catch (SQLException e) {
            core.getLogger().log(Level.SEVERE, "Error on adding ban to history", e);
        }
    }

    private void addBanToSystem(String mask, User banner, Channel channel) {
        try (Connection connection = openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO bans VALUES (null, 0, ?, ?, ?, ?, CURRENT_TIMESTAMP(), TIMESTAMPADD(HOUR, ?, CURRENT_TIMESTAMP()))")) {
                statement.setString(1, mask.replace("*", "%"));
                statement.setString(2, "00000000-0000-0000-0000-000000000000");
                statement.setString(3, core.getConfig().getString("automessage", "You are temp banned from this channel"));
                statement.setString(4, "Automatic ban when IP was banned by " + banner.getNick() + "@" + banner.getHostmask());
                statement.setInt(5, getBanTime(getPreviousBanCount(mask, channel.getName())));
                statement.execute();
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO banchannels VALUES ((SELECT id FROM bans WHERE content = ? ORDER BY issueDate DESC LIMIT 1), ?)")) {
                statement.setString(1, mask.replace("*", "%"));
                statement.setString(2, channel.getName());
                statement.execute();
            }
        } catch (SQLException e) {
            core.getLogger().log(Level.SEVERE, "Error on adding ban to system", e);
        }
    }

    private List<String> getChannels() {
        return core.getConfig().getStringList("channels");
    }

    private List<String> getIgnoredBanners() {
        return core.getConfig().getStringList("ignoredBanners");
    }

    private int getPreviousBanCount(String mask, String channel) {
        try (Connection connection = openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT count(*) AS count FROM banhistory WHERE ? LIKE mask AND channel = ?")) {
                statement.setString(1, mask);
                statement.setString(2, channel);
                ResultSet set = statement.executeQuery();
                if (set.first()) {
                    return set.getInt("count");
                }
                return -1;
            }
        } catch (SQLException e) {
            core.getLogger().log(Level.SEVERE, "Error on getting ban history", e);
            return -1;
        }
    }

    private int getBanTime(int numberOfBans) {
        List<String> times = core.getConfig().getStringList("bantimer");
        if (times.isEmpty()) {
            times.add("3");
        }

        String value;
        if (numberOfBans > times.size()) {
            value = times.get(times.size() - 1);
        } else {
            value = times.get(numberOfBans - 1);
        }
        return Integer.valueOf(value);
    }

    private Connection openConnection() throws SQLException {
        String host = core.getConfig().getString("host");
        int port = core.getConfig().getInt("port");
        String mysqlUser = core.getConfig().getString("user");
        String pass = core.getConfig().getString("pass");
        String database = core.getConfig().getString("database");
        return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, mysqlUser, pass);
    }

    private boolean isIgnored(SetChannelBanEvent event) {
        String bannerName = event.getUser().getNick();
        return bannerName.equals(event.getBot().getNick()) || getIgnoredBanners().contains(bannerName);
    }
}
