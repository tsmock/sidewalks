// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.actions.downloadtasks;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.Suggestion;
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.MapWithAIStreetLevelLayer;
import org.openstreetmap.josm.plugins.mapwithai.street_level.spi.preferences.IUrls;
import org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.annotations.MapWithAIStreetLevelConfigAnnotation;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;

/**
 * Test class for {@link DownloadMapWithAIExtendedOsmChangeTask}
 *
 * @author Taylor Smock
 */
// Needed for OSM primitives
@BasicPreferences
@MapWithAIStreetLevelConfigAnnotation(urlClass = DownloadMapWithAIExtendedOsmChangeTaskTest.WiremockUrlClass.class)
class DownloadMapWithAIExtendedOsmChangeTaskTest {
    /**
     * This is used to ensure that URLs are mocked TODO replace with
     * {@link MapWithAIStreetLevelConfigAnnotation.WiremockUrlClass}
     */
    public static class WiremockUrlClass implements IUrls {
        @Override
        public String getMapWithAIStreetLevelUrl() {
            return wireMockServer.url("/test");
        }
    }

    // TODO Reuse @Wiremock when merged
    static WireMockServer wireMockServer;

    // Needed for layer clearing
    @RegisterExtension
    JOSMTestRules josmTestRules = new JOSMTestRules();

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
        wireMockServer = null;
    }

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
    }

    private void testXmlVariations(String xml, int suggestionCount, int streetViewImageSetCount,
            int streetViewImageCount) {
        wireMockServer.addStubMapping(wireMockServer.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse().withHeader("content-type", "text/xml; charset=UTF-8").withBody(xml))));
        final DownloadMapWithAIExtendedOsmChangeTask task = new DownloadMapWithAIExtendedOsmChangeTask();

        final Future<?> future = task.download(new DownloadParams(), new Bounds(0, 0, 0, 0),
                NullProgressMonitor.INSTANCE);
        Awaitility.await().atMost(Durations.ONE_SECOND).until(future::isDone);
        assertFalse(MainApplication.getLayerManager().getLayers().isEmpty());
        assertEquals(1, MainApplication.getLayerManager().getLayersOfType(MapWithAIStreetLevelLayer.class).size());
        final MapWithAIStreetLevelLayer mapWithAIStreetLevelLayer = MainApplication.getLayerManager().getLayersOfType(
                MapWithAIStreetLevelLayer.class).get(0);

        final Collection<Suggestion<?, ?>> suggestions = mapWithAIStreetLevelLayer.getSuggestions();
        assertEquals(suggestionCount, suggestions.size());
        assertEquals(streetViewImageSetCount, suggestions.stream().map(Suggestion::getImageEntries).distinct().count());
        assertEquals(streetViewImageCount, suggestions.stream().map(Suggestion::getImageEntries).distinct()
                .mapToLong(set -> set.getCollection().size()).sum());
        assertEquals(1, wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount());
    }

    @AfterEach
    void afterEach() {
        try {
            assertTrue(wireMockServer.findNearMissesForAllUnmatchedRequests().isEmpty(),
                    wireMockServer.findNearMissesForAllUnmatchedRequests().stream().map(NearMiss::toString)
                            .collect(Collectors.joining(System.lineSeparator())));
            assertTrue(wireMockServer.findAllUnmatchedRequests().isEmpty(), wireMockServer.findAllUnmatchedRequests()
                    .stream().map(LoggedRequest::toString).collect(Collectors.joining(System.lineSeparator())));
        } finally {
            wireMockServer.resetAll();
        }
    }

    @BeforeEach
    void beforeEach() {
        wireMockServer.resetAll();
    }

    static Stream<Arguments> testAddLayerIfRequired() {
        final Supplier<OsmDataLayer> osm = () -> new OsmDataLayer(new DataSet(), "", null);
        final Function<OsmDataLayer, MapWithAIStreetLevelLayer> withAIStreetLevelLayerFunction = layer -> new MapWithAIStreetLevelLayer(new DataSet(), "", layer);
        return testValidXml().flatMap(arg -> {
            final List<Object> args = new ArrayList<>(Arrays.asList(arg.get()));
            return Stream.of(Arrays.asList(new DownloadParams().withLayerName(""), osm, null),
                    Arrays.asList(new DownloadParams().withNewLayer(true).withLayerName("Test layer"), osm, null),
                    Arrays.asList(new DownloadParams().withLayerName(""), osm, withAIStreetLevelLayerFunction),
                    Arrays.asList(new DownloadParams().withNewLayer(true).withLayerName("  "), osm, withAIStreetLevelLayerFunction),
                    Arrays.asList(new DownloadParams().withLayerName("Test layer").withNewLayer(true), osm,
                            withAIStreetLevelLayerFunction),
                    Arrays.asList(new DownloadParams().withLayerName(null).withNewLayer(true), osm, withAIStreetLevelLayerFunction))
                    .map(ArrayList::new).filter(list -> list.addAll(args)).map(ArrayList::toArray);
        }).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource
    void testAddLayerIfRequired(DownloadParams downloadParams, Supplier<OsmDataLayer> osmDataLayerSupplier,
            Function<OsmDataLayer, MapWithAIStreetLevelLayer> mapWithAIStreetLevelLayerFunction, String xml, int suggestionCount,
            int streetViewImageSetCount, int streetViewImageCount) {
        wireMockServer.addStubMapping(wireMockServer.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse().withHeader("content-type", "text/xml; charset=UTF-8").withBody(xml))));
        final OsmDataLayer osmDataLayer = osmDataLayerSupplier.get();
        MainApplication.getLayerManager().addLayer(osmDataLayer);
        if (mapWithAIStreetLevelLayerFunction != null) {
            MainApplication.getLayerManager().addLayer(mapWithAIStreetLevelLayerFunction.apply(osmDataLayer));
        }
        final Collection<MapWithAIStreetLevelLayer> initialMapWithAIStreetLevelLayers = MainApplication.getLayerManager()
                .getLayersOfType(MapWithAIStreetLevelLayer.class);
        final DownloadMapWithAIExtendedOsmChangeTask task = new DownloadMapWithAIExtendedOsmChangeTask();

        final Future<?> future = task.download(downloadParams, new Bounds(0, 0, 0, 0), NullProgressMonitor.INSTANCE);
        Awaitility.await().atMost(Durations.ONE_SECOND).until(future::isDone);
        assertFalse(MainApplication.getLayerManager().getLayers().isEmpty());

        assertEquals(
                initialMapWithAIStreetLevelLayers.size() + ((downloadParams.isNewLayer() || initialMapWithAIStreetLevelLayers.isEmpty()) ? 1 : 0),
                MainApplication.getLayerManager().getLayersOfType(MapWithAIStreetLevelLayer.class).size());

        final MapWithAIStreetLevelLayer mapWithAIStreetLevelLayer = MainApplication.getLayerManager().getLayersOfType(
                        MapWithAIStreetLevelLayer.class).stream()
                .filter(l -> !downloadParams.isNewLayer() || !initialMapWithAIStreetLevelLayers.contains(l)).findAny().orElse(null);
        assertNotNull(mapWithAIStreetLevelLayer);

        final Collection<Suggestion<?, ?>> suggestions = mapWithAIStreetLevelLayer.getSuggestions();
        assertEquals(suggestionCount, suggestions.size());
        assertEquals(streetViewImageSetCount, suggestions.stream().map(Suggestion::getImageEntries).distinct().count());
        assertEquals(streetViewImageCount, suggestions.stream().map(Suggestion::getImageEntries).distinct()
                .mapToLong(set -> set.getCollection().size()).sum());

        if (!initialMapWithAIStreetLevelLayers.contains(mapWithAIStreetLevelLayer)) {
            assertEquals(Utils.isStripEmpty(downloadParams.getLayerName()) ? "MapWithAI StreetLevel" : downloadParams.getLayerName(),
                    mapWithAIStreetLevelLayer.getName());
        }
        assertEquals(1, wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount());
    }

    @Test
    void testNonNull() {
        final DownloadMapWithAIExtendedOsmChangeTask task = new DownloadMapWithAIExtendedOsmChangeTask();
        assertNotNull(task.getPatterns());

        assertEquals(tr("Download Cubitor Extended OSM Change"), task.getTitle());
    }

    /**
     * Method source for {@link #testValidXml(String, int, int, int)}
     *
     * @return A stream of arguments
     */
    static Stream<Arguments> testValidXml() {
        final String header = "<osmChange version=\"0.6\" generator=\"cubitor sidewalk suggestion generator\">";
        final String footer = "</osmChange>";

        return Stream.of(Arguments.of(header + footer, 0, 0, 0), Arguments.of(header
                + "<cubitor-context><sidewalk-suggestion id=\"-1\" type=\"geometry\">"
                + "<street-view-image-set id=\"-2\"><street-view-image key=\"a\" url=\"https://invalid.url\" lat=\"1\""
                + " lon=\"2\" ca=\"75\"/></street-view-image-set>" + "</sidewalk-suggestion></cubitor-context>"
                + footer, 1, 1, 1));
    }

    @ParameterizedTest
    @MethodSource
    void testValidXml(String xml, int suggestionCount, int streetViewImageSetCount, int streetViewImageCount) {
        // The AI layer requires an OSM layer to "follow".
        OsmDataLayer osmDataLayer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(osmDataLayer);
        testXmlVariations(xml, suggestionCount, streetViewImageSetCount, streetViewImageCount);
    }

    @ParameterizedTest
    @MethodSource("testValidXml")
    void testValidXmlPreExistingLayer(String xml, int suggestionCount, int streetViewImageSetCount,
            int streetViewImageCount) {
        // The AI layer requires an OSM layer to "follow".
        OsmDataLayer osmDataLayer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(osmDataLayer);

        MainApplication.getLayerManager().addLayer(new MapWithAIStreetLevelLayer(new DataSet(), "", osmDataLayer));

        testXmlVariations(xml, suggestionCount, streetViewImageSetCount, streetViewImageCount);
    }
}
