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
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.Angle;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tag
 * @version $Id: TileKey.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class TileKey implements Comparable<TileKey>
{
    private final int level;
    private final int row;
    private final int col;
    private final String cacheName;
    private final int hash;

    /**
     * @param level Tile level.
     * @param row Tile row.
     * @param col Tile col.
     * @param cacheName Cache name.
     * @throws IllegalArgumentException if <code>level</code>, <code>row</code> or <code>column</code> is negative or if
     *                                  <code>cacheName</code> is null or empty
     */
    public TileKey(int level, int row, int col, String cacheName)
    {
        if (level < 0)
        {
            String msg = Logging.getMessage("TileKey.levelIsLessThanZero");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
//        if (row < 0)
//        {
//            String msg = Logging.getMessage("generic.RowIndexOutOfRange", row);
//            log.error(msg);
//            throw new IllegalArgumentException(msg);
//        }
//        if (col < 0)
//        {
//            String msg = Logging.getMessage("generic.ColumnIndexOutOfRange", col);
//            log.error(msg);
//            throw new IllegalArgumentException(msg);
//        }
        if (cacheName == null || cacheName.length() < 1)
        {
            String msg = Logging.getMessage("TileKey.cacheNameIsNullOrEmpty");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        this.level = level;
        this.row = row;
        this.col = col;
        this.cacheName = cacheName;
        this.hash = this.computeHash();
    }

    /**
     * @param latitude Tile latitude.
     * @param longitude Tile longitude.
     * @param levelSet The level set.
     * @param levelNumber Tile level number.
     * @throws IllegalArgumentException if any parameter is null
     */
    public TileKey(Angle latitude, Angle longitude, LevelSet levelSet, int levelNumber)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (levelSet == null)
        {
            String msg = Logging.getMessage("nullValue.LevelSetIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        Level l = levelSet.getLevel(levelNumber);
        this.level = levelNumber;
        this.row = Tile.computeRow(l.getTileDelta().getLatitude(), latitude, levelSet.getTileOrigin().getLatitude());
        this.col = Tile.computeColumn(l.getTileDelta().getLongitude(), longitude, levelSet.getTileOrigin().getLongitude());
        this.cacheName = l.getCacheName();
        this.hash = this.computeHash();
    }

    /**
     * @param tile The source tile.
     * @throws IllegalArgumentException if <code>tile</code> is null
     */
    public TileKey(Tile tile)
    {
        if (tile == null)
        {
            String msg = Logging.getMessage("nullValue.TileIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        this.level = tile.getLevelNumber();
        this.row = tile.getRow();
        this.col = tile.getColumn();
        this.cacheName = tile.getCacheName();
        this.hash = this.computeHash();
    }

    public int getLevelNumber()
    {
        return level;
    }

    public int getRow()
    {
        return row;
    }

    public int getColumn()
    {
        return col;
    }

    public String getCacheName()
    {
        return cacheName;
    }

    private int computeHash()
    {
        int result;
        result = this.level;
        result = 29 * result + this.row;
        result = 29 * result + this.col;
        result = 29 * result + (this.cacheName != null ? this.cacheName.hashCode() : 0);
        return result;
    }

    /**
     * Compare two tile keys. Keys are ordered based on level, row, and column (in that order).
     *
     * @param key Key to compare with.
     *
     * @return 0 if the keys are equal. 1 if this key &gt; {@code key}. -1 if this key &lt; {@code key}.
     *
     * @throws IllegalArgumentException if <code>key</code> is null
     */
    public final int compareTo(TileKey key)
    {
        if (key == null)
        {
            String msg = Logging.getMessage("nullValue.KeyIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // No need to compare Sectors because they are redundant with row and column
        if (key.level == this.level && key.row == this.row && key.col == this.col)
            return 0;

        if (this.level < key.level) // Lower-res levels compare lower than higher-res
            return -1;
        if (this.level > key.level)
            return 1;

        if (this.row < key.row)
            return -1;
        if (this.row > key.row)
            return 1;

        if (this.col < key.col)
            return -1;

        return 1; // tile.col must be > this.col because equality was tested above
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final TileKey tileKey = (TileKey) o;

        if (this.col != tileKey.col)
            return false;
        if (this.level != tileKey.level)
            return false;
        //noinspection SimplifiableIfStatement
        if (this.row != tileKey.row)
            return false;

        return !(this.cacheName != null ? !this.cacheName.equals(tileKey.cacheName) : tileKey.cacheName != null);
    }

    @Override
    public int hashCode()
    {
        return this.hash;
    }

    @Override
    public String toString()
    {
        return this.cacheName + "/" + this.level + "/" + this.row + "/" + col;
    }
}
