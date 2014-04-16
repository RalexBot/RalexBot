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
package org.hoenn.pokebot.extensions.help;

import java.util.LinkedList;
import java.util.List;
import org.hoenn.pokebot.PokeBot;
import org.hoenn.pokebot.api.CommandExecutor;
import org.hoenn.pokebot.api.events.CommandEvent;
import org.hoenn.pokebot.api.recipients.MessageRecipient;
import org.hoenn.pokebot.extension.Extension;

/**
 * @author Lord_Ralex
 */
public class HelpExtension extends Extension implements CommandExecutor {

    private final LinkedList<String> help = new LinkedList<>();

    @Override
    public String getName() {
        return "Faq Extension";
    }

    @Override
    public void load() {
        List<String> helpLines = getConfig().getStringList("help-list");
        if (helpLines != null) {
            help.addAll(helpLines);
        }
        PokeBot.getExtensionManager().addCommandExecutor(this);
    }

    @Override
    public void runEvent(CommandEvent event) {
        if (event.isCancelled()) {
            return;
        }
        MessageRecipient target = event.getChannel();
        if (target == null) {
            target = event.getUser();
        }
        if (target == null) {
            return;
        }
        String helpLine = "My commands you can know about: ";
        for (String name : help) {
            helpLine += name + ", ";
        }
        helpLine = helpLine.trim();
        if (helpLine.endsWith(",")) {
            helpLine = helpLine.substring(0, helpLine.length() - 2);
        }
        target.sendMessage(event.getUser().getNick() + ", " + helpLine);
    }

    @Override
    public String[] getAliases() {
        return new String[]{
            "help",
            "commands"
        };
    }
}
