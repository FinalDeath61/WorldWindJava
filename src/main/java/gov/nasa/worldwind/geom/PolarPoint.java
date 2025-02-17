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
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a point in space defined by a latitude, longitude and distance from the origin.
 * <p>
 * Instances of <code>PolarPoint</code> are immutable.
 *
 * @author Tom Gaskins
 * @version $Id: PolarPoint.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class PolarPoint
{
    public static final PolarPoint ZERO = new PolarPoint(Angle.ZERO, Angle.ZERO, 0d);

    private final Angle latitude;
    private final Angle longitude;
    private final double radius;

    /**
     * Obtains a <code>PolarPoint</code> from radians and a radius.
     *
     * @param latitude  the latitude in radians
     * @param longitude the longitude in radians
     * @param radius    the distance form the center
     * @return a new <code>PolarPoint</code>
     */
    public static PolarPoint fromRadians(double latitude, double longitude, double radius)
    {
        return new PolarPoint(Angle.fromRadians(latitude), Angle.fromRadians(longitude), radius);
    }

    /**
     * Obtains a <code>PolarPoint</code> from degrees and a radius.
     *
     * @param latitude  the latitude in degrees
     * @param longitude the longitude in degrees
     * @param radius    the distance form the center
     * @return a new <code>PolarPoint</code>
     */
    public static PolarPoint fromDegrees(double latitude, double longitude, double radius)
    {
        return new PolarPoint(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude), radius);
    }

    /**
     * Obtains a <code>PolarPoint</code> from a cartesian point.
     *
     * @param cartesianPoint the point to convert
     * @return the cartesian point expressed as a polar point
     * @throws IllegalArgumentException if <code>cartesianPoint</code> is null
     */
    public static PolarPoint fromCartesian(Vec4 cartesianPoint)
    {
        if (cartesianPoint == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        return PolarPoint.fromCartesian(cartesianPoint.x, cartesianPoint.y, cartesianPoint.z);
    }

    /**
     * Obtains a <code>PolarPoint</code> from cartesian coordinates.
     *
     * @param x the x coordinate of the cartesian point
     * @param y the y coordinate of the cartesian point
     * @param z the z coordinate of the cartesian point
     * @return a polar point located at (x,y,z) in cartesian space
     */
    public static PolarPoint fromCartesian(double x, double y, double z)
    {
        double radius = Math.sqrt(x * x + y * y + z * z);
        double latRads = Math.atan2(y, Math.sqrt(x * x + z * z));
        double lonRads = Math.atan2(x, z);
        return PolarPoint.fromRadians(latRads, lonRads, radius);
    }

    /**
     * Obtains a <code>PolarPoint</code> from two <code>angles</code> and a radius.
     *
     * @param latitude  the latitude
     * @param longitude the longitude
     * @param radius    the distance from the center
     * @throws IllegalArgumentException if <code>latitude</code> or <code>longitude</code> is null
     */
    public PolarPoint(Angle latitude, Angle longitude, double radius)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    /**
     * Obtains the latitude of this polar point
     *
     * @return this polar point's latitude
     */
    public final Angle getLatitude()
    {
        return this.latitude;
    }

    /**
     * Obtains the longitude of this polar point
     *
     * @return this polar point's longitude
     */
    public final Angle getLongitude()
    {
        return this.longitude;
    }

    /**
     * Obtains the radius of this polar point
     *
     * @return the distance from this polar point to its origin
     */
    public final double getRadius()
    {
        return radius;
    }

    /**
     * Obtains a cartesian point equivalent to this <code>PolarPoint</code>, except in cartesian space.
     *
     * @return this polar point in cartesian coordinates
     */
    public final Vec4 toCartesian()
    {
        return toCartesian(this.latitude, this.longitude, this.radius);
    }

    /**
     * Obtains a cartesian point from a given latitude, longitude and distance from center. This method is equivalent
     * to, but may perform faster than <code>Vec4 p = new PolarPoint(latitude, longitude, radius).toCartesian()</code>
     *
     * @param latitude  the latitude
     * @param longitude the longitude
     * @param radius    the distance from the origin
     * @return a cartesian point from two angles and a radius
     * @throws IllegalArgumentException if <code>latitude</code> or <code>longitude</code> is null
     */
    public static Vec4 toCartesian(Angle latitude, Angle longitude, double radius)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        double x = radius * longitude.sin() * latitude.cos();
        double y = radius * latitude.sin();
        double z = radius * longitude.cos() * latitude.cos();
        return new Vec4(x, y, z);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final gov.nasa.worldwind.geom.PolarPoint that = (gov.nasa.worldwind.geom.PolarPoint) o;

        if (Double.compare(that.radius, radius) != 0)
            return false;
        if (!latitude.equals(that.latitude))
            return false;
        //noinspection RedundantIfStatement
        if (!longitude.equals(that.longitude))
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        result = latitude.hashCode();
        result = 29 * result + longitude.hashCode();
        temp = radius != +0.0d ? Double.doubleToLongBits(radius) : 0L;
        result = 29 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString()
    {
        return "(lat: " + this.latitude.toString() + ", lon: " + this.longitude.toString() + ", r: " + this.radius
            + ")";
    }
}
