package net.foxpoint.minedon;

import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.FileWriter;

public class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger();

    static JSONObject GetConfig() {
        try {
            JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader(String.valueOf(FMLPaths.CONFIGDIR.get()) + "\\config.json"));
            return obj;
        } catch (Exception e) {
            try {
                FileWriter file = new FileWriter(String.valueOf(FMLPaths.CONFIGDIR.get()) + "\\config.json");
                file.write(new JSONObject().toJSONString());
                file.flush();
            } catch (Exception err) { LOGGER.error(err.getMessage()); }
            return new JSONObject();
        }
    }

    static String SaveConfig(String DonattyToken, String MineDonId) {
        try {
            FileWriter file = new FileWriter(String.valueOf(FMLPaths.CONFIGDIR.get()) + "\\config.json");
            JSONObject obj = new JSONObject();
            obj.put("DonattyToken", DonattyToken);
            obj.put("MineDonId", MineDonId);
            file.write(obj.toJSONString());
            file.flush();
            return "Saved.";
        } catch (Exception e) {
            return "Something went wrong";
        }
    }
}
