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
package gov.nasa.worldwind.formats.nmea;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.tracks.TrackPoint;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tag
 * @version $Id: NmeaTrackPoint.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class NmeaTrackPoint implements TrackPoint
{
    private double latitude;
    private double longitude;
    private double altitude;
    private double geoidHeight;
    private String time;

    /**
     * @param words The track point words to parse.
     * @throws IllegalArgumentException if <code>words</code> is null or has length less than 1
     */
    public NmeaTrackPoint(String[] words)
    {
        if (words == null)
        {
            String msg = Logging.getMessage("nullValue.ArrayIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (words.length < 1)
        {
            String msg = Logging.getMessage("generic.ArrayInvalidLength", words.length);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (words[0].equalsIgnoreCase("GPGGA"))
            this.doGGA(words);
        else if (words[0].equalsIgnoreCase("GPRMC"))
            this.doRMC(words);
    }

    /**
     * @param words
     * @throws IllegalArgumentException if <code>words</code> is null or has length less than 6
     */
    private void doGGA(String[] words)
    {
        // words won't be null, but it could be the wrong length
        if (words.length < 6)
        {
            String msg = Logging.getMessage("generic.ArrayInvalidLength", words.length);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.time = words[1];
        this.latitude = this.parseLatitude(words[2], words[3]);
        this.longitude = this.parseLongitude(words[4], words[5]);
        if (words.length >= 11)
            this.altitude = this.parseElevation(words[9], words[10]);
        if (words.length >= 13)
            this.geoidHeight = this.parseElevation(words[11], words[12]);
    }

    private void doRMC(String[] words)
    {
    }

    private double parseLatitude(String angle, String direction)
    {
        if (angle.length() == 0)
            return 0;

        double minutes = angle.length() > 2 ? Double.parseDouble(angle.substring(2, angle.length())) : 0d;
        double degrees = Double.parseDouble(angle.substring(0, 2)) + minutes / 60d;

        return direction.equalsIgnoreCase("S") ? -degrees : degrees;
    }

    private double parseLongitude(String angle, String direction)
    {
        if (angle.length() == 0)
            return 0;

        double minutes = angle.length() > 3 ? Double.parseDouble(angle.substring(3, angle.length())) : 0d;
        double degrees = Double.parseDouble(angle.substring(0, 3)) + minutes / 60d;

        return direction.equalsIgnoreCase("W") ? -degrees : degrees;
    }

    private double parseElevation(String height, String units)
    {
        if (height.length() == 0)
            return 0;

        return Double.parseDouble(height) * unitsToMeters(units);
    }

    private double unitsToMeters(String units)
    {
        double f;

        if (units.equals("M")) // meters
            f = 1d;
        else if (units.equals("f")) // feet
            f = 3.2808399;
        else if (units.equals("F")) // fathoms
            f = 0.5468066528;
        else
            f = 1d;

        return f;
    }

    public double getLatitude()
    {
        return latitude;
    }

    /**
     * @param latitude The new latitude.
     * @throws IllegalArgumentException if <code>latitude</code> is less than -90 or greater than 90
     */
    public void setLatitude(double latitude)
    {
        if (latitude > 90 || latitude < -90)
        {
            String msg = Logging.getMessage("generic.AngleOutOfRange", latitude);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.latitude = latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    /**
     * @param longitude The new longitude.
     * @throws IllegalArgumentException if <code>longitude</code> is less than -180 or greater than 180
     */
    public void setLongitude(double longitude)
    {
        if (longitude > 180 || longitude < -180)
        {
            String msg = Logging.getMessage("generic.AngleOutOfRange", longitude);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.longitude = longitude;
    }

    public Position getPosition()
    {
        return Position.fromDegrees(this.latitude, this.longitude, this.altitude);
    }

    public void setPosition(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.latitude = position.getLatitude().getDegrees();
        this.longitude = position.getLongitude().getDegrees();
        this.altitude = position.getElevation();
    }

    public double getElevation()
    {
        return this.altitude + this.geoidHeight;
    }

    public void setElevation(double elevation)
    {
        this.altitude = elevation;
        this.geoidHeight = 0;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    @Override
    public String toString()
    {
        return String.format("(%10.8f\u00B0, %11.8f\u00B0, %10.4g m, %10.4g m, %s)", this.latitude, this.longitude,
            this.altitude, this.geoidHeight, this.time);
    }
}
