// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.actions.mapmode;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.SidewalkTestUtils.assertLatLonEquals;
import static org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.SidewalkTestUtils.newWay;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.ReverseWayAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.NavigatableComponentMock;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * Test class for {@link SidewalkMode}
 */
@Projection
class SidewalkModeTest {
    @RegisterExtension
    static Main.MainExtension mainExtension = new Main.MainExtension()
            .setNavigableComponentMocker(NavigatableComponentMock::new);
    private static final boolean REMOTE_CONTROL = Logging.isTraceEnabled();

    private DataSet ds;

    /**
     * Load failed data into JOSM to see why it failed
     */
    @RegisterExtension
    TestExecutionExceptionHandler executionExceptionHandler = (context, throwable) -> {
        if (throwable == null) {
            return;
        }
        loadIntoJosm(
                "/load_data?new_layer=true&layer_name=" + context.getTestMethod().map(Method::getName).orElse("Unknown")
                        + "&data=%3Cosm%20version%3D%220.6%22%3E%3C%2Fosm%3E");
        for (Node node : ds.getNodes()) {
            loadIntoJosm("/add_node?lon=" + node.lon() + "&lat=" + node.lat()
                    + (!node.hasKeys() ? "" : "&addtags=" + convertToUriTags(node)));
        }
        for (Way way : ds.getWays()) {
            loadIntoJosm(
                    "/add_way?way="
                            + way.getNodes().stream().map(node -> node.lat() + "," + node.lon())
                                    .collect(Collectors.joining(";"))
                            + (!way.hasKeys() ? "" : "&addtags=" + convertToUriTags(way)));
        }
        throw throwable;
    };

    private SidewalkMode action;

    @BeforeEach
    void setup() {
        this.ds = new DataSet();
        OsmDataLayer osmDataLayer = new OsmDataLayer(this.ds, "SidewalkModeTest", null);
        MainApplication.getLayerManager().addLayer(osmDataLayer);
        this.action = new SidewalkMode();
        this.action.actionPerformed(null);
    }

    @Test
    void testBasic() {
        final var highway = newWay("highway=residential", 39.0700657, -108.4654122, 39.0701854, -108.4654118);
        this.ds.addPrimitiveRecursive(highway);
        final var sidewalk = newWay("highway=footway footway=sidewalk", 39.0701677, -108.4653373, 39.0701554,
                -108.4653242, 39.0701461, -108.465308);
        this.ds.addPrimitiveRecursive(sidewalk);
        this.ds.setSelected(sidewalk.getNode(1));
        clickAt(39.0701499, -108.4653312);
        clickAt(39.0701494, -108.4654942);
        clickAt(39.0701538, -108.4655006);
        assertAll(() -> assertEquals(9, ds.getNodes().size()), () -> assertEquals(5, ds.getWays().size()));
        clickAt(39.0701538, -108.4655006); // Click again to finish drawing
        // Check state of dataset
        assertAll(() -> assertEquals(9, ds.getNodes().size()), () -> assertEquals(5, ds.getWays().size()));
        // Check ways
        final var crossings = this.ds.getWays().stream().filter(w -> w.hasTag("footway", "crossing")).toList();
        assertEquals(1, crossings.size());
        final var crossing = crossings.get(0);
        final var firstNode = crossing.firstNode();
        final var lastNode = crossing.lastNode();
        assertNotNull(firstNode);
        assertNotNull(lastNode);
        assertAll(() -> assertEquals(3, crossing.getNodesCount()),
                () -> assertTrue(new LatLon(39.0701497, -108.4654119).equalsEpsilon(crossing.getNode(1))),
                () -> assertTrue(crossing.getNode(1).hasTag("highway", "crossing")),
                () -> assertTrue(firstNode.hasTag("barrier", "kerb")),
                () -> assertTrue(lastNode.hasTag("barrier", "kerb")),
                () -> assertEquals(2, firstNode.getParentWays().size()),
                () -> assertEquals(2, lastNode.getParentWays().size()));
        final var links = this.ds.getWays().stream().filter(w -> w.getLength() < 1).toList();
        assertEquals(2, links.size());
    }

