// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.preferences;

import org.openstreetmap.josm.plugins.mapwithai.street_level.spi.preferences.IUrls;

/**
 * A class holding config information
 *
 * @author Taylor Smock
 */
public final class MapWithAIStreetLevelConfig {
    private static IUrls iUrls;

    private MapWithAIStreetLevelConfig() {
        // Hide constructor
    }

    /**
     * Get the class holding URL information
     *
     * @return The URL information class
     */
    public static IUrls getUrls() {
        return iUrls;
    }

    /**
     * Set the URLs
     *
     * @param urls The url holding class
     */
    public static void setUrls(IUrls urls) {
        iUrls = urls;
    }
}
