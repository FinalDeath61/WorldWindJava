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

import com.jogamp.opengl.GL;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;

/**
 * A <code>{@link gov.nasa.worldwind.render.GlobeBalloon}</code> that displays HTML, JavaScript, and Flash content using
 * the system's native browser, and who's origin is located at a position on the <code>Globe</code>.
 *
 * @author pabercrombie
 * @version $Id: GlobeBrowserBalloon.java 2272 2014-08-25 23:24:45Z tgaskins $
 * @see gov.nasa.worldwind.render.AbstractBrowserBalloon
 * @deprecated 
 */
@Slf4j
@Deprecated
public class GlobeBrowserBalloon extends AbstractBrowserBalloon implements GlobeBalloon
{
    protected class OrderedGlobeBrowserBalloon extends OrderedBrowserBalloon
    {
        /** The model-coordinate point corresponding to this balloon's position. May be <code>null</code>. */
        protected Vec4 placePoint;
        /**
         * The projection of this balloon's <code>placePoint</code> in the viewport (on the screen). May be
         * <code>null</code>.
         */
        protected Vec4 screenPlacePoint;
    }

    /**
     * Indicates this balloon's geographic position. The position's altitude is interpreted relative to this balloon's
     * <code>altitudeMode</code>. Initialized to a non-<code>null</code> value at construction.
     */
    protected Position position;
    /**
     * Indicates how this balloon's altitude is interpreted. One of <code>WorldWind.ABSOLUTE</code>,
     * <code>WorldWind.RELATIVE_TO_GROUND</code>, or <code>WorldWind.CLAMP_TO_GROUND</code>. If the altitude mode is 0
     * or an unrecognized code, this balloon assumes an altitude mode of <code>WorldWind.ABSOLUTE</code>. Initially 0.
     */
    protected int altitudeMode;

    /**
     * Constructs a new <code>GlobeBrowserBalloon</code> with the specified text content and position.
     *
     * @param text     the balloon's initial text content.
     * @param position the balloon's initial position.
     *
     * @throws IllegalArgumentException if either <code>text</code> or <code>position</code> are <code>null</code>.
     */
    public GlobeBrowserBalloon(String text, Position position)
    {
        super(text);

        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.position = position;
    }

    /** {@inheritDoc} */
    public Position getPosition()
    {
        return this.position;
    }

    /** {@inheritDoc} */
    public void setPosition(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.position = position;
    }

    /** {@inheritDoc} */
    public int getAltitudeMode()
    {
        return altitudeMode;
    }

    /** {@inheritDoc} */
    public void setAltitudeMode(int altitudeMode)
    {
        this.altitudeMode = altitudeMode;
    }

    @Override
    protected OrderedBrowserBalloon createOrderedRenderable()
    {
        return new OrderedGlobeBrowserBalloon();
    }

