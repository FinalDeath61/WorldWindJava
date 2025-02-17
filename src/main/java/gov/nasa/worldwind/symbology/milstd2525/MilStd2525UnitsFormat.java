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

package gov.nasa.worldwind.symbology.milstd2525;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.UnitsFormat;
import lombok.extern.slf4j.Slf4j;

/**
 * Units format configured to format locations and altitudes according to the defaults defined by MIL-STD-2525C.
 *
 * @author pabercrombie
 * @version $Id: MilStd2525UnitsFormat.java 482 2012-03-27 01:27:15Z pabercrombie $
 */
@Slf4j
public class MilStd2525UnitsFormat extends UnitsFormat
{
    /**
     * Construct an instance that displays length in kilometers, area in square kilometers and angles in degrees,
     * minutes, seconds.
     */
    public MilStd2525UnitsFormat()
    {
        this(UnitsFormat.KILOMETERS, UnitsFormat.SQUARE_KILOMETERS, true);
    }

    /**
     * Constructs an instance that display length and area in specified units, and angles in degrees, minutes, seconds.
     *
     * @param lengthUnits the desired length units. Available length units are <code>METERS, KILOMETERS, MILES,
     *                    NAUTICAL_MILES, YARDS</code> and <code>FEET</code>.
     * @param areaUnits   the desired area units. Available area units are <code>SQUARE_METERS, SQUARE_KILOMETERS,
     *                    HECTARE, ACRE, SQUARE_YARD</code> and <code>SQUARE_FEET</code>.
     *
     * @throws IllegalArgumentException if either <code>lengthUnits</code> or <code>areaUnits</code> is null.
     */
    public MilStd2525UnitsFormat(String lengthUnits, String areaUnits)
    {
        this(lengthUnits, areaUnits, true);
    }

    /**
     * Constructs an instance that display length and area in specified units, and angles in a specified format.
     *
     * @param lengthUnits the desired length units. Available length units are <code>METERS, KILOMETERS, MILES,
     *                    NAUTICAL_MILES, YARDS</code> and <code>FEET</code>.
     * @param areaUnits   the desired area units. Available area units are <code>SQUARE_METERS, SQUARE_KILOMETERS,
     *                    HECTARE, ACRE, SQUARE_YARD</code> and <code>SQUARE_FEET</code>.
     * @param showDMS     true if the desired angle format is degrees-minutes-seconds, false if the format is decimal
     *                    degrees.
     *
     * @throws IllegalArgumentException if either <code>lengthUnits</code> or <code>areaUnits</code> is null.
     */
    public MilStd2525UnitsFormat(String lengthUnits, String areaUnits, boolean showDMS)
    {
        super(lengthUnits, areaUnits, showDMS);
        this.setAltitudeUnits(FEET);
    }

    /**
     * Format a latitude value. If this format is configured to show degrees, minutes, and seconds then the returned
     * string will use the format "DDMMSS.S".
     *
     * @param angle the angle to format.
     *
     * @return a string containing the formatted angle.
     *
     * @throws IllegalArgumentException if the angle is null.
     */
    @Override
    public String latitude(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.isShowDMS())
        {
            double[] dms = angle.toDMS();
            return String.format("%02.0f%02.0f%04.1f%s", Math.abs(dms[0]), dms[1], dms[2], dms[0] < 0 ? "S" : "N");
        }
        else
        {
            return super.latitude(angle);
        }
    }

    /**
     * Format a longitude value. If this format is configured to show degrees, minutes, and seconds then the returned
     * string will use the format "DDDMMSS.S".
     *
     * @param angle the angle to format.
     *
     * @return a string containing the formatted angle.
     *
     * @throws IllegalArgumentException if the angle is null.
     */
    @Override
    public String longitude(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.isShowDMS())
        {
            double[] dms = angle.toDMS();
            return String.format("%03.0f%02.0f%04.1f%s", Math.abs(dms[0]), dms[1], dms[2], dms[0] < 0 ? "W" : "E");
        }
        else
        {
            return super.longitude(angle);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String latLon(LatLon latlon)
    {
        if (latlon == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.latitude(latlon.getLatitude()) + this.longitude(latlon.getLongitude());
    }

    /** {@inheritDoc} */
    @Override
    public void setAltitudeUnits(String altitudeUnits)
    {
        super.setAltitudeUnits(altitudeUnits);

        // Convert the altitude symbol to upper case, as per MIL-STD-2525C section 5.5.2.5.2 (pg. 41).
        this.altitudeUnitsSymbol = this.altitudeUnitsSymbol.toUpperCase();
    }

    /** {@inheritDoc} */
    @Override
    protected void setDefaultLabels()
    {
        this.setLabel(LABEL_LATITUDE, "");
        this.setLabel(LABEL_LONGITUDE, "");
        this.setLabel(LABEL_EYE_ALTITUDE, "");
    }
}
