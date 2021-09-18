package net.foxpoint.minedon;

import net.minecraftforge.client.event.ClientChatEvent;

import java.util.Optional;

public interface ICustomCommand {
    String getCommandName();

    void Execute(ClientChatEvent event, String[] args);
}