    /**
     * Computes and stores this balloon's model and screen coordinates. This assigns balloon coordinate properties as
     * follows:
     * <ul> <li><code>placePoint</code> - this balloon's model-coordinate point, according to its altitude mode.</li>
     * <li><code>screenPlacePoint</code> - screen-space projection of the <code>placePoint</code>.</li>
     * <li><code>screenOffset</code> - the balloon frame's screen-coordinate offset from this balloon's
     * <code>screenPlacePoint</code>.</li> <li><code>screenRect</code> - the balloon frame's screen-coordinate
     * rectangle.</li> <li><code>screenExtent</code> - this balloon's screen-coordinate bounding rectangle.</li>
     * <li><code>screenPickExtent</code> - this balloon's screen-coordinate bounding rectangle, including area covered
     * by the balloon's pickable outline.</li> <li><code>webViewRect</code> - the WebView's screen-coordinate content
     * frame.</li> <li><code>eyeDistance</code> - always 0.</li></ul>
     *
     * @param dc the current draw context.
     */
    protected void computeBalloonPoints(DrawContext dc, OrderedBrowserBalloon obb)
    {
        OrderedGlobeBrowserBalloon ogpm = (OrderedGlobeBrowserBalloon) obb;

        ogpm.placePoint = null;
        ogpm.screenPlacePoint = null;
        this.screenOffset = null;
        obb.screenRect = null;
        obb.screenExtent = null;
        obb.screenPickExtent = null;
        obb.webViewRect = null;
        obb.eyeDistance = 0;

        if (this.altitudeMode == WorldWind.CLAMP_TO_GROUND || dc.is2DGlobe())
        {
            ogpm.placePoint = dc.computeTerrainPoint(
                this.position.getLatitude(), this.position.getLongitude(), 0);
        }
        else if (this.altitudeMode == WorldWind.RELATIVE_TO_GROUND)
        {
            ogpm.placePoint = dc.computeTerrainPoint(
                this.position.getLatitude(), this.position.getLongitude(), this.position.getAltitude());
        }
        else // Default to ABSOLUTE
        {
            double height = this.position.getElevation() * dc.getVerticalExaggeration();
            ogpm.placePoint = dc.getGlobe().computePointFromPosition(
                this.position.getLatitude(), this.position.getLongitude(), height);
        }

        // Exit immediately if the place point is null. In this case we cannot compute the data that depends on the
        // place point: screen place point, screen rectangle, WebView rectangle, and eye distance.
        if (ogpm.placePoint == null)
            return;

        BalloonAttributes activeAttrs = this.getActiveAttributes();
        Dimension size = this.computeSize(dc, activeAttrs);

        // Compute the screen place point as the projection of the place point into screen coordinates.
        ogpm.screenPlacePoint = dc.getView().project(ogpm.placePoint);
        // Cache the screen offset computed from the active attributes.
        this.screenOffset = this.computeOffset(dc, activeAttrs, size.width, size.height);
        // Compute the screen rectangle given the screen projection of the place point, the current screen offset, and
        // the current screen size. Note: The screen offset denotes how to place the screen reference point relative to
        // the frame. For example, an offset of (-10, -10) in pixels places the reference point below and to the left
        // of the frame. Since the screen reference point is fixed, the frame appears to move relative to the reference
        // point.
        obb.screenRect = new Rectangle((int) (ogpm.screenPlacePoint.x - this.screenOffset.x),
            (int) (ogpm.screenPlacePoint.y - this.screenOffset.y),
            size.width, size.height);
        // Compute the screen extent as the rectangle containing the balloon's screen rectangle and its place point.
        obb.screenExtent = new Rectangle(obb.screenRect);
        obb.screenExtent.add(ogpm.screenPlacePoint.x, ogpm.screenPlacePoint.y);
        // Compute the pickable screen extent as the screen extent, plus the width of the balloon's pickable outline.
        // This extent is used during picking to ensure that the balloon's outline is pickable when it exceeds the
        // balloon's screen extent.
        obb.screenPickExtent = this.computeFramePickRect(obb.screenExtent);
        // Compute the WebView rectangle as an inset of the screen rectangle, given the current inset values.
        obb.webViewRect = this.computeWebViewRectForFrameRect(activeAttrs, obb.screenRect);
        // Compute the eye distance as the distance from the place point to the View's eye point.
        obb.eyeDistance = this.isAlwaysOnTop() ? 0 : dc.getView().getEyePoint().distanceTo3(ogpm.placePoint);
    }

    /** {@inheritDoc} */
    protected void setupDepthTest(DrawContext dc, OrderedBrowserBalloon obb)
    {
        OrderedGlobeBrowserBalloon ogpm = (OrderedGlobeBrowserBalloon) obb;

        GL gl = dc.getGL();

        if (!this.isAlwaysOnTop() && ogpm.screenPlacePoint != null
            && dc.getView().getEyePosition().getElevation() < (dc.getGlobe().getMaxElevation()
            * dc.getVerticalExaggeration()))
        {
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthMask(false);

            // Adjust depth of image to bring it slightly forward
            double depth = ogpm.screenPlacePoint.z - (8d * 0.00048875809d);
            depth = depth < 0d ? 0d : (depth > 1d ? 1d : depth);
            gl.glDepthFunc(GL.GL_LESS);
            gl.glDepthRange(depth, depth);
        }
        else
        {
            gl.glDisable(GL.GL_DEPTH_TEST);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return <code>false</code> if the balloon's position is either behind the <code>View's</code> near
     * clipping plane or in front of the <code>View's</code> far clipping plane. Otherwise this delegates to the super
     * class' behavior.
     */
    @Override
    protected boolean intersectsFrustum(DrawContext dc, OrderedBrowserBalloon obb)
    {
        OrderedGlobeBrowserBalloon ogpm = (OrderedGlobeBrowserBalloon) obb;

        View view = dc.getView();

        // Test the balloon against the near and far clipping planes.
        Frustum frustum = view.getFrustumInModelCoordinates();
        //noinspection SimplifiableIfStatement
        if (ogpm.placePoint != null
            && (frustum.getNear().distanceTo(ogpm.placePoint) < 0
            || frustum.getFar().distanceTo(ogpm.placePoint) < 0))
        {
            return false;
        }

        return super.intersectsFrustum(dc, obb);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to use this balloon's position as the picked object's position.
     */
    @Override
    protected PickedObject createPickedObject(DrawContext dc, Color pickColor)
    {
        PickedObject po = super.createPickedObject(dc, pickColor);
        // Set the picked object's position to the balloon's position.
        po.setPosition(this.position);

        return po;
    }
}
