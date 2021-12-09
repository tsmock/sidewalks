// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.jcs3.access.CacheAccess;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.plugins.mapillary.oauth.OAuthUtils;
import org.openstreetmap.josm.plugins.mapillary.utils.MapillaryURL;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.ImageSourceProvider;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

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
    private static final byte[] EMPTY_BYTES = {};
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");
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
     * Set the image key for this image
     *
     * @param key The key to set (will also set URL)
     */
    public void setKey(String key) {
        if (NUMBER_PATTERN.matcher(key).matches()) {
            this.key = Long.parseLong(key);
            this.setMapillaryV4URL();
        } else {
            throw new JosmRuntimeException(tr("Unknown Mapillary id {0}", key));
        }
    }

    /**
     * Set the URL for Mapillary v4 image ids.
     */
    private void setMapillaryV4URL() {
        // get Mapillary v4 url
        final String mapillaryV4Url = MapillaryURL.APIv4.getImageInformation(this.key);
        final byte[] bytes = CACHE.get(mapillaryV4Url, () -> {
            HttpClient client = null;
            try {
                client = OAuthUtils.addAuthenticationHeader(HttpClient.create(new URL(mapillaryV4Url)));
                client.connect();
                return client.getResponse().fetchContent().getBytes(StandardCharsets.UTF_8);
            } catch (IOException e) {
                Logging.error(e);
            } finally {
                if (client != null) {
                    client.disconnect();
                }
            }
            return null;
        });
        try (JsonReader jsonReader = Json.createReader(new ByteArrayInputStream(bytes == null ? EMPTY_BYTES : bytes))) {
            JsonObject data = jsonReader.readObject();
            if (data.containsKey("altitude")) {
                super.setElevation(data.getJsonNumber("altitude").doubleValue());
            }
            if (data.containsKey("computed_altitude")) {
                super.setElevation(data.getJsonNumber("computed_altitude").doubleValue());
            }
            if (data.containsKey("compass_angle")) {
                super.setExifImgDir(data.getJsonNumber("compass_angle").doubleValue());
            }
            if (data.containsKey("computed_compass_angle")) {
                super.setExifImgDir(data.getJsonNumber("computed_compass_angle").doubleValue());
            }
            if (data.containsKey("captured_at")) {
                super.setExifTime(Instant.ofEpochMilli(data.getInt("captured_at")));
            }
            if (data.containsKey("geometry")) {
                final JsonArray coordinates = data.getJsonObject("geometry").getJsonArray("coordinates");
                super.setExifCoor(new LatLon(coordinates.getJsonNumber(1).doubleValue(),
                        coordinates.getJsonNumber(0).doubleValue()));
            }
            if (data.containsKey("computed_geometry")) {
                final JsonArray coordinates = data.getJsonObject("computed_geometry").getJsonArray("coordinates");
                super.setPos(new LatLon(coordinates.getJsonNumber(1).doubleValue(),
                        coordinates.getJsonNumber(0).doubleValue()));
            }
            if (data.containsKey("width")) {
                super.setWidth(data.getInt("width"));
            }
            if (data.containsKey("height")) {
                super.setHeight(data.getInt("height"));
            }
            this.url = data.getString("thumb_2048_url");
        }
    }

    @Override
    protected URL getImageUrl() throws MalformedURLException {
        return new URL(this.url);
    }

    @Override
    public long getSource() {
        return this.key;
    }
}
