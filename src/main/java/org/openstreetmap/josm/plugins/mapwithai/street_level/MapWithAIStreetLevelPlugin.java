// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2022 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.Component;
import java.lang.reflect.Field;
import java.util.Collection;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
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
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.io.importexport.CubitorOsmChangeImporter;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.ReflectionUtils;

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
        ExtensionFileFilter.addImporterFirst(CubitorOsmChangeImporter.INSTANCE);
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
        removeImporter();
    }

    /**
     * Remove the importer we added (uses reflection, unfortunately)
     */
    private static void removeImporter() {
        // FIXME: Add method in JOSM to remove importer
        try {
            final Field importersField = ExtensionFileFilter.class.getDeclaredField("importers");
            ReflectionUtils.setObjectsAccessible(importersField);
            final Object importers = importersField.get(null);
            if (importers instanceof Collection) {
                ((Collection<?>) importers).remove(CubitorOsmChangeImporter.INSTANCE);
            }
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new JosmRuntimeException(reflectiveOperationException);
        }
    }
}