    @Test
    void testValidWaysNonProblematic() {
        final var highway = newWay("highway=residential", 39.0700657, -108.4654122, 39.0701854, -108.4654118);
        this.ds.addPrimitiveRecursive(highway);
        final var sidewalk = newWay("highway=footway footway=sidewalk", 39.0701677, -108.4653373, 39.0701554,
                -108.4653242, 39.0701461, -108.465308);
        this.ds.addPrimitiveRecursive(sidewalk);
        final var kerb = TestUtils.newNode("barrier=kerb");
        kerb.setCoor(new LatLon(39.0701494, -108.4654942));
        this.ds.addPrimitive(kerb);
        this.ds.setSelected(sidewalk.getNode(1));
        clickAt(39.0701499, -108.4653312);
        clickAt(39.0701494, -108.4654942);
        clickAt(39.0701538, -108.4655006);
        clickAt(39.0701538, -108.4655006); // Click again to finish drawing
        assertAll(this.ds.getWays().stream().map(way -> () -> assertTrue(way.getNodesCount() > 1)));
    }

    /**
     * This seems to occur when we have a crossing and a sidewalk at the start of
     * the way, and then cross a highway.
     */
    @Test
    void testValidWaysProblematic() {
        final var crossing = newWay("crossing:markings=yes crossing=uncontrolled footway=crossing highway=footway",
                38.2477457, -104.643939, 38.2477466, -104.643881, 38.2477482, -104.64377);
        final var sidewalk = newWay("footway=sidewalk highway=footway", 38.2475898, -104.643719, 38.2477489,
                -104.6437271);
        final var highway = newWay("highway=service oneway=yes surface=asphalt", 38.2485921, -104.6437852, 38.2488394,
                -104.6435905);
        this.ds.addPrimitiveRecursive(sidewalk);
        this.ds.addPrimitiveRecursive(crossing);
        this.ds.addPrimitiveRecursive(highway);
        crossing.addNode(sidewalk.lastNode());
        this.ds.setSelected(sidewalk.lastNode());
        clickAt(38.2493254, -104.6437485);
        clickAt(38.2493254, -104.6437485); // Finish
        assertAll(this.ds.getWays().stream().map(way -> () -> assertTrue(way.getNodesCount() > 1)));
    }

    @Test
    void testDividedHighway() {
        final var southBound = newWay("highway=unclassified surface=concrete oneway=yes", 38.3125107, -104.625117,
                38.3121189, -104.6251017);
        final var northBound = newWay("highway=unclassified surface=asphalt oneway=yes", 38.3121198, -104.6250227,
                38.3125131, -104.625037);
        final var westSidewalk = newWay("highway=footway footway=sidewalk", 38.3124233, -104.6251597, 38.3124337,
                -104.6251768, 38.3124362, -104.6252079);
        this.ds.addPrimitiveRecursive(southBound);
        this.ds.addPrimitiveRecursive(northBound);
        this.ds.addPrimitiveRecursive(westSidewalk);
        clickAt(38.3124337, -104.6251768);
        clickAt(38.3124372, -104.6251651); // Kerb 1
        clickAt(38.3124393, -104.6249971); // Kerb 2
        clickAt(38.3124351, -104.6249914);
        clickAt(38.3124351, -104.6249914); // Create a new stub footway
        final var crossing = southBound.getNode(1).getParentWays().stream().filter(not(southBound::equals)).findFirst()
                .orElseThrow();
        assertEquals(6, this.ds.getWays().size());
        assertAll(() -> assertEquals("kerb", Objects.requireNonNull(crossing.firstNode()).get("barrier")),
                () -> assertEquals("kerb", Objects.requireNonNull(crossing.lastNode()).get("barrier")),
                () -> assertFalse(crossing.getNode(1).hasTag("barrier")),
                () -> assertFalse(crossing.getNode(2).hasTag("barrier")),
                () -> assertTrue(crossing.getNode(1).hasTag("highway", "crossing")),
                () -> assertTrue(crossing.getNode(2).hasTag("highway", "crossing")),
                () -> assertLatLonEquals(new LatLon(38.3124378, -104.6251142), crossing.getNode(1)),
                () -> assertLatLonEquals(new LatLon(38.3124388, -104.6250343), crossing.getNode(2)),
                () -> assertEquals(4, crossing.getNodesCount()));
    }

