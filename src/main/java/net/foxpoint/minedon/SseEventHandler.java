package net.foxpoint.minedon;

import java.io.InputStream;

public interface SseEventHandler {
    void HandleEvent(InputStream IS, String data);
}
