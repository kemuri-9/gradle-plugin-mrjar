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

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

class VersionDependencyImpl implements VersionDependency {

    private final Property<Boolean> dependsOnSource;
    private final Property<Boolean> dependsOnTest;
    private final Property<Boolean> extendConfigurations;

    @javax.inject.Inject
    public VersionDependencyImpl(ObjectFactory factory) {
        dependsOnSource = factory.property(Boolean.class);
        dependsOnTest = factory.property(Boolean.class);
        extendConfigurations = factory.property(Boolean.class);
    }

    @Override
    public Property<Boolean> getDependsOnSource() {
        return dependsOnSource;
    }

    @Override
    public Property<Boolean> getDependsOnTest() {
        return dependsOnTest;
    }

    @Override
    public Property<Boolean> getExtendConfigurations() {
        return extendConfigurations;
    }
}
