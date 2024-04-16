// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.actions.mapmode;

import static java.util.function.Predicate.not;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.command.ChangeNodesCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * An action to make sidewalk mapping easier
 */
public class SidewalkMode extends MapMode implements MapFrame.MapModeChangeListener {
    /**
     * Make protected methods available for this class
     */
    private static final class DrawActionCustom extends DrawAction {
        @Override
        public void updateStatusLine() {
            super.updateStatusLine();
        }

        @Override
        public void updateEnabledState() {
            super.updateEnabledState();
        }

        @Override
        public void readPreferences() {
            super.readPreferences();
        }

        @Override
        public void updateKeyModifiers(InputEvent e) {
            super.updateKeyModifiers(e);
        }

        @Override
        public void updateKeyModifiers(MouseEvent e) {
            super.updateKeyModifiers(e);
        }

        @Override
        public void updateKeyModifiers(ActionEvent e) {
            super.updateKeyModifiers(e);
        }

        @Override
        public void updateKeyModifiersEx(int modifiers) {
            super.updateKeyModifiersEx(modifiers);
        }

        @Override
        public void requestFocusInMapView() {
            super.requestFocusInMapView();
        }

        @Override
        public boolean isEditableDataLayer(Layer l) {
            return super.isEditableDataLayer(l);
        }
    }

    private static final String HIGHWAY = "highway";
    private static final String FOOTWAY = "footway";
    private static final String SURFACE = "surface";
    private final DrawActionCustom drawAction = new DrawActionCustom();
    private boolean entered;

    /**
     * Create a new sidewalk action
     */
    public SidewalkMode() {
        super(tr("Sidewalk mode"), "presets/transport/way/way_pedestrian.svg", tr("Draw sidewalks more efficiently"),
                Shortcut.registerShortcut("sidewalk:sidewalk", tr("Sidewalk mode"), KeyEvent.CHAR_UNDEFINED,
                        Shortcut.NONE),
                ImageProvider.getCursor("crosshair", null));
        new ImageProvider("presets/transport/way/way_pedestrian.svg").setOptional(true).getResource()
                .attachImageIcon(this);
        MapFrame.addMapModeChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        this.entered = MainApplication.getMap().mapMode == this;
    }

    @Override
    public void enterMode() {
        this.drawAction.enterMode();
        super.enterMode();

        // Order matters here -- we want to be called ''after'' drawAction does its
        // thing.
        MapFrame map = MainApplication.getMap();
        map.mapView.addMouseListener(this);
        map.mapView.addMouseMotionListener(this);
    }

    @Override
    public void exitMode() {
        this.drawAction.exitMode();
        super.exitMode();

        MapFrame map = MainApplication.getMap();
        map.mapView.removeMouseListener(this);
        map.mapView.removeMouseMotionListener(this);
    }

    @Override
    protected void updateStatusLine() {
        this.drawAction.updateStatusLine();
    }

    @Override
    public String getModeHelpText() {
        return this.drawAction.getModeHelpText();
    }

