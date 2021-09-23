// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.testutils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ModifierSupport;
import org.opentest4j.AssertionFailedError;

/**
 * Utility class test utils
 *
 * @author Taylor Smock
 */
public final class UtilityClassTestUtil {
    private UtilityClassTestUtil() {
        // hide constructor
    }

    /**
     * Check that a Utility class follows "best practices" for a utility class.
     *
     * @param clazz The class to check
     */
    public static void assertUtilityClassWellDefined(Class<?> clazz) throws AssertionFailedError {
        assertFinalClass(clazz);
        assertNoPublicConstructors(clazz);
        assertOnlyPublicStaticMethods(clazz);
        assertOnlyPublicStaticFinalFields(clazz);
    }

    private static void assertOnlyPublicStaticFinalFields(Class<?> clazz) throws AssertionFailedError {
        final List<Field> badFields = Stream.concat(Stream.of(clazz.getDeclaredFields()), Stream.of(clazz.getFields()))
                .distinct().filter(ModifierSupport::isPublic).collect(Collectors.toList());
        assertTrue(badFields.isEmpty(), badFields.stream().map(Field::getName).collect(
                Collectors.joining(System.lineSeparator(), "Non-private fields:" + System.lineSeparator(), "")));
    }

    private static void assertOnlyPublicStaticMethods(Class<?> clazz) throws AssertionFailedError {
        final List<Method> objectMethods = Arrays.asList(Object.class.getDeclaredMethods());
        final List<Method> badMethods = Stream
                .concat(Stream.of(clazz.getDeclaredMethods()), Stream.of(clazz.getMethods())).distinct()
                .filter(ModifierSupport::isNotPrivate).filter(ModifierSupport::isNotStatic)
                .filter(method -> !objectMethods.contains(method)).collect(Collectors.toList());
        assertTrue(badMethods.isEmpty(), badMethods.stream().map(Method::getName).collect(Collectors
                .joining(System.lineSeparator(), "Non-private non-static methods:" + System.lineSeparator(), "")));
    }

    private static void assertFinalClass(Class<?> clazz) throws AssertionFailedError {
        assertTrue(ModifierSupport.isFinal(clazz), "Class " + clazz.getSimpleName() + " should be final");
    }

    private static void assertNoPublicConstructors(Class<?> clazz) throws AssertionFailedError {
        final List<Constructor<?>> badConstructors = Stream
                .concat(Stream.of(clazz.getConstructors()), Stream.of(clazz.getDeclaredConstructors())).distinct()
                .filter(ModifierSupport::isNotPrivate).collect(Collectors.toList());
        assertTrue(badConstructors.isEmpty(), badConstructors.stream().map(Constructor::toGenericString).collect(
                Collectors.joining(System.lineSeparator(), "Non-private constructors:" + System.lineSeparator(), "")));
    }

    /**
     * Test class for {@link UtilityClassTestUtil}
     */
    static class UtilityClassTestUtilTest {
        @Test
        void testOnlyPublicStaticMethods() {
            assertDoesNotThrow(() -> assertOnlyPublicStaticMethods(OnlyPublicStaticMethods.class));
            assertThrows(AssertionFailedError.class, () -> assertOnlyPublicStaticMethods(FinalClass.class));
            assertThrows(AssertionFailedError.class,
                    () -> assertOnlyPublicStaticMethods(NoPublicConstructorsClass.class));
        }

        @Test
        void testFinalClass() {
            assertDoesNotThrow(() -> assertFinalClass(FinalClass.class));
            assertThrows(AssertionFailedError.class, () -> assertFinalClass(NoPublicConstructorsClass.class));
            assertThrows(AssertionFailedError.class, () -> assertFinalClass(OnlyPublicStaticMethods.class));
        }

        @Test
        void testNoPublicConstructors() {
            assertDoesNotThrow(() -> assertNoPublicConstructors(NoPublicConstructorsClass.class));
            assertThrows(AssertionFailedError.class, () -> assertNoPublicConstructors(FinalClass.class));
            assertThrows(AssertionFailedError.class, () -> assertNoPublicConstructors(OnlyPublicStaticMethods.class));
        }

        @Test
        void testGoodUtilityClass() {
            assertDoesNotThrow(() -> assertUtilityClassWellDefined(GoodUtilityClass.class));
        }
    }

    /**
     * A known-bad utility class that is partially correct
     */
    static class OnlyPublicStaticMethods {
        public boolean checkBoolean;

        public static boolean checkObject(Object object) {
            return object != null;
        }
    }

    static final class FinalClass {
        public boolean checkObject(Object object) {
            return object != null;
        }
    }

    // CHECKSTYLE.OFF: FinalClass
    /**
     * A known-bad utility class that is partially correct
     */
    static class NoPublicConstructorsClass {
        private NoPublicConstructorsClass() {
            // Hide constructor
        }

        public boolean checkObject(Object object) {
            return object != null;
        }
    }
    // CHECKSTYLE.ON: FinalClass

    /**
     * A known-bad utility class that is partially correct
     */
    static final class GoodUtilityClass {
        private static final GoodUtilityClass INSTANT = new GoodUtilityClass();

        private GoodUtilityClass() {
            // Hide constructor
        }

        public static boolean checkObject(Object object) {
            return INSTANT.realCheckObject(object);
        }

        private boolean realCheckObject(Object object) {
            return this.equals(object);
        }
    }
}
