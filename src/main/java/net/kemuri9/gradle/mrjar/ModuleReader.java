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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for reading details out of the module-info.java
 * This is not actually necessary in practice, but it does help with debugging
 */
final class ModuleReader {

    private static final class State {
        boolean inComment;
        boolean inModuleName;
        StringBuilder moduleName;

        State() {
            moduleName = new StringBuilder(128);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ModuleReader.class);

    private static String handleComments(State state, String line) {
        // filter out multi-line comment style
        int commentStart = line.indexOf("/*");
        int commentEnd = line.indexOf("*/");
        /* there technically can be multiple multi-line comments on a single line, so keep checking
          and a comment can start on the same line another ends*/
        while (commentStart != -1 && commentEnd != -1 && commentEnd > commentStart) {
            line = line.substring(0, commentStart) + line.substring(commentEnd+2);
            commentStart = line.indexOf("/*");
            commentEnd = line.indexOf("*/");
        }
        if (commentEnd != -1) {
            state.inComment = false;
            line = line.substring(commentEnd+2);
        }
        if (commentStart != -1) {
            state.inComment = true;
            line = line.substring(0, commentStart);
        } else if (state.inComment) {
            return "";
        }

        // filter out one-line comment
        int oneLineComment = line.indexOf("//");
        if (oneLineComment != -1) {
            line = line.substring(0, oneLineComment);
        }
        return line;
    }

    private static String handleAnnotations(State state, String line) {
        if (line.isEmpty()) {
            return line;
        }

        // TODO - Annotations are a bit more complex as they can nest
        return line;
    }

    private static void handleModuleName(State state, String line) {
        if (line.isEmpty()) {
            // nothing to do when line is empty
            return;
        }
        // the name is between the "module" keyword and "{", but it can be split across lines
        int moduleNameStart = line.indexOf("module");
        int moduleNameEnd = line.indexOf("{", moduleNameStart+1);
        // name declaration is only a single line
        if (moduleNameStart != -1 && moduleNameEnd != -1) {
            state.moduleName.append(line.substring(moduleNameStart+6, moduleNameEnd));
        } else if (moduleNameStart != -1) {
            state.moduleName.append(line.substring(moduleNameStart+6));
            state.inModuleName = true;
        } else if (moduleNameEnd != -1) {
            state.moduleName.append(line.substring(0, moduleNameEnd));
            state.inModuleName = false;
        } else if (state.inModuleName) {
            state.moduleName.append(line);
        }
    }

    static String readModuleName(Path moduleInfo) {
        log.debug("reading module name from {}", moduleInfo);
        State state = new State();
        try (Stream<String> lines = Files.lines(moduleInfo, StandardCharsets.UTF_8)) {
            for (Iterator<String> lineIter = lines.iterator(); lineIter.hasNext();) {
                String line = lineIter.next();
                line = handleComments(state, line);
                line = handleAnnotations(state, line);
                handleModuleName(state, line);
                if (state.moduleName.length() > 0 && !state.inModuleName) {
                    // completed reading module name
                    break;
                }
            }
        } catch (IOException | IndexOutOfBoundsException ex) {
            log.error("failed to read contents of {}", moduleInfo, ex);
        }
        // remove any white space
        String moduleName = state.moduleName.toString().replaceAll("\\s", "");
        log.debug("read name {} from {}", moduleName, moduleInfo);
        return moduleName;
    }
}
