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

package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements <code>WWObject</code> functionality. Meant to be either subclassed or aggregated by classes implementing
 * <code>WWObject</code>.
 *
 * @author Tom Gaskins
 * @version $Id: WWObjectImpl.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class WWObjectImpl extends AVListImpl implements WWObject
{
    /**
     * Constructs a new <code>WWObjectImpl</code>.
     */
    public WWObjectImpl()
    {
    }

    public WWObjectImpl(Object source)
    {
        super(source);
    }

    /**
     * The property change listener for <em>this</em> instance.
     * Receives property change notifications that this instance has registered with other property change notifiers.
     * @param propertyChangeEvent the event
     * @throws IllegalArgumentException if <code>propertyChangeEvent</code> is null
     */
    public void propertyChange(java.beans.PropertyChangeEvent propertyChangeEvent)
    {
        if (propertyChangeEvent == null)
        {
            String msg = Logging.getMessage("nullValue.PropertyChangeEventIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Notify all *my* listeners of the change that I caught
        super.firePropertyChange(propertyChangeEvent);
    }

    /** Empty implementation of MessageListener. */
    public void onMessage(Message message)
    {
        // Empty implementation
    }
}
