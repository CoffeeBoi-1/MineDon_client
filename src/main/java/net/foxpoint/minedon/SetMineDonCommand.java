package net.foxpoint.minedon;

import net.minecraftforge.client.event.ClientChatEvent;
import okhttp3.Request;
import okhttp3.Response;

public class SetMineDonCommand implements ICustomCommand {
    public static String getCommandName() {
        return ".setMD";
    }

    @Override
    public void Execute(ClientChatEvent event, String[] args) {
        if (args.length < 1) {
            event.setMessage("Id mustn't be empty");
            return;
        }
        if (!ValidMineDonId(args[0])) {
            event.setMessage("Нет такого пользователя!");
            return;
        }

        event.setMessage(ConfigManager.SaveConfig(MineDon.getInstance().donattyToken, args[0]));
        MineDon.getInstance().mineDonId = args[0];
    }

    private boolean ValidMineDonId(String id) {
        try {
            Request request = new Request.Builder()
                    .url(MineDon.getInstance().MAIN_ADDRESS + "api/id_valid?id=" + id)
                    .get()
                    .build();

            Response response = MineDon.getInstance().client.newCall(request).execute();

            if (response.code() == 200) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
