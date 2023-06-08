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

package gov.nasa.worldwind.render.markers;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tag
 * @version $Id: BasicMarkerAttributes.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class BasicMarkerAttributes implements MarkerAttributes
{
    private Material material = Material.WHITE;
    private Material headingMaterial = Material.RED;
    protected double headingScale = 3;
    private String shapeType = BasicMarkerShape.SPHERE;
    private double opacity = 1d;
    private double markerPixels = 8d;
    private double minMarkerSize = 3d;
    private double maxMarkerSize = Double.MAX_VALUE;

    public BasicMarkerAttributes()
    {
    }

    public BasicMarkerAttributes(Material material, String shapeType, double opacity)
    {
        if (material == null)
        {
            String message = Logging.getMessage("nullValue.MaterialIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.Shape");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.material = material;
        this.shapeType = shapeType;
        this.opacity = opacity;
    }

    public BasicMarkerAttributes(Material material, String shapeType, double opacity, double markerPixels,
        double minMarkerSize)
    {
        if (material == null)
        {
            String message = Logging.getMessage("nullValue.MaterialIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.Shape");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (opacity < 0)
        {
            String message = Logging.getMessage("generic.OpacityOutOfRange", opacity);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (markerPixels < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", markerPixels);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (minMarkerSize < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", minMarkerSize);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.material = material;
        this.shapeType = shapeType;
        this.opacity = opacity;
        this.markerPixels = markerPixels;
        this.minMarkerSize = minMarkerSize;
    }

    public BasicMarkerAttributes(Material material, String shapeType, double opacity, double markerPixels,
        double minMarkerSize, double maxMarkerSize)
    {
        if (material == null)
        {
            String message = Logging.getMessage("nullValue.MaterialIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.Shape");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (opacity < 0)
        {
            String message = Logging.getMessage("generic.OpacityOutOfRange", opacity);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (markerPixels < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", markerPixels);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (minMarkerSize < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", minMarkerSize);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (maxMarkerSize < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", maxMarkerSize);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.material = material;
        this.shapeType = shapeType;
        this.opacity = opacity;
        this.markerPixels = markerPixels;
        this.minMarkerSize = minMarkerSize;
        this.maxMarkerSize = maxMarkerSize;
    }

    public BasicMarkerAttributes(BasicMarkerAttributes that)
    {
        if (that == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.material = that.material;
        this.headingMaterial = that.headingMaterial;
        this.headingScale = that.headingScale;
        this.shapeType = that.shapeType;
        this.opacity = that.opacity;
        this.markerPixels = that.markerPixels;
        this.minMarkerSize = that.minMarkerSize;
        this.maxMarkerSize = that.maxMarkerSize;
    }

    public Material getMaterial()
    {
        return material;
    }

    public void setMaterial(Material material)
    {
        if (material == null)
        {
            String message = Logging.getMessage("nullValue.MaterialIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.material = material;
    }

    public Material getHeadingMaterial()
    {
        return headingMaterial;
    }

    public void setHeadingMaterial(Material headingMaterial)
    {
        if (material == null)
        {
            String message = Logging.getMessage("nullValue.MaterialIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.headingMaterial = headingMaterial;
    }

    public double getHeadingScale()
    {
        return headingScale;
    }

    public void setHeadingScale(double headingScale)
    {
        if (headingScale < 0)
        {
            String message = Logging.getMessage("generic.ScaleOutOfRange", headingScale);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.headingScale = headingScale;
    }

    public String getShapeType()
    {
        return shapeType;
    }

    public void setShapeType(String shapeType)
    {
        if (shapeType == null)
        {
            String message = Logging.getMessage("nullValue.ShapeType");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.shapeType = shapeType;
    }

    public MarkerShape getShape(DrawContext dc)
    {
        MarkerShape shape = (MarkerShape) dc.getValue(this.shapeType);

        if (shape == null)
        {
            shape = BasicMarkerShape.createShapeInstance(this.shapeType);
            dc.setValue(this.shapeType, shape);
        }

        return shape;
    }

    public double getOpacity()
    {
        return opacity;
    }

    public void setOpacity(double opacity)
    {
        if (opacity < 0)
        {
            String message = Logging.getMessage("generic.OpacityOutOfRange", opacity);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.opacity = opacity;
    }

    public double getMarkerPixels()
    {
        return markerPixels;
    }

    public void setMarkerPixels(double markerPixels)
    {
        if (markerPixels < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", markerPixels);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.markerPixels = markerPixels;
    }

    public double getMinMarkerSize()
    {
        return minMarkerSize;
    }

    public void setMinMarkerSize(double minMarkerSize)
    {
        if (minMarkerSize < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", minMarkerSize);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.minMarkerSize = minMarkerSize;
    }

    public double getMaxMarkerSize()
    {
        return maxMarkerSize;
    }

    public void setMaxMarkerSize(double markerSize)
    {
        if (markerSize < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", markerSize);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.maxMarkerSize = markerSize;
    }

    public void apply(DrawContext dc)
    {
        if (!dc.isPickingMode() && this.material != null)
        {
            GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

            if (this.opacity < 1)
                this.material.apply(gl, GL2.GL_FRONT, (float) this.opacity);
            else
                this.material.apply(gl, GL2.GL_FRONT);
        }
    }
}