    @Override
    protected void readPreferences() {
        this.drawAction.readPreferences();
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return this.drawAction.layerIsSupported(l);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        this.updateKeyModifiers(e);
        if (this.ctrl) { // Add nodes uses ctrl to avoid adding node to existing ways.
            return;
        }
        final var undoRedoHandler = UndoRedoHandler.getInstance();
        if (undoRedoHandler == null || undoRedoHandler.getLastCommand() == null
                || undoRedoHandler.getLastCommand().getChildren() == null) {
            return;
        }
        final var way = undoRedoHandler.getLastCommand().getParticipatingPrimitives().stream()
                .filter(Way.class::isInstance).map(Way.class::cast).findFirst().orElse(null);
        if (way == null || way.firstNode() == null) {
            return;
        }
        final var addedNode = undoRedoHandler.getLastCommand().getChildren().stream()
                .filter(AddCommand.class::isInstance).map(AddCommand.class::cast)
                .map(AddCommand::getParticipatingPrimitives).flatMap(Collection::stream).filter(Node.class::isInstance)
                .map(Node.class::cast).findFirst().orElse(way.lastNode());
        final var forwardDirection = way.lastNode().equals(addedNode);

        final var segmentStart = forwardDirection ? way.getNodesCount() - 2 : 0;

        // We want to get the parent ways of the node on the other side.
        final var parentWays = (forwardDirection ? way.firstNode() : way.lastNode()).getParentWays().stream()
                .filter(not(way::equals)).filter(w -> w.hasTag(HIGHWAY, FOOTWAY)).toList();
        if (way.hasTag(HIGHWAY, FOOTWAY) && way.getNodesCount() >= 3
                && way.getNode(forwardDirection ? segmentStart : 1).hasTag("barrier", "kerb")) {
            switchToFootway(way, parentWays, forwardDirection);
        } else if (way.hasTag(HIGHWAY, FOOTWAY) || !parentWays.isEmpty()) {
            final var segment = new WaySegment(way, segmentStart);
            final var crossingWay = segment.toWay();
            final var possibleWays = new ArrayList<>(Optional.ofNullable(way.getDataSet())
                    .orElse(OsmDataManager.getInstance().getEditDataSet()).searchWays(crossingWay.getBBox()));
            possibleWays.remove(way);
            possibleWays.removeIf(w -> !w.hasKey(HIGHWAY));
            for (var possibleCrossing : possibleWays) {
                if (Geometry.getDistanceWayWay(possibleCrossing, crossingWay) == 0
                        && !possibleCrossing.containsNode(crossingWay.firstNode())
                        && !possibleCrossing.containsNode(crossingWay.lastNode())) {
                    // Lanes are somewhere around 3.6m; most roads are going to be less than 3 lanes
                    // one way.
                    // This means 3 lanes/way * 2 ways * 3.6 metres/lane = 21.6 metres should be a
                    // good "max length"
                    // for automatically adding a crossing way (this should avoid situations where
                    // the user is
                    // trying to make a curve and gets a bunch of crossing ways)
                    if (crossingWay.getLength() < Config.getPref().getInt("sidewalk.crossing.maxlength", 22)) {
                        createCrossingWay(way, crossingWay, possibleCrossing, parentWays, forwardDirection);
                    } else {
                        final var commands = new ArrayList<Command>(1);
                        createCrossingNodes(way, possibleCrossing, commands);
                        UndoRedoHandler.getInstance()
                                .add(SequenceCommand.wrapIfNeeded(tr("Create crossing nodes"), commands));
                    }
                }
            }
        }
    }