    @Test
    void testDividedHighwayReversed() throws UserCancelException {
        final var southBound = newWay("highway=unclassified surface=concrete oneway=yes", 38.3125107, -104.625117,
                38.3121189, -104.6251017);
        final var northBound = newWay("highway=unclassified surface=asphalt oneway=yes", 38.3121198, -104.6250227,
                38.3125131, -104.625037);
        final var westSidewalk = newWay("highway=footway footway=sidewalk", 38.3124233, -104.6251597, 38.3124337,
                -104.6251768, 38.3124362, -104.6252079);
        this.ds.addPrimitiveRecursive(southBound);
        this.ds.addPrimitiveRecursive(northBound);
        this.ds.addPrimitiveRecursive(westSidewalk);
        clickAt(38.3124337, -104.6251768);
        clickAt(38.3124372, -104.6251651); // Kerb 1
        ReverseWayAction.reverseWay(this.ds.getLastSelectedWay()).getReverseCommand().executeCommand();
        clickAt(38.3124393, -104.6249971); // Kerb 2
        clickAt(38.3124351, -104.6249914);
        clickAt(38.3124351, -104.6249914); // Create a new stub footway
        final var crossing = southBound.getNode(1).getParentWays().stream().filter(not(southBound::equals)).findFirst()
                .orElseThrow();
        assertEquals(6, this.ds.getWays().size());
        assertAll(() -> assertEquals("kerb", Objects.requireNonNull(crossing.firstNode()).get("barrier")),
                () -> assertEquals("kerb", Objects.requireNonNull(crossing.lastNode()).get("barrier")),
                () -> assertFalse(crossing.getNode(1).hasTag("barrier")),
                () -> assertFalse(crossing.getNode(2).hasTag("barrier")),
                () -> assertTrue(crossing.getNode(1).hasTag("highway", "crossing")),
                () -> assertTrue(crossing.getNode(2).hasTag("highway", "crossing")),
                () -> assertLatLonEquals(new LatLon(38.3124388, -104.6250343), crossing.getNode(1)),
                () -> assertLatLonEquals(new LatLon(38.3124378, -104.6251142), crossing.getNode(2)),
                () -> assertEquals(4, crossing.getNodesCount()));
    }

    @Test
    void testFourWayIntersectionCrossing() {
        final var neSidewalk = newWay("highway=footway footway=sidewalk", 39.0995956, -108.5016914, 39.0995628,
                -108.5016756, 39.0995459, -108.501632);
        final var nwSidewalk = newWay("highway=footway footway=sidewalk", 39.0995904, -108.5018225, 39.0995591,
                -108.5018476, 39.0995466, -108.5018838);
        final var swSidewalk = newWay("highway=footway footway=sidewalk", 39.099448, -108.5018858, 39.0994309,
                -108.5018433, 39.0993986, -108.5018265);
        final var seSidewalk = newWay("highway=footway footway=sidewalk", 39.0994007, -108.5016987, 39.0994319,
                -108.5016816, 39.0994465, -108.5016357);
        final var nsHighway = newWay("highway=residential", 39.0991704, -108.5017607, 39.0994965, -108.5017631,
                39.0995913, -108.5017581);
        final var weHighway = newWay("highway=residential", 39.0994972, -108.5027182, 39.0994869, -108.4985298);
        for (OsmPrimitive p : Arrays.asList(neSidewalk, nwSidewalk, swSidewalk, seSidewalk, nsHighway, weHighway)) {
            this.ds.addPrimitiveRecursive(p);
        }
        weHighway.addNode(1, nsHighway.getNode(1));
        clickAt(39.0995591, -108.5018476);
        clickAt(39.0995529, -108.5018378);
        clickAt(39.0995561, -108.5016848);
        clickAt(39.0995628, -108.5016756);
        clickAt(39.0995628, -108.5016756); // First crossing finish
        // There should be no 1 node ways
        assertAll(this.ds.getWays().stream().map(w -> () -> assertNotEquals(1, w.getNodesCount())));

        clickAt(39.0995561, -108.5016848);
        clickAt(39.0994362, -108.5016904);
        clickAt(39.0994319, -108.5016816);
        clickAt(39.0994319, -108.5016816); // Second crossing finish
        // There should be no 1 node ways
        assertAll(this.ds.getWays().stream().map(w -> () -> assertNotEquals(1, w.getNodesCount())));

        clickAt(39.0994362, -108.5016904);
        clickAt(39.0994364, -108.5018366);
        clickAt(39.0994309, -108.5018433);
        clickAt(39.0994309, -108.5018433); // Third crossing finish
        // There should be no 1 node ways
        assertAll(this.ds.getWays().stream().map(w -> () -> assertNotEquals(1, w.getNodesCount())));

        clickAt(39.0994309, -108.5018433);
        clickAt(39.0995529, -108.5018378);
        clickAt(39.0995529, -108.5018378); // Fourth crossing finish

        // There should be no 1 node ways
        assertAll(this.ds.getWays().stream().map(w -> () -> assertNotEquals(1, w.getNodesCount())));
    }

