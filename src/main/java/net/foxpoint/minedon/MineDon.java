package net.foxpoint.minedon;


import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.InputStream;
import java.util.*;

public class MineDon {
    private static MineDon instance;

    private static final Logger LOGGER = LogManager.getLogger();
    private HashMap<String, ICustomCommand> COMMANDS = new HashMap<String, ICustomCommand>();
    public OkHttpClient client = new OkHttpClient();
    public final String MAIN_ADDRESS = "http://92.53.107.191/";
    public String donattyToken;
    public String mineDonId;
    public JSONObject OPTIONS;
    public InputStream SSEBot;
    public InputStream SSEDonatty;

    private MineDon() {
        MinecraftForge.EVENT_BUS.register(this);
        JSONObject obj = ConfigManager.GetConfig();
        donattyToken = (String) obj.get("DonattyToken");
        mineDonId = (String) obj.get("MineDonId");

        COMMANDS.put(LoadCommand.getCommandName(), new LoadCommand());
        COMMANDS.put(ConnectSSEBotCommand.getCommandName(), new ConnectSSEBotCommand());
        COMMANDS.put(SetDonattyCommand.getCommandName(), new SetDonattyCommand());
        COMMANDS.put(SetMineDonCommand.getCommandName(), new SetMineDonCommand());
        COMMANDS.put(HelpCommand.getCommandName(), new HelpCommand());
    }

    public static MineDon getInstance() {
        if (instance == null) {
            instance = new MineDon();
        }
        return instance;
    }

    @SubscribeEvent
    public void onWorldExit(FMLServerStoppingEvent event) {
        if (SSEBot != null) {
            try {
                SSEBot.close();
                SSEBot = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (SSEDonatty != null) {
            try {
                SSEDonatty.close();
                SSEDonatty = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatEvent event) {
        String[] words = event.getMessage().split(" ");
        String command = words[0];
        String[] args = Arrays.copyOfRange(words, 1, words.length);

        if (!COMMANDS.containsKey(command)) return;

        COMMANDS.get(command).Execute(event, args);
    }
}

