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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.reflect.ObjectInstantiationException;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.jvm.JavaModuleDetector;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.kemuri9.gradle.mrjar.languages.LanguageSupport;
import net.kemuri9.gradle.mrjar.languages.LanguageSupport.TaskHandler;

final class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    static Modularity calculateModularity(SourceSet set) {
        boolean isModule = JavaModuleDetector.isModuleSource(true, set.getAllJava().getSourceDirectories());
        if (!isModule) {
            return new Modularity(Modularity.State.NOT_MODULE);
        }
        // try and determine the module name from the file
        String moduleName = null;
        for (File sourceDir : set.getAllJava().getSourceDirectories()) {
            Path moduleInfo = sourceDir.toPath().resolve("module-info.java");
            if (!Files.exists(moduleInfo)) {
                continue;
            }
            moduleName = ModuleReader.readModuleName(moduleInfo);
        }
        return new Modularity(Modularity.State.IS_MODULE, moduleName);
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object o) {
        return (T) o;
    }

    static FileCollection fileCollectionAdd(FileCollection base, FileCollection... add) {
        return fileCollectionAdd(base, Arrays.asList(add));
    }

    static FileCollection fileCollectionAdd(FileCollection base, Iterable<FileCollection> add) {
        for (FileCollection plus : add) {
            base = base.plus(plus);
        }
        return base;
    }

    static FileCollection fileCollectionMinus(FileCollection base, FileCollection... minus) {
        return fileCollectionMinus(base, Arrays.asList(minus));
    }

    static FileCollection fileCollectionMinus(FileCollection base, Iterable<FileCollection> minus) {
        for (FileCollection subtract : minus) {
            base = base.minus(subtract);
        }
        return base;
    }

    static Set<String> filterLanguages(Project project, SourceSet set, Collection<String> langs) {
        Set<String> languages = new HashSet<>(langs);
        MRJarExtensionImpl mrjar = getExtension(project);
        languages.retainAll(mrjar.languages.keySet());
        for (Iterator<String> langIter = languages.iterator(); langIter.hasNext();) {
            String name = langIter.next();
            LanguageSupport lang = mrjar.languages.get(name);
            if (!lang.isIncluded(project, set) || lang.getCompilerHandler() == null) {
                langIter.remove();
                continue;
            }
            try {
                lang.getCompilerHandler().getTask(project, set);
            } catch (ClassCastException | UnknownTaskException ex) {
                // task does not exist
                langIter.remove();
            }
        }
        return languages;
    }

    static JavaVersion getBaseVersion(Project project) {
        JavaPluginExtension conv = Utils.getExtensionJava(project);
        JavaCompile baseCompile = (JavaCompile) project.getTasks().getByName("compileJava");
        JavaVersion baseCompileVer = null;
        /* reading from java compiler causes it to become final, which can cause problems if it still needs to be set.
         * So do NOT read javaCompiler from JavaCompile */
        if (baseCompile.getOptions().getRelease().isPresent()) {
            baseCompileVer = JavaVersion.toVersion(baseCompile.getOptions().getRelease().get());
        } else if (baseCompile.getTargetCompatibility() != null) {
            baseCompileVer = JavaVersion.toVersion(baseCompile.getTargetCompatibility());
        } else if (conv.getToolchain().getLanguageVersion().isPresent()) {
            baseCompileVer = JavaVersion.toVersion(conv.getToolchain().getLanguageVersion().get());
        } else {
            baseCompileVer = conv.getTargetCompatibility();
        }
        if (baseCompileVer == null) {
            baseCompileVer = JavaVersion.current();
        }
        return baseCompileVer;
    }

    static MRJarExtensionImpl getExtension(Project project) {
        return (MRJarExtensionImpl) project.getExtensions().findByType(MRJarExtension.class);
    }

    static JavaPluginExtension getExtensionJava(Project project) {
        return (JavaPluginExtension) project.getExtensions().getByName("java");
    }

    static boolean getIsProperty(Property<Boolean> property, Property<Boolean> global, boolean defValue) {
        if (property.isPresent()) {
            return property.get();
        }
        // if extension global value is set, use it
        return global.getOrElse(defValue);
    }

    static SourceSetContainer getSourceSets(Project project) {
        return (SourceSetContainer) project.getExtensions().getByName("sourceSets");
    }

    static JavaToolchainService getToolchains(Project project) {
        return (JavaToolchainService) project.getExtensions().getByName("javaToolchains");
    }

    static Instantiator instantiateFromFactory(ObjectFactory factory) {
        return new Instantiator() {
            @Override
            public <T> @NotNull T newInstance(@NotNull Class<? extends T> type, Object @NotNull ... parameters)
                    throws ObjectInstantiationException {
                return factory.newInstance(type, parameters);
            }
        };
    }

    static boolean isTestSource(SourceSet sourceSet) {
        return sourceSet.getName().toLowerCase().endsWith("test");
    }

    static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " may not null");
        }
        return value;
    }

    static void setLauncher(Test test, JavaToolchainService javaToolchains, JavaVersion version) {
        Provider<JavaLauncher> launcher = javaToolchains.launcherFor((l)-> {
            l.getLanguageVersion().set(JavaLanguageVersion.of(version.getMajorVersion()));
        });
        try {
            test.getJavaLauncher().set(launcher);
        } catch (IllegalStateException ex) {
            log.error("failed to set launcher for {}", test);
        }
    }

    static void setOptions(Project project, JavaToolchainService javaToolchains,
            SourceSet sourceSet, Modularity modularity, boolean useToolchain, JavaVersion version) {
        MRJarExtensionImpl ext = getExtension(project);
        Set<String> usedLangs = filterLanguages(project, sourceSet, ext.languages.keySet());
        for (String usedLangName : usedLangs) {
            LanguageSupport usedLang = ext.languages.get(usedLangName);
            setOptions(project, javaToolchains, sourceSet,
                    cast(usedLang.getCompilerHandler()), useToolchain, modularity, version);
        }
    }

    static <T extends DefaultTask> void setOptions(Project project, JavaToolchainService javaToolchains, SourceSet sourceSet,
            TaskHandler<T> handler, boolean useToolchain, Modularity modularity, JavaVersion version) {
        T task = handler.getTask(project, sourceSet);
        if (version != null) {
            handler.setVersionOptions(task, version);
        }

        if (useToolchain && version != null) {
            try {
                handler.setToolchain(task, javaToolchains, version);
            } catch (IllegalStateException ex) {
                log.error("failed to set toolchain for {}", task);
            }
        }
        if (modularity != null) {
            handler.setModularity(task, modularity);
        }
    }
}
