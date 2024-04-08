// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.openstreetmap.josm.data.gpx.GpxImageEntry;
import org.openstreetmap.josm.data.osm.IPrimitive;

/**
 * Suggestions for OSM objects with supporting images
 *
 * @author Taylor Smock
 */
@Immutable
public final class Suggestion<I extends GpxImageEntry & ImageSourceProvider, T extends Collection<I>> {
    private final long identifier;
    private final Set<IPrimitive> primitives;
    private final StreetViewImageSet<I, T> streetViewImageSet;

    /**
     * Create a new suggestion
     *
     * @param identifier         The unique id for the suggestion
     * @param primitives         The primitives for the suggestion
     * @param streetViewImageSet The supporting imagery
     * @param <O>                The primitive type
     */
    public <O extends IPrimitive> Suggestion(long identifier, @Nonnull Collection<O> primitives,
            @Nonnull StreetViewImageSet<I, T> streetViewImageSet) {
        Objects.requireNonNull(primitives);
        Objects.requireNonNull(streetViewImageSet);
        this.identifier = identifier;
        this.primitives = Collections.unmodifiableSet(new HashSet<>(primitives));
        this.streetViewImageSet = streetViewImageSet;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Suggestion) {
            return this.identifier == ((Suggestion<?, ?>) other).identifier;
        }
        return false;
    }

    /**
     * Get the unique identifier for this suggestion
     *
     * @return The unique identifier
     */
    public long getIdentifier() {
        return this.identifier;
    }

    /**
     * Get the image entries for this suggestion
     *
     * @return The image entries
     */
    @Nonnull
    public StreetViewImageSet<I, T> getImageEntries() {
        return this.streetViewImageSet;
    }

    /**
     * Get the primitives for this suggestion
     *
     * @return The primitives
     */
    @Nonnull
    public Set<IPrimitive> getPrimitives() {
        return this.primitives;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.identifier);
    }
}
