// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.annotations.MapWithAIStreetLevelConfigAnnotation;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Test class for {@link MapWithAIStreetLevelLayer}
 *
 * @author Taylor Smock
 */
@BasicPreferences
@MapWithAIStreetLevelConfigAnnotation
class MapWithAIStreetLevelLayerTest {
    /**
     * A mock to ensure that download isn't called multiple times
     */
    static class MapWithAIStreetLevelLayerMock extends MockUp<MapWithAIStreetLevelLayer> {
        int downloadCalled;

        @Mock
        void download(Invocation invocation, Bounds bounds) {
            downloadCalled++;
            invocation.proceed(bounds);
        }
    }

    // Used for layer cleanup
    @RegisterExtension
    JOSMTestRules josmTestRules = new JOSMTestRules();

    static Stream<Arguments> testDataSourceChange() {
        return Stream.of(Arguments.of(0, new OsmDataLayer(new DataSet(), "", null)));
    }

    @ParameterizedTest
    @MethodSource
    @Disabled("Server side support not done")
    void testDataSourceChange(final int expectedCalls, final OsmDataLayer osmDataLayer) {
        final MapWithAIStreetLevelLayerMock mapWithAIStreetLevelLayerMock = new MapWithAIStreetLevelLayerMock();
        final MapWithAIStreetLevelLayer mapWithAIStreetLevelLayer = new MapWithAIStreetLevelLayer(new DataSet(), "",
                osmDataLayer);
        assertEquals(osmDataLayer.getData().getDataSources().size(),
                mapWithAIStreetLevelLayer.getData().getDataSources().size());
        assertEquals(expectedCalls, mapWithAIStreetLevelLayerMock.downloadCalled);

        osmDataLayer.getDataSet().addDataSource(new DataSource(new Bounds(-1, -1, 1, 1), ""));
        assertEquals(expectedCalls + 1, mapWithAIStreetLevelLayerMock.downloadCalled);
        assertEquals(osmDataLayer.getData().getDataSources().size(),
                mapWithAIStreetLevelLayer.getData().getDataSources().size());
    }
}
