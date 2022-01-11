// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2022 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.io;

import javax.xml.stream.Location;

import org.xml.sax.Locator;

/**
 * An adaptor from {@link Location} to {@link Locator}
 *
 * @author Taylor Smock
 */
public class XmlLocationToLocator implements Locator {
    private final Location location;

    public XmlLocationToLocator(final Location location) {
        this.location = location;
    }

    @Override
    public String getPublicId() {
        return this.location.getPublicId();
    }

    @Override
    public String getSystemId() {
        return this.location.getSystemId();
    }

    @Override
    public int getLineNumber() {
        return this.location.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        return this.location.getColumnNumber();
    }
}
