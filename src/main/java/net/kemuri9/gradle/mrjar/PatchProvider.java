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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.external.javadoc.internal.JavadocOptionFileOptionInternal;
import org.gradle.external.javadoc.internal.JavadocOptionFileWriterContext;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * When using modules in Java 9+, to "depend" on another compiled source directory is to
 * patch it into the module.
 */
public class PatchProvider implements CommandLineArgumentProvider,
        JavadocOptionFileOptionInternal<Map<String, List<File>>> {

    /**
     * Retrieve a {@link PatchProvider} for the specified {@link List} of {@link CommandLineArgumentProvider}s.
     * If a {@link PatchProvider} is not contained in the {@link List}, one is added.
     * @param providers {@link List} of {@link CommandLineArgumentProvider}s to retrieve the provider for.
     * @return {@link PatchProvider} for the specified {@code providers}
     */
    public static PatchProvider getProvider(List<CommandLineArgumentProvider> providers) {
        PatchProvider provider = providers.stream().filter(PatchProvider.class::isInstance)
                .findFirst().map(PatchProvider.class::cast).orElse(null);
        if (provider == null) {
            provider = new PatchProvider();
            providers.add(provider);
        }
        return provider;
    }

    /** patches to add */
    protected final Map<String, List<File>> patches;

    /**
     * Create a new {@link PatchProvider}
     */
    public PatchProvider() {
        this.patches = new HashMap<>();
    }

    /**
     * Add a directory to the list of patches for the module
     * @param moduleName name of the module to add to its patch list
     * @param directory {@link File} representing the directory to patch the module with
     */
    public void add(String moduleName, File directory) {
        List<File> modulePatches = patches.computeIfAbsent(moduleName, (s)-> new ArrayList<>());
        if (!modulePatches.contains(directory)) {
            modulePatches.add(directory);
        }
    }

    @Override
    public Iterable<String> asArguments() {
        prune();
        if (patches.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> args = new ArrayList<>(patches.size() * 2);
        for (String moduleName : patches.keySet()) {
            args.add("-" + getOption());
            args.add(calcPatchArgument(moduleName));
        }

        return args;
    }

    /**
     * Retrieve the value of the {@code --patch-module} argument which is {@code moduleName=directories}
     * @param moduleName name of the module to get its patch argument
     * @return value of the {@code --patch-module} argument
     */
    protected String calcPatchArgument(String moduleName) {
        List<File> modulePatches = patches.get(moduleName);
        String paths = modulePatches.stream().map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));
        return moduleName + "=" + paths;
    }

    @Override
    public PatchProvider duplicate() {
        PatchProvider dup = new PatchProvider();
        dup.patches.putAll(patches);
        return dup;
    }

    @Override
    @Internal
    public String getOption() {
        // only one - as the additional - gets added by JavadocOptionFileWriterContext
        return "-patch-module";
    }

    @Input
    @Override
    public Map<String, List<File>> getValue() {
        return patches;
    }

    /**
     * Prune {@code null} and empty patch lists from the map
     */
    protected void prune() {
        for (Iterator<Map.Entry<String, List<File>>> entryIter = patches.entrySet().iterator(); entryIter.hasNext();) {
            Map.Entry<String, List<File>> entry = entryIter.next();
            while (entry.getValue().contains(null)) {
                entry.getValue().remove(null);
            }
            if (entry.getValue().isEmpty()) {
                entryIter.remove();
                continue;
            }
        }
    }

    @Override
    public void setValue(Map<String, List<File>> value) {
        value = (value == null) ? Collections.emptyMap() : value;
        patches.clear();
        patches.putAll(value);
    }

    @Override
    public void write(JavadocOptionFileWriterContext writerContext) throws IOException {
        prune();
        for (String moduleName : patches.keySet()) {
            writerContext.writeValueOption(getOption(), calcPatchArgument(moduleName));
        }
    }
}
