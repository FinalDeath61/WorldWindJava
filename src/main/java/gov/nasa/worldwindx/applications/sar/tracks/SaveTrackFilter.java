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
package gov.nasa.worldwindx.applications.sar.tracks;

import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @author dcollins
 * @version $Id: SaveTrackFilter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class SaveTrackFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter
{
    private final int format;
    private final String description;
    private final String[] suffixes;

    public SaveTrackFilter(int format, String description, String[] suffixes)
    {
        this.format = format;
        this.description = description;
        this.suffixes = new String[suffixes.length];
        System.arraycopy(suffixes, 0, this.suffixes, 0, suffixes.length);
    }

    public int getFormat()
    {
        return this.format;
    }

    public String getDescription()
    {
        return this.description;
    }

    public String[] getSuffixes()
    {
        String[] copy = new String[this.suffixes.length];
        System.arraycopy(this.suffixes, 0, copy, 0, this.suffixes.length);
        return copy;
    }

    public boolean accept(java.io.File file)
    {
        return true;
    }

    public java.io.File appendSuffix(java.io.File file)
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        String path = file.getPath();

        String lowerCasePath = path.toLowerCase();
        for (String suffix : this.suffixes)
        {
            if (lowerCasePath.endsWith(suffix))
                return file;
        }

        return new File(WWIO.replaceSuffix(path, this.suffixes[0]));
    }
}