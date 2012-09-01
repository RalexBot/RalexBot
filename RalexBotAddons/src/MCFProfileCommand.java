
import com.lordralex.ralexbot.api.Listener;
import com.lordralex.ralexbot.api.Priority;
import com.lordralex.ralexbot.api.events.CommandEvent;
import com.lordralex.ralexbot.api.events.EventType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @version 1.0
 * @author Joshua
 */
public class MCFProfileCommand extends Listener {

    @Override
    public void onCommand(CommandEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final String channel = event.getChannel();
        final String sender = event.getSender();
        final String[] args = event.getArgs();
        BufferedReader reader = null;
        String target = channel;
        if (target
                == null) {
            target = sender;
        }
        if (target
                == null) {
            return;
        }
        String total = buildArgs(args);
        if (args.length == 0 || total.isEmpty()) {
            return;
        }


        try {
            String url = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=site:www.minecraftforum.net/user%20viewing%20%22profile%20" + total.replace(" ", "%20") + "%22";
            URL path = new URL(url);
            reader = new BufferedReader(new InputStreamReader(path.openStream()));
            List<String> parts = new ArrayList<>();
            String s;
            while ((s = reader.readLine()) != null) {
                parts.add(s);
            }
            List<String> b = new ArrayList<>();
            for (String part : parts) {
                String[] c = part.split(",");
                b.addAll(Arrays.asList(c));
            }
            for (String string : b) {
                if (string.startsWith("\"url\":")) {
                    string = string.replace("\"", "");
                    string = string.replace("url:", "");
                    String id = string.split("http://www.minecraftforum.net/user/")[1];
                    id = id.split("/")[0];
                    string = "http://www.minecraftforum.net/user/" + id;
                    sendMessage(target, string);
                    break;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MCFCommand.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(MCFCommand.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public String[] getAliases() {
        return new String[]{
                    "mcfprofile",
                    "mcfp",
                    "mp"
                };
    }

    @Override
    public void declarePriorities() {
        priorities.put(EventType.Command, Priority.NORMAL);
    }
}
