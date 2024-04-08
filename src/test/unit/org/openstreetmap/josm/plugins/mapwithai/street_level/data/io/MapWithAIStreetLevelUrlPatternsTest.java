// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021-2024 Taylor Smock <tsmock@fb.com>
package org.openstreetmap.josm.plugins.mapwithai.street_level.data.io;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.UtilityClassTestUtil;

/**
 * Test class for {@link MapWithAIStreetLevelUrlPatterns}
 *
 * @author Taylor Smock
 */
class MapWithAIStreetLevelUrlPatternsTest {
    static Stream<Arguments> testEnums() {
        return Stream.of(Arguments.of(MapWithAIStreetLevelUrlPatterns.MapWithAIStreetLevelUrlPattern.class));
    }

    @ParameterizedTest
    @MethodSource
    void testEnums(Class<? extends Enum<?>> enumClazz) {
        TestUtils.superficialEnumCodeCoverage(enumClazz);
    }

    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(MapWithAIStreetLevelUrlPatterns.class);
    }
}
