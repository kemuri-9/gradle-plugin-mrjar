/**
 * Copyright 2021 Steven Walters
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

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

/**
 * A Version of JVM-language source added to the multi-release JAR.
 */
public interface Version {

    /**
     * State of the tests for the version also including the "main" tests.
     * Overrides behavior from {@link MRJarExtension#getIncludeBaseTests()}
     * @return {@link Property} for the state of the version centric tests using the "main" tests.
     */
    @Input
    public Property<Boolean> getIncludeBaseTests();

    /**
     * State of the version providing the primary Module definition that goes into the root of the JAR.
     * When {@code true}, the {@code module-info} is located into the root of the JAR instead of the versioned folder.
     * @return {@link Property} for the state of the version providing the primary Module definition.
     */
    @Input
    public Property<Boolean> getPrimaryModuleDefinition();

    /**
     * State of the tests utilizing the Jar instead of the class files directly.
     * For some scenarios where module definitions are provided, the version of a class file desired to be
     * tested may not be able to be tested directly due to module patching behaviors.
     * So instead of testing class files, test the actual multi-release Jar.
     * Overrides behavior from {@link MRJarExtension#getUseJarInTests()}
     * @return {@link Property} for the state of the tests using the Jar in testing.
     */
    @Input
    public Property<Boolean> getUseJarInTests();

    /**
     * State of the version using the associated version-specific Java toolchain to perform tasks with.
     * Overrides behavior from {@link MRJarExtension#getUseToolchain()}
     * @return {@link Property} for the state of the version using version-specific Java toolchain to perform tasks
     */
    @Input
    public Property<Boolean> getUseToolchain();

    /**
     * Version of Java that the configuration applies to
     * @return version of Java that the configuration applies to
     */
    public JavaVersion getVersion();

    /**
     * Configure the main source code for the JVM-language version
     * @param configure {@link Action} indicating the configuration to apply on {@link VersionSource}
     */
    void main(Action<VersionSource> configure);

    /**
     * Configure the test source code for the JVM-language version
     * @param configure {@link Action} indicating the configuration to apply on {@link VersionSource}
     */
    void test(Action<VersionSource> configure);
}
