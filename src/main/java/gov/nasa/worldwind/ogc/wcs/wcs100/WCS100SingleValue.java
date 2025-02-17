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

package gov.nasa.worldwind.ogc.wcs.wcs100;

import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.xml.AbstractXMLEventParser;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tag
 * @version $Id: WCS100SingleValue.java 2066 2014-06-20 20:41:46Z tgaskins $
 */
@Slf4j
public class WCS100SingleValue extends AbstractXMLEventParser
{
    public WCS100SingleValue(String namespaceURI)
    {
        super(namespaceURI);
    }

    public String getType()
    {
        return (String) this.getField("type");
    }

    public String getSemantic()
    {
        return (String) this.getField("semantic");
    }

    public String getSingleValueString()
    {
        return (String) this.getField("CharactersContent");
    }

    public Double getSingleValue()
    {
        if (this.getSingleValueString() == null)
            return null;

        try
        {
            return Double.parseDouble(this.getSingleValueString());
        }
        catch (NumberFormatException e)
        {
            String message = Logging.getMessage("generic.NumberFormatException");
            log.warn(message, e);
            return null;
        }
    }
}
