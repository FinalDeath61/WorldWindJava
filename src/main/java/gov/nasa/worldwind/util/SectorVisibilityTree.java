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
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Determines the visible sectors at a specifed resolution within the draw context's current visible sector.
 *
 * @author Tom Gaskins
 * @version $Id: SectorVisibilityTree.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class SectorVisibilityTree
{
    protected static class Context
    {
        private final DrawContext dc;
        private final double sectorSize;
        private final List<Sector> sectors;

        public Context(DrawContext dc, double sectorSize, List<Sector> sectors)
        {
            this.dc = dc;
            this.sectorSize = sectorSize;
            this.sectors = sectors;
        }
    }

    protected double sectorSize;
    protected Object globeStateKey;
    protected HashMap<Sector, Extent> prevExtents = new HashMap<Sector, Extent>();
    protected HashMap<Sector, Extent> newExtents = new HashMap<Sector, Extent>();
    protected ArrayList<Sector> sectors = new ArrayList<Sector>();
    protected long timeStamp;

    public SectorVisibilityTree()
    {
    }

    public double getSectorSize()
    {
        return sectorSize;
    }

    public List<Sector> getSectors()
    {
        return this.sectors;
    }

    public long getTimeStamp()
    {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp)
    {
        this.timeStamp = timeStamp;
    }

    public void clearSectors()
    {
        this.sectors.clear();
    }

    protected DecisionTree<Sector, Context> tree = new DecisionTree<Sector, Context>(
        new DecisionTree.Controller<Sector, Context>()
        {
            public boolean isTerminal(Sector s, Context context)
            {
                if (s.getDeltaLat().degrees > context.sectorSize)
                    return false;

                context.sectors.add(s);
                return true;
            }

            public Sector[] split(Sector s, Context context)
            {
                return s.subdivide();
            }

            public boolean isVisible(Sector s, Context c)
            {
                Extent extent = prevExtents.get(s);
                if (extent == null)
                    extent = Sector.computeBoundingBox(c.dc.getGlobe(), c.dc.getVerticalExaggeration(), s);

                if (extent.intersects(c.dc.getView().getFrustumInModelCoordinates()))
                {
                    newExtents.put(s, extent);
                    return true;
                }

                return false;
            }
        });

    /**
     * Determines the visible sectors at a specifed resolution within the draw context's current visible sector.
     *
     * @param dc         the current draw context
     * @param sectorSize the granularity of sector visibility, in degrees. All visible sectors of this size are found.
     *                   The value must be in the range, 1 second &lt;= sectorSize &lt;= 180 degrees.
     *
     * @return the list of visible sectors. The list will be empty if no sectors are visible.
     *
     * @throws IllegalArgumentException if the draw context is null.
     */
    public List<Sector> refresh(DrawContext dc, double sectorSize)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (sectorSize < Angle.SECOND.degrees || sectorSize > 180)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", sectorSize);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (dc.getVisibleSector() == null)
            return Collections.emptyList();

        this.sectors = new ArrayList<Sector>();
        this.sectorSize = sectorSize;
        this.swapCylinderLists(dc);
        this.tree.traverse(dc.getVisibleSector(), new Context(dc, sectorSize, this.sectors));

        Collections.sort(this.sectors);
        return this.sectors;
    }

    /**
     * Determines the visible sectors at a specified resolution within a specified sector.
     *
     * @param dc           the current draw context
     * @param sectorSize   the granularity of sector visibility, in degrees. All visible sectors of this size are found.
     *                     The value must be in the range, 1 second &lt;= sectorSize &lt;= 180 degrees.
     * @param searchSector the overall sector for which to determine visibility. May be null, in which case the current
     *                     visible sector of the draw context is used.
     *
     * @return the list of visible sectors. The list will be empty if no sectors are visible.
     *
     * @throws IllegalArgumentException if the draw context is null, the sector size is less than or equal to zero, or
     *                                  the search sector list is null.
     */
    public List<Sector> refresh(DrawContext dc, double sectorSize, Sector searchSector)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (sectorSize < Angle.SECOND.degrees || sectorSize > 180)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", sectorSize);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (searchSector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.sectors = new ArrayList<Sector>();
        this.sectorSize = sectorSize;
        this.swapCylinderLists(dc);
        this.tree.traverse(searchSector, new Context(dc, sectorSize, this.sectors));

        Collections.sort(this.sectors);
        return this.sectors;
    }

    /**
     * Determines the visible sectors at a specified resolution within a collection of sectors. This method can be used
     * to recursively determine visible sectors: the output of one invocation can be passed as an argument to the next
     * invocation.
     *
     * @param dc            the current draw context
     * @param sectorSize    the granularity of sector visibility, in degrees. All visible sectors of this size are The
     *                      value must be in the range, 1 second &lt;= sectorSize &lt;= 180 degrees. found.
     * @param searchSectors the sectors for which to determine visibility.
     *
     * @return the list of visible sectors. The list will be empty if no sectors are visible.
     *
     * @throws IllegalArgumentException if the draw context is null, the sector size is less than or equal to zero or
     *                                  the search sector list is null.
     */
    public List<Sector> refresh(DrawContext dc, double sectorSize, List<Sector> searchSectors)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (sectorSize < Angle.SECOND.degrees || sectorSize > 180)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", sectorSize);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (searchSectors == null)
        {
            String message = Logging.getMessage("nullValue.SectorListIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.swapCylinderLists(dc);
        this.sectors = new ArrayList<Sector>();
        this.sectorSize = sectorSize;
        for (Sector s : searchSectors)
        {
            this.tree.traverse(s, new Context(dc, sectorSize, this.sectors));
        }

        Collections.sort(this.sectors);
        return this.sectors;
    }

    protected void swapCylinderLists(DrawContext dc)
    {
        if (this.globeStateKey != null && !dc.getGlobe().getStateKey(dc).equals(this.globeStateKey))
            this.newExtents.clear();

        this.prevExtents.clear();
        HashMap<Sector, Extent> temp = this.prevExtents;
        this.prevExtents = newExtents;
        this.newExtents = temp;

        this.globeStateKey = dc.getGlobe().getStateKey(dc);
    }
}
