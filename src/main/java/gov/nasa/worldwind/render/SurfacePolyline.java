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
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Exportable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.ogc.kml.KMLConstants;
import gov.nasa.worldwind.ogc.kml.impl.KMLExportUtil;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.util.SurfaceTileDrawContext;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static gov.nasa.worldwind.ogc.kml.impl.KMLExportUtil.kmlBoolean;

/**
 * @author dcollins
 * @version $Id: SurfacePolyline.java 2406 2014-10-29 23:39:29Z dcollins $
 */
@Slf4j
public class SurfacePolyline extends AbstractSurfaceShape implements Exportable
{
    protected boolean closed;
    protected Iterable<? extends LatLon> locations;

    /** Constructs a new surface polyline with the default attributes and no locations. */
    public SurfacePolyline()
    {
    }

    /**
     * Creates a shallow copy of the specified source shape.
     *
     * @param source the shape to copy.
     */
    public SurfacePolyline(SurfacePolyline source)
    {
        super(source);

        this.closed = source.closed;
        this.locations = source.locations;
    }

    /**
     * Constructs a new surface polyline with the specified normal (as opposed to highlight) attributes and no
     * locations. Modifying the attribute reference after calling this constructor causes this shape's appearance to
     * change accordingly.
     *
     * @param normalAttrs the normal attributes. May be null, in which case default attributes are used.
     */
    public SurfacePolyline(ShapeAttributes normalAttrs)
    {
        super(normalAttrs);
    }

    /**
     * Constructs a new surface polyline with the default attributes and the specified iterable of locations.
     * <p>
     * Note: If fewer than two locations is specified, no polyline is drawn.
     *
     * @param iterable the polyline locations.
     *
     * @throws IllegalArgumentException if the locations iterable is null.
     */
    public SurfacePolyline(Iterable<? extends LatLon> iterable)
    {
        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.locations = iterable;
    }

    /**
     * Constructs a new surface polyline with the specified normal (as opposed to highlight) attributes and the
     * specified iterable of locations. Modifying the attribute reference after calling this constructor causes this
     * shape's appearance to change accordingly.
     * <p>
     * Note: If fewer than two locations is specified, no polyline is drawn.
     *
     * @param normalAttrs the normal attributes. May be null, in which case default attributes are used.
     * @param iterable    the polyline locations.
     *
     * @throws IllegalArgumentException if the locations iterable is null.
     */
    public SurfacePolyline(ShapeAttributes normalAttrs, Iterable<? extends LatLon> iterable)
    {
        super(normalAttrs);

        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.locations = iterable;
    }

    public boolean isClosed()
    {
        return this.closed;
    }

