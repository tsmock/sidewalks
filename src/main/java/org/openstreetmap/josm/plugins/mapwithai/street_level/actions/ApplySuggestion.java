// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.mapwithai.commands.MapWithAIAddCommand;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.suggestions.Suggestion;
import org.openstreetmap.josm.plugins.mapwithai.street_level.gui.layer.MapWithAIStreetLevelLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Apply suggestions from cubitor context
 * @author Taylor Smock
 */
public class ApplySuggestion extends JosmAction {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");
    private static final String SUGGESTION_ID = "suggestion-id";

    /**
     * Create a new {@link ApplySuggestion} action
     */
    public ApplySuggestion() {
        super(tr("MapWithAI StreetLevel: Apply suggestion"), null,
                tr("MapWithAI StreetLevel: Apply Suggestion to current data layer"),
                Shortcut.registerShortcut("mapwithai:streetlevel:apply_suggestion",
                        tr("MapWithAI StreetLevel: Apply Suggestion"),
                KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), false, true);
    }

    @Override
    protected void updateEnabledState(final Collection<? extends OsmPrimitive> selection) {
        super.updateEnabledState(selection);
        this.setEnabled(selection.stream().anyMatch(primitive -> primitive.hasKey(SUGGESTION_ID))
        && MainApplication.getLayerManager().getActiveLayer() instanceof MapWithAIStreetLevelLayer);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final Layer layer = MainApplication.getLayerManager().getActiveLayer();
        if (layer instanceof MapWithAIStreetLevelLayer) {
            final MapWithAIStreetLevelLayer mapWithAIStreetLevelLayer = (MapWithAIStreetLevelLayer) layer;
            final DataSet dataSet = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class)
                    .stream().filter(i -> !i.equals(layer)).filter(i -> layer.getName().contains(i.getName()))
                    .map(OsmDataLayer::getDataSet).findFirst().orElse(null);
            if (dataSet != null) {
                final List<Suggestion<?, ?>> suggestions = mapWithAIStreetLevelLayer.getDataSet()
                        .getSelected().stream().filter(p -> p.hasKey(SUGGESTION_ID)).map(p -> p.get(SUGGESTION_ID))
                        .filter(NUMBER_PATTERN.asPredicate()).mapToLong(Long::parseLong)
                        .mapToObj(id -> mapWithAIStreetLevelLayer.getSuggestions().stream().filter(sug -> sug.getIdentifier() == id))
                        .flatMap(i -> i).collect(Collectors.toList());
                // TODO finish merge
                final MapWithAIAddCommand mapwithAiAddCommand = new MapWithAIAddCommand(
                        mapWithAIStreetLevelLayer.getDataSet(), dataSet, mapWithAIStreetLevelLayer.data.getSelected());
                UndoRedoHandler.getInstance().add(mapwithAiAddCommand);
            }
        }
    }
}
