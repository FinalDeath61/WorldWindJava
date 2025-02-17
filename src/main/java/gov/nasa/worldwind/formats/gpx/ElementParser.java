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
package gov.nasa.worldwind.formats.gpx;

import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tag
 * @version $Id: ElementParser.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class ElementParser
{
    protected final String elementName;
    protected ElementParser currentElement = null;
    protected String currentCharacters = null;

    /**
     * @param elementName the element's name, may not be null
     * @throws IllegalArgumentException if <code>elementName</code> is null
     */
    protected ElementParser(String elementName)
    {
        if (elementName == null)
        {
            String msg = Logging.getMessage("nullValue.ElementNameIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.elementName = elementName;
    }

    public String getElementName()
    {
        return this.elementName;
    }

    /**
     * Starts an element. No parameters may be null.
     *
     * @param uri Element URI.
     * @param lname Element lname.
     * @param qname Element qname.
     * @param attributes Element attributes.
     * @throws org.xml.sax.SAXException if a parsing error occurs.
     * @throws IllegalArgumentException if any argument is null
     */
    public void startElement(String uri, String lname, String qname, org.xml.sax.Attributes attributes)
        throws org.xml.sax.SAXException
    {
        if (uri == null)
        {
            String msg = Logging.getMessage("nullValue.URIIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (lname == null)
        {
            String msg = Logging.getMessage("nullValue.LNameIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (qname == null)
        {
            String msg = Logging.getMessage("nullValue.QNameIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (attributes == null)
        {
            String msg = Logging.getMessage("nullValue.org.xml.sax.AttributesIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.currentElement != null)
            this.currentElement.startElement(uri, lname, qname, attributes);
        else
            this.doStartElement(uri, lname, qname, attributes);
    }

    /**
     * Finishes an element. No parameters may be null.
     *
     * @param uri Element URI.
     * @param lname Element lname.
     * @param qname Element qname.
     * @throws org.xml.sax.SAXException  if a parsing error occurs.
     * @throws IllegalArgumentException if any argument is null
     */
    public void endElement(String uri, String lname, String qname) throws org.xml.sax.SAXException
    {
        if (uri == null)
        {
            String msg = Logging.getMessage("nullValue.URIIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (lname == null)
        {
            String msg = Logging.getMessage("nullValue.LNameIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (qname == null)
        {
            String msg = Logging.getMessage("nullValue.QNameIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.currentElement != null)
        {
            this.currentElement.endElement(uri, lname, qname);
            if (lname.equalsIgnoreCase(this.currentElement.elementName))
                this.currentElement = null;
        }

        this.doEndElement(uri, lname, qname);

        this.currentCharacters = null;
    }

    protected void doStartElement(String uri, String lname, String qname, org.xml.sax.Attributes attributes)
        throws org.xml.sax.SAXException
    {
    }

    protected void doEndElement(String uri, String lname, String qname) throws org.xml.sax.SAXException
    {
    }

    /**
     * @param data The data to set currentCharacters from.
     * @param start The start index of the data.
     * @param length The length of the data.
     * @throws IllegalArgumentException if <code>data</code> has length less than 1
     */
    public void characters(char[] data, int start, int length)
    {
        if (data == null)
        {
            String msg = Logging.getMessage("nullValue.ArrayIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (data.length < 1)
        {
            String msg = Logging.getMessage("generic.ArrayInvalidLength", data.length);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (start < 0)
        {
            String msg = Logging.getMessage("generic.indexOutOfRange", start);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (start + length > data.length)
        {
            String msg = Logging.getMessage("generic.indexOutOfRange", start + length);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.currentElement != null)
            this.currentElement.characters(data, start, length);
        else if (this.currentCharacters != null)
            this.currentCharacters += new String(data, start, length);
        else
            this.currentCharacters = new String(data, start, length);
    }
}
