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
package gov.nasa.worldwindx.examples.dataimport;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.data.BasicDataRasterReaderFactory;
import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.DataRasterReaderFactory;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWXML;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
import java.io.File;

/**
 * DataInstallUtil is a collection of utility methods for common data import tasks.
 *
 * @author dcollins
 * @version $Id: DataInstallUtil.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class DataInstallUtil
{
    /**
     * Returns true if the specified input source is non-null and represents a data raster (imagery or elevation), and
     * false otherwise. The input source may be one of the following: <ul> <li><code>{@link String}</code></li>
     * <li><code>{@link java.io.File}</code></li> <li><code>{@link java.net.URL}</code></li> <li><code>{@link
     * java.net.URI}</code></li> <li><code>{@link java.io.InputStream}</code></li> </ul>
     *
     * @param source the input source reference to test as a data raster.
     * @param params the parameter list associated with the input source, or <code>null</code> to indicate there are no
     *               known parameters. If the raster is already known to be imagery or elevation data, specify a
     *               non-<code>null</code> parameter list with the key <code>AVKey.PIXEL_FORMAT</code> set to
     *               <code>AVKey.IMAGE</code> or <code>AVKey.ELEVATION</code>.
     *
     * @return <code>true</code> if the input source is data raster, and <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if the input source is <code>null</code>.
     */
    public static boolean isDataRaster(Object source, AVList params)
    {
        if (source == null)
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        DataRasterReaderFactory readerFactory;
        try
        {
            readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
                AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
        }
        catch (Exception e)
        {
            readerFactory = new BasicDataRasterReaderFactory();
        }

        params = (null == params) ? new AVListImpl() : params;
        DataRasterReader reader = readerFactory.findReaderFor(source, params);
        if (reader == null)
            return false;

        if (!params.hasKey(AVKey.PIXEL_FORMAT))
        {
            try
            {
                reader.readMetadata(source, params);
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("generic.ExceptionWhileReading", e.getMessage());
                log.debug(message);
            }
        }

        return AVKey.IMAGE.equals(params.getStringValue(AVKey.PIXEL_FORMAT))
            || AVKey.ELEVATION.equals(params.getStringValue(AVKey.PIXEL_FORMAT));
    }

    /**
     * Returns true if the specified input source is non-null and represents a reference to a WorldWind .NET LayerSet
     * XML document, and false otherwise. The input source may be one of the following: <ul> <li>{@link String}</li>
     * <li>{@link java.io.File}</li> <li>{@link java.net.URL}</li> <li>{@link java.net.URI}</li> <li>{@link
     * java.io.InputStream}</li> </ul>
     *
     * @param source the input source reference to test as a WorldWind .NET LayerSet document.
     *
     * @return true if the input source is a WorldWind .NET LayerSet document, and false otherwise.
     *
     * @throws IllegalArgumentException if the input source is null.
     */
    public static boolean isWWDotNetLayerSet(Object source)
    {
        if (source == null)
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        String path = WWIO.getSourcePath(source);
        if (path != null)
        {
            String suffix = WWIO.getSuffix(path);
            if (suffix != null && !suffix.toLowerCase().endsWith("xml"))
                return false;
        }

        // Open the document in question as an XML event stream. Since we're only interested in testing the document
        // element, we avoiding any unnecessary overhead incurred from parsing the entire document as a DOM.
        XMLEventReader eventReader = null;
        try
        {
            eventReader = WWXML.openEventReader(source);

            // Get the first start element event, if any exists, then determine if it represents a LayerSet
            // configuration document.
            XMLEvent event = WWXML.nextStartElementEvent(eventReader);
            return event != null && DataConfigurationUtils.isWWDotNetLayerSetConfigEvent(event);
        }
        catch (Exception e)
        {
            log.debug(Logging.getMessage("generic.ExceptionAttemptingToParseXml", source));
            return false;
        }
        finally
        {
            WWXML.closeEventReader(eventReader, source.toString());
        }
    }

    /**
     * Returns a location in the specified {@link gov.nasa.worldwind.cache.FileStore} which should be used as the
     * default location for installing data. This attempts to use the first FileStore location marked as an "install"
     * location. If no install location exists, this falls back to the FileStore's default write location, the same
     * location where downloaded data is cached.
     * <p>
     * The returned {@link java.io.File} represents an abstract path, and therefore may not exist. In this case, the
     * caller must create the missing directories composing the abstract path.
     *
     * @param fileStore the FileStore to determine the default location for installing data.
     *
     * @return the default location in the specified FileStore to be used for installing data.
     *
     * @throws IllegalArgumentException if the FileStore is null.
     */
    public static File getDefaultInstallLocation(FileStore fileStore)
    {
        if (fileStore == null)
        {
            String message = Logging.getMessage("nullValue.FileStoreIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        for (File location : fileStore.getLocations())
        {
            if (fileStore.isInstallLocation(location.getPath()))
                return location;
        }

        return fileStore.getWriteLocation();
    }
}
