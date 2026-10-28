package com.aell.zonefocus.zone.model;

import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** Правило разбиения участка на зоны. */
public sealed interface Rule permits Rule.Grid, Rule.SubRegions, Rule.Manual, Rule.Cuts
{
    ZoneState.Kind kind();

    JsonObject toJson();

    /** Сетка nx×ny×nz по габариту (размещения или общему). */
    record Grid(int nx, int ny, int nz) implements Rule
    {
        public Grid
        {
            nx = Math.max(1, nx);
            ny = Math.max(1, ny);
            nz = Math.max(1, nz);
        }

        @Override
        public ZoneState.Kind kind()
        {
            return ZoneState.Kind.GRID;
        }

        @Override
        public JsonObject toJson()
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("kind", "GRID");
            obj.addProperty("nx", this.nx);
            obj.addProperty("ny", this.ny);
            obj.addProperty("nz", this.nz);
            return obj;
        }
    }

    /**
     * Разрезы в блоках по осям — сетка, которую подвинули мышью на превью: {@code xs} — смещения плоскостей от
     * минимального угла габарита участка (плоскость {@code c} отделяет блок {@code c − 1} от блока {@code c}),
     * отсортированы, без повторов, внутри габарита. Габарит — общий у всего участка (у одной схемы — её
     * собственный). Для фасада и моста это та же сетка: {@code nx = xs.length + 1}.
     */
    record Cuts(int[] xs, int[] ys, int[] zs) implements Rule
    {
        public Cuts
        {
            xs = normalize(xs);
            ys = normalize(ys);
            zs = normalize(zs);
        }

        private static int[] normalize(int @Nullable [] cuts)
        {
            if (cuts == null || cuts.length == 0)
            {
                return new int[0];
            }

            TreeSet<Integer> set = new TreeSet<>();

            for (int c : cuts)
            {
                if (c > 0)
                {
                    set.add(c);
                }
            }

            int[] out = new int[set.size()];
            int i = 0;

            for (int c : set)
            {
                out[i++] = c;
            }

            return out;
        }

        /** Разрезы сетки {@code n} равных долей по стороне {@code size} — те же границы, что у {@link Grid}. */
        public static int[] even(int size, int n)
        {
            int count = Math.max(1, Math.min(n, Math.max(1, size)));
            int[] out = new int[count - 1];
            int base = size / count;
            int remainder = size % count;
            int start = 0;

            for (int i = 0; i < count - 1; ++i)
            {
                start += base + (i < remainder ? 1 : 0);
                out[i] = start;
            }

            return out;
        }

        /** Сетка как разрезы по габариту. */
        public static Cuts of(Grid grid, ZoneBox bounds)
        {
            return new Cuts(even(bounds.sizeX(), grid.nx()), even(bounds.sizeY(), grid.ny()), even(bounds.sizeZ(), grid.nz()));
        }

        public int[] axis(int axis)
        {
            return switch (axis) { case 0 -> this.xs; case 1 -> this.ys; default -> this.zs; };
        }

        public Cuts withAxis(int axis, int[] cuts)
        {
            return switch (axis)
            {
                case 0 -> new Cuts(cuts, this.ys, this.zs);
                case 1 -> new Cuts(this.xs, cuts, this.zs);
                default -> new Cuts(this.xs, this.ys, cuts);
            };
        }

        /** Разрезы внутри габарита: вне {@code (0, size)} отбрасываются. */
        public Cuts clamp(ZoneBox bounds)
        {
            return new Cuts(inside(this.xs, bounds.sizeX()), inside(this.ys, bounds.sizeY()), inside(this.zs, bounds.sizeZ()));
        }

        private static int[] inside(int[] cuts, int size)
        {
            return Arrays.stream(cuts).filter(c -> c > 0 && c < size).toArray();
        }

        public int nx() { return this.xs.length + 1; }
        public int ny() { return this.ys.length + 1; }
        public int nz() { return this.zs.length + 1; }

        @Override
        public ZoneState.Kind kind()
        {
            return ZoneState.Kind.GRID;
        }

        @Override
        public JsonObject toJson()
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("kind", "CUTS");
            obj.addProperty("xs", Json.ints(Arrays.stream(this.xs).boxed().toList()));
            obj.addProperty("ys", Json.ints(Arrays.stream(this.ys).boxed().toList()));
            obj.addProperty("zs", Json.ints(Arrays.stream(this.zs).boxed().toList()));
            return obj;
        }

        @Override
        public boolean equals(Object other)
        {
            return other instanceof Cuts c && Arrays.equals(this.xs, c.xs) && Arrays.equals(this.ys, c.ys) && Arrays.equals(this.zs, c.zs);
        }

        @Override
        public int hashCode()
        {
            return 31 * (31 * Arrays.hashCode(this.xs) + Arrays.hashCode(this.ys)) + Arrays.hashCode(this.zs);
        }

        @Override
        public String toString()
        {
            return "Cuts" + Arrays.toString(this.xs) + Arrays.toString(this.ys) + Arrays.toString(this.zs);
        }
    }

    /** Каждый подрегион размещения — зона. */
    record SubRegions() implements Rule
    {
        public static final SubRegions INSTANCE = new SubRegions();

        @Override
        public ZoneState.Kind kind()
        {
            return ZoneState.Kind.SUB_REGIONS;
        }

        @Override
        public JsonObject toJson()
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("kind", "SUB_REGIONS");
            return obj;
        }
    }

    /**
     * Коробки мира, снятые при положении базового размещения {@code origin}: при чтении сдвигаются на
     * {@code base.origin − origin}, чтобы ручная зона ехала вместе со схемой. Базы нет — без сдвига.
     */
    record Manual(List<ZoneBox> boxes, @Nullable PlacementRef base, @Nullable BlockPos origin) implements Rule
    {
        public Manual
        {
            boxes = List.copyOf(boxes);
        }

        @Override
        public ZoneState.Kind kind()
        {
            return ZoneState.Kind.MANUAL;
        }

        public Manual withBoxes(List<ZoneBox> newBoxes)
        {
            return new Manual(newBoxes, this.base, this.origin);
        }

        @Override
        public JsonObject toJson()
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("kind", "MANUAL");
            JsonArray list = new JsonArray();

            for (ZoneBox box : this.boxes)
            {
                list.add(box.signature());
            }

            obj.add("boxes", list);

            if (this.base != null)
            {
                obj.add("base", this.base.toJson());
            }

            Json.putPos(obj, "origin", this.origin);

            return obj;
        }
    }

    static @Nullable Rule fromJson(@Nullable JsonObject obj)
    {
        if (obj == null)
        {
            return null;
        }

        return switch (Json.str(obj, "kind", ""))
        {
            case "GRID" -> new Grid(Json.integer(obj, "nx", 1), Json.integer(obj, "ny", 1), Json.integer(obj, "nz", 1));
            case "CUTS" -> new Cuts(toArray(Json.ints(Json.str(obj, "xs", ""))), toArray(Json.ints(Json.str(obj, "ys", ""))),
                    toArray(Json.ints(Json.str(obj, "zs", ""))));
            case "SUB_REGIONS" -> SubRegions.INSTANCE;
            case "MANUAL" ->
            {
                List<ZoneBox> boxes = new ArrayList<>();
                JsonElement list = obj.get("boxes");

                if (list != null && list.isJsonArray())
                {
                    for (JsonElement item : list.getAsJsonArray())
                    {
                        ZoneBox box = item.isJsonPrimitive() ? Json.parseBox(item.getAsString()) : null;

                        if (box != null)
                        {
                            boxes.add(box);
                        }
                    }
                }

                if (boxes.isEmpty())
                {
                    yield null;
                }

                JsonObject base = Json.object(obj, "base");
                yield new Manual(boxes, base != null ? PlacementRef.fromJson(base) : null, Json.pos(obj, "origin"));
            }
            default -> null;
        };
    }

    private static int[] toArray(List<Integer> values)
    {
        return values.stream().mapToInt(Integer::intValue).toArray();
    }
}
