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

import gov.nasa.worldwind.tracks.Track;
import gov.nasa.worldwind.tracks.TrackPoint;
import gov.nasa.worldwind.tracks.TrackSegment;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * @author tag
 * @version $Id: GpxRoute.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class GpxRoute extends gov.nasa.worldwind.formats.gpx.ElementParser implements Track, TrackSegment
{
    private String name;
    private java.util.List<TrackPoint> points = new java.util.ArrayList<TrackPoint>();

    @SuppressWarnings({"UnusedDeclaration"})
    public GpxRoute(String uri, String lname, String qname, org.xml.sax.Attributes attributes)
    {
        super("rte");

        // dont' validate uri, lname, qname or attributes as they aren't used.
    }

    public List<TrackSegment> getSegments()
    {
        return Arrays.asList((TrackSegment) this);
    }

    public String getName()
    {
        return this.name;
    }

    public int getNumPoints()
    {
        return this.points.size();
    }

    public java.util.List<TrackPoint> getPoints()
    {
        return this.points;
    }

    @Override
    public void doStartElement(String uri, String lname, String qname, org.xml.sax.Attributes attributes)
        throws org.xml.sax.SAXException
    {
        if (lname == null)
        {
            String msg = Logging.getMessage("nullValue.LNameIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (uri == null)
        {
            String msg = Logging.getMessage("nullValue.URIIsNull");
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
            String msg = Logging.getMessage("nullValue.AttributesIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (lname.equalsIgnoreCase("rtept"))
        {
            this.currentElement = new GpxRoutePoint(uri, lname, qname, attributes);
            this.points.add((TrackPoint) this.currentElement);
        }
    }
    
    @Override
    public void doEndElement(String uri, String lname, String qname) throws org.xml.sax.SAXException
    {
        // don't validate uri or qname - they aren't used
        if (lname == null)
        {
            String msg = Logging.getMessage("nullValue.LNameIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        
        if (lname.equalsIgnoreCase("name"))
        {
            this.name = this.currentCharacters;
        }
    }
}
