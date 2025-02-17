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

package gov.nasa.worldwind.ogc.wcs.wcs100;

import gov.nasa.worldwind.ogc.OGCConstants;
import gov.nasa.worldwind.ogc.gml.GMLPos;
import gov.nasa.worldwind.ogc.ows.OWSAddress;
import gov.nasa.worldwind.ogc.ows.OWSContactInfo;
import gov.nasa.worldwind.ogc.ows.OWSPhone;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwind.util.xml.*;
import gov.nasa.worldwind.wms.CapabilitiesRequest;
import lombok.extern.slf4j.Slf4j;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author tag
 * @version $Id: WCS100Capabilities.java 2072 2014-06-21 21:20:25Z tgaskins $
 */
@Slf4j
public class WCS100Capabilities extends AbstractXMLEventParser
{
    protected XMLEventReader eventReader;
    protected XMLEventParserContext parserContext;

    /**
     * Retrieves the WCS capabilities document from a specified WCS server.
     *
     * @param uri The URI of the server.
     *
     * @return The WCS capabilities document for the specified server.
     * @throws java.lang.Exception if a general error occurs.
     *
     * @throws IllegalArgumentException if the specified URI is invalid.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if an error occurs retrieving the document.
     */
    public static WCS100Capabilities retrieve(URI uri) throws Exception
    {
        try
        {
            CapabilitiesRequest request = new CapabilitiesRequest(uri, "WCS");
            request.setVersion("1.0.0");

            return new WCS100Capabilities(request.toString());
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("OGC.GetCapabilitiesURIInvalid", uri);
            log.warn(message);
            throw new IllegalArgumentException(message);
        }
    }

    public WCS100Capabilities(Object docSource)
    {
        super(OGCConstants.WCS_1_0_0_NAMESPACE_URI);

        this.eventReader = this.createReader(docSource);

        this.initialize();
    }

    protected void initialize()
    {
        this.parserContext = this.createParserContext(this.eventReader);
    }

    protected XMLEventReader createReader(Object docSource)
    {
        return WWXML.openEventReader(docSource);
    }

    protected XMLEventParserContext createParserContext(XMLEventReader reader)
    {
        this.parserContext = new BasicXMLEventParserContext(reader);
        this.parserContext.setDefaultNamespaceURI(this.getNamespaceURI());

        return this.parserContext;
    }

    public XMLEventParserContext getParserContext()
    {
        return this.parserContext;
    }

    /**
     * Returns the document's version number.
     *
     * @return the document's version number.
     */
    public String getVersion()
    {
        return (String) this.getField("version");
    }

    /**
     * Returns the document's update sequence.
     *
     * @return the document's update sequence.
     */
    public String getUpdateSequence()
    {
        return (String) this.getField("updateSequence");
    }

    public WCS100Service getService()
    {
        return (WCS100Service) this.getField("Service");
    }

    public WCS100Capability getCapability()
    {
        return (WCS100Capability) this.getField("Capability");
    }

    public WCS100ContentMetadata getContentMetadata()
    {
        return (WCS100ContentMetadata) this.getField("ContentMetadata");
    }

    /**
     * Starts document parsing. This method initiates parsing of the XML document and returns when the full capabilities
     * document has been parsed.
     *
     * @param args optional arguments to pass to parsers of sub-elements.
     *
     * @return <code>this</code> if parsing is successful, otherwise  null.
     *
     * @throws javax.xml.stream.XMLStreamException
     *          if an exception occurs while attempting to read the event stream.
     */
    public WCS100Capabilities parse(Object... args) throws XMLStreamException
    {
        XMLEventParserContext ctx = this.parserContext;
        QName capsName = new QName(this.getNamespaceURI(), "WCS_Capabilities");

        for (XMLEvent event = ctx.nextEvent(); ctx.hasNext(); event = ctx.nextEvent())
        {
            if (event == null)
                continue;

            if (event.isStartElement() && event.asStartElement().getName().equals(capsName))
            {
                // Parse the attributes in order to get the version number.
                this.doParseEventAttributes(ctx, event);
                ctx.setDefaultNamespaceURI(this.getNamespaceURI());

                // Now register the parsers.
                this.registerParsers(ctx);

                super.parse(ctx, event, args);

                return this;
            }
        }

        return null;
    }

    protected void registerParsers(XMLEventParserContext ctx)
    {
        ctx.addStringParsers(this.getNamespaceURI(), new String[]
            {
                "accessConstraints",
                "administrativeArea",
                "city",
                "country",
                "deliveryPoint",
                "description",
                "electronicMailAddress",
                "facsimile",
                "fees",
                "Format",
                "individualName",
                "label",
                "keyword",
                "name",
                "organisationName",
                "positionName",
                "postalCode",
                "voice",
            });

        ctx.addStringParsers(OGCConstants.GML_NAMESPACE_URI, new String[]
            {
                "timePosition",
            });

        ctx.registerParser(new QName(this.getNamespaceURI(), "address"),
            new OWSAddress(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "Capability"),
            new WCS100Capability(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "contactInfo"),
            new OWSContactInfo(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "ContentMetadata"),
            new WCS100ContentMetadata(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "CoverageOfferingBrief"),
            new WCS100CoverageOfferingBrief(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "DCPType"),
            new WCS100DCPType(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "DescribeCoverage"),
            new WCS100RequestDescription(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "Exception"),
            new WCS100Exception(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "Get"),
            new AttributesOnlyXMLEventParser(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "GetCapabilities"),
            new WCS100RequestDescription(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "GetCoverage"),
            new WCS100RequestDescription(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "HTTP"),
            new WCS100HTTP(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "keywords"),
            new StringListXMLEventParser(this.getNamespaceURI(), new QName(this.getNamespaceURI(), "keyword")));

        ctx.registerParser(new QName(this.getNamespaceURI(), "lonLatEnvelope"),
            new WCS100LonLatEnvelope(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "metadataLink"),
            new WCS100MetadataLink(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "OnlineResource"),
            new AttributesOnlyXMLEventParser(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "onlineResource"),
            new AttributesOnlyXMLEventParser(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "phone"),
            new OWSPhone(this.getNamespaceURI()));

        ctx.registerParser(new QName(OGCConstants.GML_NAMESPACE_URI, "pos"),
            new GMLPos(OGCConstants.GML_NAMESPACE_URI));

        ctx.registerParser(new QName(this.getNamespaceURI(), "Post"),
            new AttributesOnlyXMLEventParser(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "Request"),
            new WCS100Request(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "responsibleParty"),
            new WCS100ResponsibleParty(this.getNamespaceURI()));

        ctx.registerParser(new QName(this.getNamespaceURI(), "Service"),
            new WCS100Service(this.getNamespaceURI()));
    }
}
