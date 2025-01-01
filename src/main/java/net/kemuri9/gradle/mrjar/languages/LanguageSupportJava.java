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
package net.kemuri9.gradle.mrjar.languages;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.*;
import org.gradle.api.plugins.internal.JvmPluginsHelper;
import org.gradle.api.provider.Provider;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.jvm.toolchain.JavaCompiler;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavadocTool;

import net.kemuri9.gradle.mrjar.Modularity;
import net.kemuri9.gradle.mrjar.PatchProvider;

/**
 * {@link LanguageSupport} for the base Java language
 */
public class LanguageSupportJava implements LanguageSupport {

    /**
     * {@link CompileHandler} for {@link JavaCompile}
     */
    public static class JavaCompileHandler implements CompileHandler<JavaCompile> {
        @Override
        public void addDependency(Project project, JavaCompile compile, AbstractCompile dependsOn) {
            compile.dependsOn(dependsOn);

            Modularity compileMod = LanguageSupport.getTaskModularity(compile);
            Modularity dependsMod = LanguageSupport.getTaskModularity(dependsOn);
            // if this task is a module and the target is a module fragment then it needs to be patched in
            if (compileMod != null && compileMod.state == Modularity.State.IS_MODULE &&
                    dependsMod != null && dependsMod.state == Modularity.State.MODULE_PATCH) {
                addDependencyModulePatch(project, compile, dependsOn);
            } else {
                addDependencyClasspath(project, compile, dependsOn);
            }
        }

        /**
         * Add the output of a compilation task to the classpath
         * @param project {@link Project} being built out
         * @param compile {@link JavaCompile} to add to its classpath
         * @param dependsOn {@link AbstractCompile} to add to the classpath
         */
        public void addDependencyClasspath(Project project, JavaCompile compile, AbstractCompile dependsOn) {
            /* add to the classpath (module path when it applies).
             * Do NOT perform a contains check here as it causes the system
             * to resolve the entire compilation task graph! */
            FileCollection classpath = compile.getClasspath().plus(project.files(dependsOn.getDestinationDirectory()));
            compile.setClasspath(classpath);
        }

        /**
         * Add the output of a compilation task to the module path
         * @param project {@link Project} being built out
         * @param compile {@link JavaCompile} to add to its module path
         * @param dependsOn {@link AbstractCompile} to add to the module path
         */
        public void addDependencyModulePatch(Project project, JavaCompile compile, AbstractCompile dependsOn) {
            // add patch for the output directory of the dependent
            PatchProvider provider = PatchProvider.getProvider(compile.getOptions().getCompilerArgumentProviders());
            String moduleName = LanguageSupport.getTaskModularity(dependsOn).moduleName;
            provider.add(moduleName, dependsOn.getDestinationDirectory());
        }

        @Override
        public void configure(Project project, SourceSet set, Action<? super JavaCompile> configure) {
            project.getPlugins().withType(JavaBasePlugin.class, plugin -> {
                JavaCompile javaCompile = getTask(project, set);
                configure.execute(javaCompile);
            });
        }

        @Override
        public JavaCompile getTask(Project project, SourceSet set) {
            return (JavaCompile) project.getTasks().getByName(set.getCompileJavaTaskName());
        }

        @Override
        public void setModularity(JavaCompile task, Modularity modularity) {
            if (task.getModularity().getInferModulePath().get()) {
                CompileHandler.super.setModularity(task, modularity);
            }
        }

        @Override
        public void setToolchain(JavaCompile compile, JavaToolchainService javaToolchains, JavaVersion version) {
            Provider<JavaCompiler> compiler = javaToolchains.compilerFor(configure -> {
                configure.getLanguageVersion().set(JavaLanguageVersion.of(version.getMajorVersion()));
            });
            compile.getJavaCompiler().set(compiler);
        }

        @Override
        public void setVersionOptions(JavaCompile compile, JavaVersion version) {
            compile.setSourceCompatibility(version.getMajorVersion());
            compile.setTargetCompatibility(version.getMajorVersion());
            if (version.isCompatibleWith(JavaVersion.VERSION_1_10)) {
                // set release if >= 10, skip 9 because it is broken there
                compile.getOptions().getRelease().set(Integer.parseInt(version.getMajorVersion()));
            }
        }
    }

