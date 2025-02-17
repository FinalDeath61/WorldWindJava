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
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.exception.WWUnrecognizedException;
import gov.nasa.worldwind.ogc.OGCCapabilities;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capabilities;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.WWXML;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamException;

/**
 * A basic implementation of the {@link Factory} interface.
 *
 * @author tag
 * @version $Id: BasicFactory.java 2072 2014-06-21 21:20:25Z tgaskins $
 */
@Slf4j
public class BasicFactory implements Factory
{
    /**
     * Static method to create an object from a factory and configuration source.
     *
     * @param factoryKey   the key identifying the factory in {@link Configuration}.
     * @param configSource the configuration source. May be any of the types listed for {@link
     *                     #createFromConfigSource(Object, gov.nasa.worldwind.avlist.AVList)}
     *
     * @return a new instance of the requested object.
     *
     * @throws IllegalArgumentException if the factory key is null, or if the configuration source is null or an empty
     *                                  string.
     */
    public static Object create(String factoryKey, Object configSource)
    {
        if (factoryKey == null)
        {
            String message = Logging.getMessage("generic.FactoryKeyIsNull");
            throw new IllegalArgumentException(message);
        }

        if (WWUtil.isEmpty(configSource))
        {
            String message = Logging.getMessage("generic.ConfigurationSourceIsInvalid", configSource);
            throw new IllegalArgumentException(message);
        }

        Factory factory = (Factory) WorldWind.createConfigurationComponent(factoryKey);
        return factory.createFromConfigSource(configSource, null);
    }

    /**
     * Static method to create an object from a factory, a configuration source, and an optional configuration parameter
     * list.
     *
     * @param factoryKey   the key identifying the factory in {@link Configuration}.
     * @param configSource the configuration source. May be any of the types listed for {@link
     *                     #createFromConfigSource(Object, gov.nasa.worldwind.avlist.AVList)}
     * @param params       key-value parameters to override or supplement the information provided in the specified
     *                     configuration source. May be null.
     *
     * @return a new instance of the requested object.
     *
     * @throws IllegalArgumentException if the factory key is null, or if the configuration source is null or an empty
     *                                  string.
     */
    public static Object create(String factoryKey, Object configSource, AVList params)
    {
        if (factoryKey == null)
        {
            String message = Logging.getMessage("generic.FactoryKeyIsNull");
            throw new IllegalArgumentException(message);
        }

        if (WWUtil.isEmpty(configSource))
        {
            String message = Logging.getMessage("generic.ConfigurationSourceIsInvalid", configSource);
            throw new IllegalArgumentException(message);
        }

        Factory factory = (Factory) WorldWind.createConfigurationComponent(factoryKey);
        return factory.createFromConfigSource(configSource, params);
    }

    /**
     * Creates an object from a general configuration source. The source can be one of the following: <ul> <li>{@link
     * java.net.URL}</li> <li>{@link java.io.File}</li> <li>{@link java.io.InputStream}</li> <li>{@link Element}</li>
     * <li>{@link gov.nasa.worldwind.ogc.OGCCapabilities}</li>
     * <li>{@link gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capabilities}</li>
     * <li>{@link String} holding a file name, a name of a resource on the classpath, or a string representation of a
     * URL</li></ul>
     * <p>
     *
     * @param configSource the configuration source. See above for supported types.
     * @param params       key-value parameters to override or supplement the information provided in the specified
     *                     configuration source. May be null.
     *
     * @return the new object.
     *
     * @throws IllegalArgumentException if the configuration source is null or an empty string.
     * @throws WWUnrecognizedException  if the source type is unrecognized.
     * @throws WWRuntimeException       if object creation fails. The exception indicating the source of the failure is
     *                                  included as the {@link Exception#initCause(Throwable)}.
     */
    public Object createFromConfigSource(Object configSource, AVList params)
    {
        if (WWUtil.isEmpty(configSource))
        {
            String message = Logging.getMessage("generic.ConfigurationSourceIsInvalid", configSource);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        Object o = null;

        try
        {
            if (configSource instanceof Element)
            {
                o = this.doCreateFromElement((Element) configSource, params);
            }
            else if (configSource instanceof OGCCapabilities)
                o = this.doCreateFromCapabilities((OGCCapabilities) configSource, params);
            else if (configSource instanceof WCS100Capabilities)
                o = this.doCreateFromCapabilities((WCS100Capabilities) configSource, params);
            else
            {
                Document doc = WWXML.openDocument(configSource);
                if (doc != null)
                    o = this.doCreateFromElement(doc.getDocumentElement(), params);
            }
        }
        catch (Exception e)
        {
            String msg = Logging.getMessage("generic.CreationFromConfigurationFileFailed", configSource);
            throw new WWRuntimeException(msg, e);
        }

        return o;
    }

    /**
     * Create an object such as a layer or elevation model given a local OGC capabilities document containing named
     * layer descriptions.
     *
     * @param capsFileName the path to the capabilities file. The file must be either an absolute path or a relative
     *                     path available on the classpath. The file contents must be a valid OGC capabilities
     *                     document.
     * @param params       a list of configuration properties. These properties override any specified in the
     *                     capabilities document. The list should contain the {@link AVKey#LAYER_NAMES} property for
     *                     services that define layer, indicating which named layers described in the capabilities
     *                     document to create. If this argumet is null or contains no layers, the first named layer is
     *                     used.
     *
     * @return the requested object.
     *
     * @throws IllegalArgumentException if the file name is null or empty.
     * @throws IllegalStateException    if the capabilites document contains no named layer definitions.
     * @throws WWRuntimeException       if an error occurs while opening, reading or parsing the capabilities document.
     *                                  The exception indicating the source of the failure is included as the {@link
     *                                  Exception#initCause(Throwable)}.
     */
    public Object createFromCapabilities(String capsFileName, AVList params)
    {
        if (WWUtil.isEmpty(capsFileName))
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        WMSCapabilities caps = new WMSCapabilities(capsFileName);

        try
        {
            caps.parse();
        }
        catch (XMLStreamException e)
        {
            String message = Logging.getMessage("generic.CannotParseCapabilities", capsFileName);
            log.error(message, e);
            throw new WWRuntimeException(message, e);
        }

        return this.doCreateFromCapabilities(caps, params);
    }

    /**
     * Implemented by subclasses to perform the actual object creation. This default implementation always returns
     * null.
     *
     * @param caps   the capabilities document.
     * @param params a list of configuration properties. These properties override any specified in the capabilities
     *               document. The list should contain the {@link AVKey#LAYER_NAMES} property for services that define
     *               layers, indicating which named layers described in the capabilities document to create. If this
     *               argumet is null or contains no layers, the first named layer is used.
     *
     * @return the requested object.
     */
    protected Object doCreateFromCapabilities(OGCCapabilities caps, AVList params)
    {
        return null;
    }

    /**
     * Implemented by subclasses to perform the actual object creation. This default implementation always returns
     * null.
     *
     * @param caps   the capabilities document.
     * @param params a list of configuration properties. These properties override any specified in the capabilities
     *               document.
     *
     * @return the requested object.
     */
    protected Object doCreateFromCapabilities(WCS100Capabilities caps, AVList params)
    {
        return null;
    }

    protected Object doCreateFromElement(Element domElement, AVList params) throws Exception
    {
        return null;
    }
}
