package dev.imb11.skinshuffle;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.properties.Property;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import org.bukkit.craftbukkit.entity.CraftPlayer;

import java.lang.reflect.InvocationTargetException;

public final class SkinShufflePlugin extends JavaPlugin implements Listener, PluginMessageListener {
    public final static String CBS = Bukkit.getServer().getClass().getPackage().getName();
    private static boolean debugMode = false;

    public static Class<?> bukkitClass(String clazz) throws ClassNotFoundException {
        return Class.forName(CBS + "." + clazz);
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "skinshuffle:handshake");
        getServer().getMessenger().registerIncomingPluginChannel(this, "skinshuffle:skin_refresh", this);
        getCommand("skinshuffle").setExecutor(new SkinShuffleCommand());
        if (debugMode) {
            System.out.println("SkinShuffle plugin enabled");
        }
    }

    @Override
    public void onDisable() {}

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (debugMode) {
            System.out.println("Trying to send skinshuffle handshake to player: " + event.getPlayer().getName());
        }
        getServer().getScheduler().runTaskLater(this, () -> {
            if (debugMode) {
                System.out.println("Send packet!");
            }
        getServer().getScheduler().runTaskLater(this, () -> {
            event.getPlayer().sendPluginMessage(this, "skinshuffle:handshake", new byte[0]);
        }, 20L);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (debugMode) {
            System.out.println("Received plugin message from player: " + player.getName());
        }
        if (channel.equals("skinshuffle:refresh") || channel.equals("skinshuffle:skin_refresh")) {
            if (debugMode) {
                System.out.println("Received skin refresh message from player: " + player.getName());
            }
        if(channel.equals("skinshuffle:refresh") || channel.equals("skinshuffle:skin_refresh")) {
            PlayerProfile playerProfile = player.getPlayerProfile();
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
            Property prop;
            if (buf.readBoolean()) {
                prop = new Property(buf.readUtf(), buf.readUtf(), buf.readUtf());
            } else {
                prop = new Property(buf.readUtf(), buf.readUtf(), null);
            }
            playerProfile.getProperties().removeIf(profileProperty -> profileProperty.getName().equals("textures"));
            playerProfile.getProperties().add(new ProfileProperty("textures", prop.value(), prop.signature()));
            player.setPlayerProfile(playerProfile);
            CraftPlayer craftPlayer = (CraftPlayer) player;
            try {
                var method = bukkitClass("entity.CraftPlayer").getDeclaredMethod("refreshPlayer");
                method.setAccessible(true);
                method.invoke(craftPlayer);
                try {
                    var triggerHealthUpdate = bukkitClass("entity.CraftPlayer").getDeclaredMethod("triggerHealthUpdate");
                    triggerHealthUpdate.setAccessible(true);
                    triggerHealthUpdate.invoke(craftPlayer);
                } catch (NoSuchMethodException e) {
                    player.resetMaxHealth();
                }
            } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class SkinShuffleCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
                debugMode = !debugMode;

                sender.sendMessage("SkinShuffle debug: " + (debugMode ? "enabled" : "disabled"));
                return true;
            }
            return false;
        }
    }
}