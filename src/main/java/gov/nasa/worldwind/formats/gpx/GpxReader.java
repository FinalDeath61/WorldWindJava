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

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.tracks.Track;
import gov.nasa.worldwind.tracks.TrackPointIterator;
import gov.nasa.worldwind.tracks.TrackPointIteratorImpl;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

/**
 * @author tag
 * @version $Id: GpxReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class GpxReader // TODO: I18N, proper exception handling, remove stack-trace prints
{
    private javax.xml.parsers.SAXParser parser;
    private java.util.List<Track> tracks = new java.util.ArrayList<Track>();

    public GpxReader() throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException
    {
        javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        this.parser = factory.newSAXParser();
    }

    /**
     * @param path The file spec to read from.
     * @throws IllegalArgumentException if <code>path</code> is null
     * @throws java.io.IOException      if no file exists at the location specified by <code>path</code>
     * @throws org.xml.sax.SAXException if a parsing error occurs.
     */
    public void readFile(String path) throws java.io.IOException, org.xml.sax.SAXException
    {
        if (path == null)
        {
            String msg = Logging.getMessage("nullValue.PathIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        java.io.File file = new java.io.File(path);
        if (!file.exists())
        {
            String msg = Logging.getMessage("generic.FileNotFound", path);
            log.error(msg);
            throw new java.io.FileNotFoundException(path);
        }

        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        this.doRead(fis);
    }

    /**
     * @param stream The stream to read from.
     * @throws IllegalArgumentException if <code>stream</code> is null
     * @throws java.io.IOException if a problem is encountered reading the stream.
     * @throws org.xml.sax.SAXException if a parsing error occurs.
     */
    public void readStream(java.io.InputStream stream) throws java.io.IOException, org.xml.sax.SAXException
    {
        if (stream == null)
        {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.doRead(stream);
    }

    public java.util.List<Track> getTracks()
    {
        return this.tracks;
    }

    public Iterator<Position> getTrackPositionIterator()
    {
        return new Iterator<Position>()
        {
            private TrackPointIterator trackPoints = new TrackPointIteratorImpl(GpxReader.this.tracks);

            public boolean hasNext()
            {
                return this.trackPoints.hasNext();
            }

            public Position next()
            {
                return this.trackPoints.next().getPosition();
            }

            public void remove()
            {
                this.trackPoints.remove();
            }
        };
    }

    private void doRead(java.io.InputStream fis) throws java.io.IOException, org.xml.sax.SAXException
    {
        this.parser.parse(fis, new Handler());
    }

    private class Handler extends org.xml.sax.helpers.DefaultHandler
    {
        // this is a private class used solely by the containing class, so no validation occurs in it.

        private gov.nasa.worldwind.formats.gpx.ElementParser currentElement = null;

        @Override
        public void warning(org.xml.sax.SAXParseException saxParseException) throws org.xml.sax.SAXException
        {
            saxParseException.printStackTrace();
            super.warning(saxParseException);
        }

        @Override
        public void error(org.xml.sax.SAXParseException saxParseException) throws org.xml.sax.SAXException
        {
            saxParseException.printStackTrace();
            super.error(saxParseException);
        }

        @Override
        public void fatalError(org.xml.sax.SAXParseException saxParseException) throws org.xml.sax.SAXException
        {
            saxParseException.printStackTrace();
            super.fatalError(saxParseException);
        }

        private boolean firstElement = true;

        @Override
        public void startElement(String uri, String lname, String qname, org.xml.sax.Attributes attributes)
            throws org.xml.sax.SAXException
        {
            if (this.firstElement)
            {
                if (!lname.equalsIgnoreCase("gpx"))
                    throw new IllegalArgumentException(Logging.getMessage("formats.notGPX", uri));
                else
                    this.firstElement = false;
            }

            if (this.currentElement != null)
            {
                this.currentElement.startElement(uri, lname, qname, attributes);
            }
            else if (lname.equalsIgnoreCase("trk"))
            {
                GpxTrack track = new GpxTrack(uri, lname, qname, attributes);
                this.currentElement = track;
                GpxReader.this.tracks.add(track);
            }
            else if (lname.equalsIgnoreCase("rte"))
            {
                GpxRoute route = new GpxRoute(uri, lname, qname, attributes);
                this.currentElement = route;
                GpxReader.this.tracks.add(route);
            }
        }

        @Override
        public void endElement(String uri, String lname, String qname) throws org.xml.sax.SAXException
        {
            if (this.currentElement != null)
            {
                this.currentElement.endElement(uri, lname, qname);

                if (lname.equalsIgnoreCase(this.currentElement.getElementName()))
                    this.currentElement = null;
            }
        }

        @Override
        public void characters(char[] data, int start, int length) throws org.xml.sax.SAXException
        {
            if (this.currentElement != null)
                this.currentElement.characters(data, start, length);
        }
    }
}
