// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Synchronize crossing way and crossing node tags
 */
public class CrossingCommandListener implements UndoRedoHandler.CommandQueuePreciseListener {
    private static final String HIGHWAY = "highway";
    private static final String FOOTWAY = "footway";
    private static final String CROSSING = "crossing";
    private static final String[] COMMON_TAGS = new String[] { "bicycle", CROSSING, "crossing:continuous",
            "crossing:markings", "crossing:signals", "crossing_ref", "cycleway", "flashing_lights", FOOTWAY, HIGHWAY,
            "horse", "path", "railway", "segregated", "traffic_signals:sound", "traffic_signals:vibration", };

    @Override
    public void commandAdded(UndoRedoHandler.CommandAddedEvent e) {
        if (!Config.getPref().getBoolean("sidewalk.crossing.sync", true)) {
            return;
        }
        final Command command;
        if (e.getCommand()instanceof ChangePropertyCommand changePropertyCommand) {
            command = processChangePropertyCommand(changePropertyCommand);
        } else if (e.getCommand()instanceof SequenceCommand sequenceCommand) {
            final var commands = sequenceCommand.getChildren().stream().filter(ChangePropertyCommand.class::isInstance)
                    .map(ChangePropertyCommand.class::cast).map(CrossingCommandListener::processChangePropertyCommand)
                    .filter(Objects::nonNull).toList();
            command = commands.isEmpty() ? null
                    : SequenceCommand.wrapIfNeeded(tr("Synchronize crossing tags"), commands);
        } else {
            return;
        }
        if (command != null) {
            UndoRedoHandler.getInstance().add(command);
        }
    }

    private static Command processChangePropertyCommand(ChangePropertyCommand changePropertyCommand) {
        final var newTags = getCommonCrossingTags(changePropertyCommand.getTags());
        if (!newTags.isEmpty() && changePropertyCommand.getParticipatingPrimitives().size() == 1) {
            final var changed = changePropertyCommand.getParticipatingPrimitives().iterator().next();
            newTags.putAll(getCommonCrossingTags(changed.getKeys()));
            final Collection<OsmPrimitive> linked;
            if (changed instanceof Way footway) {
                linked = processWay(newTags, footway);
            } else if (changed instanceof Node crossing) {
                linked = processNode(newTags, crossing);
            } else {
                // Relations are not supported for syncing crossing tags
                return null;
            }
            if (!newTags.entrySet().stream().allMatch(
                    entry -> linked.stream().allMatch(link -> link.hasTag(entry.getKey(), entry.getValue())))) {
                return new ChangePropertyCommand(linked, newTags);
            }
        }
        return null;
    }

    private static Collection<OsmPrimitive> processWay(Map<String, String> newTags, Way footway) {
        if (newTags.containsKey(FOOTWAY)) {
            newTags.put(HIGHWAY, newTags.remove(FOOTWAY));
        } else {
            newTags.remove(HIGHWAY); // Don't copy the highway tag over
        }
        return footway.getNodes().stream().filter(node -> node.hasTag(HIGHWAY, CROSSING)).collect(Collectors.toSet());
    }

    private static Collection<OsmPrimitive> processNode(Map<String, String> newTags, Node crossing) {
        if (newTags.containsKey(HIGHWAY))
            newTags.put(FOOTWAY, newTags.remove(HIGHWAY));
        return crossing.getParentWays().stream()
                .filter(way -> way.hasTag(HIGHWAY, FOOTWAY) && way.hasTag(FOOTWAY, CROSSING))
                .collect(Collectors.toSet());
    }

    private static Map<String, String> getCommonCrossingTags(Map<String, String> object) {
        final var crossingMap = new TreeMap<String, String>();
        for (var key : COMMON_TAGS) {
            if (object.containsKey(key)) {
                crossingMap.put(key, object.get(key));
            }
        }
        return crossingMap;
    }

    @Override
    public void cleaned(UndoRedoHandler.CommandQueueCleanedEvent e) {
        // Don't care
    }

    @Override
    public void commandUndone(UndoRedoHandler.CommandUndoneEvent e) {
        // Don't care
    }

    @Override
    public void commandRedone(UndoRedoHandler.CommandRedoneEvent e) {
        // Don't care
    }
}
