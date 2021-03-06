/**
 *  Copyright 2005-2018 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.jboss.fuse.patch.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.fuse.patch.management.PatchData;
import org.jboss.fuse.patch.management.Utils;

/**
 * A few useful methods for testing the patching mechanism
 */
public abstract class PatchTestSupport {

    protected static File getTestResourcesDirectory() {
        return getDirectoryForResource("log4j.properties");
    }

    /*
     * Get the directory where a test resource has been stored
     */
    protected static File getDirectoryForResource(String name) {
        File r = getFileForResource(name);
        return r == null ? new File(name) : r.getParentFile();
    }

    /*
     * Get the File object for a test resource
     */
    protected static File getFileForResource(String name) {
        URL base = PatchTestSupport.class.getClassLoader().getResource(name);
        try {
            return base == null ? null : new File(base.toURI());
        } catch (URISyntaxException e) {
            return new File(base.getPath());
        }
    }

    protected static File createKarafDirectory(File basedir) throws Exception {
        File result = new File(basedir, "karaf");
        delete(result);
        Utils.mkdirs(result);
        Utils.mkdirs(new File(result, "etc"));
        boolean created = new File(result, "etc/startup.properties").createNewFile();
        Utils.mkdirs(new File(result, "bin"));
        FileUtils.write(new File(result, "bin/karaf"), "This is the original bin/karaf file", "UTF-8");
        System.setProperty("karaf.base", result.getAbsolutePath());
        System.setProperty("karaf.home", result.getAbsolutePath());
        return result;
    }

    protected static void delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    delete(child);
                }
            }
            file.delete();
        } else if (file.isFile()) {
            file.delete();
        }
    }

    protected static PatchBuilder patch(String name) throws Exception {
        return new PatchBuilder(getFileForResource(name));
    }

    protected static final class PatchBuilder {

        private final PatchData data;
        private final File file;
        private final ZipOutputStream zip;

        private PatchBuilder(File patch) throws IOException {
            data = PatchData.load(new FileInputStream(patch));

            File workdir = new File("target/generated-patches");
            Utils.mkdirs(workdir);
            file = new File(workdir, String.format("%s-%tQ.zip", data.getId(), new Date()));

            zip = new ZipOutputStream(new FileOutputStream(file));
            zip.putNextEntry(new ZipEntry(patch.getName()));
            IOUtils.copy(new FileInputStream(patch), zip);
            zip.closeEntry();
        }

        protected PatchBuilder withFile(String name) throws Exception {
            ZipEntry entry = new ZipEntry(name);
            zip.putNextEntry(entry);
            IOUtils.write("Some random data goes here", zip, "UTF-8");
            zip.closeEntry();
            return this;
        }

        protected File build() throws Exception {
            // let's make sure all necessary files are in the zip
            for (String file : data.getFiles()) {
                withFile(file);
            }

            zip.flush();
            zip.close();
            return file;
        }
    }

}
