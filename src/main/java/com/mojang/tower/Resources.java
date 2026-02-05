package com.mojang.tower;

public class Resources
{
    public static final int WOOD = 0;
    public static final int ROCK = 1;
    public static final int FOOD = 2;

    public int wood = 100;
    public int rock = 100;
    public int food = 100;

    public void add(int resourceId, int count)
    {
        switch (resourceId) {
            case WOOD -> wood += count;
            case ROCK -> rock += count;
            case FOOD -> food += count;
            default -> {} // silently ignore invalid resourceId (matches original behavior)
        }
    }

    public void charge(HouseType type)
    {
        type.cost.chargeFrom(this);
    }

    public boolean canAfford(HouseType type)
    {
        return type.cost.canAfford(this);
    }
}
