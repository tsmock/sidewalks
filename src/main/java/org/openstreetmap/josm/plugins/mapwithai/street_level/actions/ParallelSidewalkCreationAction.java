// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.osm.ParallelSidewalkCreation;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * An action for creating parallel sidewalks
 *
 * @author Taylor Smock
 */
public class ParallelSidewalkCreationAction extends JosmAction {
    /**
     * Create a new ParallelSidewalkCreation action
     */
    public ParallelSidewalkCreationAction() {
        super(tr("Create parallel sidewalks"), (String) null, tr("Add parallel sidewalks"),
                Shortcut.registerShortcut("data:auto_sidewalk:parallel_sidewalks", tr("Add parallel sidewalks"),
                        KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                true, "auto_sidewalk:parallel_sidewalks", true);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final DataSet dataSet = MainApplication.getLayerManager().getActiveDataSet();
        if (dataSet == null) {
            return;
        }
        final List<Way> ways = new ArrayList<>(dataSet.getSelectedWays());
        if (ways.size() == 1) {
            final Map<ParallelSidewalkCreation.Options, Way> sidewalks = ParallelSidewalkCreation
                    .createParallelSidewalks(ways.get(0), ParallelSidewalkCreation.Options.values());
            final List<PrimitiveData> dataCollection = new ArrayList<>(
                    sidewalks.values().size() + sidewalks.values().stream().mapToInt(Way::getNodesCount).sum());
            sidewalks.values().stream().map(Way::getNodes).flatMap(Collection::stream).map(OsmPrimitive::save)
                    .forEach(dataCollection::add);
            final List<PrimitiveData> newSidewalks = sidewalks.values().stream().map(Way::save)
                    .collect(Collectors.toList());
            dataCollection.addAll(newSidewalks);
            Command addCommand = new AddPrimitivesCommand(dataCollection, newSidewalks, dataSet);
            UndoRedoHandler.getInstance().add(addCommand);
        }
    }
}