    public void setClosed(boolean closed)
    {
        this.closed = closed;
        this.onShapeChanged(); // Potentially causes the shape's geometry to change.
    }

    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        return this.getLocations();
    }

    public Iterable<? extends LatLon> getLocations()
    {
        return this.locations;
    }

    public void setLocations(Iterable<? extends LatLon> iterable)
    {
        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.locations = iterable;
        this.onShapeChanged();
    }

    public Position getReferencePosition()
    {
        if (this.locations == null)
            return null;

        Iterator<? extends LatLon> iterator = this.locations.iterator();
        if (!iterator.hasNext())
            return null;

        return new Position(iterator.next(), 0);
    }

    protected List<List<LatLon>> createGeometry(Globe globe, double edgeIntervalsPerDegree)
    {
        if (this.locations == null)
            return null;

        ArrayList<LatLon> drawLocations = new ArrayList<LatLon>();
        this.generateIntermediateLocations(this.locations, edgeIntervalsPerDegree, this.isClosed(), drawLocations);

        if (drawLocations.size() < 2)
            return null;

        ArrayList<List<LatLon>> geom = new ArrayList<List<LatLon>>();
        geom.add(drawLocations);

        return geom;
    }

    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition)
    {
        if (this.locations == null)
            return;

        ArrayList<LatLon> newLocations = new ArrayList<LatLon>();

        for (LatLon ll : this.locations)
        {
            Angle heading = LatLon.greatCircleAzimuth(oldReferencePosition, ll);
            Angle pathLength = LatLon.greatCircleDistance(oldReferencePosition, ll);
            newLocations.add(LatLon.greatCircleEndPosition(newReferencePosition, heading, pathLength));
        }

        this.setLocations(newLocations);
    }

    protected void doMoveTo(Globe globe, Position oldReferencePosition, Position newReferencePosition)
    {
        if (this.locations == null)
            return;

        List<LatLon> newLocations = LatLon.computeShiftedLocations(globe, oldReferencePosition, newReferencePosition,
            this.getLocations());

        this.setLocations(newLocations);
    }

    protected void drawInterior(DrawContext dc, SurfaceTileDrawContext sdc)
    {
        // Intentionally left blank; SurfacePolyline does not render an interior.
    }

    /** {@inheritDoc} Overridden to treat the shape as an open path if the polyline is not closed. */
    @Override
    protected boolean canContainPole()
    {
        return this.isClosed();
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        Iterable<? extends LatLon> iterable = this.getLocations();
        if (iterable != null)
            rs.addStateValueAsLatLonList(context, "locationList", iterable);

        rs.addStateValueAsBoolean(context, "closed", this.isClosed());
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        Iterable<LatLon> iterable = rs.getStateValueAsLatLonList(context, "locationList");
        if (iterable != null)
            this.setLocations(iterable);

        Boolean b = rs.getStateValueAsBoolean(context, "closed");
        if (b != null)
            this.setClosed(b);
    }

    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.legacyRestoreState(rs, context);

        List<LatLon> locations = rs.getStateValueAsLatLonList(context, "locations");
        if (locations != null)
            this.setLocations(locations);
    }

    //**************************************************************//
    //************************* Export *****************************//
    //**************************************************************//

    /**
     * Export the polyline to KML as a {@code <Placemark>} element. The {@code output} object will receive the data.
     * This object must be one of: java.io.Writer java.io.OutputStream javax.xml.stream.XMLStreamWriter
     *
     * @param output Object to receive the generated KML.
     *
     * @throws XMLStreamException If an exception occurs while writing the KML
     * @throws IOException        if an exception occurs while exporting the data.
     * @see #export(String, Object)
     */
    protected void exportAsKML(Object output) throws IOException, XMLStreamException
    {
        XMLStreamWriter xmlWriter = null;
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        boolean closeWriterWhenFinished = true;

        if (output instanceof XMLStreamWriter)
        {
            xmlWriter = (XMLStreamWriter) output;
            closeWriterWhenFinished = false;
        }
        else if (output instanceof Writer)
        {
            xmlWriter = factory.createXMLStreamWriter((Writer) output);
        }
        else if (output instanceof OutputStream)
        {
            xmlWriter = factory.createXMLStreamWriter((OutputStream) output);
        }

        if (xmlWriter == null)
        {
            String message = Logging.getMessage("Export.UnsupportedOutputObject");
            log.warn(message);
            throw new IllegalArgumentException(message);
        }

        xmlWriter.writeStartElement("Placemark");

        String property = getStringValue(AVKey.DISPLAY_NAME);
        if (property != null)
        {
            xmlWriter.writeStartElement("name");
            xmlWriter.writeCharacters(property);
            xmlWriter.writeEndElement();
        }

        xmlWriter.writeStartElement("visibility");
        xmlWriter.writeCharacters(kmlBoolean(this.isVisible()));
        xmlWriter.writeEndElement();

        String shortDescription = (String) getValue(AVKey.SHORT_DESCRIPTION);
        if (shortDescription != null)
        {
            xmlWriter.writeStartElement("Snippet");
            xmlWriter.writeCharacters(shortDescription);
            xmlWriter.writeEndElement();
        }

        String description = (String) getValue(AVKey.BALLOON_TEXT);
        if (description != null)
        {
            xmlWriter.writeStartElement("description");
            xmlWriter.writeCharacters(description);
            xmlWriter.writeEndElement();
        }

        final ShapeAttributes normalAttributes = getAttributes();
        final ShapeAttributes highlightAttributes = getHighlightAttributes();

        // Write style map
        if (normalAttributes != null || highlightAttributes != null)
        {
            xmlWriter.writeStartElement("StyleMap");
            KMLExportUtil.exportAttributesAsKML(xmlWriter, KMLConstants.NORMAL, normalAttributes);
            KMLExportUtil.exportAttributesAsKML(xmlWriter, KMLConstants.HIGHLIGHT, highlightAttributes);
            xmlWriter.writeEndElement(); // StyleMap
        }

        // Write geometry
        xmlWriter.writeStartElement("LineString");

        xmlWriter.writeStartElement("extrude");
        xmlWriter.writeCharacters("0");
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("tessellate");
        xmlWriter.writeCharacters("1");
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("altitudeMode");
        xmlWriter.writeCharacters("clampToGround");
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("coordinates");
        for (LatLon position : this.getLocations())
        {
            xmlWriter.writeCharacters(Double.toString(position.getLongitude().getDegrees()));
            xmlWriter.writeCharacters(",");
            xmlWriter.writeCharacters(Double.toString(position.getLatitude().getDegrees()));
            xmlWriter.writeCharacters(" ");
        }
        xmlWriter.writeEndElement(); // coordinates

        xmlWriter.writeEndElement(); // LineString
        xmlWriter.writeEndElement(); // Placemark

        xmlWriter.flush();
        if (closeWriterWhenFinished)
            xmlWriter.close();
    }
}
