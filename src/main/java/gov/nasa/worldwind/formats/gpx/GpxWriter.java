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

/**
 * @author dcollins
 * @version $Id: GpxWriter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class GpxWriter
{
    private final org.w3c.dom.Document doc;
    private final javax.xml.transform.Result result;

    public GpxWriter(String path) throws java.io.IOException, javax.xml.parsers.ParserConfigurationException
    {
        if (path == null)
        {
            String msg = Logging.getMessage("nullValue.PathIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        this.doc = factory.newDocumentBuilder().newDocument();
        this.result = new javax.xml.transform.stream.StreamResult(new java.io.File(path));
        createGpxDocument(this.doc);
    }

    public GpxWriter(java.io.OutputStream stream) throws java.io.IOException, javax.xml.parsers.ParserConfigurationException
    {
        if (stream == null)
        {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        this.doc = factory.newDocumentBuilder().newDocument();
        this.result = new javax.xml.transform.stream.StreamResult(stream);
        createGpxDocument(this.doc);
    }

    public void writeTrack(Track track) throws javax.xml.transform.TransformerException
    {
        if (track == null)
        {
            String msg = Logging.getMessage("nullValue.TrackIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        doWriteTrack(track, this.doc.getDocumentElement());
        doFlush();
    }

    public void close()
    {
        // Intentionally left blank,
        // as a placeholder for future functionality.
    }

    private void createGpxDocument(org.w3c.dom.Document doc)
    {
        // Create the GPX document root when the document
        // doesn't already have a root element.
        if (doc != null)
        {
            if (doc.getDocumentElement() != null)
                doc.removeChild(doc.getDocumentElement());

            doc.setXmlStandalone(false);

            org.w3c.dom.Element gpx = doc.createElement("gpx");
            gpx.setAttribute("version", "1.1");
            gpx.setAttribute("creator", "NASA WorldWind");
            doc.appendChild(gpx);
        }
    }

    private void doWriteTrack(Track track, org.w3c.dom.Element elem)
    {
        if (track != null)
        {
            org.w3c.dom.Element trk = this.doc.createElement("trk");

            if (track.getName() != null)
            {
                org.w3c.dom.Element name = this.doc.createElement("name");
                org.w3c.dom.Text nameText = this.doc.createTextNode(track.getName());
                name.appendChild(nameText);
                trk.appendChild(name);
            }

            if (track.getSegments() != null)
            {
                for (TrackSegment ts : track.getSegments())
                    doWriteTrackSegment(ts, trk);
            }

            elem.appendChild(trk);
        }
    }

    private void doWriteTrackSegment(TrackSegment segment, org.w3c.dom.Element elem)
    {
        if (segment != null)
        {
            org.w3c.dom.Element trkseg = this.doc.createElement("trkseg");

            if (segment.getPoints() != null)
            {
                for (TrackPoint tp : segment.getPoints())
                    doWriteTrackPoint(tp, trkseg);
            }

            elem.appendChild(trkseg);
        }
    }

    private void doWriteTrackPoint(TrackPoint point, org.w3c.dom.Element elem)
    {
        if (point != null)
        {
            org.w3c.dom.Element trkpt = this.doc.createElement("trkpt");
            trkpt.setAttribute("lat", Double.toString(point.getLatitude()));
            trkpt.setAttribute("lon", Double.toString(point.getLongitude()));

            org.w3c.dom.Element ele = this.doc.createElement("ele");
            org.w3c.dom.Text eleText = this.doc.createTextNode(Double.toString(point.getElevation()));
            ele.appendChild(eleText);
            trkpt.appendChild(ele);

            if (point.getTime() != null)
            {
                org.w3c.dom.Element time = this.doc.createElement("time");
                org.w3c.dom.Text timeText = this.doc.createTextNode(point.getTime());
                time.appendChild(timeText);
                trkpt.appendChild(time);
            }

            elem.appendChild(trkpt);
        }
    }

    private void doFlush() throws javax.xml.transform.TransformerException
    {
        javax.xml.transform.TransformerFactory factory = javax.xml.transform.TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = factory.newTransformer();
        javax.xml.transform.Source source = new javax.xml.transform.dom.DOMSource(this.doc);
        transformer.transform(source, this.result);
    }
}
