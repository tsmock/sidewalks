// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.actions.mapmode;

import static java.util.function.Predicate.not;
import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JOptionPane;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
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
        private final Cursor cursorCopy = this.cursor;

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

    private static final String CROSSING = "crossing";
    private static final String FOOTWAY = "footway";
    private static final String STEPS = "steps";
    private static final String HIGHWAY = "highway";
    private static final String SIDEWALK = "sidewalk";
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
                ImageProvider.getCursor("crosshair", SIDEWALK));
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
        new Notification(tr("How to exit {0}:<br/>Enter {1} mode twice (shortcut {2}).", this.getValue(NAME),
                this.drawAction.getValue(NAME), this.drawAction.getShortcut().toString()))
                        .setIcon(JOptionPane.INFORMATION_MESSAGE).show();
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
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        final var currentCursor = MainApplication.getMap().mapView.getCursor();
        if (currentCursor.equals(this.drawAction.cursorCopy)) {
            MainApplication.getMap().mapView.setNewCursor(this.cursor, this);
        }
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
                .filter(Way.class::isInstance).map(Way.class::cast).reduce((w1, w2) -> w1.isSelected() ? w1 : w2)
                .orElse(null);
        if (way == null || way.firstNode() == null) {
            return;
        }
        final var addedNode = undoRedoHandler.getLastCommand().getChildren().stream()
                .filter(AddCommand.class::isInstance).map(AddCommand.class::cast)
                .map(AddCommand::getParticipatingPrimitives).flatMap(Collection::stream).filter(Node.class::isInstance)
                .map(Node.class::cast).findFirst().orElse(way.lastNode());
        // Add sidewalk keys to a *new* way, but not if the user has explicitly undone
        // the tag add.
        if (way.getNodesCount() == 2 && !way.hasKeys() && (!undoRedoHandler.hasRedoCommands()
                || !(undoRedoHandler.getRedoCommands().get(0)instanceof ChangePropertyCommand changePropertyCommand
                        && Map.of(HIGHWAY, FOOTWAY, FOOTWAY, SIDEWALK).equals(changePropertyCommand.getTags())
                        && changePropertyCommand.getParticipatingPrimitives().contains(way)))) {
            undoRedoHandler.add(
                    new ChangePropertyCommand(Collections.singleton(way), Map.of(HIGHWAY, FOOTWAY, FOOTWAY, SIDEWALK)));
        }
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
            final var sidewalkLayer = OsmUtils.getLayer(way);
            for (var possibleCrossing : possibleWays) {
                if (Objects.equals(OsmUtils.getLayer(possibleCrossing), sidewalkLayer)
                        && Geometry.getDistanceWayWay(possibleCrossing, crossingWay) == 0
                        && !possibleCrossing.containsNode(crossingWay.firstNode())
                        && !possibleCrossing.containsNode(crossingWay.lastNode())) {
                    // Lanes are somewhere around 3.6m; most roads are going to be less than 3 lanes
                    // one way. This means 3 lanes/way * 2 ways * 3.6 metres/lane = 21.6 metres
                    // should be a good "max length" for automatically adding a crossing way (this
                    // should avoid situations where the user is trying to make a curve and gets a
                    // bunch of crossing ways). Rounded up to 30m due to feedback.
                    if (crossingWay.getLength() < Config.getPref().getInt("sidewalk.crossing.maxlength", 30)) {
                        createCrossingWay(way, crossingWay, possibleCrossing, parentWays, forwardDirection);
                    } else {
                        final var commands = new ArrayList<Command>(1);
                        createCrossingNodes(way, possibleCrossing, commands);
                        if (!commands.isEmpty()) {
                            undoRedoHandler.add(SequenceCommand.wrapIfNeeded(tr("Create crossing nodes"), commands));
                        }
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
        final boolean isCrossing = !possibleCrossing.hasTag(HIGHWAY, "pedestrian", FOOTWAY, "path", STEPS)
                && !(possibleCrossing.hasTag(HIGHWAY, "service")
                        // Service roads with these "common" tags are often small and don't have a real
                        // crossing from a pedestrian perspective. We do want to add crossing
                        // information when there is no service information.
                        && possibleCrossing.hasTag("service", "alley", "drive-through", "driveway",
                                "emergency_access"));
        crossingWay.put(HIGHWAY, FOOTWAY);
        crossingWay.put(FOOTWAY, isCrossing ? CROSSING : SIDEWALK);
        if (possibleCrossing.hasTag(SURFACE)) {
            crossingWay.put(SURFACE, possibleCrossing.get(SURFACE));
        } else if (way.hasTag(SURFACE)) {
            crossingWay.put(SURFACE, way.get(SURFACE));
        }
        // This is necessary when we are crossing _multiple_ ways
        if (Geometry.getDistanceWayWay(way, possibleCrossing) == 0) {
            newNodes.remove(forwardDirection ? newNodes.size() - 1 : 0);
        }
        if (newNodes.isEmpty() || newNodes.size() == 1) {
            usuallyRightCommands.add(DeleteCommand.delete(Collections.singleton(way), false, true));
        } else {
            usuallyRightCommands.add(new ChangeNodesCommand(way, newNodes));
        }
        addKerbTagging(way, crossingWay, usuallyRightCommands, isCrossing);
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
            intersection.stream().filter(n -> n.getDataSet() == null).forEach(n -> n.put(HIGHWAY, CROSSING));
            intersection.removeIf(n -> n.getDataSet() == null);
            if (!intersection.isEmpty()) {
                undoRedoHandler.add(new ChangePropertyCommand(intersection, HIGHWAY, CROSSING));
            }
        }
        // Needed to continue drawing. It would be nice to pass the original footway
        // tags on, but that isn't currently possible.
        way.getDataSet().setSelected(forwardDirection ? crossingWay.lastNode() : crossingWay.firstNode());
        this.drawAction.updateKeyModifiers(new MouseEvent(MainApplication.getMap(),
                Long.hashCode(System.currentTimeMillis()), System.currentTimeMillis(),
                InputEvent.ALT_DOWN_MASK | (this.ctrl ? InputEvent.CTRL_DOWN_MASK : 0)
                        | (this.shift ? InputEvent.SHIFT_DOWN_MASK : 0) | (this.meta ? InputEvent.META_DOWN_MASK : 0),
                0, 0, 0, false));
    }

    private static void addKerbTagging(Way originalWay, Way crossingWay, Collection<Command> usuallyRightCommands,
            boolean isCrossing) {
        if (isCrossing && Config.getPref().getBoolean("sidewalk.crossing.kerb", true)) {
            final var tagMap = Config.getPref().getListOfMaps("sidewalk.crossing.kerb.tags").stream().map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (old, n) -> n, TreeMap::new));
            tagMap.putIfAbsent("barrier", "kerb");
            final var changingNodes = Stream.of(crossingWay.firstNode(), crossingWay.lastNode())
                    .filter(Objects::nonNull).filter(node -> !inMiddleOfSidewalk(originalWay, crossingWay, node))
                    .toList();
            if (!changingNodes.isEmpty()) {
                usuallyRightCommands.add(new ChangePropertyCommand(changingNodes, tagMap));
            }
        }
    }

    private static boolean inMiddleOfSidewalk(Way originalWay, Way crossingWay, Node node) {
        final var footways = node.getParentWays().stream().filter(not(crossingWay::equals))
                .filter(not(originalWay::equals)).filter(way -> way.hasTag(HIGHWAY, FOOTWAY)).toList();
        if (footways.isEmpty()) {
            return false;
        } else if (footways.size() >= 2) {
            return true;
        }
        final var footway = footways.get(0);
        return footway.isClosed() || footway.isInnerNode(node);
    }

    private static Set<Node> createCrossingNodes(Way crossingWay, Way possibleCrossing, Collection<Command> commands) {
        final var intersectionCommands = new ArrayList<Command>(3);
        final var intersection = Geometry.addIntersections(Arrays.asList(possibleCrossing, crossingWay), false,
                intersectionCommands);
        if (intersection.size() == 1) {
            var node = intersection.iterator().next();
            final var crossingSegment = Geometry.getClosestWaySegment(possibleCrossing, node);
            final var maxCrossingDistance = Config.getPref().getDouble("sidewalk.crossing.node.maxdistance", 6);
            // Check if the crossing segment has a node with crossing tags already
            final var closestCrossing = Stream.of(crossingSegment.getFirstNode(), crossingSegment.getSecondNode())
                    .filter(n -> n.hasTag(HIGHWAY, CROSSING) && n.getParentWays().size() == 1)
                    .min(Comparator.comparingDouble(node::distanceSq));
            boolean changeNodes = false;
            if (closestCrossing.isPresent() && node.greatCircleDistance(closestCrossing.get()) < maxCrossingDistance) {
                node = closestCrossing.get();
                changeNodes = true;
            } else {
                // Then check for a very close node ''without'' other tags
                final var dupeNodeDistance = Config.getPref().getDouble("sidewalk.crossing.node.dupedistance", 1);
                final var closestNode = Stream.of(crossingSegment.getFirstNode(), crossingSegment.getSecondNode())
                        .filter(n -> n.getParentWays().size() == 1 && !n.isTagged())
                        .min(Comparator.comparingDouble(node::distanceSq)).orElse(null);
                if (closestNode != null && node.greatCircleDistance(closestNode) < dupeNodeDistance) {
                    node = closestNode;
                    changeNodes = true;
                }
            }
            if (changeNodes) {
                final var nodes = new ArrayList<>(crossingWay.getNodes());
                nodes.add(Geometry.getClosestWaySegment(crossingWay, node).getUpperIndex(), node);
                intersectionCommands.clear();
                if (crossingWay.getDataSet() != null) {
                    intersectionCommands.add(new ChangeNodesCommand(crossingWay, nodes));
                }
                intersection.clear();
                intersection.add(node);
            }
            if (crossingWay.getDataSet() == null) {
                // The crossing way isn't part of the dataset yet, and we add it by "saving" the
                // crossing way.
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
