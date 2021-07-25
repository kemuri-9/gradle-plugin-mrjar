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

/**
 * Extension placed on tasks to indicate some of the modular details.
 */
public class Modularity {

    /** State of Modularity */
    public static enum State {

        /** Is a declared module: includes the module-info.java */
        IS_MODULE,

        /** Is a module fragment */
        MODULE_PATCH,

        /** Not modular */
        NOT_MODULE;
    }

    /** Name of the Module. Applicable when is modular related */
    public String moduleName;

    /** {@link State} of the modularity */
    public State state;

    /**
     * Create a new {@link Modularity} with the specified state
     * @param state {@link State} of the {@link Modularity}
     */
    public Modularity(State state) {
        this(state, null);
    }

    /**
     * Create a new {@link Modularity} with the specified details
     * @param state {@link State} of the {@link Modularity}
     * @param moduleName name of the module that is associated with the state
     */
    public Modularity(State state, String moduleName) {
        this.state = state;
        this.moduleName = moduleName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64).append(state);
        if (moduleName != null) {
            sb.append(": \"").append(moduleName).append("\"");
        }
        return sb.toString();
    }

    /**
     * Update the existing {@link Modularity} based on new details
     * @param newModularity new {@link Modularity} details to update from
     */
    public void update(Modularity newModularity) {
        this.moduleName = newModularity.moduleName;
        this.state = newModularity.state;
    }
}