    @Test
    void testUndoRedoActionsNoChildCommands() {
        // Check tag change
        final var neSidewalk = newWay("highway=footway footway=sidewalk", 39.0995956, -108.5016914, 39.0995628,
                -108.5016756, 39.0995459, -108.501632);
        this.ds.addPrimitiveRecursive(neSidewalk);
        UndoRedoHandler.getInstance().add(new ChangePropertyCommand(neSidewalk, "surface", "concrete"));
        assertDoesNotThrow(() -> this.action.mouseReleased(mouseClickAt(neSidewalk.getNode(1))));
    }

    @Test
    void testCrossingNodeNoWay() {
        final var alley = newWay("highway=service service=alley", 38.5683524, -121.4924243, 38.5679984, -121.4910983);
        final var sidewalk = newWay("highway=footway footway=sidewalk", 38.5675992, -121.491466, 38.567585,
                -121.4914197);
        this.ds.addPrimitiveRecursive(alley);
        this.ds.addPrimitiveRecursive(sidewalk);
        clickAt(38.567585, -121.4914197);
        clickAt(38.5682456, -121.4911187);
        clickAt(38.5682456, -121.4911187); // Finish drawing
        assertEquals(4, sidewalk.getNodesCount());
        assertLatLonEquals(new LatLon(38.5680301, -121.4912169), sidewalk.getNode(2));
        assertEquals(2, sidewalk.getNode(2).getParentWays().size());
    }

    @Test
    void testClosedSidewalkLoopWithCrossing() {
        final var highway = newWay("highway=residential", 47.6959017, -122.1157234, 47.6958032, -122.1156135);
        final var sidewalk = newWay("highway=footway footway=sidewalk", 47.6957785, -122.1158262, 47.6957082,
                -122.1155322);
        final var originalNodes = sidewalk.getNodes();
        this.ds.addPrimitiveRecursive(highway);
        this.ds.addPrimitiveRecursive(sidewalk);
        this.ds.setSelected(sidewalk.lastNode());
        clickAt(47.6959489, -122.1155503);
        clickAt(47.6957785, -122.1158262); // Close the loop
        clickAt(47.6957785, -122.1158262); // Finish drawing
        assertEquals(3, sidewalk.getNodesCount());
        assertSame(originalNodes.get(0), sidewalk.firstNode());
        assertSame(originalNodes.get(1), sidewalk.getNode(1));
        final var lastNode = sidewalk.lastNode();
        assertNotNull(lastNode);
        // Relevant XKCD: https://xkcd.com/2170/
        // Note that we aren't as precise as I would like here since we are clicking on
        // the map, and the UI decides to put the point at a slightly different position
        // from where we clicked.
        assertLatLonEquals(47.6959489, -122.1155503, lastNode.lat(), lastNode.lon(), 1e-6);
    }

