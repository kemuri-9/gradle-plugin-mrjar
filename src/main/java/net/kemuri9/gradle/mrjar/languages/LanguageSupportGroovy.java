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

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.GroovyBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.internal.JvmPluginsHelper;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.GroovySourceSet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.api.tasks.javadoc.Groovydoc;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;

/**
 * {@link LanguageSupport} for the groovy language
 */
public class LanguageSupportGroovy implements LanguageSupport {

    /**
     * {@link CompileHandler} for {@link GroovyCompile}
     */
    public static class GroovyCompileHandler implements CompileHandler<GroovyCompile> {
        @Override
        public void addDependency(Project project, GroovyCompile compile, AbstractCompile dependsOn) {
            compile.dependsOn(dependsOn);

            /* add to the classpath. Do NOT perform a contains check here as it causes the system
             * to resolve the entire compilation task graph! */
            FileCollection classpath = compile.getClasspath().plus(project.files(dependsOn.getDestinationDir()));
            compile.setClasspath(classpath);
        }

        @Override
        public void configure(Project project, SourceSet set, Action<? super GroovyCompile> configure) {
           project.getPlugins().withType(GroovyBasePlugin.class, plugin ->  {
                GroovyCompile groovyCompile = getTask(project, set);
                configure.execute(groovyCompile);
            });
        }

        @Override
        public GroovyCompile getTask(Project project, SourceSet set) {
            return (GroovyCompile) project.getTasks().getByName(set.getCompileTaskName("groovy"));
        }

        @Override
        public void setToolchain(GroovyCompile compile, JavaToolchainService javaToolchains, JavaVersion version) {
            Provider<JavaLauncher> launcher = javaToolchains.launcherFor(configure -> {
                configure.getLanguageVersion().set(JavaLanguageVersion.of(version.getMajorVersion()));
            });
            compile.getJavaLauncher().set(launcher);
        }

        @Override
        public void setVersionOptions(GroovyCompile compile, JavaVersion version) {
            compile.setSourceCompatibility(version.getMajorVersion());
            compile.setTargetCompatibility(version.getMajorVersion());
            compile.getOptions().getRelease().set(Integer.parseInt(version.getMajorVersion()));
        }
    }

    /**
     * {@link DocumentationHandler} for {@link Groovydoc}
     */
    public static class GroovydocHandler implements DocumentationHandler<Groovydoc> {

        @Override
        public void addSourceSet(Project project, Groovydoc task, SourceSet included) {
            DuplicateClassRemover dupRemover = new DuplicateClassRemover();
            task.source(getGroovySource(included).getAllGroovy());
            task.getSource().visit(dupRemover);
            task.exclude(dupRemover);
        }

        @Override
        public void configure(Project project, SourceSet set, Action<? super Groovydoc> configure) {
            project.getPlugins().withType(GroovyBasePlugin.class, plugin -> {
                Groovydoc groovydoc = getTask(project, set);
                configure.execute(groovydoc);
            });
        }

        /**
         * Retrieve the {@link GroovySourceSet} for a {@link SourceSet}
         * @param set {@link SourceSet} to retrieve the associated {@link GroovySourceSet}
         * @return {@link GroovySourceSet} for the {@link SourceSet}
         */
        public GroovySourceSet getGroovySource(SourceSet set) {
            GroovySourceSet groovySourceSet = (new DslObject(set)).getConvention().getPlugin(GroovySourceSet.class);
            return groovySourceSet;
        }

        /**
         * Retrieve the name of the {@link Groovydoc} task for the specified {@link SourceSet}
         * @param set {@link SourceSet} to retrieve its associated groovy documentation task name
         * @return groovy documentation task name for the {@link SourceSet}
         */
        public String getTaskName(SourceSet set) {
            return set.getTaskName(null, "groovydoc");
        }

        @Override
        public Groovydoc getTask(Project project, SourceSet set) {
            return (Groovydoc) project.getTasks().getByName(getTaskName(set));
        }

        @Override
        public void register(Project project, SourceSet set, JavaVersion version) {
            project.getPlugins().withType(GroovyBasePlugin.class, plugin -> {
                registerDocTask(project, set, version);
                registerDocJarTask(project, set, version);
            });
        }

