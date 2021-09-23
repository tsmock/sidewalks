// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxImageEntry;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSourceChangeEvent;
import org.openstreetmap.josm.data.osm.DataSourceListener;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.plugins.mapwithai.street_level.actions.downloadtasks.DownloadMapWithAIExtendedOsmChangeTask;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.Suggestion;

/**
 * The layer storing street-level AI data
 */
public class MapWithAIStreetLevelLayer extends OsmDataLayer implements DataSourceListener {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");
    private static final String IMAGE_LAYER_NAME = marktr("Mapillary Images (MapWithAI)");
    private final Collection<Suggestion<?, ?>> suggestions = new HashSet<>();

    /**
     * Construct a new {@code OsmDataLayer}.
     *
     * @param data         OSM data
     * @param name         Layer name
     * @param osmDataLayer The OSM layer to follow
     */
    public MapWithAIStreetLevelLayer(@Nonnull DataSet data, @Nonnull String name, @Nonnull OsmDataLayer osmDataLayer) {
        super(data, name, null);
        osmDataLayer.getDataSet().addDataSourceListener(this);
        osmDataLayer.getDataSet().getDataSources().stream().map(dataSource -> dataSource.bounds)
                .filter(b -> b.isValid() && !b.isCollapsed() && !b.isOutOfTheWorld()).forEach(this::download);
        this.getDataSet().setUploadPolicy(UploadPolicy.BLOCKED);
        this.getDataSet().setDownloadPolicy(DownloadPolicy.BLOCKED);
    }

    /**
     * Add suggestions to the layer
     *
     * @param suggestions The suggestions to add
     * @param <T>         The collection type
     * @param <I>         The image entry type
     */
    public <I extends GpxImageEntry, T extends Collection<I>> void addSuggestions(
            Collection<Suggestion<I, T>> suggestions) {
        this.suggestions.addAll(suggestions);
    }

    @Override
    public void dataSourceChange(@Nonnull DataSourceChangeEvent event) {
        event.getAdded().forEach(dataSource -> this.download(dataSource.bounds));
    }

    /**
     * Get the suggestions for the layer
     *
     * @return The suggestions
     */
    public Collection<Suggestion<?, ?>> getSuggestions() {
        return Collections.unmodifiableCollection(this.suggestions);
    }

    /**
     * Download the data for a specific bounding box
     *
     * @param bounds The bounds to download
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    void download(@Nonnull final Bounds bounds) {
        final DownloadTask downloadTask = new DownloadMapWithAIExtendedOsmChangeTask();
        final DownloadParams downloadParams = new DownloadParams();
        downloadParams.withLayerName(this.getName());
        downloadParams.withNewLayer(false);
        downloadTask.download(downloadParams, bounds, NullProgressMonitor.INSTANCE);
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        super.selectionChanged(event);
        // Keep images visible so users can select different images.
        if (event.getSelection().isEmpty()) {
            return;
        }
        Optional<GeoImageLayer> layerOptional = MainApplication.getLayerManager().getLayersOfType(GeoImageLayer.class).stream()
                .filter(layer -> tr(IMAGE_LAYER_NAME).equals(layer.getName())).findFirst();
        final GeoImageLayer layer;
        if (!layerOptional.isPresent()) {
            layer = new GeoImageLayer(new ArrayList<>(), null);
            layer.setName(tr(IMAGE_LAYER_NAME));
            MainApplication.getLayerManager().addLayer(layer);
        } else {
            layer = layerOptional.get();
        }
        final List<ImageEntry> images = event.getSelection().stream()
                .filter(p -> p.hasKey("suggestion-id")).map(p -> p.get("suggestion-id"))
                .filter(NUMBER_PATTERN.asPredicate()).mapToLong(Long::parseLong)
                .mapToObj(id -> this.suggestions.stream().filter(suggestion -> suggestion.getIdentifier() == id))
                .flatMap(i -> i)
                .flatMap(suggestion -> suggestion.getImageEntries().getCollection().stream())
                .filter(ImageEntry.class::isInstance).map(ImageEntry.class::cast)
                .collect(Collectors.toList());
        // Remove images on layer
        new ArrayList<>(layer.getImageData().getImages()).forEach(layer.getImageData()::removeImage);
        layer.getImageData().getImages().addAll(images);
        if (!images.isEmpty()) {
            layer.getImageData().setSelectedImage(images.get(0));
            images.forEach(layer.getImageData()::fireNodeMoved);
        } else {
            layer.getImageData().clearSelectedImage();
        }
        layer.getImageData().notifyImageUpdate();
    }
}
