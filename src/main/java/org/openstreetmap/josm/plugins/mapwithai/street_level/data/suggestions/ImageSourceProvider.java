// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions;

/**
 * An interface for objects to provide a source value
 */
public interface ImageSourceProvider {
    /**
     * Get the source value
     *
     * @return The source for this suggestion
     */
    long getSource();
}
