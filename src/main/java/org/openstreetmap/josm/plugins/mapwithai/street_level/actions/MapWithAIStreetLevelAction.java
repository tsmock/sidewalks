// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.MapWithAIStreetLevelLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * An action to create a MapWithAI layer
 *
 * @author Taylor Smock
 */
public class MapWithAIStreetLevelAction extends JosmAction {
    /**
     * Create a new {@link MapWithAIStreetLevelAction}
     */
    public MapWithAIStreetLevelAction() {
        super(tr("MapWithAI StreetLevel: Add layer"), (ImageProvider) null, tr("MapWithAI sidewalk assistant"),
                Shortcut.registerShortcut("mapwithai:streetlevel:add_layer", tr("MapWithAI StreetLevel: Add layer"),
                        KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                true, "mapwithai:streetlevel:add_layer", true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final MainLayerManager layerManager = MainApplication.getLayerManager();
        final OsmDataLayer osmDataLayer = layerManager.getActiveDataLayer();
        // This is pretty much just a check to make certain we are using an actual
        // OsmDataLayer instead of a class that extends it (something I've done
        // often to avoid some implementation duplication)
        if (osmDataLayer != null && OsmDataLayer.class.equals(osmDataLayer.getClass())) {
            layerManager.addLayer(new MapWithAIStreetLevelLayer(new DataSet(),
                    tr("MapWithAI StreetLevel: {0}", osmDataLayer.getName()), osmDataLayer));
        }
    }

    @Override
    protected void updateEnabledState() {
        this.setEnabled(MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class).stream()
                .anyMatch(layer -> OsmDataLayer.class.equals(layer.getClass())));
    }
}
