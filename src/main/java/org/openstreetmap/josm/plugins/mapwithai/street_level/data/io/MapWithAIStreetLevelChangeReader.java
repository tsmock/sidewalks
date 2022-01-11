// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2022 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmChangeReader;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.StreetViewImageSet;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.Suggestion;
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.geoimage.RemoteImageEntry;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.XmlParsingException;

/**
 * The class to actually read MapWithAI extended changesets. This is extremely
 * similar to {@link OsmChangeReader}.
 *
 * @author Taylor Smock
 */
public class MapWithAIStreetLevelChangeReader extends OsmChangeReader {
    /** An id string */
    private static final String ID = "id";
    /** The suggestion-id key value */
    private static final String SUGGESTION_ID = "suggestion-id";
    private final Collection<StreetViewImageSet<RemoteImageEntry, Collection<RemoteImageEntry>>> streetViewImageSets = new HashSet<>();
    private final Collection<Suggestion<RemoteImageEntry, Collection<RemoteImageEntry>>> suggestionCollection = new HashSet<>();

    /**
     * constructor (for private and subclasses use only)
     *
     * @see #parseDataSet(InputStream, ProgressMonitor)
     */
    protected MapWithAIStreetLevelChangeReader() {
        // Restricts visibility
    }

    /**
     * Parse the given input source and return the dataset.
     *
     * @param source          the source input stream. Must not be
     *                        <code>null</code>.
     * @param progressMonitor the progress monitor. If <code>null</code>,
     *                        {@link org.openstreetmap.josm.gui.progress.NullProgressMonitor#INSTANCE}
     *                        is assumed
     *
     * @return the dataset with the parsed data
     * @throws IllegalDataException     if the an error was found while parsing the
     *                                  data from the source
     * @throws IllegalArgumentException if source is <code>null</code>
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor)
            throws IllegalDataException {
        return new MapWithAIStreetLevelChangeReader().doParseDataSet(source, progressMonitor);
    }

    /**
     * Parse the given input source and return the dataset and suggestions.
     *
     * @param source          the source input stream. Must not be
     *                        <code>null</code>.
     * @param progressMonitor the progress monitor. If <code>null</code>,
     *                        {@link org.openstreetmap.josm.gui.progress.NullProgressMonitor#INSTANCE}
     *                        is assumed
     *
     * @return the dataset with the parsed data and the parsed suggestions
     * @throws IllegalDataException     if the an error was found while parsing the
     *                                  data from the source
     * @throws IllegalArgumentException if source is <code>null</code>
     */
    public static Pair<DataSet, Collection<Suggestion<RemoteImageEntry, Collection<RemoteImageEntry>>>> parseDataSetAndSuggestions(
            InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        final MapWithAIStreetLevelChangeReader mapWithAIStreetLevelChangeReader = new MapWithAIStreetLevelChangeReader();
        return new Pair<>(mapWithAIStreetLevelChangeReader.doParseDataSet(source, progressMonitor),
                mapWithAIStreetLevelChangeReader.suggestionCollection);
    }

    @Override
    public Node parseNode() throws XMLStreamException {
        // IntelliJ doesn't consider this a valid method reference
        // TODO check if this is still the case after 2022-01-01
        return parseSuggestionId(() -> super.parseNode());
    }

    @Override
    public Relation parseRelation() throws XMLStreamException {
        // IntelliJ doesn't consider this a valid method reference
        // TODO check if this is still the case after 2022-01-01
        return parseSuggestionId(() -> super.parseRelation());
    }

    @Override
    protected void parseUnknown() throws XMLStreamException {
        if ("cubitor-context".equals(parser.getLocalName())) {
            try {
                this.parseCubitorContext();
            } catch (XmlParsingException e) {
                throw new XMLStreamException(e);
            }
        } else {
            super.parseUnknown();
        }
    }

    @Override
    protected Way parseWay() throws XMLStreamException {
        // IntelliJ doesn't consider this a valid method reference
        // TODO check if this is still the case after 2022-01-01
        return parseSuggestionId(() -> super.parseWay());
    }

    // Duplicate code with OsmReader#parseWayNode TODO make protected?
    protected long parseWayNode2(WayData w) throws XMLStreamException {
        if (parser.getAttributeValue(null, "ref") == null) {
            throwException(tr("Missing mandatory attribute ''{0}'' on <nd> of way {1}.", "ref",
                    Long.toString(w.getUniqueId())));
        }
        long id = getLong2("ref").orElse(0);
        if (id == 0) {
            throwException(tr("Illegal value of attribute ''ref'' of element <nd>. Got {0}.", Long.toString(id)));
        }
        jumpToEnd();
        return id;
    }

