package net.foxpoint.minedon;

import net.minecraftforge.client.event.ClientChatEvent;

import java.io.InputStream;

public class ConnectSSEBotCommand extends LoadCommand {
    @Override
    public String getCommandName() {
        return ".conBot";
    }

    @Override
    public void Execute(ClientChatEvent event, String[] args) {
        if (args.length < 1) {
            event.setMessage("Не хватает ID сервера Дискорд!");
            return;
        }

        try {
            if (MineDon.getInstance().SSEBot != null) {
                MineDon.getInstance().SSEBot.close();
                MineDon.getInstance().SSEBot = null;
            }
            String getOptionsRes = GetOptions();
            if (getOptionsRes.equals("OK")) StartBotSSE(event, args[0]);
            else { event.setMessage("Что-то пошло не так!"); }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void StartBotSSE(ClientChatEvent event, String serverID) {
        InputStream inputStream = SseClient.GetSseInputStream(MineDon.getInstance().MAIN_ADDRESS + "bot/" + serverID + "/sse");
        if (inputStream == null) { event.setMessage("Что-то пошло не так! Попробуйте обновить токен"); return; }

        MineDon.getInstance().SSEBot = inputStream;
        SseClient.ReadStream(MineDon.getInstance().SSEBot, this::onMessage);

        event.setMessage("OK");
    }
}
