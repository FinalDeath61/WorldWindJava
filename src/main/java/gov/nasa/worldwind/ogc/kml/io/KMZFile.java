/*
 * Copyright 2006-2009, 2017, 2020 United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 * 
 * The NASA World Wind Java (WWJ) platform is licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 * NASA World Wind Java (WWJ) also contains the following 3rd party Open Source
 * software:
 * 
 *     Jackson Parser – Licensed under Apache 2.0
 *     GDAL – Licensed under MIT
 *     JOGL – Licensed under  Berkeley Software Distribution (BSD)
 *     Gluegen – Licensed under Berkeley Software Distribution (BSD)
 * 
 * A complete listing of 3rd Party software notices and licenses included in
 * NASA World Wind Java (WWJ)  can be found in the WorldWindJava-v2.2 3rd-party
 * notices and licenses PDF found in code directory.
 */

package gov.nasa.worldwind.ogc.kml.io;

import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Implements the {@link KMLDoc} interface for KMZ files located within a computer's file system.
 * <p>
 * Note: This class does not yet resolve references to files in other KMZ archives. For example, it does not resolve
 * references like this: <i>../other.kmz/file.png</i>.
 *
 * @author tag
 * @version $Id: KMZFile.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class KMZFile implements KMLDoc
{
    /** The {@link ZipFile} reference specified to the constructor. */
    protected ZipFile zipFile;

    /** A mapping of the files in the KMZ file to their location in the temporary directory. */
    protected Map<String, File> files = new HashMap<String, File>();

    /** The directory to hold files copied from the KMZ file. The directory and the files copied there are temporary. */
    protected File tempDir;

    /**
     * Construct a KMZFile instance.
     *
     * @param file path to the KMZ file.
     *
     * @throws IOException              if an error occurs while attempting to query or open the file.
     * @throws IllegalArgumentException if the specified file is null.
     * @throws ZipException             if a Zip error occurs.
     */
    public KMZFile(File file) throws IOException
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.zipFile = new ZipFile(file);
    }

    /**
     * Returns the file file specified to the constructor as a {@link ZipFile}.
     *
     * @return the file specified to the constructor, as a <code>ZipFile</code>.
     */
    public ZipFile getZipFile()
    {
        return this.zipFile;
    }

    /**
     * Returns an {@link InputStream} to the first KML file in the KMZ file.
     *
     * @return an input stream positioned to the first KML file in the KMZ file, or null if the KMZ file does not
     *         contain a KML file.
     */
    public synchronized InputStream getKMLStream() throws IOException
    {
        Enumeration<? extends ZipEntry> zipEntries = this.zipFile.entries();
        while (zipEntries.hasMoreElements())
        {
            ZipEntry entry = zipEntries.nextElement();
            if (entry.getName().toLowerCase().endsWith(".kml"))
            {
                return this.zipFile.getInputStream(entry);
            }
        }

        return null;
    }

    /**
     * Returns an {@link InputStream} to a specified file within the KMZ file. The file's path is resolved relative to
     * the internal root of the KMZ file.
     * <p>
     * Note: This class does not yet resolve references to files in other KMZ archives. For example, it does not resolve
     * references like this: <i>../other.kmz/file.png</i>.
     *
     * @param path the path of the requested file.
     *
     * @return an input stream positioned to the start of the requested file, or null if the file does not exist or the
     *         specified path is absolute.
     *
     * @throws IllegalArgumentException if the path is null.
     * @throws IOException              if an error occurs while attempting to create or open the input stream.
     */
    public synchronized InputStream getSupportFileStream(String path) throws IOException
    {
        // This method is called by the native WebView implementation to resolve resources in KMZ balloons. It may
        // not perform any synchronization with the EDT (such as calling invokeAndWait), or it will introduce a
        // potential deadlock when called by the WebView's native UI thread.

        if (path == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        Enumeration<? extends ZipEntry> zipEntries = this.zipFile.entries();
        while (zipEntries.hasMoreElements())
        {
            ZipEntry entry = zipEntries.nextElement();
            if (entry.getName().equals(path))
            {
                return this.zipFile.getInputStream(entry);
            }
        }

        return null;
    }

    /**
     * Returns an absolute path to a specified file within the KMZ file. The file's path is resolved relative to the
     * internal root of the KMZ file.
     * <p>
     * Note: This class does not yet resolve references to files in other KMZ archives. For example, it does not resolve
     * references like this: <i>../other.kmz/file.png</i>. // TODO
     *
     * @param path the path of the requested file.
     *
     * @return an absolute path to the requested file, or null if the file does not exist or the specified path is
     *         absolute.
     *
     * @throws IllegalArgumentException if the path is null.
     * @throws IOException              if an error occurs while attempting to create a temporary file.
     */
    public synchronized String getSupportFilePath(String path) throws IOException
    {
        // This method is called by the native WebView implementation to resolve resources in KMZ balloons. It may
        // not perform any synchronization with the EDT (such as calling invokeAndWait), or it will introduce a
        // potential deadlock when called by the WebView's native UI thread.

        if (path == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        File file = this.files.get(path);
        if (file != null)
            return file.getPath();

        Enumeration<? extends ZipEntry> zipEntries = this.zipFile.entries();
        while (zipEntries.hasMoreElements())
        {
            ZipEntry entry = zipEntries.nextElement();
            if (entry.getName().equals(path))
            {
                return this.copyEntryToTempDir(entry);
            }
        }

        return null;
    }

    /**
     * Copies a zip entry to a temporary file. This method should only be called by a synchronized public method.
     *
     * @param entry the entry to copy.
     *
     * @return the path to the file, or null if the entry is a directory or the temporary directory cannot be created.
     *
     * @throws IOException if an error occurs during the copy.
     */
    protected String copyEntryToTempDir(ZipEntry entry) throws IOException
    {
        if (entry.isDirectory())
            return null;

        if (this.tempDir == null)
            this.tempDir = WWIO.makeTempDir();

        if (this.tempDir == null) // unlikely to occur, but define a reaction
        {
            String message = Logging.getMessage("generic.UnableToCreateTempDir", this.tempDir);
            log.warn(message);
            return null;
        }

        // Create the path for the temp file and ensure all directories leading to it exist.
        String tempFileName = this.tempDir + File.separator + entry.getName();
        WWIO.makeParentDirs(tempFileName);

        // Copy the entry.
        File outFile = new File(tempFileName);
        outFile.deleteOnExit();
        WWIO.saveBuffer(WWIO.readStreamToBuffer(this.zipFile.getInputStream(entry), true), outFile);
        this.files.put(entry.getName(), outFile);

        return outFile.getPath();
    }
}
