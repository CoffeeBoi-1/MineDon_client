package net.foxpoint.minedon;

import net.minecraftforge.client.event.ClientChatEvent;
import okhttp3.Request;
import okhttp3.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SetDonattyCommand implements ICustomCommand{

    @Override
    public String getCommandName() {
        return ".setDT";
    }

    @Override
    public void Execute(ClientChatEvent event, String[] args) {
        if (args.length < 2) {
            event.setMessage("Token mustn't be empty");
            return;
        }
        String sseLink = GetSSE(args[0], args[1]);
        if (sseLink == null) {
            event.setMessage("Something went wrong! Check token");
            return;
        }


        event.setMessage(ConfigManager.SaveConfig(sseLink, MineDon.getInstance().mineDonId));
        MineDon.getInstance().donattyToken = sseLink;
    }

    private String GetSSE(String widgetId, String widgetToken) {
        try {
            Request request = new Request.Builder()
                    .url("https://api-007.donatty.com/auth/tokens/" + widgetToken)
                    .get()
                    .build();

            Response response = MineDon.getInstance().client.newCall(request).execute();
            JSONObject serverAnswer = null;
            serverAnswer = (JSONObject) new JSONParser().parse(response.body().string());

            if (serverAnswer.get("response") == null) return null;
            if (((JSONObject) serverAnswer.get("response")).get("accessToken") == null) return null;
            if (serverAnswer == null) return null;
            if (response.code() != 200) {
                return null;
            }

            String answer = "https://api-007.donatty.com/widgets/" + widgetId + "/sse?jwt=" + (String) ((JSONObject) serverAnswer.get("response")).get("accessToken");
            return answer;
        } catch (Exception e) {
            return null;
        }
    }
}
