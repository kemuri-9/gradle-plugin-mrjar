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

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.internal.jvm.JavaModuleDetector;

import net.kemuri9.gradle.mrjar.languages.LanguageSupport;

class MRJarExtensionImpl implements MRJarExtension, Action<Project> {

    private final Project project;
    private VersionBase baseVersion;
    private JavaVersion baseJavaVersion;
    private final Map<JavaVersion, VersionAdd> versions;

    private final Property<Boolean> includeBaseTests;
    private final Property<Boolean> multireleaseSourceJar;
    private final Property<Boolean> useJarInTests;
    private final Property<Boolean> useToolchain;
    private final ObjectFactory objFactory;
    final JavaModuleDetector moduleDetector;

    final Map<String, LanguageSupport> languages;

    @javax.inject.Inject
    public MRJarExtensionImpl(Project project, ObjectFactory objFactory, JavaModuleDetector moduleDetector) {
        this.project = project;
        this.objFactory = objFactory;
        versions = new EnumMap<>(JavaVersion.class);
        this.includeBaseTests = objFactory.property(Boolean.class);
        this.multireleaseSourceJar = objFactory.property(Boolean.class);
        this.useJarInTests = objFactory.property(Boolean.class);
        this.useToolchain = objFactory.property(Boolean.class);
        this.moduleDetector = moduleDetector;

        // load languages list
        languages = new HashMap<>();
        for (LanguageSupport support : ServiceLoader.load(LanguageSupport.class)) {
            languages.put(support.getName(), support);
            support.initialize(project);
        }
    }

    @Override
    public void addLanguage(LanguageSupport support) {
        Utils.notNull(support, "support");
        languages.put(support.getName(), support);
    }

    @Override
    public void addVersion(Object versionRaw, Action<? super Version> configure) {
        JavaVersion version = JavaVersion.toVersion(Utils.notNull(versionRaw, "version"));
        if (JavaVersion.VERSION_1_8.isCompatibleWith(version)) {
            throw new IllegalArgumentException("java version " + version + " cannot be utilized as a Multi-release Jar version specific source");
        }

        VersionAdd ver = versions.computeIfAbsent(version, (v)-> objFactory.newInstance(VersionAdd.class, project, v));
        if (configure != null) {
            configure.execute(ver);
        }
    }

    @Override
    public void allAddedVersions(Action<? super Version> configure) {
        Utils.notNull(configure, "configure");
        versions.values().forEach(configure::execute);
    }

    @Override
    public void allVersions(Action<? super Version> configure) {
        allAddedVersions(configure);
        if (baseVersion != null) {
            configure.execute(baseVersion);
        }
    }

    @Override
    public void baseVersion(Action<? super Version> configure) {
        if (baseVersion == null) {
            baseVersion = objFactory.newInstance(VersionBase.class, project);
        }
        configure.execute(baseVersion);
    }

    @Override
    public void execute(Project project) {

        // evaluate for validity
        List<JavaVersion> isPrimaryModuleDefinition = versions.values().stream()
                .filter(ver -> ver.getPrimaryModuleDefinition().getOrElse(Boolean.FALSE))
                .map(Version::getVersion).collect(Collectors.toList());
        if (isPrimaryModuleDefinition.size() > 1) {
            throw new IllegalStateException("multiple java versions are declared to provide the primary module definition: "
                    + isPrimaryModuleDefinition);
        }

        // calculate modularity of source sets
        SourceSetContainer sourceSets = Utils.getSourceSets(project);
        for (SourceSet sourceSet : sourceSets) {
            Modularity modularity = Utils.calculateModularity(sourceSet);
            new DslObject(sourceSet).getExtensions().add("modularity", modularity);
        }

        if (baseVersion != null) {
            baseVersion.postConfigure();
        }

        for (VersionAdd version : versions.values()) {
            version.postConfigure();
        }

        // setup jar
        setupJar("jar", SourceSet::getOutput);
        // setup source jar
        if (getMultireleaseSourceJar().getOrElse(Boolean.FALSE)) {
            String name = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getSourcesJarTaskName();
            setupJar(name, SourceSet::getAllSource);
        }
    }

    JavaVersion getBaseVersion() {
        if (baseJavaVersion == null) {
            baseJavaVersion = Utils.getBaseVersion(project);
        }
        return baseJavaVersion;
    }

    @Override
    public Property<Boolean> getIncludeBaseTests() {
        return includeBaseTests;
    }

    @Override
    public Map<String, LanguageSupport> getLanguages() {
        return Collections.unmodifiableMap(languages);
    }

    @Override
    public Property<Boolean> getMultireleaseSourceJar() {
        return multireleaseSourceJar;
    }

    @Override
    public Property<Boolean> getUseJarInTests() {
        return useJarInTests;
    }

    @Override
    public Property<Boolean> getUseToolchain() {
        return useToolchain;
    }

    Map<JavaVersion, VersionAdd> getVersions() {
        return versions;
    }

    private void setupJar(String name, Function<SourceSet, Object> getCopyContents) {
        project.getTasks().named(name, Jar.class, jar -> {
            SourceSetContainer sourceSets = Utils.getSourceSets(project);
            // declare that jar is a multi-release
            jar.getManifest().attributes(Collections.singletonMap("Multi-Release", "true"));
            // handle insertions into the jar
            for (Map.Entry<JavaVersion, VersionAdd> entry : versions.entrySet()) {
                Version ver = entry.getValue();
                String verNum = entry.getKey().getMajorVersion();
                SourceSet verSource = sourceSets.findByName("java" + verNum);
                if (verSource == null) {
                    // may be test only source
                    continue;
                }

                Object copySource = getCopyContents.apply(verSource);
                if (ver.getPrimaryModuleDefinition().getOrElse(Boolean.FALSE)) {
                    // is primary definition, so the module-info goes into the root of the jar
                    jar.into("", (copy)-> {
                        copy.from(copySource);
                        copy.include("module-info**");
                    });
                    // but the rest goes into the version folder
                    jar.into("META-INF/versions/" + verNum, (copy)-> {
                       copy.from(copySource);
                       copy.exclude("module-info**");
                    });
                } else {
                    // no special handling of module-info, everything goes into the version folder
                    jar.into("META-INF/versions/" + verNum, (copy)-> copy.from(copySource));
                }
            }
        });
    }
}