    /**
     * Switch to drawing a footway from a crossing
     *
     * @param way              The way to split (switch to footway from sidewalk)
     * @param parentWays       The parent ways of the node we are switching to a
     *                         footway
     * @param forwardDirection {@code true} if adding to the end of the way
     */
    private static void switchToFootway(Way way, Collection<Way> parentWays, boolean forwardDirection) {
        // We want to use sidewalk tags from the "right" side of the road
        final var newParentWays = new ArrayList<>((forwardDirection ? way.lastNode() : way.firstNode()).getParentWays()
                .stream().filter(not(way::equals)).filter(w -> w.hasTag(HIGHWAY, FOOTWAY)).toList());
        final var actualWays = newParentWays.isEmpty() ? parentWays : newParentWays;
        final var commands = new ArrayList<Command>(3);
        final var startIndex = forwardDirection ? 0 : 1;
        commands.add(new ChangeNodesCommand(way,
                new ArrayList<>(way.getNodes().subList(startIndex, way.getNodesCount() - 1 + startIndex))));
        final var stubWay = new Way();
        if (forwardDirection) {
            stubWay.addNode(way.getNode(way.getNodesCount() - 2));
            stubWay.addNode(way.lastNode());
        } else {
            stubWay.addNode(way.firstNode());
            stubWay.addNode(way.getNode(1));
        }
        stubWay.putAll(TagCollection.commonToAllPrimitives(actualWays).asList().stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue)));
        commands.add(new AddPrimitivesCommand(Collections.singletonList(stubWay.save()), way.getDataSet()));
        UndoRedoHandler.getInstance().add(SequenceCommand.wrapIfNeeded(tr("Add footway"), commands));
    }

    /**
     * Create the crossing way
     *
     * @param way              The original way
     * @param crossingWay      The new crossing way
     * @param possibleCrossing The way that we may be crossing
     * @param parentWays       The parent ways of the node at the end of the way
     *                         that is not being modified (for tag information)
     * @param forwardDirection {@code true} if adding to the last node of the way
     */
    private void createCrossingWay(Way way, Way crossingWay, Way possibleCrossing, Collection<Way> parentWays,
            boolean forwardDirection) {
        final var undoRedoHandler = UndoRedoHandler.getInstance();
        final var newNodes = new ArrayList<>(way.getNodes());
        final var usuallyRightCommands = new ArrayList<Command>(6);
        final boolean isCrossing = !possibleCrossing.hasTag(HIGHWAY, "pedestrian", FOOTWAY, "path")
                && !(possibleCrossing.hasTag(HIGHWAY, "service")
                        // Service roads with these "common" tags are often small and don't have a real
                        // crossing from a pedestrian perspective. We do want to add crossing
                        // information
                        // when there is no service information.
                        && possibleCrossing.hasTag("service", "alley", "drive-through", "driveway",
                                "emergency_access"));
        crossingWay.put(HIGHWAY, FOOTWAY);
        crossingWay.put(FOOTWAY, isCrossing ? "crossing" : "sidewalk");
        if (possibleCrossing.hasTag(SURFACE)) {
            crossingWay.put(SURFACE, possibleCrossing.get(SURFACE));
        } else if (way.hasTag(SURFACE)) {
            crossingWay.put(SURFACE, way.get(SURFACE));
        }
        newNodes.remove(forwardDirection ? crossingWay.lastNode() : crossingWay.firstNode());
        if (newNodes.isEmpty() || newNodes.size() == 1) {
            usuallyRightCommands.add(DeleteCommand.delete(Collections.singleton(way), false, true));
        } else {
            usuallyRightCommands.add(new ChangeNodesCommand(way, newNodes));
        }
        if (isCrossing && Config.getPref().getBoolean("sidewalk.crossing.kerb", true)) {
            final var tagMap = Config.getPref().getListOfMaps("sidewalk.crossing.kerb.tags").stream().map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (old, n) -> n, TreeMap::new));
            tagMap.putIfAbsent("barrier", "kerb");
            usuallyRightCommands.add(
                    new ChangePropertyCommand(Arrays.asList(crossingWay.firstNode(), crossingWay.lastNode()), tagMap));
        }
        // Now add the intersection node
        final var intersection = createCrossingNodes(crossingWay, possibleCrossing, usuallyRightCommands);
        usuallyRightCommands
                .add(new AddPrimitivesCommand(Collections.singletonList(crossingWay.save()), way.getDataSet()));
        undoRedoHandler.add(SequenceCommand.wrapIfNeeded(tr("Create crossing way"), usuallyRightCommands));
        if (!way.hasKeys() && !parentWays.isEmpty()) {
            undoRedoHandler.add(new ChangePropertyCommand(Collections.singletonList(way),
                    TagCollection.commonToAllPrimitives(parentWays).asList().stream()
                            .collect(Collectors.toMap(Tag::getKey, Tag::getValue))));
        }
        if (isCrossing) {
            undoRedoHandler.add(new ChangePropertyCommand(intersection, HIGHWAY, "crossing"));
        }
        // Needed to continue drawing. It would be nice to pass the original footway
        // tags on, but that isn't
        // currently possible.
        way.getDataSet().setSelected(forwardDirection ? crossingWay.lastNode() : crossingWay.firstNode());
        this.drawAction.updateKeyModifiers(new MouseEvent(MainApplication.getMap(),
                Long.hashCode(System.currentTimeMillis()), System.currentTimeMillis(),
                InputEvent.ALT_DOWN_MASK | (this.ctrl ? InputEvent.CTRL_DOWN_MASK : 0)
                        | (this.shift ? InputEvent.SHIFT_DOWN_MASK : 0) | (this.meta ? InputEvent.META_DOWN_MASK : 0),
                0, 0, 0, false));
    }

    private static Set<Node> createCrossingNodes(Way crossingWay, Way possibleCrossing, Collection<Command> commands) {
        final var intersectionCommands = new ArrayList<Command>(3);
        final var intersection = Geometry.addIntersections(Arrays.asList(possibleCrossing, crossingWay), false,
                intersectionCommands);
        if (intersection.size() == 1) {
            final var node = intersection.iterator().next();
            // The crossing way isn't part of the dataset yet, and we add it by "saving" the
            // crossing way.
            if (crossingWay.getDataSet() == null) {
                crossingWay.addNode(Geometry.getClosestWaySegment(crossingWay, node).getUpperIndex(), node);
                intersectionCommands.removeIf(command -> command.getParticipatingPrimitives().contains(crossingWay));
            }
            commands.addAll(intersectionCommands);
        }
        return intersection;
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        super.preferenceChanged(e);
        this.drawAction.preferenceChanged(e);
    }

    @Override
    protected boolean isEditableDataLayer(Layer l) {
        return this.drawAction.isEditableDataLayer(l);
    }

    @Override
    public void mapModeChange(MapMode oldMapMode, MapMode newMapMode) {
        if (oldMapMode == this && newMapMode instanceof DrawAction) {
            this.entered = false;
        } else if (this.entered && newMapMode instanceof DrawAction) {
            this.actionPerformed(null);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        MapFrame.removeMapModeChangeListener(this);
    }
}
