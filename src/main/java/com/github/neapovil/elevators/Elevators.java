package com.github.neapovil.elevators;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.packetlistener.PacketListenerAPI;
import org.inventivetalent.packetlistener.handler.PacketHandler;
import org.inventivetalent.packetlistener.handler.ReceivedPacket;
import org.inventivetalent.packetlistener.handler.SentPacket;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.github.neapovil.elevators.event.PlayerSneakEvent;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Elevators extends JavaPlugin implements Listener
{
    private static Elevators instance;
    private final Sound sound = Sound.sound(org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT.key(), Source.PLAYER, 1, 1);
    private final List<Material> elevators = Stream.of(Material.values()).filter(i -> i.toString().toLowerCase().endsWith("wool")).toList();
    private FileConfig config;
    private FileConfig messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable()
    {
        instance = this;

        this.config = this.initConfig("elevators.json");
        this.messages = this.initConfig("messages.json");

        this.getServer().getPluginManager().registerEvents(this, this);

        PacketListenerAPI.addPacketHandler(new PacketHandler(this) {
            @Override
            public void onSend(SentPacket packet)
            {
            }

            @Override
            public void onReceive(ReceivedPacket packet)
            {
                if (!packet.getPacketName().equals("PacketPlayInEntityAction"))
                {
                    return;
                }

                if (!packet.hasPlayer())
                {
                    return;
                }

                if (packet.getPacketValue(1).toString().equals("PRESS_SHIFT_KEY"))
                {
                    this.getPlugin().getServer().getScheduler().runTask(this.getPlugin(), () -> {
                        this.getPlugin().getServer().getPluginManager().callEvent(new PlayerSneakEvent(packet.getPlayer()));
                    });
                }
            }
        });
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
        final int x = event.getPlayer().getLocation().getBlockX();
        final int y = event.getPlayer().getLocation().getBlockY();
        final int z = event.getPlayer().getLocation().getBlockZ();

        final Location location = new Location(event.getPlayer().getWorld(), x, y - 1, z);

        if (!this.elevators.contains(location.getBlock().getType()))
        {
            return;
        }

        for (int i = y; i > event.getPlayer().getWorld().getMinHeight(); i--)
        {
            if (event.getPlayer().getWorld().getBlockAt(x, i, z).getLocation().equals(location))
            {
                continue;
            }

            if (!this.elevators.contains(event.getPlayer().getWorld().getBlockAt(x, i, z).getType()))
            {
                continue;
            }

            if (this.config.get("elevators." + (x + i + z)) == null)
            {
                break;
            }

            event.getPlayer().playSound(this.sound);
            event.getPlayer().teleport(event.getPlayer().getWorld().getBlockAt(x, i + 1, z).getLocation().toCenterLocation());
            break;
        }
    }

    @EventHandler
    private void playerJump(PlayerJumpEvent event)
    {
        final int x = event.getPlayer().getLocation().getBlockX();
        final int y = event.getPlayer().getLocation().getBlockY();
        final int z = event.getPlayer().getLocation().getBlockZ();

        final Material material = new Location(event.getPlayer().getWorld(), x, y - 1, z).getBlock().getType();

        if (!this.elevators.contains(material))
        {
            return;
        }

        if (this.config.get("elevators." + (x + (y - 1) + z)) == null)
        {
            return;
        }

        for (int i = y; i < event.getPlayer().getWorld().getMaxHeight(); i++)
        {
            if (!this.elevators.contains(event.getPlayer().getWorld().getBlockAt(x, i, z).getType()))
            {
                continue;
            }

            if (this.config.get("elevators." + (x + i + z)) == null)
            {
                break;
            }

            event.getPlayer().playSound(this.sound);
            event.getPlayer().teleport(event.getPlayer().getWorld().getBlockAt(x, i + 1, z).getLocation().toCenterLocation());
            break;
        }
    }

    @EventHandler
    private void playerInteract(PlayerInteractEvent event)
    {
        if (event.getClickedBlock() == null)
        {
            return;
        }

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
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
        
        event.setCancelled(true);

        final int x = event.getClickedBlock().getLocation().getBlockX();
        final int y = event.getClickedBlock().getLocation().getBlockY();
        final int z = event.getClickedBlock().getLocation().getBlockZ();

        if (this.config.get("elevators." + (x + y + z)) != null)
        {
            event.getPlayer().sendMessage(this.deserializeMessage("elevator_exists"));
            return;
        }

        this.config.set("elevators." + (x + y + z), 0);

        final int newamount = event.getItem().getAmount() - 1;

        if (newamount == 0)
        {
            event.getPlayer().getInventory().remove(event.getItem());
        }
        else
        {
            event.getItem().setAmount(newamount);
        }

        event.getPlayer().sendMessage(this.deserializeMessage("elevator_created"));
    }

    @EventHandler
    private void blockBreak(BlockBreakEvent event)
    {
        final int x = event.getBlock().getLocation().getBlockX();
        final int y = event.getBlock().getLocation().getBlockY();
        final int z = event.getBlock().getLocation().getBlockZ();

        if (this.config.remove("elevators." + (x + y + z)) != null)
        {
            event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.ENDER_PEARL));
            event.getPlayer().sendMessage(this.deserializeMessage("elevator_destroyed"));
        }
    }
    
    private final FileConfig initConfig(String fileName)
    {
        this.saveResource(fileName, false);

        final FileConfig config = FileConfig.builder(new File(this.getDataFolder(), fileName))
                .autoreload()
                .autosave()
                .build();

        config.load();
        
        return config;
    }
    
    private final Component deserializeMessage(String path)
    {
        final String message = this.messages.get("messages." + path);
        return this.miniMessage.deserialize(message);
    }
}
