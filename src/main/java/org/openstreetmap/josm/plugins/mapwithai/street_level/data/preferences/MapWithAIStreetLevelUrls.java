// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.preferences;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.plugins.mapwithai.street_level.spi.preferences.IUrls;

/**
 * Class with default MapWithAI Street Level Urls
 *
 * @author Taylor Smock
 */
public class MapWithAIStreetLevelUrls implements IUrls {
    private static final String BASE_URL = "https://www.mapwith.ai/maps/ml_roads";
    private static final Map<String, String> PARAMETER_MAP = new LinkedHashMap<>();

    private static final String RESULT_TYPE = "result_type";

    static {
        // TODO FIXME: These should be JOSM specific keys, not the RapiD keys
        PARAMETER_MAP.put("collaborator", "rapid");
        PARAMETER_MAP.put("token",
                "ASbYX8wITNCWnU1XMF1V-d2_iRiBMKmW2nT85IhjS4TOQXie-YJMCOGppe-DiCxUSfQ4hG4MDxyfXIprF5YO3QNR");
        PARAMETER_MAP.put("hash", "ASaPD6M5i29Nf8jGGb0");
    }

    @Override
    public String getMapWithAIStreetLevelUrl() {
        final Map<String, String> parameterMap = new LinkedHashMap<>();
        parameterMap.put(RESULT_TYPE, "extended_osc");
        parameterMap.put("conflate_with_osm", "true");
        parameterMap.put("theme", "streetview_ai_suggestion");
        parameterMap.putAll(PARAMETER_MAP);
        parameterMap.put("ext", "1918681607");
        parameterMap.put("sources", "fb_footway");
        parameterMap.put("bbox", "{0}");
        return BASE_URL
                + parameterMap.entrySet().stream().map(entry -> String.join("=", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining("&", "?", ""));
    }
}