    // Duplicate code with OsmReader#getLong TODO make protected?
    private OptionalLong getLong2(String name) throws XMLStreamException {
        String value = parser.getAttributeValue(null, name);
        try {
            return OptionalLong.of(getLong(name, value));
        } catch (IllegalDataException e) {
            throwException(e);
        }
        return OptionalLong.empty(); // may happen -- if there are no images, there might not be an id
    }

    private void parseCubitorContext() throws XMLStreamException, XmlParsingException {
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (Optional.ofNullable(parser.getLocalName()).filter(string -> string.endsWith("-suggestion"))
                        .isPresent()) {
                    parseSuggestion();
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private WayData parseOsmRoad() throws XMLStreamException, XmlParsingException {
        final long identifier = getLong2("way-id").orElseThrow(() -> new XmlParsingException("No id for way-id")
                .rememberLocation(new XmlLocationToLocator(this.parser.getLocation())));
        WayData wayData = new WayData(identifier);
        List<Long> nodeIds = new ArrayList<>();
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("nd".equals(parser.getLocalName())) {
                    nodeIds.add(parseWayNode2(wayData));
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        wayData.setNodeIds(nodeIds);
        return wayData;
    }

    private void parseSuggestion() throws XMLStreamException, XmlParsingException {
        final long identifier = getLong2(ID).orElseThrow(() -> new XmlParsingException("No id for suggestion")
                .rememberLocation(new XmlLocationToLocator(this.parser.getLocation())));
        final Collection<PrimitiveData> primitiveData = new HashSet<>();
        final List<StreetViewImageSet<RemoteImageEntry, Collection<RemoteImageEntry>>> streetViewImageSet = new ArrayList<>();
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("osm-road".equals(parser.getLocalName())) {
                    primitiveData.add(parseOsmRoad());
                } else if ("street-view-image-set".equals(parser.getLocalName())) {
                    streetViewImageSet.add(parseStreetViewImageSet());
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        streetViewImageSets.addAll(streetViewImageSet);
        Suggestion<RemoteImageEntry, Collection<RemoteImageEntry>> suggestion = new Suggestion<>(identifier,
                primitiveData, streetViewImageSet.get(0));
        suggestionCollection.add(suggestion);
    }

    private RemoteImageEntry parseStreetViewImage() throws XMLStreamException, IOException {
        final String id = parser.getAttributeValue(null, "id");
        final double ca = Double.parseDouble(parser.getAttributeValue(null, "ca"));
        final double lat = Double.parseDouble(parser.getAttributeValue(null, "lat"));
        final double lon = Double.parseDouble(parser.getAttributeValue(null, "lon"));
        parser.next();
        final RemoteImageEntry imageEntry = new RemoteImageEntry();
        imageEntry.setPos(new LatLon(lat, lon));
        imageEntry.setExifCoor(imageEntry.getExifCoor());
        imageEntry.setExifImgDir(ca);
        imageEntry.setKey(id);
        return imageEntry;
    }

    private StreetViewImageSet<RemoteImageEntry, Collection<RemoteImageEntry>> parseStreetViewImageSet()
            throws XMLStreamException, XmlParsingException {
        final long identifier = getLong2(ID)
                .orElseThrow(() -> new XmlParsingException("No id for street view image set")
                        .rememberLocation(new XmlLocationToLocator(this.parser.getLocation())));
        final Optional<StreetViewImageSet<RemoteImageEntry, Collection<RemoteImageEntry>>> encounteredEntry = streetViewImageSets
                .stream().filter(entry -> identifier == entry.getIdentifier()).findAny();
        if (encounteredEntry.isPresent()) {
            return encounteredEntry.get();
        }
        final List<RemoteImageEntry> imageEntries = new ArrayList<>();
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("street-view-image".equals(parser.getLocalName())) {
                    try {
                        imageEntries.add(parseStreetViewImage());
                    } catch (IOException ioException) {
                        Logging.error(ioException);
                    }
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return new StreetViewImageSet<>(identifier, Collections.unmodifiableList(imageEntries));
    }

    /**
     * Get the suggestion id
     *
     * @param supplier The primitive supplier
     * @param <T>      The primitive type
     * @param <E>      The exception thrown
     * @return A new object with a {@link #SUGGESTION_ID} tag
     * @throws E see {@link ThrowableSupplier#get()}
     */
    private <T extends IPrimitive, E extends Exception> T parseSuggestionId(final ThrowableSupplier<T, E> supplier)
            throws E {
        // Parse first, as otherwise we may not get the right object
        final String suggestion = parser.getAttributeValue(null, SUGGESTION_ID);
        T object = supplier.get();
        object.put(SUGGESTION_ID, suggestion);
        return object;
    }

    /**
     * An interface that allows throwing
     *
     * @param <T> The object type to return
     * @param <E> The exception type
     */
    private interface ThrowableSupplier<T, E extends Exception> {
        /**
         * Get the appropriate object
         *
         * @return The supplied object
         * @throws E An exception if something occurs. You better know what it its.
         */
        T get() throws E;
    }
}
