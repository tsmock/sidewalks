// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2022 Taylor Smock <tsmock@fb.com>
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
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

/**
 * Create sidewalks parallel to a way
 *
 * @author Taylor Smock
 */
public final class ParallelSidewalkCreation {
    private static final Pattern NUMBER_REGEX = Pattern.compile("[0-9]*[.]?[0-9]*");

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
            if (NUMBER_REGEX.matcher(widthValue).matches()) {
                width = Float.parseFloat(widthValue) + 3; // 3m for sidewalks
            } else {
                // TODO something better
                throw new JosmRuntimeException("Width not yet understood");
            }
        } else {
            width = 11.5f; // 8.5m for road, another 3m for sidewalk
        }
        List<N> nodes = way.getNodes();
        Map<Options, List<LatLon>> sidewalkLatLons = new EnumMap<>(Options.class);
        if (Arrays.asList(options).contains(Options.RIGHT)) {
            sidewalkLatLons.put(Options.RIGHT, createParallelNodes(nodes, width / 2));
        }
        if (Arrays.asList(options).contains(Options.LEFT)) {
            Collections.reverse(nodes);
            sidewalkLatLons.put(Options.LEFT, createParallelNodes(nodes, width / 2));
        }

        // Suppress "unchecked" warnings here, since these are already typed
        @SuppressWarnings("unchecked")
        Class<N> nodeClass = (Class<N>) way.firstNode().getClass();
        @SuppressWarnings("unchecked")
        Class<W> wayClass = (Class<W>) way.getClass();
        try {
            Map<Options, W> sidewalkList = new EnumMap<>(Options.class);
            nodeClass.getConstructor().newInstance();
            for (Map.Entry<Options, List<LatLon>> sidewalk : sidewalkLatLons.entrySet()) {
                List<N> tWayNodes = new ArrayList<>(sidewalk.getValue().size());
                for (LatLon latLon : sidewalk.getValue()) {
                    N tNode = nodeClass.getConstructor().newInstance();
                    tNode.setCoor(latLon);
                    tWayNodes.add(tNode);
                }
                W tWay = wayClass.getConstructor().newInstance();
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
     * Create a list of parallel nodes
     *
     * @param nodes The nodes to create a parallel list (right)
     * @param <N>   The node type
     * @return The parallel nodes
     */
    private static <N extends INode> List<LatLon> createParallelNodes(final List<N> nodes, final float offset) {
        final List<LatLon> latLons = new ArrayList<>(nodes.size());

        double angle = nodes.get(0).getEastNorth().heading(nodes.get(1).getEastNorth()) + Math.PI / 2;
        latLons.add(getLatLon(nodes.get(0).getCoor(), angle, offset));
        // Every node beside first/last need three other nodes
        for (int i = 1; i < nodes.size() - 1; i++) {
            // Note: getCornerAngle is (node1, commonNode, node2), angleIsClockwise is
            // (commonNode, node1, node2)
            final double cornerAngle = Geometry.getCornerAngle(nodes.get(i - 1).getEastNorth(),
                    nodes.get(i).getEastNorth(), nodes.get(i + 1).getEastNorth());
            final boolean isClockwise = Geometry.angleIsClockwise(nodes.get(i), nodes.get(i - 1), nodes.get(i + 1));
            final double lastAngle = nodes.get(i).getEastNorth().heading(nodes.get(i + 1).getEastNorth());
            if (isClockwise) {
                angle = lastAngle + (2 * Math.PI - cornerAngle) / 2;
            } else {
                angle = lastAngle - cornerAngle / 2;
            }
            latLons.add(getLatLon(nodes.get(i).getCoor(), angle, offset));
        }
        angle = nodes.get(nodes.size() - 2).getEastNorth().heading(nodes.get(nodes.size() - 1).getEastNorth())
                + Math.PI / 2;
        latLons.add(getLatLon(nodes.get(nodes.size() - 1).getCoor(), angle, offset));

        return latLons;
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
