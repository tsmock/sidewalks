// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.SidewalkTestUtils.assertLatLonEquals;
import static org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.SidewalkTestUtils.newWay;

import java.text.MessageFormat;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link ParallelSidewalkCreation}
 *
 * @author Taylor Smock
 */
@Projection
class ParallelSidewalkCreationTest {

    @Test
    void testFirstHorizontal() {
        Way way1 = newWay("", 39.1693132, -108.7186835, 39.1693101, -108.7175632, 39.1693223, -108.7166861);
        Map<ParallelSidewalkCreation.Options, Way> parallelSidewalks = ParallelSidewalkCreation
                .createParallelSidewalks(way1, ParallelSidewalkCreation.Options.LEFT);
        assertEquals(1, parallelSidewalks.size());
        double degrees = Math.toDegrees(parallelSidewalks.get(ParallelSidewalkCreation.Options.LEFT).firstNode()
                .getEastNorth().heading(way1.lastNode().getEastNorth()));
        assertTrue(degrees < 182 && degrees > 178,
                MessageFormat.format("Expected degrees to be {0} but was {1}", "< 182 and > 178", degrees));
        parallelSidewalks = ParallelSidewalkCreation.createParallelSidewalks(way1,
                ParallelSidewalkCreation.Options.RIGHT);
        assertEquals(1, parallelSidewalks.size());
        degrees = Math.toDegrees(parallelSidewalks.get(ParallelSidewalkCreation.Options.RIGHT).firstNode()
                .getEastNorth().heading(way1.firstNode().getEastNorth()));
        assertTrue(degrees > 358 || degrees < 2,
                MessageFormat.format("Expected degrees to be {0} but was {1}", "> 358 or < 2", degrees));
    }

    @Test
    void testMiddleHorizontal() {
        Way way1 = newWay("", 39.1693132, -108.7186835, 39.1693101, -108.7175632, 39.1693223, -108.7166861);
        Map<ParallelSidewalkCreation.Options, Way> parallelSidewalks = ParallelSidewalkCreation
                .createParallelSidewalks(way1, ParallelSidewalkCreation.Options.LEFT);
        assertEquals(1, parallelSidewalks.size());
        double degrees = Math.toDegrees(parallelSidewalks.get(ParallelSidewalkCreation.Options.LEFT).getNode(1)
                .getEastNorth().heading(way1.getNode(1).getEastNorth()));
        assertTrue(degrees < 182 && degrees > 178,
                MessageFormat.format("Expected degrees to be {0} but was {1}", "< 182 and > 178", degrees));
        parallelSidewalks = ParallelSidewalkCreation.createParallelSidewalks(way1,
                ParallelSidewalkCreation.Options.RIGHT);
        assertEquals(1, parallelSidewalks.size());
        degrees = Math.toDegrees(parallelSidewalks.get(ParallelSidewalkCreation.Options.RIGHT).getNode(1).getEastNorth()
                .heading(way1.getNode(1).getEastNorth()));
        assertTrue(degrees > 358 || degrees < 2,
                MessageFormat.format("Expected degrees to be {0} but was {1}", "> 358 or < 2", degrees));
    }

    @Test
    void testLastHorizontal() {
        Way way1 = newWay("", 39.1693132, -108.7186835, 39.1693101, -108.7175632, 39.1693223, -108.7166861);
        Map<ParallelSidewalkCreation.Options, Way> parallelSidewalks = ParallelSidewalkCreation
                .createParallelSidewalks(way1, ParallelSidewalkCreation.Options.LEFT);
        assertEquals(1, parallelSidewalks.size());
        double degrees = Math.toDegrees(parallelSidewalks.get(ParallelSidewalkCreation.Options.LEFT).lastNode()
                .getEastNorth().heading(way1.firstNode().getEastNorth()));
        assertTrue(degrees < 182 && degrees > 178,
                MessageFormat.format("Expected degrees to be {0} but was {1}", "< 182 and > 178", degrees));
        parallelSidewalks = ParallelSidewalkCreation.createParallelSidewalks(way1,
                ParallelSidewalkCreation.Options.RIGHT);
        assertEquals(1, parallelSidewalks.size());
        degrees = Math.toDegrees(parallelSidewalks.get(ParallelSidewalkCreation.Options.RIGHT).lastNode().getEastNorth()
                .heading(way1.lastNode().getEastNorth()));
        assertTrue(degrees > 358 || degrees < 2,
                MessageFormat.format("Expected degrees to be {0} but was {1}", "> 358 or < 2", degrees));
    }

    @Test
    void testClosedWay() {
        Way way1 = newWay("", 39.0680771, -108.5655669, 39.0679765, -108.5655681, 39.0679718, -108.5652099, 39.0680968,
                -108.5652026);
        way1.addNode(way1.firstNode());

        Map<ParallelSidewalkCreation.Options, Way> parallelSidewalks = ParallelSidewalkCreation
                .createParallelSidewalks(way1, ParallelSidewalkCreation.Options.LEFT);
        assertEquals(1, parallelSidewalks.size());
        var sidewalk = parallelSidewalks.values().iterator().next();
        assertSame(sidewalk.firstNode(), sidewalk.lastNode());
        assertLatLonEquals(39.0680417, -108.5655185, sidewalk.firstNode().lat(), sidewalk.firstNode().lon());

        parallelSidewalks = ParallelSidewalkCreation.createParallelSidewalks(way1,
                ParallelSidewalkCreation.Options.RIGHT);
        assertEquals(1, parallelSidewalks.size());
        sidewalk = parallelSidewalks.values().iterator().next();
        assertSame(sidewalk.firstNode(), sidewalk.lastNode());
        assertLatLonEquals(39.0681125, -108.5656153, sidewalk.firstNode().lat(), sidewalk.firstNode().lon());
    }
}
