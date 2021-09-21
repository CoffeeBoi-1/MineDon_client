package net.foxpoint.minedon;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.CommandBlockTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.ClientChatEvent;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;

public class LoadCommand implements ICustomCommand {
    @Override
    public String getCommandName() {
        return ".load";
    }

    @Override
    public void Execute(ClientChatEvent event, String[] args) {
        try {
            if (MineDon.getInstance().SSEDonatty != null) {
                MineDon.getInstance().SSEDonatty.close();
                MineDon.getInstance().SSEDonatty = null;
            }
            String getOptionsRes = GetOptions();
            if (getOptionsRes.equals("OK")) StartSse(event);
            else { event.setMessage("Что-то пошло не так!"); }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String GetOptions() {
        try {
            Request request = new Request.Builder()
                    .url(MineDon.getInstance().MAIN_ADDRESS + "api/get_options?id=" + MineDon.getInstance().mineDonId)
                    .get()
                    .build();

            Response response = MineDon.getInstance().client.newCall(request).execute();
            JSONObject serverAnswer = null;
            serverAnswer = (JSONObject) new JSONParser().parse(response.body().string());

            if (serverAnswer == null) return "Something went wrong";
            if (serverAnswer.containsKey("error")) {
                return (String) serverAnswer.get("error");
            }
            MineDon.getInstance().OPTIONS = serverAnswer;
            return "OK";
        } catch (Exception e) {
            return "Something went wrong";
        }
    }

    private void StartSse(ClientChatEvent event) {
        InputStream inputStream = SseClient.GetSseInputStream(MineDon.getInstance().donattyToken);
        if (inputStream == null) { event.setMessage("Что-то пошло не так! Попробуйте обновить токен"); return; }

        MineDon.getInstance().SSEDonatty = inputStream;
        SseClient.ReadStream(MineDon.getInstance().SSEDonatty, this::onMessage);

        event.setMessage("OK");
    }

    protected void onMessage(InputStream IS, String dataString) {
        try {
            LogManager.getLogger().info(dataString);
            if (dataString.length() < 5) return;

            JSONObject data = (JSONObject) new JSONParser().parse(dataString.substring(5));
            String action = (String) data.get("action");
            if (action.equals("PING")) return;

            data = (JSONObject) data.get("data");

            try {
                JSONObject donateObject = (JSONObject) new JSONParser().parse((String) data.get("message"));

                if (donateObject.containsKey("test")) {
                    TextComponent text = new StringTextComponent("Соединение в порядке!");
                    Minecraft.getInstance().player.sendMessage(text, new UUID(1000, 1000));
                    return;
                }
            } catch (Exception e) {
                String optionId = GetOptionIdByCost((long) data.get("amount"));
                if (!MineDon.getInstance().OPTIONS.containsKey(optionId)) return;

                JSONObject command = (JSONObject) MineDon.getInstance().OPTIONS.get(optionId);

                if ((long) data.get("amount") < (long) ((JSONObject) MineDon.getInstance().OPTIONS.get(optionId)).get("cost"))
                    return;
                TextComponent text = new StringTextComponent("Новый ивент! Название : " + command.get("name"));
                Minecraft.getInstance().player.sendMessage(text, new UUID(1000, 1000));
                ExecuteCommand((String) command.get("command"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String GetOptionIdByCost(long cost) {
        Iterator<String> keys = MineDon.getInstance().OPTIONS.keySet().iterator();

        while (keys.hasNext()) {
            String key = keys.next();
            if (MineDon.getInstance().OPTIONS.get(key) instanceof JSONObject) {
                if ((long) ((JSONObject) MineDon.getInstance().OPTIONS.get(key)).get("cost") == cost) return key;
            }
        }
        return null;
    }

    private void ExecuteCommand(String command) {
        ServerWorld serverWorld = GetPlayerWorld();
        BlockState blockState = Blocks.COMMAND_BLOCK.defaultBlockState();
        CommandBlockTileEntity entity = (CommandBlockTileEntity) blockState.getBlock().createTileEntity(blockState, serverWorld);
        BlockPos commandBlockPos = new BlockPos(0, 1, 0);
        BlockPos redBlockPos = new BlockPos(0, 1, 1);

        entity.getCommandBlock().setCommand(command);
        entity.setLevelAndPosition(serverWorld, commandBlockPos);

        serverWorld.setBlock(commandBlockPos, Blocks.COMMAND_BLOCK.defaultBlockState(), 3);
        serverWorld.setBlockEntity(commandBlockPos, entity);
        serverWorld.setBlock(redBlockPos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        entity.getCommandBlock().performCommand(serverWorld);
    }

    private static ServerWorld GetPlayerWorld() {
        Iterator<ServerWorld> worlds = Minecraft.getInstance().getSingleplayerServer().getAllLevels().iterator();
        ServerWorld serverWorld = null;
        while (worlds.hasNext()) {
            serverWorld = worlds.next();
            if (serverWorld.players().size() > 0) break;
        }
        return serverWorld;
    }
}
