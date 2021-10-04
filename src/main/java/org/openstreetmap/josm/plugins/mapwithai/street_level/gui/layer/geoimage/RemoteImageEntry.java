// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.geoimage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.jcs3.access.CacheAccess;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * A remote image entry This class may be finalized, such that it is no longer
 * modifiable. This should always be done prior to sending the entry outside of
 * the constructing method.
 *
 * @author Taylor Smock
 */
public class RemoteImageEntry extends ImageEntry {
    private static final CacheAccess<String, byte[]> CACHE = JCSCacheManager.getCache("mapwithai:streetlevel:RemoteImageEntry");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");
    private String key;
    private String url;

    @Override
    public boolean equals(Object other) {
        if (super.equals(other)) {
            RemoteImageEntry o = (RemoteImageEntry) other;
            return Objects.equals(this.url, o.url) && Objects.equals(this.key, o.key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(this.url, this.key);
    }

    /**
     * Set the image key for this image
     *
     * @param key The key to set (will also set URL)
     */
    public void setKey(String key) throws IOException {
        this.key = key;
        if (NUMBER_PATTERN.matcher(this.key).matches()) {
            // get Mapillary v4 url
            final HttpClient client = HttpClient.create(new URL("https://graph.mapillary.com/" + this.key
                    + "?fields=altitude,captured_at,compass_angle,computed_altitude,computed_compass_angle,"
                    + "computed_geometry,computed_rotation,exif_orientation,geometry,height,width,thumb_2048_url"));
            client.connect();
            HttpClient.Response response = client.getResponse();
            try (JsonReader jsonReader = Json.createReader(response.getContentReader())) {
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
                    super.setExifTime(DateUtils.parseInstant(data.getString("captured_at")));
                }
                if (data.containsKey("geometry")) {
                    final JsonArray coordinates = data.getJsonObject("geometry").getJsonArray("coordinates");
                    super.setExifCoor(new LatLon(coordinates.getJsonNumber(1).doubleValue(), coordinates.getJsonNumber(0).doubleValue()));
                }
                if (data.containsKey("computed_geometry")) {
                    final JsonArray coordinates = data.getJsonObject("computed_geometry").getJsonArray("coordinates");
                    super.setPos(new LatLon(coordinates.getJsonNumber(1).doubleValue(), coordinates.getJsonNumber(0).doubleValue()));
                }
                if (data.containsKey("width")) {
                    super.setWidth(data.getInt("width"));
                }
                if (data.containsKey("height")) {
                    super.setHeight(data.getInt("height"));
                }

                this.url = data.getString("thumb_2048_url");
            } finally {
                client.disconnect();
            }
        } else {
            // set Mapillary v3 url
            this.url = "https://images.mapillary.com/" + this.key + "/thumb-2048.jpg";
        }
    }

    @Override
    protected URL getImageUrl() throws MalformedURLException {
        return new URL(this.url);
    }
}
