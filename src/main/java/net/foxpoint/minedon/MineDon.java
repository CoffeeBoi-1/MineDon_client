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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
    private final String MAIN_ADRESS = "http://9a5dbaa65e19.ngrok.io/";
    private String donattyToken;
    private String mineDonId;
    private JSONObject OPTIONS;
    private SseEventSource SSE;

    public MineDon() {
        MinecraftForge.EVENT_BUS.register(this);
        JSONObject obj = ConfigManager.GetConfig();
        //donattyToken = (String) obj.get("DonattyToken");
        mineDonId = (String) obj.get("MineDonId");
        donattyToken = "https://api-007.donatty.com/widgets/bd5861e0-5fb3-41a6-86ed-7c3d186f4440/sse?jwt=eyJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiUlNBLU9BRVAtMjU2In0.eo2ZZqSloW7_P5U_QRVOSu-NNbds2oNkh3_tgtqbpIGfZtAwHLBWSYB5kV6lBU6FNW4Q_sK1OvuLnCBx6w0kXvHOeolX9aeiYeBc6eQsRvo4xFvuQVO5LZmqbepotemD1XeUkp2RDjv_jZLYjo1H-btegjqn_cQEFz4dHSs76IuIFOLrXIRGeN_n4-02y_5X2S9SbhvcEBOgR7ESPK4P1NS4fDkMXMvXefMsrnw99Yzy8NmN-KsxNihKKTKoGy4Hba4ZGDYI50QVx-Gxv5JgszVhPz0pbp5N5IejOGLSofuxSGSvQ2p6k-oUGH_tDj6H09ip-2CjNoa1vMx_4mQUKw.tCL_DsQvKw8gDriq.04AYxG1QX_d1_IsuFh15ZAiwcdq-T900w2mVyZumS46idXiyWND2B3pQU4q0isT8NSJ5K5_eI2szYljncO1OE-bXz7iy3PpcbTXEv-7xVbABztjaXrdKUWhrlubgrw6RFpswpCLuklDSlX3K2A-C03MKzymOByuxWMe3RUcqBall2b78MQckSv9MULldAO9RDJjdOnTjdk7uJFroKoF2o8lDgWMJzrXhjSulFkSHmUAWDsl56g6wQfAsQoUnaGrQip0K4O6Kc5TJUu_EjGcZUtj6LTxjyBKD-3LxrTOaELK2XzTx6D2pO_JfSJ_VxsgyKEEYV-z_qegJGHCL_fZjGy0V50qYPxGECmaosdmRGd5bCWQ_diU.Ie95Nwf-uXD6WWxTweX2_g&zoneOffset=-300";
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatEvent event) {
        String[] words = event.getMessage().split(" ");
        String command = words[0];
        String[] args = Arrays.copyOfRange(words, 1, words.length);
        switch (command) {
            case ".prepare": {
                try {
                    String getOptionsRes = GetOptions(args);
                    event.setMessage(getOptionsRes);
                    if (getOptionsRes.equals("OK")) StartSse();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case ".setDonatty": {
                if (args.length < 1) {
                    event.setMessage("Token mustn't be empty");
                    return;
                }
                event.setMessage(ConfigManager.SaveConfig(args[0], mineDonId));
                donattyToken = args[0];
                break;
            }
            case ".setMineDon": {
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

    private String GetOptions(String[] args) {
        try {
            Request request = new Request.Builder()
                    .url(MAIN_ADRESS + "api/get_options?id=" + args[0])
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

            ExecuteCommand((String) OPTIONS.get(optionId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
