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

package gov.nasa.worldwind.symbology.milstd2525.graphics.areas;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.symbology.BasicTacticalSymbolAttributes;
import gov.nasa.worldwind.symbology.SymbologyConstants;
import gov.nasa.worldwind.symbology.TacticalSymbol;
import gov.nasa.worldwind.symbology.TacticalSymbolAttributes;
import gov.nasa.worldwind.symbology.milstd2525.AbstractMilStd2525TacticalGraphic;
import gov.nasa.worldwind.symbology.milstd2525.graphics.TacGrpSidc;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of the Limited Access Area graphic (2.X.2.1.3.10). This graphic draws a symbol inside of a pentagon.
 * The graphic requires at least one control point, which specifies the position of the vertex of the pentagon. If a
 * second control point is provided, then a line will be drawn from the vertex of the pentagon to this point. Note that
 * the {@link SymbologyConstants#SYMBOL_INDICATOR} modifier must be set or the pentagon will not be rendered.
 *
 * @author pabercrombie
 * @version $Id: LimitedAccessArea.java 555 2012-04-25 18:59:29Z pabercrombie $
 */
@Slf4j
public class LimitedAccessArea extends AbstractMilStd2525TacticalGraphic
{
    protected TacticalSymbol symbol;
    protected Path path;

    protected Position symbolPosition;
    protected Position attachmentPosition;

    /** Altitude mode for this graphic. */
    protected int altitudeMode = WorldWind.CLAMP_TO_GROUND;

    /** Attributes applied to the symbol. */
    protected TacticalSymbolAttributes symbolAttributes;

    /**
     * Indicates the graphics supported by this class.
     *
     * @return List of masked SIDC strings that identify graphics that this class supports.
     */
    public static List<String> getSupportedGraphics()
    {
        return Arrays.asList(TacGrpSidc.C2GM_GNL_ARS_LAARA);
    }

    public LimitedAccessArea(String symbolCode)
    {
        super(symbolCode);
    }

    /** {@inheritDoc} */
    @Override
    public void setModifier(String modifier, Object value)
    {
        if (SymbologyConstants.SYMBOL_INDICATOR.equals(modifier) && value instanceof String)
        {
            this.setSymbol((String) value);
        }
        else
        {
            super.setModifier(modifier, value);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object getModifier(String modifier)
    {
        if (SymbologyConstants.SYMBOL_INDICATOR.equals(modifier))
        {
            return this.getSymbol();
        }
        else
        {
            return super.getModifier(modifier);
        }
    }

    /**
     * Indicates a symbol drawn at the center of the range fan.
     *
     * @return The symbol drawn at the center of the range fan. May be null.
     */
    public String getSymbol()
    {
        return this.symbol != null ? this.symbol.getIdentifier() : null;
    }

    /**
     * Specifies a symbol to draw at the center of the range fan. Equivalent to setting the {@link
     * SymbologyConstants#SYMBOL_INDICATOR} modifier. The symbol's position will be changed to match the range fan
     * center position.
     *
     * @param sidc Identifier for a MIL-STD-2525C symbol to draw at the center of the range fan. May be null to indicate
     *             that no symbol is drawn.
     */
    public void setSymbol(String sidc)
    {
        if (sidc != null)
        {
            if (this.symbolAttributes == null)
                this.symbolAttributes = new BasicTacticalSymbolAttributes();

            this.symbol = this.createSymbol(sidc);
        }
        else
        {
            // Null value indicates no symbol.
            this.symbol = null;
            this.symbolAttributes = null;
        }
        this.onModifierChanged();
    }

    /**
     * Indicates this graphic's altitude mode. See {@link #setAltitudeMode(int)} for a description of the valid altitude
     * modes.
     *
     * @return this graphic's altitude mode.
     */
    public int getAltitudeMode()
    {
        return this.altitudeMode;
    }

    /**
     * Specifies this graphic's altitude mode. Altitude mode defines how the altitude component of this graphic's
     * position is interpreted. Recognized modes are: <ul> <li>WorldWind.CLAMP_TO_GROUND -- this graphic is placed on
     * the terrain at the latitude and longitude of its position.</li> <li>WorldWind.RELATIVE_TO_GROUND -- this graphic
     * is placed above the terrain at the latitude and longitude of its position and the distance specified by its
     * elevation.</li> <li>WorldWind.ABSOLUTE -- this graphic is placed at its specified position.</li> </ul>
     * <p>
     * This symbol assumes the altitude mode WorldWind.ABSOLUTE if the specified mode is not recognized.
     *
     * @param altitudeMode this graphic new altitude mode.
     */
    public void setAltitudeMode(int altitudeMode)
    {
        this.altitudeMode = altitudeMode;

        if (this.symbol != null)
            this.symbol.setAltitudeMode(altitudeMode);
        if (this.path != null)
            this.path.setAltitudeMode(altitudeMode);
    }

    /** {@inheritDoc} */
    @Override
    protected void doRenderGraphic(DrawContext dc)
    {
        if (this.symbol != null)
            this.symbol.render(dc);

        if (this.path != null)
            this.path.render(dc);
    }

    /** {@inheritDoc} */
    @Override
    protected void applyDelegateOwner(Object owner)
    {
        if (this.symbol != null)
            this.symbol.setDelegateOwner(owner);
        if (this.path != null)
            this.path.setDelegateOwner(owner);
    }

    /** {@inheritDoc} */
    public Iterable<? extends Position> getPositions()
    {
        List<Position> positions = new ArrayList<Position>();
        if (this.symbolPosition != null)
            positions.add(this.symbolPosition);
        if (this.attachmentPosition != null)
            positions.add(this.attachmentPosition);

        return positions;
    }

    /** {@inheritDoc} */
    public void setPositions(Iterable<? extends Position> positions)
    {
        if (positions == null)
        {
            String message = Logging.getMessage("nullValue.PositionsListIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        Iterator<? extends Position> iterator = positions.iterator();
        if (iterator.hasNext())
        {
            this.symbolPosition = iterator.next();
        }
        else
        {
            String message = Logging.getMessage("generic.InsufficientPositions");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (iterator.hasNext())
            this.attachmentPosition = iterator.next();

        if (this.symbol != null)
        {
            this.symbol.setPosition(this.symbolPosition);
        }

        if (this.attachmentPosition != null)
        {
            if (this.path == null)
                this.path = this.createPath();

            this.path.setPositions(Arrays.asList(this.symbolPosition, this.attachmentPosition));
        }
    }

    /** {@inheritDoc} */
    public Position getReferencePosition()
    {
        return this.symbolPosition;
    }

    /** {@inheritDoc} Overridden to update symbol attributes. */
    @Override
    protected void determineActiveAttributes()
    {
        super.determineActiveAttributes();

        // Apply active attributes to the symbol.
        if (this.symbolAttributes != null)
        {
            ShapeAttributes activeAttributes = this.getActiveShapeAttributes();
            this.symbolAttributes.setOpacity(activeAttributes.getInteriorOpacity());
            this.symbolAttributes.setScale(this.activeOverrides.getScale());
        }
    }

    protected TacticalSymbol createSymbol(String sidc)
    {
        Position symbolPosition = this.getReferencePosition();
        TacticalSymbol symbol = new LimitedAccessSymbol(sidc,
            symbolPosition != null ? symbolPosition : Position.ZERO);
        symbol.setDelegateOwner(this);
        symbol.setAttributes(this.symbolAttributes);
        symbol.setAltitudeMode(this.getAltitudeMode());
        return symbol;
    }

    /**
     * Create and configure the Path used to render this graphic.
     *
     * @return New path configured with defaults appropriate for this type of graphic.
     */
    protected Path createPath()
    {
        Path path = new Path();
        path.setFollowTerrain(true);
        path.setPathType(AVKey.LINEAR);
        path.setAltitudeMode(this.getAltitudeMode());
        path.setDelegateOwner(this.getActiveDelegateOwner());
        path.setAttributes(this.getActiveShapeAttributes());
        return path;
    }
}
