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
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implements the {@link KMLDoc} interface for KML files located within a computer's file system.
 * <p>
 * Note: This class does not resolve references to files in KMZ archives. For example, it does not resolve references
 * like this: <i>../other.kmz/file.png</i>.
 *
 * @author tag
 * @version $Id: KMLFile.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class KMLFile implements KMLDoc
{
    /** The {@link File} reference specified to the constructor. */
    protected File kmlFile;

    /**
     * Construct a KMLFile instance.
     *
     * @param file path to the KML file.
     *
     * @throws IllegalArgumentException if the specified file is null.
     */
    public KMLFile(File file)
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.kmlFile = file;
    }

    /**
     * Returns the {@link File} specified to the constructor.
     *
     * @return the file specified to the constructor.
     */
    public File getZipFile()
    {
        return this.kmlFile;
    }

    /**
     * Returns an {@link InputStream} to the KML file.
     *
     * @return an input stream positioned to the start of the KML file.
     *
     * @throws IOException if an error occurs attempting to create the input stream.
     */
    public InputStream getKMLStream() throws IOException
    {
        return new FileInputStream(this.kmlFile);
    }

    /**
     * Returns an {@link InputStream} to a file indicated by a path relative to the KML file's location.
     *
     * @param path the path of the requested file.
     *
     * @return an input stream positioned to the start of the file, or null if the file does not exist.
     *
     * @throws IOException if an error occurs while attempting to query or open the file.
     */
    public InputStream getSupportFileStream(String path) throws IOException
    {
        if (path == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        File pathFile = new File(path);
        if (pathFile.isAbsolute())
            return null;

        pathFile = new File(this.kmlFile.getParentFile(), path);

        return pathFile.exists() ? new FileInputStream(pathFile) : null;
    }

    public String getSupportFilePath(String path)
    {
        if (path == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        File pathFile = new File(path);
        if (pathFile.isAbsolute())
            return null;

        pathFile = new File(this.kmlFile.getParentFile(), path);

        return pathFile.exists() ? pathFile.getPath() : null;
    }
}