    /**
     * {@link DocumentationHandler} for {@link Javadoc}
     */
    public static class JavadocHandler implements DocumentationHandler<Javadoc> {

        @Override
        public void addSourceSet(Project project, Javadoc task, SourceSet include) {
            addSourceSetSource(project, task, include);
            Modularity docMod = LanguageSupport.getTaskModularity(task);
            if (docMod != null && docMod.state != Modularity.State.NOT_MODULE) {
                addSourceSetModule(project, task, include);
            }
        }

        /**
         * Add the {@link SourceSet} to the source files to process in the {@link Javadoc} task
         * @param project {@link Project} that is being built out
         * @param task {@link Javadoc} task to add to the classpath
         * @param include {@link SourceSet} to add to the source files to process
         */
        public void addSourceSetSource(Project project, Javadoc task, SourceSet include) {
            /* inclusion of all java is required for module path resolution to work,
             * so it must be added and the duplicates most be removed after */
            DuplicateClassRemover dupRemover = new DuplicateClassRemover();
            task.source(include.getAllJava());
            task.getSource().visit(dupRemover);
            task.exclude(dupRemover);
        }

        /**
         * Add the {@link SourceSet} to the module path of the {@link Javadoc} task
         * @param project {@link Project} that is being built out
         * @param task {@link Javadoc} task to add to its module path
         * @param include {@link SourceSet} to add to the source files to process
         */
        public void addSourceSetModule(Project project, Javadoc task, SourceSet include) {
            ExtensionContainer taskExts = task.getExtensions();
            PatchProvider provider = (PatchProvider) taskExts.findByName("patchModule");
            if (provider == null) {
                provider = new PatchProvider();
                ((CoreJavadocOptions) task.getOptions()).addOption(provider);
                taskExts.add("patchModule", provider);
            }

            try {
                JavaCompile compile = (JavaCompile) project.getTasks().getByName(include.getCompileJavaTaskName());
                Modularity compMod = LanguageSupport.getTaskModularity(compile);
                if (compMod != null && compMod.state == Modularity.State.MODULE_PATCH) {
                    provider.add(compMod.moduleName, compile.getDestinationDirectory());
                } else if (compMod != null && compMod.state == Modularity.State.IS_MODULE) {
                   task.setClasspath(task.getClasspath().plus(project.files(compile.getDestinationDirectory())));
                }
                task.dependsOn(compile);
            } catch (UnknownTaskException | ClassCastException ex) {
                return;
            }
        }

        @Override
        public void configure(Project project, SourceSet set, Action<? super Javadoc> configure) {
            project.getPlugins().withType(JavaBasePlugin.class, plugin -> {
                Javadoc javadoc = getTask(project, set);
                configure.execute(javadoc);
            });
        }

        @Override
        public Javadoc getTask(Project project, SourceSet set) {
            return (Javadoc) project.getTasks().getByName(getTaskName(set));
        }

        /**
         * Retrieve the name of the Javadoc task for the specified {@link SourceSet}
         * @param set {@link SourceSet} to retrieve the name of the Javadoc task for
         * @return name of the Javadoc task for {@link SourceSet}
         */
        protected String getTaskName(SourceSet set) {
            return set.getJavadocTaskName();
        }

        @Override
        public void register(Project project, SourceSet set, JavaVersion version) {
            project.getPlugins().withType(JavaBasePlugin.class, plugin -> {
                registerDocTask(project, set, version);
                registerDocJarTask(project, set, version);
            });
        }

        /**
         * Register the Javadoc task for the specified {@link SourceSet}
         * @param project {@link Project} that is being built out
         * @param set {@link SourceSet} to register the task for
         * @param version {@link JavaVersion} that the task is targeting
         */
        public void registerDocTask(Project project, SourceSet set, JavaVersion version) {
            // register the javadoc task
            project.getTasks().register(getTaskName(set), Javadoc.class, javadoc -> {
                JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
                javadoc.setDescription("Generates Javadoc API documentation for the " + set.getName() + " source code.");
                javadoc.setGroup("documentation");
                javadoc.getConventionMapping().map("destinationDir", ()-> new File(java.getDocsDir().getAsFile().get(), getTaskName(set)));
                javadoc.getConventionMapping().map("title", ()-> project.getExtensions().getByType(ReportingExtension.class).getApiDocTitle());
                javadoc.setClasspath(set.getOutput().plus(set.getCompileClasspath()));
                javadoc.source(set.getAllJava());
                javadoc.getOptions().source(version.getMajorVersion());
            });
        }

