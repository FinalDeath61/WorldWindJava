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
package gov.nasa.worldwind.cache;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * @author tag
 * @version $Id: AbstractFileStore.java 2190 2014-08-01 21:54:20Z pabercrombie $
 */
@Slf4j
public abstract class AbstractFileStore extends WWObjectImpl implements FileStore
{
    protected static class StoreLocation extends AVListImpl
    {
        protected boolean markWhenUsed = false;

        public StoreLocation(java.io.File file, boolean isInstall)
        {
            this.setValue(AVKey.FILE_STORE_LOCATION, file);
            this.setValue(AVKey.INSTALLED, isInstall);
        }

        public StoreLocation(java.io.File file)
        {
            this(file, false);
        }

        public java.io.File getFile()
        {
            Object o = this.getValue(AVKey.FILE_STORE_LOCATION);
            return (o != null && o instanceof java.io.File) ? (java.io.File) o : null;
        }

        public void setFile(java.io.File file)
        {
            this.setValue(AVKey.FILE_STORE_LOCATION, file);
        }

        public boolean isInstall()
        {
            Object o = this.getValue(AVKey.INSTALLED);
            return (o != null && o instanceof Boolean) ? (Boolean) o : false;
        }

        public void setInstall(boolean isInstall)
        {
            this.setValue(AVKey.INSTALLED, isInstall);
        }

        public boolean isMarkWhenUsed()
        {
            return markWhenUsed;
        }

        public void setMarkWhenUsed(boolean markWhenUsed)
        {
            this.markWhenUsed = markWhenUsed;
        }
    }

    // Retrieval could be occurring on several threads when the app adds a read location, so protect the list of read
    // locations from concurrent modification.
    protected final java.util.List<StoreLocation> readLocations =
        new java.util.concurrent.CopyOnWriteArrayList<StoreLocation>();
    protected StoreLocation writeLocation = null;
    private final Object fileLock = new Object();

    //**************************************************************//
    //********************  File Store Configuration  **************//
    //**************************************************************//

    protected void initialize(java.io.InputStream xmlConfigStream)
    {
        javax.xml.parsers.DocumentBuilderFactory docBuilderFactory =
            javax.xml.parsers.DocumentBuilderFactory.newInstance();

        try
        {
            javax.xml.parsers.DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuilder.parse(xmlConfigStream);

            // The order of the following two calls is important, because building the writable location may entail
            // creating a location that's included in the specified read locations.
            this.buildWritePaths(doc);
            this.buildReadPaths(doc);

            if (this.writeLocation == null)
            {
                log.warn("FileStore.NoWriteLocation");
            }

            if (this.readLocations.size() == 0)
            {
                // This should not happen because the writable location is added to the read list, but check nonetheless
                String message = Logging.getMessage("FileStore.NoReadLocations");
                log.error(message);
                throw new IllegalStateException(message);
            }
        }
        catch (ParserConfigurationException | IOException | SAXException e)
        {
            String message = Logging.getMessage("FileStore.ExceptionReadingConfigurationFile");
            log.error(message);
            throw new IllegalStateException(message, e);
        }
    }

