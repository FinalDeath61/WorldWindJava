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
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;

/**
 * DIGEST Part 2, Annex C.2.2.2.5 and C.2.3.6: <br>A database is a collection of related libraries and additional
 * tables. The library attribute table acts as a table of contents for the database.  Database information is contained
 * in a database header table.  Database level data quality information can be maintained in the data quality table.
 *
 * @author dcollins
 * @version $Id: VPFDatabase.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class VPFDatabase extends AVListImpl
{
    private String filePath;
    private Map<String, VPFLibrary> libraryMap = new HashMap<String, VPFLibrary>();
    private VPFBufferedRecordData databaseHeaderTable;
    private VPFBufferedRecordData libraryAttributeTable;

    protected VPFDatabase(String filePath)
    {
        if (filePath == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.filePath = filePath;
    }

    /**
     * Constructs a VPF Database from a path to the Database Header table. This initializes the Database Header Table
     * and the Library Attribute Table.
     *
     * @param filePath the path to the Database Header Table.
     *
     * @return a new Database from the specified Database Header Table path.
     *
     * @throws IllegalArgumentException if the file path is null or empty.
     */
    public static VPFDatabase fromFile(String filePath)
    {
        if (WWUtil.isEmpty(filePath))
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        File file = new File(filePath);
        if (!file.exists())
        {
            String message = Logging.getMessage("generic.FileNotFound", filePath);
            log.error(message);
            throw new WWRuntimeException(message);
        }

        // Database tables.
        VPFBufferedRecordData dht = VPFUtils.readTable(file);
        if (dht == null)
        {
            String message = Logging.getMessage("VPF.DatabaseHeaderTableMissing");
            throw new WWRuntimeException(message);
        }

        VPFBufferedRecordData lat = VPFUtils.readTable(
            new File(file.getParent(), VPFConstants.LIBRARY_ATTRIBUTE_TABLE));
        if (lat == null)
        {
            String message = Logging.getMessage("VPF.LibraryAttributeTableMissing");
            throw new WWRuntimeException(message);
        }

        VPFDatabase database = new VPFDatabase(file.getParent());
        database.setDatabaseHeaderTable(dht);
        database.setLibraryAttributeTable(lat);

        // Database metadata attributes.
        VPFRecord record = dht.getRecord(1);
        if (record != null)
        {
            VPFUtils.checkAndSetValue(record, "database_name", AVKey.DISPLAY_NAME, database);
            VPFUtils.checkAndSetValue(record, "database_desc", AVKey.DESCRIPTION, database);
        }

        // Database Libraries.
        Collection<VPFLibrary> col = createLibraries(database, lat);
        if (col != null)
            database.setLibraries(col);

        return database;
    }

    public static boolean isDatabase(String filePath)
    {
        if (filePath == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        File file = new File(filePath);
        if (!file.exists())
            return false;

        VPFBufferedRecordData table = VPFUtils.readTable(file);
        if (table == null)
            return false;

        file = new File(file.getParent(), VPFConstants.LIBRARY_ATTRIBUTE_TABLE);
        if (!file.exists())
            return false;

        table = VPFUtils.readTable(file);
        //noinspection RedundantIfStatement
        if (table == null)
            return false;

        return true;
    }

    public String getFilePath()
    {
        return this.filePath;
    }

    /**
     * Returns the text name of this Database.
     *
     * @return name of this Database.
     */
    public String getName()
    {
        return this.getStringValue(AVKey.DISPLAY_NAME);
    }

    /**
     * Returns a text description of this Database.
     *
     * @return description of this Database.
     */
    public String getDescription()
    {
        return this.getStringValue(AVKey.DESCRIPTION);
    }

    /**
     * Returns the number of Libraries associated with this Database. If this Database is not associated with any
     * Library, this returns 0.
     *
     * @return number of Libraries associated with this Database.
     */
    public int getNumLibraries()
    {
        return this.libraryMap.size();
    }

    public boolean containsLibrary(String name)
    {
        if (name == null)
        {
            String message = Logging.getMessage("nullValue.NameIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        return this.libraryMap.containsKey(name);
    }

    public VPFLibrary getLibrary(String name)
    {
        if (name == null)
        {
            String message = Logging.getMessage("nullValue.NameIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        return this.libraryMap.get(name);
    }

    public Set<String> getLibraryNames()
    {
        return Collections.unmodifiableSet(this.libraryMap.keySet());
    }

    public Collection<VPFLibrary> getLibraries()
    {
        return Collections.unmodifiableCollection(this.libraryMap.values());
    }

    public void setLibraries(Collection<? extends VPFLibrary> collection)
    {
        this.removeAllLibraries();

        if (collection != null)
            this.addAllLibraries(collection);
    }

    public void addLibrary(VPFLibrary library)
    {
        if (library == null)
        {
            String message = Logging.getMessage("nullValue.LibraryIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.libraryMap.put(library.getName(), library);
    }

    public void addAllLibraries(Collection<? extends VPFLibrary> collection)
    {
        if (collection == null)
        {
            String message = Logging.getMessage("nullValue.CollectionIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        for (VPFLibrary lib : collection)
        {
            this.addLibrary(lib);
        }
    }

    public void removeLibrary(VPFLibrary library)
    {
        if (library == null)
        {
            String message = Logging.getMessage("nullValue.LibraryIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.libraryMap.remove(library.getName());
    }

    public void removeAllLibraries()
    {
        this.libraryMap.clear();
    }

    /**
     * Returns the Database Header Table associated with this Database.
     *
     * @return the Database Header Table.
     */
    public VPFBufferedRecordData getDatabaseHeaderTable()
    {
        return this.databaseHeaderTable;
    }

    /**
     * Sets the Database Header Table associated with this Database.
     *
     * @param table the Database Header Table.
     *
     * @throws IllegalArgumentException if the table is null.
     */
    public void setDatabaseHeaderTable(VPFBufferedRecordData table)
    {
        if (table == null)
        {
            String message = Logging.getMessage("nullValue.TableIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.databaseHeaderTable = table;
    }

    /**
     * Returns the Library Attribute Table associated with this Database.
     *
     * @return the Library Attribute Table.
     */
    public VPFBufferedRecordData getLibraryAttributeTable()
    {
        return this.libraryAttributeTable;
    }

    /**
     * Sets the Library Attribute Table associated with this Database.
     *
     * @param table the Library Attribute Table.
     *
     * @throws IllegalArgumentException if the table is null.
     */
    public void setLibraryAttributeTable(VPFBufferedRecordData table)
    {
        if (table == null)
        {
            String message = Logging.getMessage("nullValue.TableIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.libraryAttributeTable = table;
    }

    //**************************************************************//
    //********************  Library Assembly  **********************//
    //**************************************************************//

    protected static Collection<VPFLibrary> createLibraries(VPFDatabase db, VPFBufferedRecordData table)
    {
        ArrayList<VPFLibrary> list = new ArrayList<VPFLibrary>();

        for (VPFRecord row : table)
        {
            String name = (String) row.getValue("library_name");
            if (name != null)
            {
                VPFLibrary lib = VPFUtils.readLibrary(db, name);
                if (lib != null)
                    list.add(lib);
            }
        }

        return list;
    }
}
