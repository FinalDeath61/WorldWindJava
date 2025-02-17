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

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.OGLStackHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dcollins
 * @version $Id: AnnotationNullLayout.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class AnnotationNullLayout extends AbstractAnnotationLayout
{
    protected java.util.Map<Annotation, Object> constraintMap;

    public AnnotationNullLayout()
    {
        this.constraintMap = new java.util.HashMap<Annotation, Object>();
    }

    public Object getConstraint(Annotation annotation)
    {
        if (annotation == null)
        {
            String message = Logging.getMessage("nullValue.AnnotationIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        return this.constraintMap.get(annotation);
    }

    public void setConstraint(Annotation annotation, Object constraint)
    {
        if (annotation == null)
        {
            String message = Logging.getMessage("nullValue.AnnotationIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.constraintMap.put(annotation, constraint);
    }

    public java.awt.Dimension getPreferredSize(DrawContext dc, Iterable<? extends Annotation> annotations)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (annotations == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        // Start with an empty bounding rectangle in the lower left hand corner.
        java.awt.Rectangle annotationBounds = new java.awt.Rectangle();

        for (Annotation annotation : annotations)
        {
            java.awt.Rectangle b = this.getAnnotationBounds(dc, annotation);
            if (b != null)
            {
                annotationBounds = annotationBounds.union(b);
            }
        }

        return annotationBounds.getSize();
    }

    public void drawAnnotations(DrawContext dc, java.awt.Rectangle bounds,
        Iterable<? extends Annotation> annotations, double opacity, Position pickPosition)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (bounds == null)
        {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (annotations == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler stackHandler = new OGLStackHandler();

        for (Annotation annotation : annotations)
        {
            java.awt.Rectangle annotationBounds = this.getAnnotationBounds(dc, annotation);
            annotationBounds = this.adjustAnnotationBounds(dc, bounds, annotation, annotationBounds);

            stackHandler.pushModelview(gl);
            gl.glTranslated(bounds.getMinX(), bounds.getMinY(), 0);
            gl.glTranslated(annotationBounds.getMinX(), annotationBounds.getMinY(), 0);

            this.drawAnnotation(dc, annotation, annotationBounds.width, annotationBounds.height, opacity, pickPosition);

            stackHandler.pop(gl);
        }
    }

    protected java.awt.Rectangle getAnnotationBounds(DrawContext dc, Annotation annotation)
    {
        java.awt.Dimension size = this.getAnnotationSize(dc, annotation);
        if (size == null)
            return null;

        java.awt.Point offset = annotation.getAttributes().getDrawOffset();
        if (offset == null)
            offset = new java.awt.Point();

        return new java.awt.Rectangle(offset, size);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected java.awt.Rectangle adjustAnnotationBounds(DrawContext dc, java.awt.Rectangle parentBounds,
        Annotation annotation, java.awt.Rectangle bounds)
    {
        int x = bounds.x;
        int y = bounds.y;

        Object constraint = this.getConstraint(annotation);

        if (constraint == AVKey.WEST)
        {
            y += parentBounds.height / 2 - bounds.height / 2;
        }
        else if (constraint == AVKey.NORTHWEST)
        {
            y += parentBounds.height - bounds.height;
        }
        else if (constraint == AVKey.NORTH)
        {
            x += parentBounds.width / 2 - bounds.width / 2;
            y += parentBounds.height - bounds.height;
        }
        else if (constraint == AVKey.NORTHEAST)
        {
            x += parentBounds.width - bounds.width;
            y += parentBounds.height - bounds.height;
        }
        else if (constraint == AVKey.EAST)
        {
            x += parentBounds.width - bounds.width;
            y += parentBounds.height / 2 - bounds.height / 2;
        }
        else if (constraint == AVKey.SOUTHEAST)
        {
            x += parentBounds.width - bounds.width;
        }
        else if (constraint == AVKey.SOUTH)
        {
            x += parentBounds.width / 2 - bounds.width / 2;
        }
        else if (constraint == AVKey.CENTER)
        {
            x += parentBounds.width / 2 - bounds.width / 2;
            y += parentBounds.height / 2 - bounds.height / 2;
        }
        else // Default to anchoring in the south west corner.
        {
        }

        return new java.awt.Rectangle(x, y, bounds.width, bounds.height);
    }
}
