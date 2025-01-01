/**
 * Copyright 2021-2025 Steven Walters
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kemuri9.gradle.mrjar;

import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import net.kemuri9.gradle.mrjar.languages.LanguageSupport;

/**
 * Extension for defining the behavior and layout of the Multi-release Jar
 */
public interface MRJarExtension {

    /**
     * Add a new {@link LanguageSupport} to recognize other JVM-based languages
     * @param support {@link LanguageSupport} to recognize
     */
    void addLanguage(LanguageSupport support);

    /**
     * Add a JVM version to the multi-release jar
     * @param version version or "release" of Java to add to the jar.
     *  This can be any value that {@link JavaVersion#toVersion(Object)} supports
     * @param configure {@link Action} indicating the configuration of {@link Version}
     */
    void addVersion(Object version, Action<? super Version> configure);

    /**
     * Perform a configuration on all added configured {@link Version}s
     * @param configure {@link Action} indicating the configuration to apply on all current added {@link Version}s
     */
    void allAddedVersions(Action<? super Version> configure);

    /**
     * Perform a configuration on all configured {@link Version}s.
     * This will not configure the "base" version if it has not already been configured at least once.
     * @param configure {@link Action} indicating the configuration to apply on all current {@link Version}s
     */
    void allVersions(Action<? super Version> configure);

    /**
     * Perform a configuration on the base or "main" version of code
     * @param configure {@link Action} indicating the configuration to apply on the main version
     */
    void baseVersion(Action<? super Version> configure);

    /**
     * State of the tests for all added versions also including the "main" tests.
     * @return {@link Property} for the state of the version centric tests using the "main" tests.
     */
    @Input
    Property<Boolean> getIncludeBaseTests();

    /**
     * Retrieve the current registered languages
     * @return current registered languages
     */
    @Internal
    Map<String, LanguageSupport> getLanguages();

    /**
     * State of the source jar being multi-release, similar to the compiled code jar
     * @return {@link Property} for the state of the source jar being multi-release
     */
    @Input
    Property<Boolean> getMultireleaseSourceJar();

    /**
     * State of the tests utilizing the Jar instead of the class files directly.
     * For some scenarios where module definitions are provided, the version of a class file desired to be
     * tested may not be able to be tested directly due to module patching behaviors.
     * So instead of testing class files, test the actual multi-release Jar.
     * @return {@link Property} for the state of the tests using the Jar in testing.
     */
    @Input
    Property<Boolean> getUseJarInTests();

    /**
     * State of all tasks using version specific Java toolchains to perform the tasks with
     * @return state of all tasks using version specific Java toolchains to perform the tasks with
     */
    @Input
    Property<Boolean> getUseToolchain();
}
