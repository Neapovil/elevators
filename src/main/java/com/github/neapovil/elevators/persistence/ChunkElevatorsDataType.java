package com.github.neapovil.elevators.persistence;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import com.github.neapovil.elevators.object.ChunkElevators;
import com.google.gson.Gson;

public final class ChunkElevatorsDataType implements PersistentDataType<String, ChunkElevators>
{
    private final Gson gson = new Gson();

    @Override
    public @NotNull Class<String> getPrimitiveType()
    {
        return String.class;
    }

    @Override
    public @NotNull Class<ChunkElevators> getComplexType()
    {
        return ChunkElevators.class;
    }

    @Override
    public @NotNull String toPrimitive(@NotNull ChunkElevators complex, @NotNull PersistentDataAdapterContext context)
    {
        return this.gson.toJson(complex);
    }

    @Override
    public @NotNull ChunkElevators fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context)
    {
        return this.gson.fromJson(primitive, ChunkElevators.class);
    }

}
