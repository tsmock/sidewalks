// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.SidewalkTestUtils.newWay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.testutils.annotations.ThreadSync;

/**
 * Test class for {@link CrossingCommandListener}
 */
class CrossingCommandListenerTest {
    @RegisterExtension
    ThreadSync.ThreadSyncExtension threadSyncExtension = new ThreadSync.ThreadSyncExtension();

    @BeforeEach
    void setup() {
        UndoRedoHandler.getInstance().clean();
        UndoRedoHandler.getInstance().addCommandQueuePreciseListener(new CrossingCommandListener());
    }

    @Test
    void testSynchronization() {
        final var crossingNode = TestUtils.newNode("highway=crossing crossing=uncontrolled crossing:markings=no");
        final var crossingWay = TestUtils.newWay("highway=footway footway=crossing", new Node(LatLon.NORTH_POLE),
                crossingNode, new Node(LatLon.SOUTH_POLE));
        final var highway = TestUtils.newWay("highway=unclassified", new Node(LatLon.NORTH_POLE),
                new Node(LatLon.SOUTH_POLE));
        final DataSet dataSet = new DataSet();
        dataSet.addPrimitiveRecursive(crossingWay);
        dataSet.addPrimitiveRecursive(highway);
        highway.addNode(1, crossingNode);
        final var changePropertyCommand = new ChangePropertyCommand(crossingNode, "crossing:markings", "ladder");
        UndoRedoHandler.getInstance().add(changePropertyCommand);
        threadSyncExtension.threadSync();
        assertNotSame(changePropertyCommand, UndoRedoHandler.getInstance().getLastCommand());
        assertSame(changePropertyCommand, UndoRedoHandler.getInstance().getUndoCommands().get(0));
        assertAll(() -> assertEquals(3, crossingNode.getNumKeys(), crossingNode.toString()),
                () -> assertEquals("crossing", crossingNode.get("highway")),
                () -> assertEquals("uncontrolled", crossingNode.get("crossing")),
                () -> assertEquals("ladder", crossingNode.get("crossing:markings")),
                () -> assertEquals(4, crossingWay.getNumKeys(), crossingWay.toString()),
                () -> assertEquals("footway", crossingWay.get("highway")),
                () -> assertEquals("crossing", crossingWay.get("footway")),
                () -> assertEquals("uncontrolled", crossingWay.get("crossing")),
                () -> assertEquals("ladder", crossingWay.get("crossing:markings")));
    }

    @Test
    void testDontAddCrossingTagsToSidewalk() {
        final var footway = newWay("highway=footway", 39.0704491, -108.5592382, 39.0704478, -108.5583329, 39.0704474,
                -108.5577837);
        final var service = newWay("highway=service", 39.0703209, -108.5583331, 39.070803, -108.5583324);
        final var ds = new DataSet();
        final var undoRedo = UndoRedoHandler.getInstance();
        final var crossing = footway.getNode(1);
        ds.addPrimitiveRecursive(footway);
        ds.addPrimitiveRecursive(service);
        service.addNode(1, crossing);
        crossing.put("highway", "crossing");
        undoRedo.add(new ChangePropertyCommand(crossing, "crossing", "unmarked"));
        threadSyncExtension.threadSync();
        assertFalse(footway.hasKey("crossing"));
        assertFalse(footway.hasTag("footway", "crossing"));
    }

    @Test
    void testDontAddHighwayFootwayToCrossing() {
        final var way = newWay("", 39.1021966, -108.5542965, 39.1022497, -108.5542973, 39.1023111, -108.5542982);
        final var ds = new DataSet();
        final var crossing = way.getNode(1);
        ds.addPrimitiveRecursive(way);
        crossing.put("highway", "crossing");
        UndoRedoHandler.getInstance().add(new ChangePropertyCommand(way, "highway", "footway"));
        threadSyncExtension.threadSync();
        assertEquals("crossing", crossing.get("highway"));
    }
}
