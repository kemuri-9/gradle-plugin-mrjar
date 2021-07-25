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

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/**
 * Version details for the base version
 */
class VersionBase extends VersionCommon {

    @javax.inject.Inject
    public VersionBase(Project project, ObjectFactory factory) {
        super(project, factory);
    }

    @Override
    boolean getIncludesBaseTests() {
        return false;
    }

    @Override
    public Property<Boolean> getIncludeBaseTests() {
        throw new IllegalStateException("base version already includes base tests.");
    }

    @Override
    String getTestName() {
        return "test";
    }

    @Override
    public JavaVersion getVersion() {
        return Utils.getExtension(project).getBaseVersion();
    }
}
