// SPDX-License-Identifier: GPL-2.0-or-later
// SPDX-FileCopyrightText: 2021 Taylor Smock <tsmock@fb.com>
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapwithai.street_level.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.preferences.MapWithAIStreetLevelConfig;
import org.openstreetmap.josm.plugins.mapwithai.street_level.spi.preferences.IUrls;
import org.openstreetmap.josm.tools.Logging;

/**
 * An annotation to ensure that the config class is appropriately set
 *
 * @author Taylor Smock
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ExtendWith(MapWithAIStreetLevelConfigAnnotation.MapWithAIStreetLevelConfigExtension.class)
public @interface MapWithAIStreetLevelConfigAnnotation {
    /**
     * This is used to set a "bad" url class
     */
    class BadUrlClass implements IUrls {
        private static RuntimeException getException() {
            return new UnsupportedOperationException("Please use @MapWithAIStreetLevelConfigAnnotation in tests");
        }

        @Override
        public String getMapWithAIStreetLevelUrl() {
            throw getException();
        }
    }

    /**
     * An extension to set the config information
     */
    class MapWithAIStreetLevelConfigExtension
            implements AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {
        /**
         * Create an object using as many args as possible
         *
         * @param clazz The class to instantiate
         * @param args  The args to use
         * @param <T>   The object type
         * @return A new object
         * @throws NoSuchMethodException     if no suitable constructor is found
         * @throws InvocationTargetException see {@link Constructor#newInstance}
         * @throws InstantiationException    see {@link Constructor#newInstance}
         * @throws IllegalAccessException    see {@link Constructor#newInstance}
         */
        private static <T> T createBestObject(final Class<T> clazz, final Object... args) throws NoSuchMethodException,
                InvocationTargetException, InstantiationException, IllegalAccessException {
            final List<Class<?>> classes = Stream.of(args).map(Object::getClass)
                    .collect(Collectors.toCollection(ArrayList::new));
            final List<Object> argsList = new ArrayList<>(Arrays.asList(args));
            final Object[] emptyObjectArray = new Object[0];
            final Class<?>[] emptyClassArray = new Class<?>[0];
            while (!argsList.isEmpty()) {
                try {
                    final Constructor<T> constructor = clazz.getDeclaredConstructor(classes.toArray(emptyClassArray));
                    final T newObject = ReflectionUtils.newInstance(constructor, argsList.toArray(emptyObjectArray));
                    if (newObject != null) {
                        return newObject;
                    }
                } catch (NoSuchMethodException e) {
                    Logging.trace(e);
                }
                argsList.remove(argsList.size() - 1);
                classes.remove(classes.size() - 1);
            }
            return ReflectionUtils.newInstance(clazz);
        }

        @Override
        public void afterAll(ExtensionContext context) {
            MapWithAIStreetLevelConfig.setUrls(new BadUrlClass());
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            final Optional<MapWithAIStreetLevelConfigAnnotation> annotation = AnnotationSupport.findAnnotation(context.getElement(),
                    MapWithAIStreetLevelConfigAnnotation.class);
            if (annotation.isPresent()) {
                this.afterAll(context);
            }
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            Optional<MapWithAIStreetLevelConfigAnnotation> annotation = AnnotationSupport.findAnnotation(context.getElement(),
                    MapWithAIStreetLevelConfigAnnotation.class);
            if (annotation.isPresent()) {
                Class<? extends IUrls> clazz = annotation.get().urlClass();
                MapWithAIStreetLevelConfig.setUrls(createBestObject(clazz, context));
            }
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            final Optional<MapWithAIStreetLevelConfigAnnotation> annotation = AnnotationSupport.findAnnotation(context.getElement(),
                    MapWithAIStreetLevelConfigAnnotation.class);
            if (MapWithAIStreetLevelConfig.getUrls() == null || (annotation.isPresent()
                    && !annotation.get().urlClass().equals(MapWithAIStreetLevelConfig.getUrls().getClass()))) {
                this.beforeAll(context);
            }
        }
    }

    /**
     * This is used to ensure that URLs are mocked
     */
    class WiremockUrlClass implements IUrls {
        private final String wiremockUrl;

        WiremockUrlClass(final ExtensionContext context) {
            throw new UnsupportedOperationException("Wiremock not currently implemented");
        }

        @Override
        public String getMapWithAIStreetLevelUrl() {
            // FIXME
            return wiremockUrl + "/cubitor";
        }
    }

    /**
     * Set the URL class to use.
     *
     * @return The url class
     */
    Class<? extends IUrls> urlClass() default WiremockUrlClass.class;
}
