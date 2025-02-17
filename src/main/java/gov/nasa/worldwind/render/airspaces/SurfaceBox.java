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

package gov.nasa.worldwind.render.airspaces;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AbstractSurfaceShape;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.SurfaceTileDrawContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SurfaceBox extends AbstractSurfaceShape
{
    protected List<LatLon> locations;
    protected int lengthSegments;
    protected int widthSegments;
    protected boolean enableStartCap = true;
    protected boolean enableEndCap = true;
    protected boolean enableCenterLine;
    protected List<List<LatLon>> activeCenterLineGeometry = new ArrayList<List<LatLon>>(); // re-determined each frame

    public SurfaceBox()
    {
    }

    public List<LatLon> getLocations()
    {
        return this.locations;
    }

    public void setLocations(List<LatLon> locations)
    {
        this.locations = locations;
        this.onShapeChanged();
    }

    public int getLengthSegments()
    {
        return this.lengthSegments;
    }

    public void setLengthSegments(int lengthSegments)
    {
        this.lengthSegments = lengthSegments;
        this.onShapeChanged();
    }

    public int getWidthSegments()
    {
        return this.widthSegments;
    }

    public void setWidthSegments(int widthSegments)
    {
        this.widthSegments = widthSegments;
        this.onShapeChanged();
    }

    public boolean[] isEnableCaps()
    {
        return new boolean[] {this.enableStartCap, this.enableEndCap};
    }

    public void setEnableCaps(boolean enableStartCap, boolean enableEndCap)
    {
        this.enableStartCap = enableStartCap;
        this.enableEndCap = enableEndCap;
        this.onShapeChanged();
    }

    public boolean isEnableCenterLine()
    {
        return this.enableCenterLine;
    }

    public void setEnableCenterLine(boolean enable)
    {
        this.enableCenterLine = enable;
    }

    @Override
    public Position getReferencePosition()
    {
        return this.locations != null && this.locations.size() > 0 ? new Position(this.locations.get(0), 0) : null;
    }

    @Override
    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition)
    {
        // Intentionally left blank.
    }

    @Override
    protected void doMoveTo(Globe globe, Position oldReferencePosition, Position newReferencePosition)
    {
        // Intentionally left blank.
    }

    protected List<List<LatLon>> createGeometry(Globe globe, double edgeIntervalsPerDegree)
    {
        if (this.locations == null)
            return null;

        ArrayList<List<LatLon>> geom = new ArrayList<List<LatLon>>();

        // Generate the box interior locations. Store the interior geometry in index 0.
        ArrayList<LatLon> interior = new ArrayList<LatLon>();
        geom.add(interior);

        for (int i = 0; i < this.locations.size() - 1; i++)
        {
            LatLon a = this.locations.get(i);
            LatLon b = this.locations.get(i + 1); // first and last location are the same
            interior.add(a);
            this.addIntermediateLocations(a, b, edgeIntervalsPerDegree, interior);
        }

        // Generate the box outline locations. Store the outline locations in indices 1 through size-2.
        int[] sideSegments = {2 * this.widthSegments, this.lengthSegments, 2 * this.widthSegments, this.lengthSegments};
        boolean[] sideFlag = {this.enableStartCap, true, this.enableEndCap, true};

        int offset = 0;
        for (int i = 0; i < 4; i++)
        {
            if (sideFlag[i])
            {
                geom.add(this.makeLocations(offset, sideSegments[i], edgeIntervalsPerDegree));
            }

            offset += sideSegments[i] + 1;
        }

        // Generate the box center line locations. Store the center line geometry at index size-1.
        LatLon beginLocation = this.locations.get(this.widthSegments);
        LatLon endLocation = this.locations.get(3 * this.widthSegments + this.lengthSegments + 2);
        ArrayList<LatLon> centerLine = new ArrayList<LatLon>();
        centerLine.add(beginLocation);
        this.addIntermediateLocations(beginLocation, endLocation, edgeIntervalsPerDegree, centerLine);
        centerLine.add(endLocation);
        geom.add(centerLine);

        return geom;
    }

    protected ArrayList<LatLon> makeLocations(int offset, int count, double edgeIntervalsPerDegree)
    {
        ArrayList<LatLon> locations = new ArrayList<LatLon>();

        for (int i = offset; i < offset + count; i++)
        {
            LatLon a = this.locations.get(i);
            LatLon b = this.locations.get(i + 1);

            locations.add(a);
            this.addIntermediateLocations(a, b, edgeIntervalsPerDegree, locations);

            if (i == offset + count - 1)
                locations.add(b);
        }

        return locations;
    }

    @Override
    protected void determineActiveGeometry(DrawContext dc, SurfaceTileDrawContext sdc)
    {
        this.activeGeometry.clear();
        this.activeOutlineGeometry.clear();
        this.activeCenterLineGeometry.clear();

        List<List<LatLon>> geom = this.getCachedGeometry(dc, sdc); // calls createGeometry
        if (geom == null)
            return;

        int index = 0; // interior geometry stored in index 0
        List<LatLon> interior = geom.get(index++);
        String pole = this.containsPole(interior);
        if (pole != null) // interior compensates for poles and dateline crossing, see WWJ-284
        {
            this.activeGeometry.add(this.cutAlongDateLine(interior, pole, dc.getGlobe()));
        }
        else if (LatLon.locationsCrossDateLine(interior))
        {
            this.activeGeometry.addAll(this.repeatAroundDateline(interior));
        }
        else
        {
            this.activeGeometry.add(interior);
        }

        for (; index < geom.size() - 1; index++) // outline geometry stored in indices 1 through size-2
        {
            List<LatLon> outline = geom.get(index);
            if (LatLon.locationsCrossDateLine(outline)) // outlines compensate for dateline crossing, see WWJ-452
            {
                this.activeOutlineGeometry.addAll(this.repeatAroundDateline(outline));
            }
            else
            {
                this.activeOutlineGeometry.add(outline);
            }
        }

        if (index < geom.size()) // center line geometry stored in index size-1
        {
            List<LatLon> centerLine = geom.get(index);
            if (LatLon.locationsCrossDateLine(centerLine)) // outlines compensate for dateline crossing, see WWJ-452
            {
                this.activeCenterLineGeometry.addAll(this.repeatAroundDateline(centerLine));
            }
            else
            {
                this.activeCenterLineGeometry.add(centerLine);
            }
        }
    }

    protected void drawOutline(DrawContext dc, SurfaceTileDrawContext sdc)
    {
        super.drawOutline(dc, sdc);

        if (this.enableCenterLine)
        {
            this.drawCenterLine(dc);
        }
    }

    protected void drawCenterLine(DrawContext dc)
    {
        if (this.activeCenterLineGeometry.isEmpty())
            return;

        this.applyCenterLineState(dc, this.getActiveAttributes());

        for (List<LatLon> drawLocations : this.activeCenterLineGeometry)
        {
            this.drawLineStrip(dc, drawLocations);
        }
    }

    protected void applyCenterLineState(DrawContext dc, ShapeAttributes attributes)
    {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        if (!dc.isPickingMode() && attributes.getOutlineStippleFactor() <= 0) // don't override stipple in attributes
        {
            gl.glEnable(GL2.GL_LINE_STIPPLE);
            gl.glLineStipple(Box.DEFAULT_CENTER_LINE_STIPPLE_FACTOR, Box.DEFAULT_CENTER_LINE_STIPPLE_PATTERN);
        }
    }

    @Override
    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        return this.locations;
    }
}
