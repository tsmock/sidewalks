// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.Suggestion;
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.geoimage.RemoteImageEntry;
import org.openstreetmap.josm.tools.Pair;

/**
 * A server reader for MapWithAI StreetLevel extended OSc
 *
 * @author Taylor Smock
 */
public class MapWithAIStreetLevelServerLocationReader extends OsmServerLocationReader {
    /**
     * Constructs a new {@code OsmServerLocationReader}.
     *
     * @param url The URL to fetch
     */
    public MapWithAIStreetLevelServerLocationReader(String url) {
        super(url);
    }

    /**
     * Parse an extended OSC change
     *
     * @param progressMonitor The progress monitor to report to
     * @param compression     The compression
     * @return A pair of dataset and suggestions for it
     * @throws OsmTransferException If something couldn't transfer
     */
    public Pair<DataSet, Collection<Suggestion<RemoteImageEntry, Collection<RemoteImageEntry>>>> parseMapWithAIStreetLevelOsmChange(
            ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        final MapWithAIStreetLevelOsmChangeParser parser = new MapWithAIStreetLevelOsmChangeParser(progressMonitor,
                compression);
        return doParse(parser, progressMonitor);
    }

    /**
     * The method used to parse extended OSC changesets. Mostly used to change the
     * parser.
     */
    protected class MapWithAIStreetLevelOsmChangeParser
            extends Parser<Pair<DataSet, Collection<Suggestion<RemoteImageEntry, Collection<RemoteImageEntry>>>>> {
        Collection<Suggestion<RemoteImageEntry, Collection<RemoteImageEntry>>> suggestions;

        protected MapWithAIStreetLevelOsmChangeParser(ProgressMonitor progressMonitor, Compression compression) {
            super(progressMonitor, compression);
        }

        @Override
        public Pair<DataSet, Collection<Suggestion<RemoteImageEntry, Collection<RemoteImageEntry>>>> parse()
                throws IOException, OsmTransferException, IllegalDataException {
            in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
            if (in == null)
                return null;
            progressMonitor.subTask(tr("Downloading OSM data..."));
            return MapWithAIStreetLevelChangeReader.parseDataSetAndSuggestions(
                    compression.getUncompressedInputStream(in), progressMonitor.createSubTaskMonitor(1, false));
        }
    }
}
