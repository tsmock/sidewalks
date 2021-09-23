// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmChangeTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.UpdatePrimitivesTask;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.io.MapWithAIStreetLevelServerLocationReader;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.io.MapWithAIStreetLevelUrlPatterns;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.preferences.MapWithAIStreetLevelConfig;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.Suggestion;
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.MapWithAIStreetLevelLayer;
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.geoimage.RemoteImageEntry;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Task allowing to download OsmChange data
 * (http://wiki.openstreetmap.org/wiki/OsmChange) with MapWithAI StreetLevel extensions
 *
 * @author Taylor Smock
 */
public class DownloadMapWithAIExtendedOsmChangeTask extends DownloadOsmChangeTask {

    @Nonnull
    @Override
    public Future<?> download(final DownloadParams settings, final Bounds downloadArea,
            final ProgressMonitor progressMonitor) {
        return loadUrl(settings,
                MessageFormat.format(MapWithAIStreetLevelConfig.getUrls().getMapWithAIStreetLevelUrl(), downloadArea.toBBox().toStringCSV(",")),
                progressMonitor);
    }

    @Nonnull
    @Override
    public String[] getPatterns() {
        return patterns(MapWithAIStreetLevelUrlPatterns.MapWithAIStreetLevelUrlPattern.class);
    }

    @Nonnull
    @Override
    public String getTitle() {
        return tr("Download MapWithAI Extended OSM Change");
    }

    @Nonnull
    @Override
    public Future<?> loadUrl(final DownloadParams settings, final String url, final ProgressMonitor progressMonitor) {
        final Optional<MapWithAIStreetLevelUrlPatterns.MapWithAIStreetLevelUrlPattern> urlPattern = Arrays.stream(
                        MapWithAIStreetLevelUrlPatterns.MapWithAIStreetLevelUrlPattern.values())
                .filter(p -> p.matches(url)).findFirst();
        downloadTask = new MapWithAIExtendedOSCDownloadTask(settings, new MapWithAIStreetLevelServerLocationReader(url), progressMonitor, true,
                Compression.byExtension(url));
        // Extract .osc filename from URL to set the new layer name
        extractOsmFilename(settings, urlPattern.orElse(MapWithAIStreetLevelUrlPatterns.MapWithAIStreetLevelUrlPattern.EXTERNAL_OSC_FILE).pattern(), url);
        return MainApplication.worker.submit(downloadTask);
    }

    /**
     * OsmChange download task.
     */
    protected class MapWithAIExtendedOSCDownloadTask extends DownloadOsmChangeTask.DownloadTask {
        private Collection<Suggestion<RemoteImageEntry, Collection<RemoteImageEntry>>> suggestions;

        /**
         * Constructs a new {@code DownloadTask}.
         *
         * @param settings          download settings
         * @param reader            OSM data reader
         * @param progressMonitor   progress monitor
         * @param zoomAfterDownload If true, the map view will zoom to download area
         *                          after download
         * @param compression       compression to use
         */
        public MapWithAIExtendedOSCDownloadTask(DownloadParams settings, MapWithAIStreetLevelServerLocationReader reader,
                ProgressMonitor progressMonitor, boolean zoomAfterDownload, Compression compression) {
            super(settings, reader, progressMonitor, zoomAfterDownload, compression);
        }

        @Override
        protected MapWithAIStreetLevelLayer addNewLayerIfRequired(String newLayerName) {
            long numDataLayers = getNumModifiableDataLayers();
            if (settings.isNewLayer() || numDataLayers == 0) {
                // the user explicitly wants a new layer, we don't have any layer at all
                // or it is not clear which layer to merge to
                final MapWithAIStreetLevelLayer layer = (MapWithAIStreetLevelLayer) createNewLayer(
                        Optional.ofNullable(newLayerName).filter(it -> !Utils.isStripEmpty(it)));
                MainApplication.getLayerManager().addLayer(layer, zoomAfterDownload);
                return layer;
            }
            return null;
        }

        @Override
        protected MapWithAIStreetLevelLayer createNewLayer(DataSet ds, Optional<String> layerName) {
            return new MapWithAIStreetLevelLayer(ds, layerName.orElse(tr("MapWithAI StreetLevel")), getEditLayer());
        }

        /**
         * Get the layer
         *
         * @param newLayerName The expected name of the layer
         * @return The layer
         */
        protected MapWithAIStreetLevelLayer getMapWithAIStreetLevelLayer(@Nullable String newLayerName) {
            Collection<MapWithAIStreetLevelLayer> layers = MainApplication.getLayerManager().getLayersOfType(
                    MapWithAIStreetLevelLayer.class);
            return layers.stream().filter(layer -> Objects.equals(newLayerName, layer.getName())).findFirst()
                    .orElseGet(() -> layers.stream().findAny().orElse(null));
        }

        @Override
        protected long getNumModifiableDataLayers() {
            return MainApplication.getLayerManager().getLayersOfType(MapWithAIStreetLevelLayer.class).size();
        }

        @Override
        protected void loadData(String newLayerName, Bounds bounds) {
            MapWithAIStreetLevelLayer layer = addNewLayerIfRequired(newLayerName);
            if (layer == null) {
                layer = getMapWithAIStreetLevelLayer(newLayerName);
                if (layer == null) {
                    layer = createNewLayer(this.dataSet,
                            Optional.ofNullable(newLayerName).filter(it -> !Utils.isStripEmpty(it)));
                }
                Collection<OsmPrimitive> primitivesToUpdate = searchPrimitivesToUpdate(bounds, layer.getDataSet());
                layer.mergeFrom(this.dataSet);
                if (!primitivesToUpdate.isEmpty()) {
                    MainApplication.worker.execute(new UpdatePrimitivesTask(layer, primitivesToUpdate));
                }
            }
            layer.addSuggestions(this.suggestions);
            layer.onPostDownloadFromServer(); // for existing and newly added layer, see #19816
        }

        @Override
        protected DataSet parseDataSet() throws OsmTransferException {
            final MapWithAIStreetLevelServerLocationReader realReader = (MapWithAIStreetLevelServerLocationReader) this.reader;
            final Pair<DataSet, Collection<Suggestion<RemoteImageEntry, Collection<RemoteImageEntry>>>> pair = realReader
                    .parseMapWithAIStreetLevelOsmChange(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false),
                            compression);
            this.suggestions = pair.b;
            return pair.a;
        }
    }

}
