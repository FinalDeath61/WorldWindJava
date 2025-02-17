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

import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;

/**
 * @author tag
 * @version $Id: ScreenCreditImage.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class ScreenCreditImage extends ScreenImage implements ScreenCredit
{
    private String name;
    private String link;
    private Rectangle viewport;

    public ScreenCreditImage(String name, Object imageSource)
    {
        if (imageSource == null)
        {
            String msg = Logging.getMessage("nullValue.ImageSource");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.name = name;
        this.setImageSource(imageSource);
    }

    public void setViewport(Rectangle viewport)
    {
        if (viewport == null)
        {
            String msg = Logging.getMessage("nullValue.ViewportIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.viewport = viewport;
        this.setScreenLocation(new Point(viewport.x, viewport.y));
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Rectangle getViewport()
    {
        return this.viewport;
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    public String getLink()
    {
        return this.link;
    }

    @Override
    public int getImageWidth(DrawContext dc)
    {
        return (int) this.getViewport().getWidth();
    }

    @Override
    public int getImageHeight(DrawContext dc)
    {
        return (int) this.getViewport().getHeight();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ScreenCreditImage that = (ScreenCreditImage) o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        return name != null ? name.hashCode() : 0;
    }
}
