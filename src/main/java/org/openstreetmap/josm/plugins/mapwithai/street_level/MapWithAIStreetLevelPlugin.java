// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2022 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.Component;

import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.mapwithai.street_level.actions.ApplySuggestionAction;
import org.openstreetmap.josm.plugins.mapwithai.street_level.actions.MapWithAIStreetLevelAction;
import org.openstreetmap.josm.plugins.mapwithai.street_level.actions.ParallelSidewalkCreationAction;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.preferences.MapWithAIStreetLevelConfig;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.preferences.MapWithAIStreetLevelUrls;
import org.openstreetmap.josm.tools.Destroyable;

/**
 * The POJO for StreetLevel AI detections
 *
 * @author Taylor Smock
 */
public class MapWithAIStreetLevelPlugin extends Plugin implements Destroyable {
    /**
     * Creates the plugin
     *
     * @param info the plugin information describing the plugin.
     */
    public MapWithAIStreetLevelPlugin(PluginInformation info) {
        super(info);
        final JMenu dataMenu = MainApplication.getMenu().dataMenu;
        MainMenu.add(dataMenu, new MapWithAIStreetLevelAction());
        MainMenu.add(dataMenu, new ApplySuggestionAction());
        MainMenu.add(dataMenu, new ParallelSidewalkCreationAction());
        MapWithAIStreetLevelConfig.setUrls(new MapWithAIStreetLevelUrls());
        AbstractPrimitive.getDiscardableKeys().add("suggestion-id");
    }

    @Override
    public void destroy() {
        final JMenu dataMenu = MainApplication.getMenu().dataMenu;
        for (Component menuComponent : dataMenu.getMenuComponents()) {
            if (menuComponent instanceof JMenuItem) {
                JMenuItem jMenu = (JMenuItem) menuComponent;
                if (jMenu.getAction().getClass().getPackage().getName()
                        .contains(this.getClass().getPackage().getName())) {
                    dataMenu.remove(jMenu);
                }
            }
        }
    }
}
