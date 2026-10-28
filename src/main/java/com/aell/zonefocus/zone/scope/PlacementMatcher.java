package com.aell.zonefocus.zone.scope;

import com.aell.zonefocus.zone.FileZones;
import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.zone.model.Layout;
import com.aell.zonefocus.zone.model.PlacementRef;
import com.aell.zonefocus.zone.model.Section;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

/**
 * Сопоставление ссылок участка с живыми размещениями Litematica: по {@code id} ({@code getHashId}), затем по
 * имени и файлу, затем по имени, если оно единственное. При удаче ссылка «лечится»: id, имя, файл и габарит
 * обновляются, так что переименование и перенос схемы участок не теряют.
 */
@Environment(EnvType.CLIENT)
public final class PlacementMatcher
{
    /** Участок с найденными размещениями: {@code found} — по порядку ссылок, {@code scope} — набор S(section). */
    public record Resolved(Section section, List<SchematicPlacement> found,
                           Map<PlacementRef, SchematicPlacement> byRef, List<SchematicPlacement> scope)
    {
        public boolean isEmpty()
        {
            return this.found.isEmpty();
        }

        public boolean isPartial()
        {
            return ! this.found.isEmpty() && this.found.size() < this.section.placements.size();
        }

        public @Nullable SchematicPlacement first()
        {
            return this.found.isEmpty() ? null : this.found.getFirst();
        }

        public @Nullable SchematicPlacement of(PlacementRef ref)
        {
            return this.byRef.get(ref);
        }
    }

    private PlacementMatcher() {}

    public static @Nullable SchematicPlacement find(PlacementRef ref, List<SchematicPlacement> enabled)
    {
        SchematicPlacement hit = null;

        if (ref.id != null)
        {
            for (SchematicPlacement placement : enabled)
            {
                if (ref.id.equals(placement.getHashId()))
                {
                    hit = placement;
                    break;
                }
            }
        }

        if (hit == null && ref.file != null && ! ref.name.isEmpty())
        {
            for (SchematicPlacement placement : enabled)
            {
                if (ref.name.equals(placement.getName()) && ref.file.equals(FileZones.key(placement)))
                {
                    hit = placement;
                    break;
                }
            }
        }

        if (hit == null && ! ref.name.isEmpty())
        {
            SchematicPlacement only = null;
            int count = 0;

            for (SchematicPlacement placement : enabled)
            {
                if (ref.name.equals(placement.getName()))
                {
                    only = placement;
                    ++count;
                }
            }

            if (count == 1)
            {
                hit = only;
            }
        }

        // Ссылка без ключей (старая ручная зона без схемы): единственное включённое размещение — как у старой getPlacement()
        if (hit == null && ref.isBlank() && enabled.size() == 1)
        {
            hit = enabled.getFirst();
        }

        if (hit != null)
        {
            heal(ref, hit);
        }

        return hit;
    }

    private static void heal(PlacementRef ref, SchematicPlacement placement)
    {
        ref.id = placement.getHashId();
        ref.name = placement.getName();
        ref.file = FileZones.key(placement);
        ZoneBox box = ZoneManager.enclosingBox(placement);

        if (box != null)
        {
            ref.box = box;
        }
    }

    /** Все участки раскладки разом: нужно, чтобы участок без схемы знал, какие размещения уже заняты другими. */
    public static Map<Section, Resolved> resolveAll(Layout layout, List<SchematicPlacement> enabled)
    {
        Map<Section, Resolved> out = new IdentityHashMap<>();
        Set<SchematicPlacement> claimed = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Section> withoutPlacements = new ArrayList<>();

        for (Section section : layout.sections)
        {
            if (section.hasNoPlacements())
            {
                withoutPlacements.add(section);
                continue;
            }

            Map<PlacementRef, SchematicPlacement> byRef = new IdentityHashMap<>();
            List<SchematicPlacement> found = new ArrayList<>();

            for (PlacementRef ref : section.placements)
            {
                SchematicPlacement placement = find(ref, enabled);

                if (placement != null && ! found.contains(placement))
                {
                    byRef.put(ref, placement);
                    found.add(placement);
                    claimed.add(placement);
                }
            }

            out.put(section, new Resolved(section, found, byRef, found));
        }

        for (Section section : withoutPlacements)
        {
            List<SchematicPlacement> scope = new ArrayList<>();

            for (SchematicPlacement placement : enabled)
            {
                if (! claimed.contains(placement))
                {
                    scope.add(placement);
                }
            }

            out.put(section, new Resolved(section, List.of(), Map.of(), scope));
        }

        return out;
    }

    /** Участок, в котором состоит размещение, или {@code null}. */
    public static @Nullable Section sectionOf(Map<Section, Resolved> all, SchematicPlacement placement)
    {
        for (Resolved resolved : all.values())
        {
            if (resolved.found.contains(placement))
            {
                return resolved.section;
            }
        }

        return null;
    }
}
