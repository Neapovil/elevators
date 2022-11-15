package com.github.neapovil.elevators;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.github.neapovil.elevators.event.PlayerSneakEvent;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public final class Elevators extends JavaPlugin implements Listener
{
    private static Elevators instance;
    private final Sound sound = Sound.sound(Key.key("minecraft", "entity.enderman.teleport"), Sound.Source.PLAYER, 1, 1);
    private final List<Material> elevators = Stream.of(Material.values()).filter(i -> i.toString().toLowerCase().endsWith("wool")).toList();
    private FileConfig messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable()
    {
        instance = this;

        this.saveResource("messages.json", false);

        this.messages = FileConfig.builder(this.getDataFolder().toPath().resolve("messages.json"))
                .autoreload()
                .autosave()
                .build();

        this.messages.load();

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {
    }

    public static Elevators getInstance()
    {
        return instance;
    }

    @EventHandler
    private void playerSneak(PlayerSneakEvent event)
    {
        final Location from = event.getPlayer().getLocation().subtract(0, 1, 0);
        final NamespacedKey key = this.elevatorKey(from);

        if (!from.getChunk().getPersistentDataContainer().has(key))
        {
            return;
        }

        from.getChunk().getPersistentDataContainer()
                .getKeys()
                .stream()
                .filter(i -> i.getNamespace().equals(this.getName().toLowerCase(Locale.ROOT)))
                .map(i -> from.getChunk().getPersistentDataContainer().get(i, PersistentDataType.STRING))
                .map(i -> this.deserialize(i))
                .filter(i -> from.getBlockX() == i.getBlockX() && from.getBlockZ() == i.getBlockZ())
                .map(i -> i.getBlockY())
                .filter(i -> i < from.getBlockY())
                .max(Integer::compareTo)
                .ifPresent(i -> {
                    final Location location = from.clone();

                    location.setY(i);
                    location.add(0, 1, 0);

                    event.getPlayer().teleport(location);
                    event.getPlayer().playSound(this.sound);
                });
    }

    @EventHandler
    private void playerJump(PlayerJumpEvent event)
    {
        final NamespacedKey key = this.elevatorKey(event.getFrom().clone().subtract(0, 1, 0));

        if (!event.getFrom().getChunk().getPersistentDataContainer().has(key))
        {
            return;
        }

        event.getFrom().getChunk().getPersistentDataContainer()
                .getKeys()
                .stream()
                .filter(i -> i.getNamespace().equals(this.getName().toLowerCase(Locale.ROOT)))
                .map(i -> event.getFrom().getChunk().getPersistentDataContainer().get(i, PersistentDataType.STRING))
                .map(i -> this.deserialize(i))
                .filter(i -> event.getFrom().getBlockX() == i.getBlockX() && event.getFrom().getBlockZ() == i.getBlockZ())
                .map(i -> i.getBlockY())
                .filter(i -> i > event.getFrom().getBlockY())
                .min(Integer::compareTo)
                .ifPresent(i -> {
                    final Location location = event.getFrom().clone();

                    location.setY(i);
                    location.add(0, 1, 0);

                    event.getPlayer().teleport(location);
                    event.getPlayer().playSound(this.sound);
                });
    }

    @EventHandler
    private void playerInteract(PlayerInteractEvent event)
    {
        if (event.getClickedBlock() == null)
        {
            return;
        }

        if (!event.getAction().isRightClick())
        {
            return;
        }

        if (!this.elevators.contains(event.getClickedBlock().getType()))
        {
            return;
        }

        if (event.getItem() == null)
        {
            return;
        }

        if (!event.getItem().getType().equals(Material.ENDER_PEARL))
        {
            return;
        }

        final NamespacedKey key = this.elevatorKey(event.getClickedBlock().getLocation());

        if (event.getClickedBlock().getLocation().getChunk().getPersistentDataContainer().has(key))
        {
            final String message = this.messages.get("messages.elevator_exists");

            event.getPlayer().sendMessage(this.miniMessage.deserialize(message));

            return;
        }

        event.setCancelled(true);

        event.getClickedBlock().getLocation().getChunk().getPersistentDataContainer()
                .set(key, PersistentDataType.STRING, this.serialize(event.getClickedBlock().getLocation()));

        event.getItem().subtract();

        final String message = this.messages.get("messages.elevator_created");

        event.getPlayer().sendMessage(this.miniMessage.deserialize(message));
    }

    @EventHandler
    private void blockBreak(BlockBreakEvent event)
    {
        final NamespacedKey key = this.elevatorKey(event.getBlock().getLocation());

        if (!event.getBlock().getChunk().getPersistentDataContainer().has(key))
        {
            return;
        }

        event.getBlock().getChunk().getPersistentDataContainer().remove(key);

        event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), new ItemStack(Material.ENDER_PEARL));

        final String message = this.messages.get("messages.elevator_destroyed");

        event.getPlayer().sendMessage(this.miniMessage.deserialize(message));
    }

    @EventHandler
    private void playerJoin(PlayerJoinEvent event)
    {
        ((CraftPlayer) event.getPlayer()).getHandle().networkManager.channel.pipeline()
                .addBefore("packet_handler", event.getPlayer().getName(), new CustomHandler(event.getPlayer()));
    }

    private NamespacedKey elevatorKey(Location location)
    {
        return new NamespacedKey(this, this.serialize(location));
    }

    private String serialize(Location location)
    {
        final String worldname = location.getWorld().getName();
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();

        return worldname + "-" + x + "-" + y + "-" + z;
    }

    @Nullable
    private Location deserialize(String location)
    {
        final String[] array = location.split("-");

        final World world = this.getServer().getWorld(array[0]);

        if (world == null)
        {
            return null;
        }

        final double x = Double.valueOf(array[1]);
        final double y = Double.valueOf(array[2]);
        final double z = Double.valueOf(array[3]);

        return new Location(world, x, y, z);
    }

    class CustomHandler extends ChannelDuplexHandler
    {
        private final Player player;

        public CustomHandler(Player player)
        {
            this.player = player;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
        {
            if (msg instanceof ServerboundPlayerCommandPacket)
            {
                final ServerboundPlayerCommandPacket packet = (ServerboundPlayerCommandPacket) msg;

                if (packet.getAction().equals(ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY))
                {
                    getServer().getScheduler().runTask(instance, () -> {
                        getServer().getPluginManager().callEvent(new PlayerSneakEvent(this.player));
                    });
                }
            }

            super.channelRead(ctx, msg);
        }
    }
}
