// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;

/**
 * Create sidewalks parallel to a way
 *
 * @author Taylor Smock
 */
public final class ParallelSidewalkCreation {
    private static final Pattern NUMBER_REGEX = Pattern.compile("\\d*\\.?\\d*");
    private static final Pattern KM_NUMBER_REGEX = Pattern.compile(NUMBER_REGEX.pattern() + " km");
    private static final Pattern MI_NUMBER_REGEX = Pattern.compile(NUMBER_REGEX.pattern() + " mi");
    private static final Pattern NMI_NUMBER_REGEX = Pattern.compile(NUMBER_REGEX.pattern() + " nmi");
    private static final Pattern FT_IN_NUMBER_REGEX = Pattern
            .compile("(" + NUMBER_REGEX.pattern() + "')?(" + NUMBER_REGEX.pattern() + "\")?");

    private ParallelSidewalkCreation() {
        // Hide the constructor
    }

    /**
     * Create parallel sidewalks
     *
     * @param way The way to create parallel sidewalks for
     * @param <N> The node type
     * @param <W> The way type
     * @return A collection of ways (untagged, you may want to open up the preset
     *         editor for these)
     */
    public static <N extends INode, W extends IWay<N>> Map<Options, W> createParallelSidewalks(final W way,
            final Options... options) {
        Objects.requireNonNull(way);
        Objects.requireNonNull(options, "There must be at least one option");
        // TODO account for placement tags, meters/feet/whatever
        final float width;
        if (way.hasKey("width")) {
            String widthValue = way.get("width");
            width = parseWidth(widthValue) + 3; // 3m for sidewalks
        } else {
            width = 11.5f; // 8.5m for road, another 3m for sidewalk
        }
        List<N> nodes = way.getNodes();
        Map<Options, List<ILatLon>> sidewalkLatLons = new EnumMap<>(Options.class);
        if (Arrays.asList(options).contains(Options.RIGHT)) {
            sidewalkLatLons.put(Options.RIGHT, createParallelNodes(nodes, width / 2, way.isClosed()));
        }
        if (Arrays.asList(options).contains(Options.LEFT)) {
            Collections.reverse(nodes);
            sidewalkLatLons.put(Options.LEFT, createParallelNodes(nodes, width / 2, way.isClosed()));
        }

        // Suppress "unchecked" warnings here, since these are already typed
        @SuppressWarnings("unchecked")
        Class<N> nodeClass = (Class<N>) way.firstNode().getClass();
        @SuppressWarnings("unchecked")
        Class<W> wayClass = (Class<W>) way.getClass();
        try {
            final var nodeConstructor = nodeClass.getConstructor();
            final var wayConstructor = wayClass.getConstructor();
            Map<Options, W> sidewalkList = new EnumMap<>(Options.class);
            for (Map.Entry<Options, List<ILatLon>> sidewalk : sidewalkLatLons.entrySet()) {
                List<N> tWayNodes = new ArrayList<>(sidewalk.getValue().size());
                for (ILatLon latLon : sidewalk.getValue()) {
                    N tNode = nodeConstructor.newInstance();
                    tNode.setCoor(latLon instanceof LatLon ll ? ll : new LatLon(latLon.lat(), latLon.lon()));
                    tWayNodes.add(tNode);
                }
                if (tWayNodes.get(0).equalsEpsilon(tWayNodes.get(tWayNodes.size() - 1))) {
                    tWayNodes.remove(tWayNodes.size() - 1);
                    tWayNodes.add(tWayNodes.get(0));
                }
                W tWay = wayConstructor.newInstance();
                tWay.setNodes(tWayNodes);
                sidewalkList.put(sidewalk.getKey(), tWay);
            }
            return sidewalkList;
        } catch (ReflectiveOperationException e) {
            Logging.error(e);
            return Collections.emptyMap();
        }
    }

