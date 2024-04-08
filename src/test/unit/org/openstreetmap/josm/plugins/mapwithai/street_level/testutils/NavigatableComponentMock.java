// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.testutils;

import java.awt.Point;

import org.openstreetmap.josm.gui.NavigatableComponent;

import mockit.Mock;
import mockit.MockUp;

/**
 * A class for mocking navigatable components
 */
public class NavigatableComponentMock extends MockUp<NavigatableComponent> {
    /**
     * Get the point on screen where the component is
     *
     * @return The location for testing purposes
     * @see NavigatableComponent#getLocationOnScreen()
     */
    @Mock
    public Point getLocationOnScreen() {
        return new Point(30, 40);
    }

    /**
     * Check to see whether or not the component is visible
     *
     * @return {@code true} for testing purposes
     * @see NavigatableComponent#isVisibleOnScreen()
     */
    @Mock
    protected boolean isVisibleOnScreen() {
        return true;
    }

    /**
     * Get the width
     *
     * @return The static width for tests
     * @see NavigatableComponent#getWidth()
     */
    @Mock
    public int getWidth() {
        return 800;
    }

    /**
     * Get the height
     *
     * @return The static height for tests
     * @see NavigatableComponent#getHeight()
     */
    @Mock
    public int getHeight() {
        return 600;
    }
}
