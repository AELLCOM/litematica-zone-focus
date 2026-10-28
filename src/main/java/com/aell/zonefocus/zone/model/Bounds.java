package com.aell.zonefocus.zone.model;

/** Как участок из нескольких схем задаёт габарит разбиения: каждая схема отдельно или одна коробка на все. */
public enum Bounds
{
    PER_PLACEMENT,
    COMBINED;

    public static Bounds of(String name)
    {
        try
        {
            return valueOf(name);
        }
        catch (IllegalArgumentException | NullPointerException e)
        {
            return PER_PLACEMENT;
        }
    }
}
