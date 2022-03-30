package com.github.neapovil.elevators.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PlayerSneakEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();
    private final Player player;

    public PlayerSneakEvent(Player player)
    {
        this.player = player;
    }

    public Player getPlayer()
    {
        return player;
    }

    @NotNull
    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}
