// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.MapWithAIStreetLevelLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Test class for {@link MapWithAIStreetLevelAction}
 *
 * @author Taylor Smock
 */
@BasicPreferences
class MapWithAIStreetLevelActionTest {
    // Used for cleaning up layers
    @RegisterExtension
    JOSMTestRules rules = new JOSMTestRules();

    @Test
    void testActionPerformed() {
        final MapWithAIStreetLevelAction mapWithAIStreetLevelAction = new MapWithAIStreetLevelAction();
        final OsmDataLayer osmDataLayer = new OsmDataLayer(new DataSet(), "", null);
        mapWithAIStreetLevelAction.actionPerformed(null);
        assertTrue(MainApplication.getLayerManager().getLayersOfType(MapWithAIStreetLevelLayer.class).isEmpty());
        MainApplication.getLayerManager().addLayer(osmDataLayer);
        mapWithAIStreetLevelAction.actionPerformed(null);
        assertFalse(MainApplication.getLayerManager().getLayersOfType(MapWithAIStreetLevelLayer.class).isEmpty());
    }

    @Test
    void testEnabledState() {
        final MapWithAIStreetLevelAction mapWithAIStreetLevelAction = new MapWithAIStreetLevelAction();
        final GpxLayer gpxLayer = new GpxLayer(new GpxData());
        final OsmDataLayer osmDataLayer = new OsmDataLayer(new DataSet(), "", null);
        MainLayerManager layerManager = MainApplication.getLayerManager();
        assertFalse(mapWithAIStreetLevelAction.isEnabled());
        layerManager.addLayer(gpxLayer);
        assertFalse(mapWithAIStreetLevelAction.isEnabled());
        layerManager.addLayer(osmDataLayer);
        assertTrue(mapWithAIStreetLevelAction.isEnabled());
        layerManager.removeLayer(osmDataLayer);
        assertFalse(mapWithAIStreetLevelAction.isEnabled());
        layerManager.addLayer(osmDataLayer);
        assertTrue(mapWithAIStreetLevelAction.isEnabled());
        layerManager.removeLayer(gpxLayer);
        assertTrue(mapWithAIStreetLevelAction.isEnabled());
        layerManager.removeLayer(osmDataLayer);
        assertFalse(mapWithAIStreetLevelAction.isEnabled());

        final MapWithAIStreetLevelLayer mapWithAIStreetLevelLayer = new MapWithAIStreetLevelLayer(new DataSet(), "",
                osmDataLayer);
        layerManager.addLayer(mapWithAIStreetLevelLayer);
        assertFalse(mapWithAIStreetLevelAction.isEnabled());
        layerManager.removeLayer(mapWithAIStreetLevelLayer);
    }
}
