// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.geoimage;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Objects;

import org.apache.commons.jcs3.access.CacheAccess;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.plugins.mapillary.data.mapillary.MapillaryDownloader;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.ImageSourceProvider;

/**
 * A remote image entry This class may be finalized, such that it is no longer
 * modifiable. This should always be done prior to sending the entry outside of
 * the constructing method.
 *
 * @author Taylor Smock
 */
public class RemoteImageEntry extends ImageEntry implements ImageSourceProvider {
    private static final CacheAccess<String, byte[]> CACHE = JCSCacheManager
            .getCache("mapwithai:streetlevel:RemoteImageEntry");
    private long key;
    private String url;

    @Override
    public boolean equals(Object other) {
        if (super.equals(other)) {
            RemoteImageEntry o = (RemoteImageEntry) other;
            return Objects.equals(this.url, o.url) && this.key == o.key;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(this.url) + Long.hashCode(this.key);
    }

    /**
     * Set the image id for this image
     *
     * @param id The id to set (will also set URL)
     */
    public void setId(long id) {
        this.key = id;
        this.setMapillaryV4URL();
    }

    /**
     * Set the URL for Mapillary v4 image ids.
     */
    private void setMapillaryV4URL() {
        // get Mapillary v4 url
        final INode data = MapillaryDownloader.downloadImages(this.key).values().iterator().next().get(0);
        if (data.hasKey("altitude")) {
            super.setElevation(Double.parseDouble(data.get("altitude")));
        }
        if (data.hasKey("computed_altitude")) {
            super.setElevation(Double.parseDouble(data.get("computed_altitude")));
        }
        if (data.hasKey("compass_angle")) {
            super.setExifImgDir(Double.parseDouble(data.get("compass_angle")));
        }
        if (data.hasKey("computed_compass_angle")) {
            super.setExifImgDir(Double.parseDouble(data.get("computed_compass_angle")));
        }
        if (data.hasKey("captured_at")) {
            super.setExifTime(Instant.ofEpochMilli(Long.parseLong(data.get("captured_at"))));
        }
        super.setExifCoor(data);
        super.setPos(data); // TODO: Should this be computed geometry?
        if (data.hasKey("width")) {
            super.setWidth(Integer.parseInt(data.get("width")));
        }
        if (data.hasKey("height")) {
            super.setHeight(Integer.parseInt(data.get("height")));
        }
        this.url = data.get("thumb_2048_url");
    }

    @Override
    protected URL getImageUrl() throws MalformedURLException {
        return URI.create(this.url).toURL();
    }

    @Override
    public long getSource() {
        return this.key;
    }
}