        /**
         * Register the Jar task that will Jar the associated generated Javadoc
         * @param project {@link Project} that is being built out
         * @param set {@link SourceSet} to register the task for
         * @param version {@link JavaVersion} that the task is targeting
         */
        public void registerDocJarTask(Project project, SourceSet set, JavaVersion version) {
            // register the javadoc jar task
            boolean isMain = set.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME);
            Configuration config = JvmPluginsHelper.createDocumentationVariantWithArtifact(set.getJavadocElementsConfigurationName(),
                    isMain ? null : set.getName(), org.gradle.api.attributes.DocsType.JAVADOC, Collections.emptySet(),
                    set.getJavadocJarTaskName(), getTask(project, set).getOutputs(), (ProjectInternal) project);
            // add the target version
            config.getAttributes().attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
                    Integer.parseInt(version.getMajorVersion()));
        }

        @Override
        public void setModularity(Javadoc task, Modularity modularity) {
            if (task.getModularity().getInferModulePath().get()) {
                DocumentationHandler.super.setModularity(task, modularity);
            }
        }

        @Override
        public void setToolchain(Javadoc task, JavaToolchainService javaToolchains, JavaVersion version) {
            Provider<JavadocTool> tool = javaToolchains.javadocToolFor(configure -> {
                configure.getLanguageVersion().set(JavaLanguageVersion.of(version.getMajorVersion()));
            });
            task.getJavadocTool().set(tool);
        }
    }

    /** {@link JavaCompileHandler} instance for handling {@link JavaCompile} */
    protected JavaCompileHandler compiler;

    /** {@link JavadocHandler} instance for handling {@link Javadoc} */
    protected JavadocHandler documentation;

    /**
     * Create a new {@link LanguageSupportJava}
     */
    public LanguageSupportJava() {
        compiler = new JavaCompileHandler();
        documentation = new JavadocHandler();
    }

    @Override
    public CompileHandler<?> getCompilerHandler() {
        return compiler;
    }

    @Override
    public Map<String, String> getConfigurationNames(SourceSet set) {
        Map<String, String> names = new HashMap<>();
        names.put(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, set.getAnnotationProcessorConfigurationName());
        names.put(JavaPlugin.API_CONFIGURATION_NAME, set.getApiConfigurationName());
        names.put(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, set.getApiElementsConfigurationName());
        names.put(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME, set.getCompileClasspathConfigurationName());
        names.put(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME, set.getCompileOnlyApiConfigurationName());
        names.put(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, set.getCompileOnlyConfigurationName());
        names.put(JavaPlugin.JAVADOC_ELEMENTS_CONFIGURATION_NAME, set.getJavadocElementsConfigurationName());
        names.put(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, set.getImplementationConfigurationName());
        names.put(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, set.getRuntimeClasspathConfigurationName());
        names.put(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME, set.getRuntimeElementsConfigurationName());
        names.put(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, set.getRuntimeOnlyConfigurationName());
        names.put(JavaPlugin.SOURCES_ELEMENTS_CONFIGURATION_NAME, set.getSourcesElementsConfigurationName());
        return names;
    }

    @Override
    public DocumentationHandler<?> getDocumentationHandler() {
        return documentation;
    }

    @Override
    public String getName() {
        return "java";
    }

    @Override
    public boolean isIncluded(Project project, SourceSet set) {
        PluginManager plugins = project.getPluginManager();
        if (!plugins.hasPlugin("java") && !plugins.hasPlugin("java-library")) {
            return false;
        }
        ExtensionVisitor visitor = new ExtensionVisitor(".java");
        set.getAllJava().visit(visitor);
        return visitor.getIsContained();
    }

    @Override
    public void source(Project project, SourceSet set, Action<? super SourceDirectorySet> configure) {
        configure.execute(set.getJava());
    }
}
