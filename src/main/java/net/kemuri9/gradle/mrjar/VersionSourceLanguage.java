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
import org.gradle.api.Named;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.compile.AbstractCompile;

/**
 * Configuration of a JVM-based language for source targeting a specific version of Java
 */
public interface VersionSourceLanguage extends Named {

    /**
     * Configure the compilation task
     * @param configure {@link Action} to perform that configures the compilation task
     */
    void compile(Action<? super AbstractCompile> configure);

    /**
     * Configure the documentation task
     * @param configure {@link Action} to perform that configures the documentation task
     */
    void documentation(Action<? super SourceTask> configure);

    @Override
    String getName();

    /**
     * Configure the source directory for the language
     * @param configure {@link Action} to perform that configures the {@link SourceDirectorySet}
     */
    void src(Action<SourceDirectorySet> configure);

    /**
     * Compiled language outputs (class files) that compiling this source code requires.
     * Not specifying any language defaults to all languages.
     * @return compiled JVM-language outputs that this compilation depends on
     */
    @Input
    SetProperty<String> getDependsOnLanguages();
}
