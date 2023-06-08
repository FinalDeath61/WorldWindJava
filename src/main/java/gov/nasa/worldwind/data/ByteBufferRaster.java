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

import gov.nasa.worldwind.Version;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.formats.tiff.GeoTiff;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;

/**
 * @author dcollins
 * @version $Id: ByteBufferRaster.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class ByteBufferRaster extends BufferWrapperRaster
{
    private java.nio.ByteBuffer byteBuffer;

    public ByteBufferRaster(int width, int height, Sector sector, java.nio.ByteBuffer byteBuffer, AVList list)
    {
        super(width, height, sector, BufferWrapper.wrap(byteBuffer, list), list);

        this.byteBuffer = byteBuffer;

        this.validateParameters(list);
    }

    private void validateParameters(AVList list) throws IllegalArgumentException
    {
        this.doValidateParameters(list);
    }

    protected void doValidateParameters(AVList list) throws IllegalArgumentException
    {
    }

    public ByteBufferRaster(int width, int height, Sector sector, AVList params)
    {
        this(width, height, sector, createCompatibleBuffer(width, height, params), params);
    }

    public static java.nio.ByteBuffer createCompatibleBuffer(int width, int height, AVList params)
    {
        if (width < 1)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width < 1");
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        if (height < 1)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "height < 1");
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        Object dataType = params.getValue(AVKey.DATA_TYPE);

        int sizeOfDataType = 0;
        if (AVKey.INT8.equals(dataType))
            sizeOfDataType = (Byte.SIZE / 8);
        else if (AVKey.INT16.equals(dataType))
            sizeOfDataType = (Short.SIZE / 8);
        else if (AVKey.INT32.equals(dataType))
            sizeOfDataType = (Integer.SIZE / 8);
        else if (AVKey.FLOAT32.equals(dataType))
            sizeOfDataType = (Float.SIZE / 8);

        int sizeInBytes = sizeOfDataType * width * height;
        return java.nio.ByteBuffer.allocate(sizeInBytes);
    }

    public java.nio.ByteBuffer getByteBuffer()
    {
        return this.byteBuffer;
    }

    public static DataRaster createGeoreferencedRaster(AVList params)
    {
        if (null == params)
        {
            String msg = Logging.getMessage("nullValue.AVListIsNull");
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(AVKey.WIDTH))
        {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", AVKey.WIDTH);
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }

        int width = (Integer) params.getValue(AVKey.WIDTH);

        if (!(width > 0))
        {
            String msg = Logging.getMessage("generic.InvalidWidth", width);
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(AVKey.HEIGHT))
        {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", AVKey.HEIGHT);
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }

        int height = (Integer) params.getValue(AVKey.HEIGHT);

        if (!(height > 0))
        {
            String msg = Logging.getMessage("generic.InvalidWidth", height);
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(AVKey.SECTOR))
        {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", AVKey.SECTOR);
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }

        Sector sector = (Sector) params.getValue(AVKey.SECTOR);
        if (null == sector)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(AVKey.COORDINATE_SYSTEM))
        {
            // assume Geodetic Coordinate System
            params.setValue(AVKey.COORDINATE_SYSTEM, AVKey.COORDINATE_SYSTEM_GEOGRAPHIC);
        }

        String cs = params.getStringValue(AVKey.COORDINATE_SYSTEM);
        if (!params.hasKey(AVKey.PROJECTION_EPSG_CODE))
        {
            if (AVKey.COORDINATE_SYSTEM_GEOGRAPHIC.equals(cs))
            {
                // assume WGS84
                params.setValue(AVKey.PROJECTION_EPSG_CODE, GeoTiff.GCS.WGS_84);
            }
            else
            {
                String msg = Logging.getMessage("generic.MissingRequiredParameter", AVKey.PROJECTION_EPSG_CODE);
                log.debug(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        // if PIXEL_WIDTH is specified, we are not overriding it because UTM images
        // will have different pixel size
        if (!params.hasKey(AVKey.PIXEL_WIDTH))
        {
            if (AVKey.COORDINATE_SYSTEM_GEOGRAPHIC.equals(cs))
            {
                double pixelWidth = sector.getDeltaLonDegrees() / (double) width;
                params.setValue(AVKey.PIXEL_WIDTH, pixelWidth);
            }
            else
            {
                String msg = Logging.getMessage("generic.MissingRequiredParameter", AVKey.PIXEL_WIDTH);
                log.debug(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        // if PIXEL_HEIGHT is specified, we are not overriding it
        // because UTM images will have different pixel size
        if (!params.hasKey(AVKey.PIXEL_HEIGHT))
        {
            if (AVKey.COORDINATE_SYSTEM_GEOGRAPHIC.equals(cs))
            {
                double pixelHeight = sector.getDeltaLatDegrees() / (double) height;
                params.setValue(AVKey.PIXEL_HEIGHT, pixelHeight);
            }
            else
            {
                String msg = Logging.getMessage("generic.MissingRequiredParameter", AVKey.PIXEL_HEIGHT);
                log.debug(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        if (!params.hasKey(AVKey.PIXEL_FORMAT))
        {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", AVKey.PIXEL_FORMAT);
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }
        else
        {
            String pixelFormat = params.getStringValue(AVKey.PIXEL_FORMAT);
            if (!AVKey.ELEVATION.equals(pixelFormat) && !AVKey.IMAGE.equals(pixelFormat))
            {
                String msg = Logging.getMessage("generic.UnknownValueForKey", pixelFormat, AVKey.PIXEL_FORMAT);
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        if (!params.hasKey(AVKey.DATA_TYPE))
        {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", AVKey.DATA_TYPE);
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }

        // validate elevation parameters
        if (AVKey.ELEVATION.equals(params.getValue(AVKey.PIXEL_FORMAT)))
        {
            String type = params.getStringValue(AVKey.DATA_TYPE);
            if (!AVKey.FLOAT32.equals(type) && !AVKey.INT16.equals(type))
            {
                String msg = Logging.getMessage("generic.UnknownValueForKey", type, AVKey.DATA_TYPE);
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        if (!params.hasKey(AVKey.ORIGIN) && AVKey.COORDINATE_SYSTEM_GEOGRAPHIC.equals(cs))
        {
            // set UpperLeft corner as the origin, if not specified
            LatLon origin = new LatLon(sector.getMaxLatitude(), sector.getMinLongitude());
            params.setValue(AVKey.ORIGIN, origin);
        }

        if (!params.hasKey(AVKey.BYTE_ORDER))
        {
            String msg = Logging.getMessage("generic.MissingRequiredParameter", AVKey.BYTE_ORDER);
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!params.hasKey(AVKey.DATE_TIME))
        {
            // add NUL (\0) termination as required by TIFF v6 spec (20 bytes length)
            String timestamp = String.format("%1$tY:%1$tm:%1$td %tT\0", Calendar.getInstance());
            params.setValue(AVKey.DATE_TIME, timestamp);
        }

        if (!params.hasKey(AVKey.VERSION))
        {
            params.setValue(AVKey.VERSION, Version.getVersion());
        }

        return new ByteBufferRaster(width, height, sector, params);
    }
}