    /**
     * Parse the width value
     *
     * @param widthValue The width to parse
     * @return The width in meters
     */
    private static float parseWidth(String widthValue) {
        if (NUMBER_REGEX.matcher(widthValue).matches()) {
            return Float.parseFloat(widthValue);
        } else if (KM_NUMBER_REGEX.matcher(widthValue).matches()) {
            return Float.parseFloat(widthValue.substring(0, widthValue.length() - 3)) * 1000;
        } else if (MI_NUMBER_REGEX.matcher(widthValue).matches()) {
            return Float.parseFloat(widthValue.substring(0, widthValue.length() - 3)) * 1609.344f;
        } else if (NMI_NUMBER_REGEX.matcher(widthValue).matches()) {
            return Float.parseFloat(widthValue.substring(0, widthValue.length() - 4)) * 1852;
        } else if (FT_IN_NUMBER_REGEX.matcher(widthValue).matches()) {
            final var matcher = FT_IN_NUMBER_REGEX.matcher(widthValue);
            // Find the match. Again.
            if (matcher.matches()) {
                final var feet = matcher.group(1) == null ? 0 : Float.parseFloat(matcher.group(1).replace("'", ""));
                final var inches = matcher.group(2) == null ? 0 : Float.parseFloat(matcher.group(2).replace("\"", ""));
                return (12 * feet + inches) * 0.0254f;
            } else {
                throw new IllegalStateException("This should never be hit");
            }
        } else {
            throw new IllegalArgumentException("Width not yet understood: " + widthValue);
        }
    }

    /**
     * Create a list of parallel nodes
     *
     * @param nodes  The nodes to create a parallel list (right)
     * @param <N>    The node type
     * @param closed If the nodes are "closed"
     * @return The parallel nodes
     */
    private static <N extends INode> List<ILatLon> createParallelNodes(final List<N> nodes, final float offset,
            final boolean closed) {
        final List<ILatLon> latLons = new ArrayList<>(nodes.size());

        double angle = nodes.get(0).getEastNorth().heading(nodes.get(1).getEastNorth()) + Math.PI / 2;
        latLons.add(getLatLon(nodes.get(0).getCoor(), angle, offset));
        // Every node beside first/last need three other nodes
        for (int i = 1; i < nodes.size() - 1; i++) {
            latLons.add(generateLatLon(offset, nodes.get(i - 1), nodes.get(i), nodes.get(i + 1)));
        }
        if (!closed || latLons.size() < 3) {
            angle = nodes.get(nodes.size() - 2).getEastNorth().heading(nodes.get(nodes.size() - 1).getEastNorth())
                    + Math.PI / 2;
            latLons.add(getLatLon(nodes.get(nodes.size() - 1).getCoor(), angle, offset));
        } else {
            latLons.remove(0);
            final var first = nodes.get(nodes.size() - 2);
            final var second = nodes.get(0);
            final var third = nodes.get(1);

            latLons.add(0, generateLatLon(offset, first, second, third));
            latLons.add(latLons.get(0));
        }

        return latLons;
    }

    private static ILatLon generateLatLon(final float offset, final INode first, final INode second,
            final INode third) {
        final double angle;
        // Note: getCornerAngle is (node1, commonNode, node2), angleIsClockwise is
        // (commonNode, node1, node2)
        final double cornerAngle = Geometry.getCornerAngle(first.getEastNorth(), second.getEastNorth(),
                third.getEastNorth());
        final boolean isClockwise = Geometry.angleIsClockwise(second, first, third);
        final double lastAngle = second.getEastNorth().heading(third.getEastNorth());
        if (isClockwise) {
            angle = lastAngle + (2 * Math.PI - cornerAngle) / 2;
        } else {
            angle = lastAngle - cornerAngle / 2;
        }
        return getLatLon(second, angle, offset);
    }

    /**
     * Create a new LatLon
     *
     * @param original The originating point
     * @param angle    The angle (from true north) in radians
     * @param offset   The distance to the new point
     * @return The new latlon
     */
    private static LatLon getLatLon(final ILatLon original, final double angle, final float offset) {
        ILatLon iLatLon = Geometry.getLatLonFrom(original, angle, offset);
        if (iLatLon instanceof LatLon) {
            return (LatLon) iLatLon;
        }
        return new LatLon(iLatLon);
    }

    /**
     * Options for creating parallel sidewalks
     */
    public enum Options {
        /** Create a sidewalk on the right */
        RIGHT,
        /** Create a sidewalk on the left */
        LEFT
    }
}
