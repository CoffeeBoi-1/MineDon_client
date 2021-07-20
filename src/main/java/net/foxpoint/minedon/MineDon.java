package net.foxpoint.minedon;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.CommandBlockLogic;
import net.minecraft.tileentity.CommandBlockTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.*;

@Mod("minedon")
public class MineDon {
    private static final Logger LOGGER = LogManager.getLogger();
    private OkHttpClient client = new OkHttpClient();
    private final String MAIN_ADRESS = "https://5b3e66fe14b2.ngrok.io/";
    private String donattyToken;
    private String mineDonId;
    private JSONObject OPTIONS;
    private SseEventSource SSE;

    public MineDon() {
        MinecraftForge.EVENT_BUS.register(this);
        JSONObject obj = ConfigManager.GetConfig();
        donattyToken = (String) obj.get("DonattyToken");
        mineDonId = (String) obj.get("MineDonId");
    }

    @SubscribeEvent
    public void onWorldExit(FMLServerStoppingEvent event) {
        SSE.close();
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatEvent event) {
        String[] words = event.getMessage().split(" ");
        String command = words[0];
        String[] args = Arrays.copyOfRange(words, 1, words.length);
        switch (command) {
            case ".load": {
                try {
                    if (SSE != null) SSE.close();
                    String getOptionsRes = GetOptions();
                    event.setMessage(getOptionsRes);
                    if (getOptionsRes.equals("OK")) StartSse();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case ".setDt": {
                if (args.length < 1) {
                    event.setMessage("Token mustn't be empty");
                    return;
                }
                event.setMessage(ConfigManager.SaveConfig(args[0], mineDonId));
                donattyToken = args[0];
                break;
            }
            case ".setMD": {
                if (args.length < 1) {
                    event.setMessage("Id mustn't be empty");
                    return;
                }
                event.setMessage(ConfigManager.SaveConfig(donattyToken, args[0]));
                mineDonId = args[0];
                break;
            }
        }

    }

    private static void ExecuteCommand(String command) {
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

    private String GetOptions() {
        try {
            Request request = new Request.Builder()
                    .url(MAIN_ADRESS + "api/get_options?id=" + mineDonId)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            JSONObject serverAnswer = null;
            serverAnswer = (JSONObject) new JSONParser().parse(response.body().string());

            if (serverAnswer == null) return "Something went wrong";
            if (serverAnswer.containsKey("error")) {
                return (String) serverAnswer.get("error");
            }
            OPTIONS = serverAnswer;
            return "OK";
        } catch (Exception e) {
            return "Something went wrong";
        }
    }

    private void StartSse() {
        WebTarget target = ClientBuilder.newClient().target(donattyToken);
        SSE = SseEventSource.
                target(target)
                .reconnectingEvery(2, SECONDS)
                .build();
        SSE.register(this::onMessage);
        SSE.open();
    }

    private void onMessage(InboundSseEvent event) {
        try {
            JSONObject data = (JSONObject) new JSONParser().parse(event.readData());
            String action = (String) data.get("action");
            if (action.equals("PING")) return;

            data = (JSONObject) data.get("data");

            JSONObject donateObject = (JSONObject) new JSONParser().parse((String) data.get("message"));
            String optionId = (String) donateObject.get("optionId");
            if (!OPTIONS.containsKey(optionId)) return;

            JSONObject command = (JSONObject) OPTIONS.get(optionId);

            if ((long) data.get("amount") < (long) ((JSONObject) OPTIONS.get(optionId)).get("cost")) return;
            TextComponent text = new StringTextComponent("Новый ивент! Название : " + command.get("name"));
            Minecraft.getInstance().player.sendMessage(text, new UUID(1000, 1000));
            ExecuteCommand((String) command.get("command"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
