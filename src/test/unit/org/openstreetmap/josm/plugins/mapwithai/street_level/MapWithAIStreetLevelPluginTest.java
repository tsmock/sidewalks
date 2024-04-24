// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.jar.Attributes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link MapWithAIStreetLevelPlugin}
 *
 * @author Taylor Smock
 */
@BasicPreferences
@Main
@Projection
class MapWithAIStreetLevelPluginTest {

    static PluginInformation pluginInformation;

    @BeforeAll
    static void beforeAll() throws PluginException {
        pluginInformation = new PluginInformation(new Attributes(), "MapWithAI StreetLevel", null);
    }

    @Test
    void testDestruction() {
        int initialCount = MainApplication.getMenu().dataMenu.getMenuComponentCount();
        new MapWithAIStreetLevelPlugin(pluginInformation).destroy();
        assertEquals(initialCount, MainApplication.getMenu().dataMenu.getMenuComponentCount());
    }

    @Test
    void testInitialization() {
        int initialCount = MainApplication.getMenu().dataMenu.getMenuComponentCount();
        new MapWithAIStreetLevelPlugin(pluginInformation);
        assertEquals(
                initialCount + ReflectionUtils.findAllClassesInPackage(this.getClass().getPackage().getName(),
                        ClassFilter.of(clazz -> clazz.getName().endsWith("Action"))).size(),
                MainApplication.getMenu().dataMenu.getMenuComponentCount());
    }
}
