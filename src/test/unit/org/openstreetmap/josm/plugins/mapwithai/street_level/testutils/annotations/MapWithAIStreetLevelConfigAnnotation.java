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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openstreetmap.josm.plugins.mapwithai.street_level.data.preferences.MapWithAIStreetLevelConfig;
import org.openstreetmap.josm.plugins.mapwithai.street_level.spi.preferences.IUrls;
import org.openstreetmap.josm.tools.Logging;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

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
        public void afterAll(ExtensionContext context) throws Exception {
            MapWithAIStreetLevelConfig.setUrls(new BadUrlClass());
            getUrlsExtension(context, AfterAllCallback.class, AfterAllCallback::afterAll);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            final Optional<MapWithAIStreetLevelConfigAnnotation> annotation = AnnotationSupport
                    .findAnnotation(context.getElement(), MapWithAIStreetLevelConfigAnnotation.class);
            if (annotation.isPresent()) {
                this.afterAll(context);
            }
            getUrlsExtension(context, AfterEachCallback.class, AfterEachCallback::afterEach);
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            Optional<MapWithAIStreetLevelConfigAnnotation> annotation = AnnotationSupport
                    .findAnnotation(context.getElement(), MapWithAIStreetLevelConfigAnnotation.class);
            if (annotation.isPresent()) {
                Class<? extends IUrls> clazz = annotation.get().urlClass();
                try {
                    MapWithAIStreetLevelConfig
                            .setUrls(clazz.getConstructor(ExtensionContext.class).newInstance(context));
                } catch (NoSuchMethodException noSuchMethodException) {
                    Logging.trace(noSuchMethodException);
                    MapWithAIStreetLevelConfig.setUrls(clazz.getConstructor().newInstance());
                }
                final var store = context
                        .getStore(ExtensionContext.Namespace.create(MapWithAIStreetLevelConfigAnnotation.class));
                store.put(IUrls.class, MapWithAIStreetLevelConfig.getUrls());
            }
            getUrlsExtension(context, BeforeAllCallback.class, BeforeAllCallback::beforeAll);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            final Optional<MapWithAIStreetLevelConfigAnnotation> annotation = AnnotationSupport
                    .findAnnotation(context.getElement(), MapWithAIStreetLevelConfigAnnotation.class);
            if (MapWithAIStreetLevelConfig.getUrls() == null || (annotation.isPresent()
                    && !annotation.get().urlClass().equals(MapWithAIStreetLevelConfig.getUrls().getClass()))) {
                this.beforeAll(context);
            }
            getUrlsExtension(context, BeforeEachCallback.class, BeforeEachCallback::beforeEach);
        }

        private static <T extends Extension> void getUrlsExtension(ExtensionContext context, Class<T> clazz,
                CheckedRunnable<T, ?> runnable) throws Exception {
            final var store = context
                    .getStore(ExtensionContext.Namespace.create(MapWithAIStreetLevelConfigAnnotation.class));
            final var extension = store.get(IUrls.class, IUrls.class);
            if (clazz.isInstance(extension)) {
                runnable.accept(clazz.cast(extension), context);
            }
        }

        @FunctionalInterface
        interface CheckedRunnable<T extends Extension, E extends Exception> {
            void accept(T extension, ExtensionContext context) throws E;
        }
    }

    /**
     * This is used to ensure that URLs are mocked
     */
    class WiremockUrlClass extends WireMockExtension implements IUrls {
        private String wiremockUrl;

        protected WiremockUrlClass() {
            super(WireMockExtension.extensionOptions());
        }

        @Override
        protected void onBeforeEach(WireMockRuntimeInfo wireMockRuntimeInfo) {
            super.onBeforeEach(wireMockRuntimeInfo);
            final WireMock wireMockServer = Objects.requireNonNull(wireMockRuntimeInfo.getWireMock(),
                    "Is @Wiremock used?");
            this.wiremockUrl = wireMockRuntimeInfo.getHttpBaseUrl();
            final Map<String, StringValuePattern> queryParams = new HashMap<>(9);
            queryParams.put("result_type", WireMock.equalTo("extended_osc"));
            queryParams.put("conflate_with_osm", WireMock.equalTo("true"));
            queryParams.put("theme", WireMock.equalTo("streetview_ai_suggestion"));
            queryParams.put("collaborator", WireMock.equalTo("rapid"));
            queryParams.put("token", WireMock.equalTo(
                    "ASbYX8wITNCWnU1XMF1V-d2_iRiBMKmW2nT85IhjS4TOQXie-YJMCOGppe-DiCxUSfQ4hG4MDxyfXIprF5YO3QNR"));
            queryParams.put("hash", WireMock.equalTo("ASaPD6M5i29Nf8jGGb0"));
            queryParams.put("ext", WireMock.equalTo("1918681607"));
            queryParams.put("sources", WireMock.equalTo("fb_footway"));
            queryParams.put("bbox", WireMock.matching("([0-9-.]+,?){4}"));
            wireMockServer.register(WireMock.get("/cubitor").withQueryParams(queryParams).willReturn(
                    WireMock.aResponse().withBodyFile("cubitor/-122.3492432,47.6098665,-122.34375,47.6135698.xml")));
        }

        @Override
        public String getMapWithAIStreetLevelUrl() {
            return wiremockUrl + "/cubitor?bbox={0}";
        }
    }

    /**
     * Set the URL class to use.
     *
     * @return The url class
     */
    Class<? extends IUrls> urlClass() default WiremockUrlClass.class;
}
