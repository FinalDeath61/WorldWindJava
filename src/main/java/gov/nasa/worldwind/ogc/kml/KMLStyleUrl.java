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

package gov.nasa.worldwind.ogc.kml;

import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tag
 * @version $Id: KMLStyleUrl.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class KMLStyleUrl extends KMLAbstractObject
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLStyleUrl(String namespaceURI)
    {
        super(namespaceURI);
    }

    /**
     * Resolves a <i>styleUrl</i> to a style selector, which is either a style or style map.
     * <p>
     * If the url refers to a remote resource and the resource has not been retrieved and cached locally, this method
     * returns null and initiates a retrieval.
     *
     * @return the style or style map referred to by the style URL.
     */
    public KMLAbstractStyleSelector resolveStyleUrl()
    {
        if (WWUtil.isEmpty(this.getCharacters()))
            return null;

        Object o = this.getRoot().resolveReference(this.getCharacters());
        return o instanceof KMLAbstractStyleSelector ? (KMLAbstractStyleSelector) o : null;
    }

    @Override
    public void applyChange(KMLAbstractObject sourceValues)
    {
        if (!(sourceValues instanceof KMLStyleUrl))
        {
            String message = Logging.getMessage("KML.InvalidElementType", sourceValues.getClass().getName());
            log.warn(message);
            throw new IllegalArgumentException(message);
        }

        super.applyChange(sourceValues);

        this.onChange(new Message(KMLAbstractObject.MSG_STYLE_CHANGED, this));
    }
}
