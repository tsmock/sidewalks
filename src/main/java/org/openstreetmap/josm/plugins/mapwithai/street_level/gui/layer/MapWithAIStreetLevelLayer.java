// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2022 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import javax.annotation.Nonnull;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ImageData;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
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
import org.openstreetmap.josm.plugins.mapwithai.commands.MapWithAIAddCommand;
import org.openstreetmap.josm.plugins.mapwithai.street_level.actions.downloadtasks.DownloadMapWithAIExtendedOsmChangeTask;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.ImageSourceProvider;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.Suggestion;

/**
 * The layer storing street-level AI data
 */
public class MapWithAIStreetLevelLayer extends OsmDataLayer implements DataSourceListener {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");
    private static final String IMAGE_LAYER_NAME = marktr("Mapillary Images (MapWithAI)");
    private final Collection<Suggestion<?, ?>> suggestions = new HashSet<>();

    /**
     * Various conversion methods between latlon and tiles
     */
    private static class Conversions {
        /* The tile zoom the api accepts */
        private static final int TILE_ZOOM = 16;

        /**
         * Convert a latlon to a tile xy coordinate
         *
         * @param latLon the latlon to convert
         * @return The tile xy point coordinate
         */
        static Point latLonToTileXY(final ILatLon latLon) {
            final int x = (int) Math.floor(Math.pow(2, TILE_ZOOM) * (180 + latLon.lon()) / 360);
            final int y = (int) Math.floor(Math.pow(2, TILE_ZOOM) * (1
                    - (Math.log(Math.tan(Math.toRadians(latLon.lat())) + 1 / Math.cos(Math.toRadians(latLon.lat())))
                            / Math.PI))
                    / 2);
            return new Point(x, y);
        }

        /**
         * Convert a tile xy point to the NW latlon for that tile
         *
         * @param tileXY The tile point to conver
         * @return The NW latlon of the tile
         */
        static LatLon pointToNWLatLon(final Point tileXY) {
            final double latTemp = Math.PI - 2 * Math.PI * tileXY.y / Math.pow(2, TILE_ZOOM);
            final double lat = 180 / Math.PI * Math.atan((Math.exp(latTemp) - Math.exp(-latTemp)) / 2);
            final double lon = (tileXY.x / Math.pow(2, TILE_ZOOM)) * 360 - 180;
            return new LatLon(lat, lon);
        }

        /**
         * Convert a tile xy to a bounds
         *
         * @param tileXY The tile xy to convert
         * @return The bounds
         */
        static Bounds tileXYToBounds(final Point tileXY) {
            LatLon nw = pointToNWLatLon(tileXY);
            LatLon se = pointToNWLatLon(new Point(tileXY.x + 1, tileXY.y + 1));
            return new Bounds(nw, se);
        }

        /**
         * Convert a bounds to a series of tile bounds
         *
         * @param bounds The bounds to convert
         * @return The stream of the tile xy bounds
         */
        static Stream<Bounds> boundsToTile(final Bounds bounds) {
            final Point minXMaxYPoint = latLonToTileXY(bounds.getMin());
            final Point maxXMinYPoint = latLonToTileXY(bounds.getMax());

            return IntStream.rangeClosed(minXMaxYPoint.x, maxXMinYPoint.x)
                    .mapToObj(
                            x -> IntStream.rangeClosed(maxXMinYPoint.y, minXMaxYPoint.y).mapToObj(y -> new Point(x, y)))
                    .flatMap(stream -> stream).map(Conversions::tileXYToBounds);
        }
    }

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
                .filter(b -> b.isValid() && !b.isCollapsed() && !b.isOutOfTheWorld()).flatMap(Conversions::boundsToTile)
                .distinct().parallel().forEach(this::download);
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
    public <I extends GpxImageEntry & ImageSourceProvider, T extends Collection<I>> void addSuggestions(
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
        Optional<GeoImageLayer> layerOptional = MainApplication.getLayerManager().getLayersOfType(GeoImageLayer.class)
                .stream().filter(layer -> tr(IMAGE_LAYER_NAME).equals(layer.getName())).findFirst();
        final GeoImageLayer layer;
        if (!layerOptional.isPresent()) {
            layer = new GeoImageLayer(new ArrayList<>(), null);
            layer.setName(tr(IMAGE_LAYER_NAME));
            MainApplication.getLayerManager().addLayer(layer);
        } else {
            layer = layerOptional.get();
        }
        final List<ImageEntry> images = event.getSelection().stream().filter(p -> p.hasKey("suggestion-id"))
                .map(p -> p.get("suggestion-id")).filter(NUMBER_PATTERN.asPredicate()).mapToLong(Long::parseLong)
                .mapToObj(id -> this.suggestions.stream().filter(suggestion -> suggestion.getIdentifier() == id))
                .flatMap(i -> i).flatMap(suggestion -> suggestion.getImageEntries().getCollection().stream())
                .filter(ImageEntry.class::isInstance).map(ImageEntry.class::cast).collect(Collectors.toList());
        // Remove images on layer
        removeImages(layer.getImageData(), new ArrayList<>(layer.getImageData().getImages()));
        addImages(layer.getImageData(), images);
        if (!images.isEmpty()) {
            layer.getImageData().setSelectedImage(images.get(0));
        } else {
            layer.getImageData().clearSelectedImage();
        }
        layer.getImageData().notifyImageUpdate();
    }

    private static void removeImages(ImageData data, Collection<ImageEntry> imageEntries) {
        for (ImageEntry imageEntry : imageEntries) {
            data.removeImage(imageEntry);
            data.fireNodeMoved(imageEntry);
            imageEntry.setDataSet(null);
        }
    }

    private static void addImages(ImageData data, Collection<ImageEntry> imageEntries) {
        for (ImageEntry imageEntry : imageEntries) {
            imageEntry.setDataSet(data);
            data.getImages().add(imageEntry);
            data.fireNodeMoved(imageEntry);
        }
    }

    @Override
    public boolean autosave(File file) throws IOException {
        Files.deleteIfExists(file.toPath());
        return false;
    }

    @Override
    public String getChangesetSourceTag() {
        if (UndoRedoHandler.getInstance().getUndoCommands().stream().filter(MapWithAIAddCommand.class::isInstance)
                .map(MapWithAIAddCommand.class::cast)
                .anyMatch(command -> this.data.equals(command.getAffectedDataSet()))) {
            return "MapWithAI StreetLevel";
        }
        return null;
    }
}
