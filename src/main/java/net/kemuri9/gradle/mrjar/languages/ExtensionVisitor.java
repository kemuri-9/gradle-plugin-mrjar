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

import org.gradle.api.Action;
import org.gradle.api.file.FileVisitDetails;

/**
 * {@link Action} on {@link FileVisitDetails} to collect if there is any file(s) with a provided
 * file extension.
 */
public class ExtensionVisitor implements Action<FileVisitDetails> {

    protected final String ext;
    protected boolean contained;

    /**
     * Create a new {@link ExtensionVisitor} with the specified file extension
     * @param ext file extension to scan for.
     */
    public ExtensionVisitor(String ext) {
        if (ext == null || ext.isEmpty()) {
            throw new IllegalArgumentException("ext is null or empty");
        }
        this.ext = ext;
    }

    @Override
    public void execute(FileVisitDetails file) {
       if (file.isDirectory()) {
           return;
       } else if (file.getName().endsWith(ext)) {
           contained = true;
           file.stopVisiting();
       }
    }

    public boolean getIsContained() {
        return contained;
    }
}
