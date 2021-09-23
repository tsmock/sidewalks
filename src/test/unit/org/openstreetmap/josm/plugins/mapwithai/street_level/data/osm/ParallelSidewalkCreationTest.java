// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Test class for {@link ParallelSidewalkCreation}
 *
 * @author Taylor Smock
 */
class ParallelSidewalkCreationTest {
    @RegisterExtension
    JOSMTestRules josmTestRules = new JOSMTestRules().projection();

    @Test
    void testFirstHorizontal() {
        Node node1 = new Node(new LatLon(39.1693132, -108.7186835));
        Node node2 = new Node(new LatLon(39.1693101, -108.7175632));
        Node node3 = new Node(new LatLon(39.1693223, -108.7166861));
        Way way1 = new Way();
        way1.setNodes(Arrays.asList(node1, node2, node3));
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
        Node node1 = new Node(new LatLon(39.1693132, -108.7186835));
        Node node2 = new Node(new LatLon(39.1693101, -108.7175632));
        Node node3 = new Node(new LatLon(39.1693223, -108.7166861));
        Way way1 = new Way();
        way1.setNodes(Arrays.asList(node1, node2, node3));
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
        Node node1 = new Node(new LatLon(39.1693132, -108.7186835));
        Node node2 = new Node(new LatLon(39.1693101, -108.7175632));
        Node node3 = new Node(new LatLon(39.1693223, -108.7166861));
        Way way1 = new Way();
        way1.setNodes(Arrays.asList(node1, node2, node3));
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
}
