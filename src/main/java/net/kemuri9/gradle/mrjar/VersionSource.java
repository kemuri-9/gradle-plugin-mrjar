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
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.provider.Property;

/**
 * Configuration of "main" source code or "test" source code targeting a specific version of Java.
 * Any recognized JVM-based language can be configured
 */
@SuppressWarnings("rawtypes")
public interface VersionSource extends NamedDomainObjectContainer<VersionSourceLanguage> {

    /**
     * Specify a dependency on source code targeting a prior version of Java
     * @param version version or "release" of Java to add to the jar.
     *  This can be any value that {@link JavaVersion#toVersion(Object)} supports
     */
    void dependsOn(Object version);

    /**
     * Specify a dependency on source code targeting a prior version of Java
     * @param version version or "release" of Java to add to the jar.
     *  This can be any value that {@link JavaVersion#toVersion(Object)} supports
     * @param configure {@link Action} to configure the associated {@link VersionDependency}
     */
    void dependsOn(Object version, Action<VersionDependency> configure);

    /**
     * Retrieve the name of the module that this source set patches, if applicable.
     * @return {@link Property} of {@link String} indicating the name of the module that this source set patches
     */
    Property<String> getPatchesModule();

    /**
     * Register that a source jar should be generated for this specific version source
     */
    void registerSourceJar();
}
