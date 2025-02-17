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

package gov.nasa.worldwindx.examples.util;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.Balloon;
import gov.nasa.worldwind.render.BalloonAttributes;
import gov.nasa.worldwind.render.BasicBalloonAttributes;
import gov.nasa.worldwind.render.Size;
import gov.nasa.worldwind.util.AbstractResizeHotSpot;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Create a controller to resize a {@link Balloon} by dragging the mouse. This class should usually not be instantiated
 * directly. Instead, {@link BalloonController} will instantiate it when a balloon needs to be resized.
 *
 * @author pabercrombie
 * @version $Id: BalloonResizeController.java 1171 2013-02-11 21:45:02Z dcollins $
 *
 * @see BalloonController
 */
@Slf4j
public class BalloonResizeController extends AbstractResizeHotSpot
{
    protected WorldWindow wwd;
    protected Rectangle bounds;
    protected Balloon balloon;

    protected static final Dimension DEFAULT_MIN_SIZE = new Dimension(50, 50);

    /**
     * Create a resize controller.
     *
     * @param wwd     WorldWindow to interact with.
     * @param balloon Balloon to resize.
     */
    public BalloonResizeController(WorldWindow wwd, Balloon balloon)
    {
        if (wwd == null)
        {
            String message = Logging.getMessage("nullValue.WorldWindow");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (balloon == null)
        {
            String message = Logging.getMessage("nullValue.BalloonIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.wwd = wwd;
        this.balloon = balloon;

        this.wwd.addSelectListener(this);
        this.wwd.getInputHandler().addMouseMotionListener(this);
    }

    /**
     * Remove this controller as an event listener. The controller will not receive input events after this method is
     * called.
     */
    public void detach()
    {
        this.wwd.removeSelectListener(this);
        this.wwd.getInputHandler().removeMouseMotionListener(this);
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        if (e == null || e.isConsumed())
            return;
        
        PickedObjectList pickedObjects = wwd.getObjectsAtCurrentPosition();
        if (pickedObjects != null)
        {
            Rectangle rect = this.getBounds(pickedObjects.getTopPickedObject());
            if (rect != null)
            {
                this.setBounds(rect);
            }
        }

        super.mouseMoved(e);
        this.updateCursor();
    }

    /**
     * Is the controller currently resizing a balloon?
     *
     * @return True if the controller is currently resizing the balloon.
     */
    public boolean isResizing()
    {
        return this.isDragging();
    }

    /** {@inheritDoc} */
    protected Dimension getSize()
    {
        Rectangle bounds = this.getBounds();
        if (bounds != null)
            return bounds.getSize();
        else
            return null;
    }

    /**
     * Gets the bounds of the Balloon being sized.
     *
     * @return Balloon bounds in AWT coordinates, or {@code null} if no bounds have been set.
     */
    public Rectangle getBounds()
    {
        return this.bounds;
    }

    /**
     * Specifies the bounds of the Balloon to be sized.
     *
     * @param bounds Balloon bounds, in AWT coordinates.
     */
    public void setBounds(Rectangle bounds)
    {
        this.bounds = bounds;
    }

    /**
    * Update the WorldWindow's cursor to the current resize cursor.
    */
    protected void updateCursor()
    {
        if (this.wwd instanceof Component)
        {
            ((Component) this.wwd).setCursor(this.getCursor());
        }
    }

    /** {@inheritDoc} */
    protected void setSize(Dimension newSize)
    {
        Size size = Size.fromPixels(newSize.width, newSize.height);

        BalloonAttributes attributes = this.balloon.getAttributes();

        // If the balloon is using default attributes, create a new set of attributes that we can customize
        if (attributes == null)
        {
            attributes = new BasicBalloonAttributes();
            this.balloon.setAttributes(attributes);
        }

        attributes.setSize(size);

        // Clear the balloon's maximum size. The user should be able to resize the balloon to any size.
        attributes.setMaximumSize(null);

        // If the balloon also has highlight attributes, change the highlight size as well.
        BalloonAttributes highlightAttributes = this.balloon.getHighlightAttributes();
        if (highlightAttributes != null)
        {
            highlightAttributes.setSize(size);
            highlightAttributes.setMaximumSize(null);
        }
    }

    /**
     * Get the balloon bounds from a SelectEvent.
     *
     * @param pickedObject Top picked object. The bounds are expected to be attached to the to PickedObject
     *        under AVKey.BOUNDS.
     *
     * @return Bounds or {@code null} if no bounds are found in the top PickedObject.
     */
    protected Rectangle getBounds(PickedObject pickedObject)
    {
        if (pickedObject != null)
        {
            Object bounds = pickedObject.getValue(AVKey.BOUNDS);
            if (bounds instanceof Rectangle)
            {
                return (Rectangle) bounds;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    protected Point getScreenPoint()
    {
        Rectangle bounds = this.getBounds();
        if (bounds != null)
            return bounds.getLocation();
        else
            return null;
    }

    /** {@inheritDoc} */
    protected void setScreenPoint(Point newPoint)
    {
        // Do not set the screen point. The balloon is attached to a particular screen point, and we do not want to
        // change it. When the balloon is resized, the attachment point should remain constant, and the balloon should
        // move.
    }

    /** {@inheritDoc} */
    @Override
    protected Dimension getMinimumSize()
    {
        return DEFAULT_MIN_SIZE;
    }
}
