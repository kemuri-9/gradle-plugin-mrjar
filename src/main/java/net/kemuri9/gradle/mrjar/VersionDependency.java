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

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

/**
 * Configuration of a dependency against a prior major version of Java
 */
public interface VersionDependency {

    /**
     * State of depending on the targeted Java version's source code.
     * <p>Applies to both source code and test code</p>
     * @return state of depending on the targeted Java version's source code
     */
    @Input
    Property<Boolean> getDependsOnSource();

    /**
     * State of depending on the targeted Java version's test code.
     * <p>Only applies to test code</p>
     * @return state of depending on the targeted Java version's test code
     */
    @Input
    Property<Boolean> getDependsOnTest();

    /**
     * State of extending from the targeted Java version's configurations.
     * By extending the configurations dependencies and classpaths will be inherited
     * @return state of extending from the targeted Java version's configurations.
     */
    @Input
    Property<Boolean> getExtendConfigurations();
}
