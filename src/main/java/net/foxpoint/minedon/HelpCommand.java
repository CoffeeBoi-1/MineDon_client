package net.foxpoint.minedon;

import net.minecraftforge.client.event.ClientChatEvent;

public class HelpCommand implements ICustomCommand{
    public static String getCommandName() {
        return ".help";
    }

    @Override
    public void Execute(ClientChatEvent event, String[] args) {
        event.setMessage("https://docs.google.com/document/d/1Yiuju8KpQ05zYNOCqcOx-Dkj9FFZiIHK6cVIQW5kkUI/edit?usp=sharing");
    }
}
