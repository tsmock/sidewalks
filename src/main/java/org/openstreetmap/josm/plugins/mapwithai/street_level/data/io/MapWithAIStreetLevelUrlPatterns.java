// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2022 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.io;

import org.openstreetmap.josm.io.UrlPattern;

/**
 * MapWithAI Street Level URL patterns
 *
 * @author Taylor Smock
 */
public final class MapWithAIStreetLevelUrlPatterns {
    private static final String COMPRESSED = "(gz|xz|bz2?|zip)";
    private static final String HTTPS = "https?://";

    private MapWithAIStreetLevelUrlPatterns() {
        // Hide constructor
    }

    /**
     * Patterns for Facebook extended osc
     */
    public enum MapWithAIStreetLevelUrlPattern implements UrlPattern {
        /** URL of remote compressed osc file */
        EXTERNAL_COMPRESSED_FILE(".*/(.*\\.cubitor.osc." + COMPRESSED + ")"),
        /** URL of remote .osc file */
        EXTERNAL_OSC_FILE(".*/(.*\\.cubitor.osc)"),
        /** Extended OSC */
        FB_URL(".*result_type=extended_osc.*");

        private final String urlPattern;

        MapWithAIStreetLevelUrlPattern(String urlPattern) {
            this.urlPattern = HTTPS + urlPattern;
        }

        @Override
        public String pattern() {
            return urlPattern;
        }
    }
}
