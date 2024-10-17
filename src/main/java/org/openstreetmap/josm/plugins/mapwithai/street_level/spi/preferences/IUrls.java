// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.spi.preferences;

/**
 * Interface for storing URL information
 */
public interface IUrls {
    /**
     * Get the base street-level AI URL
     *
     * @return The string to use to construct the street-level AI URL
     */
    String getMapWithAIStreetLevelUrl();
}
