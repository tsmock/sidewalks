// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * Test class for {@link CrossingCommandListener}
 */
class CrossingCommandListenerTest {
    @BeforeEach
    void setup() {
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
        GuiHelper.runInEDTAndWait(() -> {
            /* Sync UI thread */ });
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

}
