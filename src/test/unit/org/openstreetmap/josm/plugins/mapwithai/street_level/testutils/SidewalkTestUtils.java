// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Various utilities for tests
 */
public final class SidewalkTestUtils {
    private SidewalkTestUtils() {
        // Hide the constructor
    }

    /**
     * Create a new way (uses {@link TestUtils#newWay(String, Node...)} under the
     * hood)
     *
     * @param tags        The tags to use
     * @param coordinates The coordinates (lat, lon, lat, lon, ...)
     * @return The new way
     */
    public static Way newWay(String tags, double... coordinates) {
        assertEquals(0, coordinates.length % 2);
        final var nodes = new Node[coordinates.length / 2];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new Node(new LatLon(coordinates[2 * i], coordinates[2 * i + 1]));
        }
        return TestUtils.newWay(tags, nodes);
    }
}