    protected void buildReadPaths(org.w3c.dom.Node dataFileStoreNode)
    {
        javax.xml.xpath.XPathFactory pathFactory = javax.xml.xpath.XPathFactory.newInstance();
        javax.xml.xpath.XPath pathFinder = pathFactory.newXPath();

        try
        {
            org.w3c.dom.NodeList locationNodes = (org.w3c.dom.NodeList) pathFinder.evaluate(
                "/dataFileStore/readLocations/location",
                dataFileStoreNode.getFirstChild(),
                javax.xml.xpath.XPathConstants.NODESET);
            for (int i = 0; i < locationNodes.getLength(); i++)
            {
                org.w3c.dom.Node location = locationNodes.item(i);
                String prop = pathFinder.evaluate("@property", location);
                String wwDir = pathFinder.evaluate("@wwDir", location);
                String append = pathFinder.evaluate("@append", location);
                String isInstall = pathFinder.evaluate("@isInstall", location);
                String isMarkWhenUsed = pathFinder.evaluate("@isMarkWhenUsed", location);

                String path = buildLocationPath(prop, append, wwDir);
                if (path == null)
                {
                    log.warn("FileStore.LocationInvalid {}",
                        prop != null ? prop : Logging.getMessage("generic.Unknown"));
                    continue;
                }

                StoreLocation oldStore = this.storeLocationFor(path);
                if (oldStore != null) // filter out duplicates
                    continue;

                // Even paths that don't exist or are otherwise problematic are added to the list because they may
                // become readable during the session. E.g., removable media. So add them to the search list.

                java.io.File pathFile = new java.io.File(path);
                if (pathFile.exists() && !pathFile.isDirectory())
                {
                    log.warn("FileStore.LocationIsFile {}", pathFile.getPath());
                }

                boolean pathIsInstall = isInstall != null && (isInstall.contains("t") || isInstall.contains("T"));
                StoreLocation newStore = new StoreLocation(pathFile, pathIsInstall);

                // If the input parameter "markWhenUsed" is null or empty, then the StoreLocation should keep its
                // default value. Otherwise the store location value is set to true when the input parameter contains
                // "t", and is set to false otherwise.
                if (isMarkWhenUsed != null && isMarkWhenUsed.length() > 0)
                    newStore.setMarkWhenUsed(isMarkWhenUsed.toLowerCase().contains("t"));

                this.readLocations.add(newStore);
            }
        }
        catch (javax.xml.xpath.XPathExpressionException e)
        {
            String message = Logging.getMessage("FileStore.ExceptionReadingConfigurationFile");
            log.error(message);
            throw new IllegalStateException(message, e);
        }
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    protected void buildWritePaths(org.w3c.dom.Node dataFileCacheNode)
    {
        javax.xml.xpath.XPathFactory pathFactory = javax.xml.xpath.XPathFactory.newInstance();
        javax.xml.xpath.XPath pathFinder = pathFactory.newXPath();

        try
        {
            org.w3c.dom.NodeList locationNodes = (org.w3c.dom.NodeList) pathFinder.evaluate(
                "/dataFileStore/writeLocations/location",
                dataFileCacheNode.getFirstChild(),
                javax.xml.xpath.XPathConstants.NODESET);
            for (int i = 0; i < locationNodes.getLength(); i++)
            {
                org.w3c.dom.Node location = locationNodes.item(i);
                String prop = pathFinder.evaluate("@property", location);
                String wwDir = pathFinder.evaluate("@wwDir", location);
                String append = pathFinder.evaluate("@append", location);
                String create = pathFinder.evaluate("@create", location);

                String path = buildLocationPath(prop, append, wwDir);
                if (path == null)
                {
                    log.warn("FileStore.LocationInvalid {}",
                        prop != null ? prop : Logging.getMessage("generic.Unknown"));
                    continue;
                }

                log.trace("FileStore.AttemptingWriteDir {}", path);
                java.io.File pathFile = new java.io.File(path);
                if (!pathFile.exists() && create != null && (create.contains("t") || create.contains("T")))
                {
                    log.trace("FileStore.MakingDirsFor {}", path);
                    pathFile.mkdirs();
                }

                if (pathFile.isDirectory() && pathFile.canWrite() && pathFile.canRead())
                {
                    log.trace("FileStore.WriteLocationSuccessful {}", path);
                    this.writeLocation = new StoreLocation(pathFile);

                    // Remove the writable location from search path if it already exists.
                    StoreLocation oldLocation = this.storeLocationFor(path);
                    if (oldLocation != null)
                        this.readLocations.remove(oldLocation);

                    // Writable location is always first in search path.
                    this.readLocations.add(0, this.writeLocation);

                    break; // only need one
                }
            }
        }
        catch (javax.xml.xpath.XPathExpressionException e)
        {
            String message = Logging.getMessage("FileStore.ExceptionReadingConfigurationFile");
            log.error(message);
            throw new IllegalStateException(message, e);
        }
    }

    protected static String buildLocationPath(String property, String append, String wwDir)
    {
        String path = propertyToPath(property);

        if (append != null && append.length() != 0)
            path = WWIO.appendPathPart(path, append.trim());

        if (wwDir != null && wwDir.length() != 0)
            path = WWIO.appendPathPart(path, wwDir.trim());

        return path;
    }

    protected static String propertyToPath(String propName)
    {
        if (propName == null || propName.length() == 0)
            return null;

        String prop = System.getProperty(propName);
        if (prop != null)
            return prop;

        if (propName.equalsIgnoreCase("gov.nasa.worldwind.platform.alluser.store"))
            return determineAllUserLocation();

        if (propName.equalsIgnoreCase("gov.nasa.worldwind.platform.user.store"))
            return determineSingleUserLocation();

        return null;
    }

    protected static String determineAllUserLocation()
    {
        if (gov.nasa.worldwind.Configuration.isMacOS())
        {
            return "/Library/Caches";
        }
        else if (gov.nasa.worldwind.Configuration.isWindowsOS())
        {
            String path = System.getenv("ALLUSERSPROFILE");
            if (path == null)
            {
                log.error("generic.AllUsersWindowsProfileNotKnown");
                return null;
            }
            return path + (Configuration.isWindows7OS() ? "" : "\\Application Data");
        }
        else if (gov.nasa.worldwind.Configuration.isLinuxOS() || gov.nasa.worldwind.Configuration.isUnixOS()
            || gov.nasa.worldwind.Configuration.isSolarisOS())
        {
            return "/var/cache/";
        }
        else
        {
            log.warn("generic.UnknownOperatingSystem");
            return null;
        }
    }

    protected static String determineSingleUserLocation()
    {
        String home = getUserHomeDir();
        if (home == null)
        {
            log.warn("generic.UsersHomeDirectoryNotKnown");
            return null;
        }

        String path = null;

        if (gov.nasa.worldwind.Configuration.isMacOS())
        {
            path = "/Library/Caches";
        }
        else if (gov.nasa.worldwind.Configuration.isWindowsOS())
        {
            // This produces an incorrect path with duplicate parts,
            // like "C:\Users\PatC:\Users\Pat\Application Data".
            //path = System.getenv("USERPROFILE");
            //if (path == null)
            //{
            //    log.trace("generic.UsersWindowsProfileNotKnown");
            //    return null;
            //}
            //path += "\\Application Data";

            path = "\\Application Data";
        }
        else if (gov.nasa.worldwind.Configuration.isLinuxOS() || gov.nasa.worldwind.Configuration.isUnixOS()
            || gov.nasa.worldwind.Configuration.isSolarisOS())
        {
            path = "/var/cache/";
        }
        else
        {
            log.trace("generic.UnknownOperatingSystem");
        }

        if (path == null)
            return null;

        return home + path;
    }

    protected static String getUserHomeDir()
    {
        return System.getProperty("user.home");
    }

    //**************************************************************//
    //********************  File Store Locations  ******************//
    //**************************************************************//

    public java.util.List<? extends java.io.File> getLocations()
    {
        java.util.ArrayList<java.io.File> locations = new java.util.ArrayList<java.io.File>();
        for (StoreLocation location : this.readLocations)
        {
            locations.add(location.getFile());
        }
        return locations;
    }

    public java.io.File getWriteLocation()
    {
        return (this.writeLocation != null) ? this.writeLocation.getFile() : null;
    }

    public void addLocation(String newPath, boolean isInstall)
    {
        this.addLocation(this.readLocations.size(), newPath, isInstall);
    }

    public void addLocation(int index, String newPath, boolean isInstall)
    {
        if (newPath == null || newPath.length() == 0)
        {
            String message = Logging.getMessage("nullValue.FileStorePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (index < 0)
        {
            String message = Logging.getMessage("generic.InvalidIndex", index);
            log.trace(message);
            throw new IllegalArgumentException(message);
        }

        StoreLocation oldLocation = this.storeLocationFor(newPath);
        if (oldLocation != null)
            this.readLocations.remove(oldLocation);

        if (index > 0 && index > this.readLocations.size())
            index = this.readLocations.size();
        java.io.File newFile = new java.io.File(newPath);
        StoreLocation newLocation = new StoreLocation(newFile, isInstall);
        this.readLocations.add(index, newLocation);
    }

    public void removeLocation(String path)
    {
        if (path == null || path.length() == 0)
        {
            String message = Logging.getMessage("nullValue.FileStorePathIsNull");
            log.error(message);
            // Just warn and return.
            return;
        }

        StoreLocation location = this.storeLocationFor(path);
        if (location == null) // Path is not part of this FileStore.
            return;

        if (location.equals(this.writeLocation))
        {
            String message = Logging.getMessage("FileStore.CannotRemoveWriteLocation", path);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.readLocations.remove(location);
    }

    public boolean isInstallLocation(String path)
    {
        if (path == null || path.length() == 0)
        {
            String message = Logging.getMessage("nullValue.FileStorePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        StoreLocation location = this.storeLocationFor(path);
        return location != null && location.isInstall();
    }

    protected StoreLocation storeLocationFor(String path)
    {
        java.io.File file = new java.io.File(path);

        for (StoreLocation location : this.readLocations)
        {
            if (file.equals(location.getFile()))
                return location;
        }

        return null;
    }

    //**************************************************************//
    //********************  File Store Contents  *******************//
    //**************************************************************//

    public boolean containsFile(String fileName)
    {
        if (fileName == null)
            return false;

        for (StoreLocation location : this.readLocations)
        {
            java.io.File dir = location.getFile();
            java.io.File file;

            if (fileName.startsWith(dir.getAbsolutePath()))
                file = new java.io.File(fileName);
            else
                file = makeAbsoluteFile(dir, fileName);

            if (file.exists())
                return true;
        }

        return false;
    }

    /**
     * @param fileName       the name of the file to find
     * @param checkClassPath if <code>true</code>, the class path is first searched for the file, otherwise the class
     *                       path is not searched unless it's one of the explicit paths in the cache search directories
     *
     * @return a handle to the requested file if it exists in the cache, otherwise null
     *
     * @throws IllegalArgumentException if <code>fileName</code> is null
     */
    public java.net.URL findFile(String fileName, boolean checkClassPath)
    {
        if (fileName == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (checkClassPath)
        {
            java.net.URL url = this.getClass().getClassLoader().getResource(fileName);
            if (url != null)
                return url;

            // Check for a thread context class loader. This allows the file store to find resources in a case
            // in which different parts of the application are handled by different class loaders.
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            if (tccl != null)
            {
                url = tccl.getResource(fileName);
                if (url != null)
                    return url;
            }
        }

        for (StoreLocation location : this.readLocations)
        {
            java.io.File dir = location.getFile();
            if (!dir.exists())
                continue;

            java.io.File file = new java.io.File(makeAbsolutePath(dir, fileName));
            if (file.exists())
            {
                try
                {
                    if (location.isMarkWhenUsed())
                        markFileUsed(file);
                    else
                        markFileUsed(file.getParentFile());

                    return file.toURI().toURL();
                }
                catch (java.net.MalformedURLException e)
                {
                    log.error(Logging.getMessage("FileStore.ExceptionCreatingURLForFile", file.getPath()), e);
                }
            }
        }

        return null;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    protected static void markFileUsed(java.io.File file)
    {
        if (file == null)
            return;

        long currentTime = System.currentTimeMillis();

        if (file.canWrite())
            file.setLastModified(currentTime);

        if (file.isDirectory())
            return;

        java.io.File parent = file.getParentFile();
        if (parent != null && parent.canWrite())
            parent.setLastModified(currentTime);
    }

    /**
     * @param fileName the name to give the newly created file
     *
     * @return a handle to the newly created file if it could be created and added to the file store, otherwise null
     *
     * @throws IllegalArgumentException if <code>fileName</code> is null
     */
    public java.io.File newFile(String fileName)
    {
        if (fileName == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (this.writeLocation != null)
        {
            String fullPath = makeAbsolutePath(this.writeLocation.getFile(), fileName);
            java.io.File file = new java.io.File(fullPath);
            boolean canCreateFile = false;

            // This block of code must be synchronized for proper operation. A thread may check that
            // file.getParentFile() does not exist, and become immediately suspended. A second thread may then create
            // the parent and ancestor directories. When the first thread wakes up, file.getParentFile().mkdirs()
            // fails, resulting in an erroneous log message: The log reports that the file cannot be created.
            synchronized (this.fileLock)
            {
                if (file.getParentFile().exists())
                    canCreateFile = true;
                else if (file.getParentFile().mkdirs())
                    canCreateFile = true;
            }

            if (canCreateFile)
                return file;
            else
            {
                String msg = Logging.getMessage("generic.CannotCreateFile", fullPath);
                log.error(msg);
            }
        }

        return null;
    }

    /**
     * @param url the "file:" URL of the file to remove from the file store. Only files in the writable WorldWind disk
     *            cache or temp file directory are removed by this method.
     *
     * @throws IllegalArgumentException if <code>url</code> is null
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void removeFile(java.net.URL url)
    {
        if (url == null)
        {
            String msg = Logging.getMessage("nullValue.URLIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        try
        {
            java.io.File file = new java.io.File(url.toURI());

            // This block of code must be synchronized for proper operation. A thread may check that the file exists,
            // and become immediately suspended. A second thread may then delete that file. When the first thread
            // wakes up, file.delete() fails.
            synchronized (this.fileLock)
            {
                if (file.exists())
                {
                    // Don't remove files outside the cache or temp directory.
                    String parent = file.getParent();
                    if (!(parent.startsWith(this.getWriteLocation().getPath())
                        || parent.startsWith(Configuration.getSystemTempDirectory())))
                        return;

                    file.delete();
                }
            }
        }
        catch (java.net.URISyntaxException e)
        {
            log.error( Logging.getMessage("FileStore.ExceptionRemovingFile", url.toString()),
                e);
        }
    }

    protected static java.io.File makeAbsoluteFile(java.io.File file, String fileName)
    {
        return new java.io.File(file.getAbsolutePath() + "/" + fileName);
    }

    protected static String makeAbsolutePath(java.io.File dir, String fileName)
    {
        return dir.getAbsolutePath() + "/" + fileName;
    }

    protected static String normalizeFileStoreName(String fileName)
    {
        // Convert all file separators to forward slashes, and strip any leading or trailing file separators
        // from the path.
        String normalizedName = fileName.replaceAll("\\\\", "/");
        normalizedName = WWIO.stripLeadingSeparator(normalizedName);
        normalizedName = WWIO.stripTrailingSeparator(normalizedName);

        return normalizedName;
    }

    protected static String storePathForFile(StoreLocation location, java.io.File file)
    {
        String path = file.getPath();

        if (location != null)
        {
            String locationPath = location.getFile().getPath();
            if (path.startsWith(locationPath))
                path = path.substring(locationPath.length(), path.length());
        }

        return path;
    }

    //**************************************************************//
    //********************  File Store Content Discovery  **********//
    //**************************************************************//

    public String[] listFileNames(String pathName, FileStoreFilter filter)
    {
        if (filter == null)
        {
            String msg = Logging.getMessage("nullValue.FilterIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Do not recurse.
        return this.doListFileNames(pathName, filter, false, false);
    }

    public String[] listAllFileNames(String pathName, FileStoreFilter filter)
    {
        if (filter == null)
        {
            String msg = Logging.getMessage("nullValue.FilterIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Recurse, and continue to search each branch after a match is found.
        return this.doListFileNames(pathName, filter, true, false);
    }

    public String[] listTopFileNames(String pathName, FileStoreFilter filter)
    {
        if (filter == null)
        {
            String msg = Logging.getMessage("nullValue.FilterIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Recurse, but stop searching a branch after a match is found.
        return this.doListFileNames(pathName, filter, true, true);
    }

    protected String[] doListFileNames(String pathName, FileStoreFilter filter, boolean recurse,
        boolean exitBranchOnFirstMatch)
    {
        java.util.ArrayList<String> nameList = null;

        for (StoreLocation location : this.readLocations)
        {
            // If the path name is null, then just search from the root of each location. Otherwise search from the
            // named cache path.
            java.io.File dir = location.getFile();
            if (pathName != null)
                dir = new java.io.File(makeAbsolutePath(dir, pathName));

            // Either the location does not exists, or the speciifed path does not exist under that location. In either
            // case we skip searching this location.
            if (!dir.exists())
                continue;

            // Lazily initialize the list of file names. If no location contains the specified path, then the list is
            // not created, and this method will return null.
            if (nameList == null)
                nameList = new java.util.ArrayList<String>();

            this.doListFileNames(location, dir, filter, recurse, exitBranchOnFirstMatch, nameList);
        }

        if (nameList == null)
            return null;

        String[] names = new String[nameList.size()];
        nameList.toArray(names);
        return names;
    }

    protected void doListFileNames(StoreLocation location, java.io.File dir, FileStoreFilter filter,
        boolean recurse, boolean exitBranchOnFirstMatch, java.util.Collection<String> names)
    {
        java.util.ArrayList<java.io.File> subDirs = new java.util.ArrayList<java.io.File>();

        // Search the children of the specified directory. If the child is a directory, append it to the list of sub
        // directories to search later. Otherwise, try to list the file as a match. If the file is a match and
        // exitBranchOnFirstMatch is true, then exit this branch without considering any other files. This has the
        // effect of choosing files closest to the search root.
        for (java.io.File childFile : dir.listFiles())
        {
            if (childFile == null)
                continue;

            if (childFile.isDirectory())
            {
                subDirs.add(childFile);
            }
            else
            {
                if (this.listFile(location, childFile, filter, names) && exitBranchOnFirstMatch)
                    return;
            }
        }

        if (!recurse)
            return;

        // Recursively search each sub-directory. If exitBranchOnFirstMatch is true, then we did not find a match under
        // this directory.
        for (java.io.File childDir : subDirs)
        {
            this.doListFileNames(location, childDir, filter, recurse, exitBranchOnFirstMatch, names);
        }
    }

    protected boolean listFile(StoreLocation location, java.io.File file, FileStoreFilter filter,
        java.util.Collection<String> names)
    {
        String fileName = storePathForFile(location, file);
        if (fileName == null)
            return false;

        String normalizedName = normalizeFileStoreName(fileName);
        return this.listFileName(location, normalizedName, filter, names);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected boolean listFileName(StoreLocation location, String fileName, FileStoreFilter filter,
        java.util.Collection<String> names)
    {
        if (!filter.accept(this, fileName))
            return false;

        names.add(fileName);
        return true;
    }
}
