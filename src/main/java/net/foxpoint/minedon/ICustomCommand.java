package net.foxpoint.minedon;

import net.minecraftforge.client.event.ClientChatEvent;

public interface ICustomCommand {
    static String getCommandName() {
        return null;
    }

    void Execute(ClientChatEvent event, String[] args);
}
