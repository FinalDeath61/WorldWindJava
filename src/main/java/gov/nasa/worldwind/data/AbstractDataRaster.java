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
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author Lado Garakanidze
 * @version $Id: AbstractDataRaster.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public abstract class AbstractDataRaster extends AVListImpl implements DataRaster
{
    protected int width = 0;
    protected int height = 0;

    protected AbstractDataRaster()
    {
        super();
    }

    protected AbstractDataRaster(int width, int height, Sector sector) throws IllegalArgumentException
    {
        super();

        if (width < 0)
        {
            String message = Logging.getMessage("generic.InvalidWidth", width);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (height < 0)
        {
            String message = Logging.getMessage("generic.InvalidHeight", height);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            log.debug(message);
//            throw new IllegalArgumentException(message);
        }

        // for performance reasons we are "caching" these parameters in addition to AVList
        this.width = width;
        this.height = height;

        if (null != sector)
        {
            this.setValue(AVKey.SECTOR, sector);
        }

        this.setValue(AVKey.WIDTH, width);
        this.setValue(AVKey.HEIGHT, height);
    }

    protected AbstractDataRaster(int width, int height, Sector sector, AVList list) throws IllegalArgumentException
    {
        this(width, height, sector);

        if (null != list)
        {
            for (Map.Entry<String, Object> entry : list.getEntries())
            {
                this.setValue(entry.getKey(), entry.getValue());
            }
        }
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    public Sector getSector()
    {
        if (this.hasKey(AVKey.SECTOR))
        {
            return (Sector) this.getValue(AVKey.SECTOR);
        }
        return null;
    }

    @Override
    public Object setValue(String key, Object value)
    {
        if (null == key)
        {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        // Do not allow to change existing WIDTH or HEIGHT

        if (this.hasKey(key))
        {
            if (AVKey.WIDTH.equals(key) && this.getWidth() != (Integer) value)
            {
                String message = Logging.getMessage("generic.AttemptToChangeReadOnlyProperty", key);
                log.debug(message);
                // relax restriction, just log and continue
//                throw new IllegalArgumentException(message);
                return this;
            }
            else if (AVKey.HEIGHT.equals(key) && this.getHeight() != (Integer) value)
            {
                String message = Logging.getMessage("generic.AttemptToChangeReadOnlyProperty", key);
                log.debug(message);
                // relax restriction, just log and continue
//                throw new IllegalArgumentException(message);
                return this;
            }
        }
        return super.setValue(key, value);
    }

    protected java.awt.Rectangle computeClipRect(Sector clipSector, DataRaster clippedRaster)
    {
        java.awt.geom.AffineTransform geographicToRaster = this.computeGeographicToRasterTransform(
            clippedRaster.getWidth(), clippedRaster.getHeight(), clippedRaster.getSector());

        java.awt.geom.Point2D geoPoint = new java.awt.geom.Point2D.Double();
        java.awt.geom.Point2D ul = new java.awt.geom.Point2D.Double();
        java.awt.geom.Point2D lr = new java.awt.geom.Point2D.Double();

        geoPoint.setLocation(clipSector.getMinLongitude().degrees, clipSector.getMaxLatitude().degrees);
        geographicToRaster.transform(geoPoint, ul);

        geoPoint.setLocation(clipSector.getMaxLongitude().degrees, clipSector.getMinLatitude().degrees);
        geographicToRaster.transform(geoPoint, lr);

        int x = (int) Math.floor(ul.getX());
        int y = (int) Math.floor(ul.getY());
        int width = (int) Math.ceil(lr.getX() - ul.getX());
        int height = (int) Math.ceil(lr.getY() - ul.getY());

        return new java.awt.Rectangle(x, y, width, height);
    }

    protected java.awt.geom.AffineTransform computeSourceToDestTransform(
        int sourceWidth, int sourceHeight, Sector sourceSector,
        int destWidth, int destHeight, Sector destSector)
    {
        // Compute the the transform from source to destination coordinates. In this computation a pixel is assumed
        // to cover a finite area.

        double ty = destHeight * -(sourceSector.getMaxLatitude().degrees - destSector.getMaxLatitude().degrees)
            / destSector.getDeltaLatDegrees();
        double tx = destWidth * (sourceSector.getMinLongitude().degrees - destSector.getMinLongitude().degrees)
            / destSector.getDeltaLonDegrees();

        double sy = ((double) destHeight / (double) sourceHeight)
            * (sourceSector.getDeltaLatDegrees() / destSector.getDeltaLatDegrees());
        double sx = ((double) destWidth / (double) sourceWidth)
            * (sourceSector.getDeltaLonDegrees() / destSector.getDeltaLonDegrees());

        java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform();
        transform.translate(tx, ty);
        transform.scale(sx, sy);
        return transform;
    }

    protected java.awt.geom.AffineTransform computeGeographicToRasterTransform(int width, int height, Sector sector)
    {
        // Compute the the transform from geographic to raster coordinates. In this computation a pixel is assumed
        // to cover a finite area.

        double ty = -sector.getMaxLatitude().degrees;
        double tx = -sector.getMinLongitude().degrees;

        double sy = -(height / sector.getDeltaLatDegrees());
        double sx = (width / sector.getDeltaLonDegrees());

        java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform();
        transform.scale(sx, sy);
        transform.translate(tx, ty);
        return transform;
    }

    public DataRaster getSubRaster(int width, int height, Sector sector, AVList params)
    {
        params = (null == params) ? new AVListImpl() : params;

        // copy parent raster keys/values; only those key/value will be copied that do exist in the parent raster
        // AND does NOT exist in the requested raster
        String[] keysToCopy = new String[] {
            AVKey.DATA_TYPE, AVKey.MISSING_DATA_SIGNAL, AVKey.BYTE_ORDER, AVKey.PIXEL_FORMAT, AVKey.ELEVATION_UNIT
        };
        WWUtil.copyValues(this, params, keysToCopy, false);

        params.setValue(AVKey.WIDTH, width);
        params.setValue(AVKey.HEIGHT, height);
        params.setValue(AVKey.SECTOR, sector);

        return this.getSubRaster(params);
    }

    /**
     * Reads the specified region of interest (ROI) with given extent, width, and height, and type
     *
     * @param params Required parameters are:
     *               <p>
     *               AVKey.HEIGHT as Integer, specifies a height of the desired ROI AVKey.WIDTH as Integer, specifies a
     *               width of the desired ROI AVKey.SECTOR as Sector, specifies an extent of the desired ROI
     *               <p>
     *               Optional parameters are:
     *               <p>
     *               AVKey.BAND_ORDER as array of integers, examples: for RGBA image: new int[] { 0, 1, 2, 3 }, or for
     *               ARGB image: new int[] { 3, 0, 1, 2 } or if you want only RGB bands of the RGBA image: new int[] {
     *               0, 1, 2 } or only Intensity (4th) band of the specific aerial image: new int[] { 3 }
     *
     * @return DataRaster (BufferedImageRaster for imagery or ByteBufferDataRaster for elevations)
     */
    public DataRaster getSubRaster(AVList params)
    {
        if (null == params)
        {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (!params.hasKey(AVKey.WIDTH))
        {
            String message = Logging.getMessage("generic.MissingRequiredParameter", AVKey.WIDTH);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        int roiWidth = (Integer) params.getValue(AVKey.WIDTH);
        if (roiWidth <= 0)
        {
            String message = Logging.getMessage("generic.InvalidWidth", roiWidth);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (!params.hasKey(AVKey.HEIGHT))
        {
            String message = Logging.getMessage("generic.MissingRequiredParameter", AVKey.HEIGHT);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        int roiHeight = (Integer) params.getValue(AVKey.HEIGHT);
        if (roiHeight <= 0)
        {
            String message = Logging.getMessage("generic.InvalidHeight", roiHeight);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (!params.hasKey(AVKey.SECTOR))
        {
            String message = Logging.getMessage("generic.MissingRequiredParameter", AVKey.SECTOR);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        Sector roiSector = (Sector) params.getValue(AVKey.SECTOR);
        if (null == roiSector || Sector.EMPTY_SECTOR.equals(roiSector))
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (Sector.EMPTY_SECTOR.equals(roiSector))
        {
            String message = Logging.getMessage("nullValue.SectorGeometryIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        // copy parent raster keys/values; only those key/value will be copied that do exist in the parent raster
        // AND does NOT exist in the requested raster
        String[] keysToCopy = new String[] {
            AVKey.DATA_TYPE, AVKey.MISSING_DATA_SIGNAL, AVKey.BYTE_ORDER, AVKey.PIXEL_FORMAT, AVKey.ELEVATION_UNIT
        };
        WWUtil.copyValues(this, params, keysToCopy, false);

        return this.doGetSubRaster(roiWidth, roiHeight, roiSector, params);
    }

    abstract DataRaster doGetSubRaster(int roiWidth, int roiHeight, Sector roiSector, AVList roiParams);
}