        /**
         * Register the {@link Groovydoc} task for the specified {@link SourceSet}
         * @param project {@link Project} that is being built out
         * @param set {@link SourceSet} to register the documentation task for
         * @param version {@link JavaVersion} that the task is targeting
         */
        public void registerDocTask(Project project, SourceSet set, JavaVersion version) {
            project.getTasks().register(getTaskName(set), Groovydoc.class, groovyDoc -> {
                groovyDoc.setDescription("Generates Groovydoc API documentation for the " + set.getName() + " source code.");
                groovyDoc.setGroup("documentation");
                groovyDoc.setClasspath(set.getOutput().plus(set.getCompileClasspath()));
                GroovySourceSet groovySourceSet = getGroovySource(set);
                groovyDoc.source(groovySourceSet.getGroovy());
                File docDir = project.getConvention().getPlugin(JavaPluginConvention.class).getDocsDir();
                groovyDoc.setDestinationDir(new File(docDir, getTaskName(set)));
            });
        }

        /**
         * Register that a Jar will be built from the associated generated {@link Groovydoc}
         * @param project {@link Project} that is being built out
         * @param set {@link SourceSet} to register the documentation jar task for
         * @param version {@link JavaVersion} that the task is targeting
         */
        public void registerDocJarTask(Project project, SourceSet set, JavaVersion version) {
            boolean isMain = set.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME);
            String configName = isMain ? "groovydocElements" : set.getName() + "GroovydocElements";
            String taskName = getTaskName(set);
            TaskContainer tasks = project.getTasks();
            JvmPluginsHelper.configureDocumentationVariantWithArtifact(configName,
                isMain ? null : set.getName(), "groovydoc", Collections.emptyList(), taskName + "Jar",
                tasks.named(taskName), JvmPluginsHelper.findJavaComponent(project.getComponents()),
                project.getConfigurations(), tasks, project.getObjects());
            // add the target version
            Configuration config = project.getConfigurations().getByName(configName);
            config.getAttributes().attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
                    Integer.parseInt(version.getMajorVersion()));
        }
    }

    /** {@link GroovyCompileHandler} instance for handling {@link GroovyCompile} */
    protected GroovyCompileHandler compiler;

    /** {@link GroovydocHandler} instance for handling {@link Groovydoc} */
    protected GroovydocHandler documentation;

    /**
     * Create a new {@link LanguageSupportGroovy}
     */
    public LanguageSupportGroovy() {
        compiler = new GroovyCompileHandler();
        documentation = new GroovydocHandler();
    }

    @Override
    public GroovyCompileHandler getCompilerHandler() {
        return compiler;
    }

    @Override
    public Map<String, String> getConfigurationNames(SourceSet set) {
        // create a configuration for the groovy documentation, mimicing how plan javadoc functions
        String name = "groovydocElements";
        if (!set.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
            name = set.getName() + name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return Collections.singletonMap("groovydocElements", name);
    }

    @Override
    public GroovydocHandler getDocumentationHandler() {
        return documentation;
    }

    @Override
    public String getName() {
        return "groovy";
    }

    @Override
    public void initialize(Project project) {
        // create the base groovyDocElements configuration
        project.getPlugins().withType(GroovyBasePlugin.class, plugin -> {
            project.getConfigurations().maybeCreate("groovyDocElements");
        });
    }

    @Override
    public boolean isIncluded(Project project, SourceSet set) {
        if (!project.getPluginManager().hasPlugin("groovy")) {
            return false;
        }
        ExtensionVisitor visitor = new ExtensionVisitor(".groovy");
        // may not use allJava as that includes a **.java filter which defeats the point
        set.getAllSource().visit(visitor);
        return visitor.getIsContained();
    }

    @Override
    public void source(Project project, SourceSet set, Action<? super SourceDirectorySet> configure) {
        project.getPlugins().withType(GroovyBasePlugin.class, plugin -> {
            GroovySourceSet groovySource = (GroovySourceSet) new DslObject(set).getConvention().getPlugins().get("groovy");
            groovySource.groovy(configure);
        });
    }
}