    @Test
    void testDontAddKerbsInMiddleOfSidewalk() {
        final var sidewalk1 = newWay("highway=footway footway=sidewalk", 47.6956969, -122.1190318, 47.6956808,
                -122.1189738);
        final var sidewalk2 = TestUtils.newWay("highway=footway footway=sidewalk", sidewalk1.lastNode(),
                new Node(new LatLon(47.6956658, -122.118944)));
        final var sidewalk3 = newWay("highway=footway footway=sidewalk", 47.6956331, -122.1187476, 47.6956663,
                -122.1187291, 47.695686, -122.1187006);
        final var highway = newWay("highway=residential", 47.6957655, -122.1188517, 47.6945597, -122.1186691);
        this.ds.addPrimitiveRecursive(sidewalk1);
        this.ds.addPrimitiveRecursive(sidewalk2);
        this.ds.addPrimitiveRecursive(sidewalk3);
        this.ds.addPrimitiveRecursive(highway);
        this.ds.setSelected(sidewalk2.firstNode());
        clickAt(sidewalk3.getNode(1));
        clickAt(sidewalk3.getNode(1));// Finish sidewalk
        assertFalse(sidewalk3.getNode(1).hasKeys());
        assertFalse(Objects.requireNonNull(sidewalk1.lastNode()).hasKeys());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testMergeExistingCrosswalk(boolean tooFar) {
        final var highway = newWay("highway=residential", 39.0692698, -108.5661303, 39.0692697, -108.5660189, 39.069269,
                -108.56514);
        final var crossingNode = highway.getNode(1);
        crossingNode.put("highway", "crossing");
        if (tooFar) {
            crossingNode.setCoor(new LatLon(39.0692697, -108.5659494));
        }
        this.ds.addPrimitiveRecursive(highway);
        clickAt(39.0693705, -108.5660188);
        clickAt(39.0691728, -108.566019);
        clickAt(39.0691728, -108.566019); // Finish drawing
        final var crossing = this.ds.getLastSelectedWay();
        assertEquals(Map.of("highway", "footway", "footway", "crossing"), crossing.getKeys());
        assertEquals(!tooFar, crossing.containsNode(crossingNode));
        assertEquals(3, crossing.getNodesCount());
        assertEquals(tooFar ? 4 : 3, highway.getNodesCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testMergeExistingNode(boolean tooFar) {
        final var highway = newWay("highway=residential", 39.0692698, -108.5661303, 39.0692697, -108.5660081, 39.069269,
                -108.56514);
        final var crossingNode = highway.getNode(1);
        if (tooFar) {
            crossingNode.setCoor(new LatLon(39.0692697, -108.5659864));
        }
        this.ds.addPrimitiveRecursive(highway);
        clickAt(39.0693705, -108.5660188);
        clickAt(39.0691728, -108.566019);
        clickAt(39.0691728, -108.566019); // Finish drawing
        final var crossing = this.ds.getLastSelectedWay();
        assertEquals(Map.of("highway", "footway", "footway", "crossing"), crossing.getKeys());
        assertEquals(!tooFar, crossing.containsNode(crossingNode));
        assertEquals(3, crossing.getNodesCount());
        assertEquals(tooFar ? 4 : 3, highway.getNodesCount());
    }

    /**
     * We want to avoid merging to a crossing node near a road intersection, like in
     * a roundabout. See <a href="https://github.com/tsmock/sidewalks/issues/9">GH
     * #9</a> for further details.
     */
    @Test
    void testCrossingNearIntersection() {
        final var highwayOne = newWay("highway=residential", 39.0673069, -108.5518113, 39.0673068, -108.5508516);
        final var highwayTwo = newWay("highway=residential junction=roundabout", 39.0673552, -108.5508056, 39.0672551,
                -108.5508058);
        this.ds.addPrimitiveRecursive(highwayOne);
        this.ds.addPrimitiveRecursive(highwayTwo);
        highwayTwo.addNode(1, highwayOne.lastNode());
        clickAt(39.0673799, -108.5508617);
        clickAt(39.0672264, -108.5508622);
        clickAt(39.0672264, -108.5508622); // Finish drawing
        final var crossing = this.ds.getWays().stream().filter(w -> !Arrays.asList(highwayOne, highwayTwo).contains(w))
                .filter(not(IPrimitive::isDeleted)).findFirst().orElseThrow();
        assertAll(() -> assertEquals("footway", crossing.get("highway")),
                () -> assertEquals("crossing", crossing.get("footway")),
                () -> assertEquals(3, crossing.getNodesCount()));
        assertEquals(3, highwayTwo.getNodesCount());
        assertEquals(3, highwayOne.getNodesCount());
        final var crossingNode = highwayOne.getNode(1);
        assertAll(() -> assertSame(crossingNode, crossing.getNode(1)),
                () -> assertEquals("crossing", crossingNode.get("highway")));
    }

    @Test
    void testDontContinueCrossingSurfaceTagging() {
        final var highwayOne = newWay("highway=residential surface=asphalt", 39.0673069, -108.5518113, 39.0673063,
                -108.5507395);
        final var highwayTwo = newWay("highway=residential", 39.0674262, -108.5507393, 39.0671796, -108.5507402);
        final var sidewalkOne = newWay("highway=footway footway=sidewalk", 39.0671798, -108.5508623, 39.0672264,
                -108.5508622);
        final var sidewalkTwo = newWay("highway=footway footway=sidewalk", 39.0674294, -108.5523571, 39.0674263,
                -108.5507985);
        final var originalWays = Arrays.asList(highwayOne, highwayTwo, sidewalkOne, sidewalkTwo);
        originalWays.forEach(this.ds::addPrimitiveRecursive);
        highwayTwo.addNode(1, highwayOne.lastNode());
        clickAt(39.0672264, -108.5508622);
        clickAt(39.0673799, -108.5508617);
        clickAt(39.0674264, -108.5508615);
        clickAt(39.0674264, -108.5508615);
        final var crossing = sidewalkOne.lastNode().getParentWays().stream().filter(not(originalWays::contains))
                .findFirst().orElseThrow();
        final var newSidewalk = crossing.lastNode().getParentWays().stream().filter(not(crossing::equals))
                .filter(not(originalWays::contains)).findFirst().orElseThrow();
        assertFalse(newSidewalk.hasTag("surface"));
        assertEquals(3, sidewalkTwo.getNodesCount());
        assertEquals(2, newSidewalk.lastNode().getParentWays().size());
    }

    /**
     * Ensure that crossing a highway and a footway does not delete the last segment from the footway being drawn
     * See <a href="https://github.com/tsmock/sidewalks/issues/20">GH #20</a> for details.
     */
    @Test
    void testCrossingHighwayAndFootway() {
        final var highwayOne = newWay("highway=residential", 39.062895, -108.5228415, 39.0624124, -108.5228448);
        final var footwayOne = newWay("highway=footway", 39.0626661, -108.5227671, 39.0624119, -108.5227586);
        final var originalWays = Arrays.asList(highwayOne, footwayOne);
        originalWays.forEach(this.ds::addPrimitiveRecursive);
        clickAt(39.0624641, -108.5234194);
        clickAt(39.0624616, -108.5229965);
        clickAt(39.0624743, -108.5229272);
        clickAt(39.0624717, -108.5227507);
        final var footway = this.ds.getWays().stream().filter(not(originalWays::contains))
                // The crossing way will have three nodes with additional parents; the target footway will only have 1.
                .filter(w -> w.getNodes().stream().mapToInt(n -> n.getParentWays().size() - 1).sum() == 1).findFirst()
                .orElseThrow();
        assertEquals(3, footway.getNodesCount());
        assertLatLonEquals(new LatLon(39.0624641, -108.5234194), footway.getNode(0));
        assertLatLonEquals(new LatLon(39.0624616, -108.5229965), footway.getNode(1));
        assertLatLonEquals(new LatLon(39.0624743, -108.5229272), footway.getNode(2));
    }

    private void clickAt(double lat, double lon) {
        clickAt(new LatLon(lat, lon));
    }

    private void clickAt(ILatLon location) {
        final var click = mouseClickAt(location);
        MainApplication.getMap().mapModeDraw.mouseReleased(click);
        this.action.mouseReleased(click);
        GuiHelper.runInEDTAndWait(() -> {
            /* Sync UI thread */ });
    }

    private MouseEvent mouseClickAt(ILatLon location) {
        final var mapView = MainApplication.getMap().mapView;
        mapView.zoomTo(mapView.getCenter(), 0.005);
        mapView.zoomTo(location);
        final var point = mapView.getPoint(location);
        return new MouseEvent(MainApplication.getMap(), Long.hashCode(System.currentTimeMillis()),
                System.currentTimeMillis(), 0, point.x, point.y, 1, false, MouseEvent.BUTTON1);
    }

    private static void loadIntoJosm(String uri) throws IOException {
        if (REMOTE_CONTROL) {
            final var client = HttpClient
                    .create(URI.create("http:/" + RemoteControl.getInet4Address() + ":8111" + uri).toURL());
            try {
                client.connect();
            } finally {
                client.disconnect();
            }
        }
    }

    private static String convertToUriTags(Tagged tagged) {
        return tagged.getKeys().entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("%7C"));
    }
}
