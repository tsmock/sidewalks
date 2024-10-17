// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.testutils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.ILatLon;
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

    /**
     * Check that two latlons are equal with the default server precision
     * {@link ILatLon#MAX_SERVER_PRECISION}.
     *
     * @param expected The expected latlon
     * @param actual   The actual latlon
     */
    public static void assertLatLonEquals(ILatLon expected, ILatLon actual) {
        assertLatLonEquals(expected.lat(), expected.lon(), actual.lat(), actual.lon());
    }

    /**
     * Check that two latlons are equal with the default server precision
     * {@link ILatLon#MAX_SERVER_PRECISION}.
     *
     * @param expectedLat The expected latitude
     * @param expectedLon The expected longitude
     * @param actualLat   The actual latitude
     * @param actualLon   The actual longitude
     */
    public static void assertLatLonEquals(double expectedLat, double expectedLon, double actualLat, double actualLon) {
        assertLatLonEquals(expectedLat, expectedLon, actualLat, actualLon, ILatLon.MAX_SERVER_PRECISION);
    }

    /**
     * Check that two latlons are equal to a specified precision.
     *
     * @param expectedLat The expected latitude
     * @param expectedLon The expected longitude
     * @param actualLat   The actual latitude
     * @param actualLon   The actual longitude
     * @param delta       The maximum delta between each coordinate. Usually
     *                    {@link ILatLon#MAX_SERVER_PRECISION} is good enough.
     */
    public static void assertLatLonEquals(double expectedLat, double expectedLon, double actualLat, double actualLon,
            double delta) {
        assertAll("Expected " + expectedLat + "," + expectedLon + " but was " + actualLat + "," + actualLon,
                () -> assertEquals(expectedLat, actualLat, delta), () -> assertEquals(expectedLon, actualLon, delta));
    }
}
