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
package net.kemuri9.gradle.mrjar.languages;

import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.jvm.toolchain.JavaToolchainService;

import net.kemuri9.gradle.mrjar.MRJarExtension;
import net.kemuri9.gradle.mrjar.Modularity;

/**
 * Interface defining the registration of a JVM-based language.
 * This can be registered either using the Service Loader pattern, or
 * using {@link MRJarExtension#addLanguage(LanguageSupport)}.
 */
public interface LanguageSupport {

    /**
     * Base type for handling task extensions related to the language support
     * @param <T> Type of {@link DefaultTask}
     */
    public interface TaskHandler<T extends DefaultTask> {
        /**
         * Configure the task for the specified {@link SourceSet}.
         * The dependent plugin for the task may not yet be applied,
         * so the configuration should be guaranteed to be applied after the plugin is applied.
         * @param project {@link Project} that is being built out
         * @param set {@link SourceSet} to configure the task for
         * @param configure {@link Action} indicating the configuration to perform on the task
         */
        public void configure(Project project, SourceSet set, Action<? super T> configure);

        /**
         * Retrieve the task's {@link Modularity}
         * @param task {@link DefaultTask} task to retrieve its {@link Modularity}
         * @return {@link Modularity} for the {@link DefaultTask}
         */
        public default Modularity getModularity(T task) {
            return getTaskModularity(task);
        }

        /**
         * Retrieve the task for the specified {@link SourceSet}
         * @param project {@link Project} that is being built out
         * @param set {@link SourceSet} to retrieve the compilation task for
         * @return task for the specified {@link SourceSet}
         * @throws UnknownTaskException When the expected task does not exist
         * @throws ClassCastException When the expected task is not of the expected type
         */
        public T getTask(Project project, SourceSet set);

        /**
         * Configure the task's modularity, when supported.
         * @param task {@link DefaultTask} task to configure
         * @param modularity {@link Modularity} details of the {@link DefaultTask}'s modularity
         */
        public default void setModularity(T task, Modularity modularity) {
           setTaskModularity(task, modularity);
        }

        /**
         * Configure the task to utilize the specified version of Java to compile with
         * @param task {@link Task} task to configure
         * @param javaToolchains {@link JavaToolchainService} that manages Java tool chains in Gradle
         * @param version version of Java to utilize in the task
         */
        public default void setToolchain(T task, JavaToolchainService javaToolchains, JavaVersion version) {};

        /**
         * Configure the task to target a specific version of Java
         * @param task {@link Task} task to configure
         * @param version version of Java to compile with
         */
        public default void setVersionOptions(T task, JavaVersion version) {};
    }

    /**
     * Handler associated to the compilation task associated to the language.
     *
     * @param <T> Type of {@link AbstractCompile}
     */
    public interface CompileHandler<T extends AbstractCompile> extends TaskHandler<T> {
        /**
         * Flag that the specified task depends on the output of a compilation
         * @param project {@link Project} that is being built out
         * @param task {@link AbstractCompile} that depends on the output of another
         * @param dependedOn {@link AbstractCompile} that is depended on
         */
        public void addDependency(Project project, T task, AbstractCompile dependedOn);
    }

    /**
     * Handler associated to the documentation task associated to the language.
     *
     * @param <T> Type of {@link SourceTask}
     */
    public interface DocumentationHandler<T extends SourceTask> extends TaskHandler<T> {

        /**
         * Flag that the specified task includes the {@link SourceSet} for processing
         * @param project {@link Project} that is being built out
         * @param task {@link SourceTask} that depends on the output of another
         * @param included {@link SourceSet} to include
         */
        public void addSourceSet(Project project, T task, SourceSet included);

        /**
         * Register the documentation task(s) for the specified {@link SourceSet}.
         * The dependent plugin for the task may not yet be applied,
         * so the configuration should be guaranteed to be applied after the plugin is applied.
         * @param project {@link Project} that is being built out
         * @param set {@link SourceSet} to register the documentation task for
         * @param version {@link JavaVersion} that the task is targeting
         */
        public void register(Project project, SourceSet set, JavaVersion version);
    }

    /**
     * Retrieve the task's {@link Modularity}
     * @param task {@link DefaultTask} task to retrieve its {@link Modularity}
     * @return {@link Modularity} for the {@link DefaultTask}
     */
    static Modularity getTaskModularity(DefaultTask task) {
        return task.getExtensions().findByType(Modularity.class);
    }

    /**
     * Configure the task's base modularity.
     * @param task {@link DefaultTask} task to configure its modularity
     * @param modularity {@link Modularity} details of the {@link DefaultTask}'s modularity
     */
    static void setTaskModularity(DefaultTask task, Modularity modularity) {
        Modularity taskMod = task.getExtensions().findByType(Modularity.class);
        if (taskMod == null) {
            task.getExtensions().add("modularity", modularity);
        } else {
            taskMod.update(modularity);
        }
    }

    /**
     * Retrieve the handler for the compilation task for the language.
     * This is called frequently, so it should be cached by the implementation.
     * @return {@link CompileHandler} for the language
     */
    public CompileHandler<?> getCompilerHandler();

    /**
     * Retrieve the handler for the documentation task for the language.
     * This is called frequently, so it should be cached by the implementation.
     * @return {@link DocumentationHandler} for the language
     */
    public DocumentationHandler<?> getDocumentationHandler();

    /**
     * Retrieve the configuration name mapping for the source set added by the language.
     * @param set {@link SourceSet} to retrieve the configuration name mapping
     * @return {@link Map} of "standard" configuration names to the corresponding name for the source set
     */
    public Map<String, String> getConfigurationNames(SourceSet set);

    /**
     * Retrieve the name of the JVM-based language
     * @return name of the JVM-based language
     */
    public String getName();

    /**
     * Perform any necessary initialization routines on the {@link Project}
     * @param project {@link Project} to perform initialization routines on
     */
    public default void initialize(Project project) {};

    /**
     * Check if this JVM-based language is utilized in the specified {@link SourceSet}
     * @param project {@link Project} that contains the {@link SourceSet}
     * @param set {@link SourceSet} to configure
     * @return state of the JVM-based language being utilized in the {@link SourceSet}
     */
    public boolean isIncluded(Project project, SourceSet set);

    /**
     * Configure the {@link SourceDirectorySet} for the language
     * @param project {@link Project} that the configuration is for
     * @param set {@link SourceSet} source set that contains the {@link SourceDirectorySet}
     * @param configure configuration {@link Action} on {@link SourceDirectorySet} to perform
     */
    public void source(Project project, SourceSet set, Action<? super SourceDirectorySet> configure);
}
