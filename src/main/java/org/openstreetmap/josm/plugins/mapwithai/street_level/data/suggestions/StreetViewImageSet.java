// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2022 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions;

import javax.annotation.concurrent.Immutable;

import java.util.Collection;

import org.openstreetmap.josm.data.gpx.GpxImageEntry;
import org.openstreetmap.josm.tools.Logging;

/**
 * A collection of street view images
 *
 * @author Taylor Smock
 */
@Immutable
public class StreetViewImageSet<I extends GpxImageEntry, T extends Collection<I>> {
    private final T collection;
    private final long identifier;

    /**
     * Create a new image set
     *
     * @param identifier The identifier for the image set
     * @param collection The collection. It <i>must</i> be immutable, and the source
     *                   collection must not be used or modified further.
     */
    public StreetViewImageSet(final long identifier, final T collection) {
        this.identifier = identifier;
        this.collection = collection;
        try {
            collection.clear();
            collection.add(null);
            throw new IllegalArgumentException("MapWithAI StreetLevel: collection is modifiable");
        } catch (UnsupportedOperationException e) {
            // This is expected. Don't do anything.
            Logging.trace(e);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof StreetViewImageSet) {
            return this.identifier == ((StreetViewImageSet<?, ?>) other).identifier;
        }
        return false;
    }

    /**
     * Get the unmodifiable collection of images
     *
     * @return The images for this set
     */
    public T getCollection() {
        return collection;
    }

    /**
     * Get the unique id for this set
     *
     * @return The unique identifier
     */
    public long getIdentifier() {
        return identifier;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.identifier);
    }
}
