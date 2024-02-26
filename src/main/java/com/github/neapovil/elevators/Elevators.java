package com.github.neapovil.elevators;

import java.util.List;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.github.neapovil.elevators.object.ChunkElevators;
import com.github.neapovil.elevators.persistence.ChunkElevatorsDataType;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public final class Elevators extends JavaPlugin implements Listener
{
    private static Elevators instance;
    private final Sound sound = Sound.sound(Key.key("minecraft", "entity.enderman.teleport"), Sound.Source.PLAYER, 1, 1);
    private final List<Material> elevators = Stream.of(Material.values()).filter(i -> i.toString().toLowerCase().endsWith("wool")).toList();
    private final NamespacedKey chunkElevatorsKey = new NamespacedKey(this, "chunk-elevators");

    @Override
    public void onEnable()
    {
        instance = this;

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {
    }

    public static Elevators instance()
    {
        return instance;
    }

    @EventHandler
    private void playerSneak(PlayerToggleSneakEvent event)
    {
        if (!event.isSneaking())
        {
            return;
        }

        final ChunkElevators elevators = event.getPlayer().getChunk().getPersistentDataContainer().get(this.chunkElevatorsKey, new ChunkElevatorsDataType());

        if (elevators == null)
        {
            return;
        }

        final Location wool = event.getPlayer().getLocation().subtract(0, 1, 0);

        if (elevators.has(wool))
        {
            elevators.elevators
                    .stream()
                    .filter(i -> wool.getBlockX() == i.getBlockX() && wool.getBlockZ() == i.getBlockZ())
                    .map(i -> i.getBlockY())
                    .filter(i -> i < wool.getBlockY())
                    .max(Integer::compareTo)
                    .ifPresent(i -> {
                        final Location location = wool.clone();

                        location.setY(i);
                        location.add(0, 1, 0);

                        event.getPlayer().teleport(location);
                        event.getPlayer().playSound(this.sound);
                    });
        }
    }

    @EventHandler
    private void playerJump(PlayerJumpEvent event)
    {
        final ChunkElevators elevators = event.getPlayer().getChunk().getPersistentDataContainer().get(this.chunkElevatorsKey, new ChunkElevatorsDataType());

        if (elevators == null)
        {
            return;
        }

        final Location wool = event.getFrom().clone().subtract(0, 1, 0);

        if (elevators.has(wool))
        {
            elevators.elevators
                    .stream()
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
    }

    @EventHandler
    private void createElevator(PlayerInteractEvent event)
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

        ChunkElevators elevators = event.getClickedBlock().getChunk().getPersistentDataContainer().get(this.chunkElevatorsKey, new ChunkElevatorsDataType());

        if (elevators == null)
        {
            elevators = new ChunkElevators();
        }

        if (elevators.has(event.getClickedBlock().getLocation()))
        {
            event.getPlayer().sendRichMessage("<red>This block is already an elevator");
        }
        else
        {
            event.setCancelled(true);
            elevators.elevators.add(event.getClickedBlock().getLocation());
            event.getClickedBlock().getChunk().getPersistentDataContainer().set(this.chunkElevatorsKey, new ChunkElevatorsDataType(), elevators);
            event.getItem().subtract();
            event.getPlayer().sendMessage("Elevator created");
        }
    }

    @EventHandler
    private void destroyElevator(BlockBreakEvent event)
    {
        final ChunkElevators elevators = event.getBlock().getChunk().getPersistentDataContainer().get(this.chunkElevatorsKey, new ChunkElevatorsDataType());

        if (elevators == null)
        {
            return;
        }

        final boolean removed = elevators.elevators.removeIf(i -> i.equals(event.getBlock().getLocation()));

        if (removed)
        {
            event.getBlock().getChunk().getPersistentDataContainer().set(this.chunkElevatorsKey, new ChunkElevatorsDataType(), elevators);
            event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), new ItemStack(Material.ENDER_PEARL));
            event.getPlayer().sendMessage("Elevator destroyed");
        }
    }
}
