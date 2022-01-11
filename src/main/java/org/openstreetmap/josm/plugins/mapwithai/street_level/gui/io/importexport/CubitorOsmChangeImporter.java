// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2022 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.io.MapWithAIStreetLevelChangeReader;
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.MapWithAIStreetLevelLayer;

/**
 * An importer class for OSM Change Files that have a &lt;cubitor-context&gt;
 * section.
 *
 * @author Taylor Smock
 */
public class CubitorOsmChangeImporter extends FileImporter {
    /**
     * File filter for OsmChange files.
     */
    private static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter
            .newFilterWithArchiveExtensions("cubitor.osc", "cubitor.osc", tr("Cubitor extended OsmChange File"), true);

    /** The instance of the file importer */
    public static final FileImporter INSTANCE = new CubitorOsmChangeImporter();

    /**
     * Constructs a new {@link CubitorOsmChangeImporter}
     */
    protected CubitorOsmChangeImporter() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        importData(Compression.getUncompressedFileInputStream(file), file, progressMonitor);
    }

    private void importData(InputStream uncompressedFileInputStream, File file, ProgressMonitor progressMonitor)
            throws IllegalDataException {
        final DataSet dataSet = MapWithAIStreetLevelChangeReader.parseDataSet(uncompressedFileInputStream,
                progressMonitor);
        final OsmDataLayer osmDataLayer = Optional.ofNullable(MainApplication.getLayerManager().getEditLayer())
                .orElseGet(() -> {
                    final OsmDataLayer dataLayer = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
                    MainApplication.getLayerManager().addLayer(dataLayer);
                    return dataLayer;
                });
        final MapWithAIStreetLevelLayer layer = new MapWithAIStreetLevelLayer(dataSet, file.getName(), osmDataLayer);
        MainApplication.getLayerManager().addLayer(layer);
        if (!layer.data.getDataSources().isEmpty()) {
            final DownloadParams downloadParams = new DownloadParams().withLayerName(osmDataLayer.getName());
            DownloadTask downloadTask = new DownloadOsmTask();
            layer.data.getDataSources().stream().map(source -> source.bounds)
                    .forEach(bounds -> downloadTask.download(downloadParams, bounds, NullProgressMonitor.INSTANCE));
        }
    }
}
