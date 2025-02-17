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

package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * The <code>CachedDataRaster</code> is used to hold data raster's source and metadata, while the actual data raster may
 * not be loaded in to the memory. This is mostly used together with a memory caches. <code>CachedDataRaster</code>
 * actually implements all interfaces of the <code>DataRaster</code>, and acts as a proxy, that loads a real data raster
 * only when it is actually needed.
 *
 * @author Lado Garakanidze
 * @version $Id: CachedDataRaster.java 3037 2015-04-17 23:08:47Z tgaskins $
 */
@Slf4j
public class CachedDataRaster extends AVListImpl implements DataRaster
{
    protected enum ErrorHandlerMode
    {
        ALLOW_EXCEPTIONS, DISABLE_EXCEPTIONS
    }

    protected Object dataSource = null;
    protected DataRasterReader dataReader = null;

    protected MemoryCache rasterCache = null;
    protected MemoryCache.CacheListener cacheListener = null;

    protected final Object rasterUsageLock = new Object();
    protected final Object rasterRetrievalLock = new Object();

    protected String[] requiredKeys = new String[] {AVKey.SECTOR, AVKey.PIXEL_FORMAT};

    /**
     * Create a cached data raster.
     *
     * @param source the location of the local file, expressed as either a String path, a File, or a file URL.
     * @param params metadata as AVList, it is expected to next parameters: AVKey.WIDTH, AVKey.HEIGHT, AVKey.SECTOR,
     *               AVKey.PIXEL_FORMAT.
     *               <p>
     *               If any of these keys is missing, there will be an attempt made to retrieve missign metadata from
     *               the source using the reader.
     * @param reader A reference to a DataRasterReader instance
     * @param cache  A reference to a MemoryCache instance
     *
     * @throws java.io.IOException      thrown if there is an error to read metadata from the source
     * @throws IllegalArgumentException thrown when a source or a reader are null
     */
    public CachedDataRaster(Object source, AVList params, DataRasterReader reader, MemoryCache cache)
        throws java.io.IOException, IllegalArgumentException
    {
        if (source == null)
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (reader == null)
        {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        params = (null == params) ? new AVListImpl() : params;
        this.assembleMetadata(source, params, reader);

        this.dataSource = source;
        this.dataReader = reader;
        this.setValues(params.copy());

        this.rasterCache = cache;
        if (this.rasterCache != null)
        {
            this.cacheListener = new CacheListener(this.dataSource);
            this.rasterCache.addCacheListener(this.cacheListener);
        }
    }

    protected void assembleMetadata(Object source, AVList params, DataRasterReader reader)
        throws java.io.IOException, IllegalArgumentException
    {
        if (!this.hasRequiredMetadata(params, ErrorHandlerMode.DISABLE_EXCEPTIONS))
        {
            if (!reader.canRead(source, params))
            {
                String message = Logging.getMessage("DataRaster.CannotRead", source);
                log.error(message);
                throw new java.io.IOException(message);
            }

            if (!this.hasRequiredMetadata(params, ErrorHandlerMode.DISABLE_EXCEPTIONS))
            {
                reader.readMetadata(source, params);
                this.hasRequiredMetadata(params, ErrorHandlerMode.ALLOW_EXCEPTIONS);
            }
        }
    }

    protected String[] getRequiredKeysList()
    {
        return this.requiredKeys;
    }

    /**
     * Validates if params (AVList) has all required keys.
     *
     * @param params         AVList of key/value pairs
     * @param throwException specifies weather to throw exception when a key/value is missing, or just return false.
     *
     * @return TRUE, if all required keys are present in the params list, or both params and required keys are empty,
     *         otherwise returns FALSE (if throwException is false)
     *
     * @throws IllegalArgumentException If a key/value is missing and throwException is set to TRUE
     */
    protected boolean hasRequiredMetadata(AVList params, ErrorHandlerMode throwException)
        throws IllegalArgumentException
    {
        String[] keys = this.getRequiredKeysList();

        if (null == params || params.getEntries().size() == 0)
        {
            // return TRUE if required keys is empty, otherwise return FALSE
            return (null == keys || keys.length == 0);
        }

        if (null != keys && keys.length > 0)
        {
            for (String key : keys)
            {
                Object value = params.getValue(key);
                if (WWUtil.isEmpty(value))
                {
                    if (throwException == ErrorHandlerMode.ALLOW_EXCEPTIONS)
                    {
                        String message = Logging.getMessage("generic.MissingRequiredParameter", key);
                        log.debug(message);
                        throw new IllegalArgumentException(message);
                    }
                    else
                        return false;
                }
            }
        }

        return true;
    }

    public int getWidth()
    {
        Object o = this.getValue(AVKey.WIDTH);
        if (null != o && o instanceof Integer)
            return (Integer) o;
        throw new WWRuntimeException(Logging.getMessage("generic.MissingRequiredParameter", AVKey.WIDTH));
    }

    public int getHeight()
    {
        Object o = this.getValue(AVKey.HEIGHT);
        if (null != o && o instanceof Integer)
            return (Integer) o;
        throw new WWRuntimeException(Logging.getMessage("generic.MissingRequiredParameter", AVKey.HEIGHT));
    }

    public Sector getSector()
    {
        Object o = this.getValue(AVKey.SECTOR);
        if (null != o && o instanceof Sector)
            return (Sector) o;
        throw new WWRuntimeException(Logging.getMessage("generic.MissingRequiredParameter", AVKey.SECTOR));
    }

    public Object getDataSource()
    {
        return this.dataSource;
    }

    public AVList getParams()
    {
        return this.getMetadata();
    }

    public AVList getMetadata()
    {
        return this.copy();
    }

    public DataRasterReader getDataRasterReader()
    {
        return this.dataReader;
    }

    public void dispose()
    {
        String message = Logging.getMessage("generic.ExceptionWhileDisposing", this.dataSource);
        log.error(message);
        throw new IllegalStateException(message);
    }

    protected DataRaster[] getDataRasters() throws IOException, WWRuntimeException
    {
        synchronized (this.rasterRetrievalLock)
        {
            DataRaster[] rasters = (this.rasterCache != null)
                ? (DataRaster[]) this.rasterCache.getObject(this.dataSource) : null;

            if (null != rasters)
                return rasters;

            // prevent an attempt to re-read rasters which failed to load
            if (this.rasterCache == null || !this.rasterCache.contains(this.dataSource))
            {
                long memoryDelta = 0L;

                try
                {
                    AVList rasterParams = this.copy();

                    try
                    {
                        long before = getTotalUsedMemory();
                        rasters = this.dataReader.read(this.getDataSource(), rasterParams);
                        memoryDelta = getTotalUsedMemory() - before;
                    }
                    catch (OutOfMemoryError e)
                    {
                        log.debug(this.composeExceptionReason(e));
                        this.releaseMemory();
                        // let's retry after the finalization and GC

                        long before = getTotalUsedMemory();
                        rasters = this.dataReader.read(this.getDataSource(), rasterParams);
                        memoryDelta = getTotalUsedMemory() - before;
                    }
                }
                catch (Throwable t)
                {
                    disposeRasters(rasters); // cleanup in case of exception
                    rasters = null;
                    String message = Logging.getMessage("DataRaster.CannotRead", this.composeExceptionReason(t));
                    log.error(message);
                    throw new WWRuntimeException(message);
                }
                finally
                {
                    // Add rasters to the cache, even if "rasters" is null to prevent multiple failed reads.
                    if (this.rasterCache != null)
                    {
                        long totalBytes = getSizeInBytes(rasters);
                        totalBytes = (memoryDelta > totalBytes) ? memoryDelta : totalBytes;
                        if (totalBytes > 0L)
                            this.rasterCache.add(this.dataSource, rasters, totalBytes);
                    }
                }
            }

            if (null == rasters || rasters.length == 0)
            {
                String message = Logging.getMessage("generic.CannotCreateRaster", this.getDataSource());
                log.error(message);
                throw new WWRuntimeException(message);
            }

            return rasters;
        }
    }

    public void drawOnTo(DataRaster canvas)
    {
        synchronized (this.rasterUsageLock)
        {
            try
            {
                DataRaster[] rasters;
                try
                {
                    rasters = this.getDataRasters();
                    for (DataRaster raster : rasters)
                    {
                        raster.drawOnTo(canvas);
                    }
                }
                catch (OutOfMemoryError e)
                {
                    log.debug(this.composeExceptionReason(e));
                    this.releaseMemory();

                    rasters = this.getDataRasters();
                    for (DataRaster raster : rasters)
                    {
                        raster.drawOnTo(canvas);
                    }
                }
            }
            catch (Throwable t)
            {
                String reason = this.composeExceptionReason(t);
                log.error(reason, t);
            }
        }
    }

    public DataRaster getSubRaster(AVList params)
    {
        synchronized (this.rasterUsageLock)
        {
            try
            {
                DataRaster[] rasters;
                try
                {
                    rasters = this.getDataRasters();
                    return rasters[0].getSubRaster(params);
                }
                catch (OutOfMemoryError e)
                {
                    log.debug(this.composeExceptionReason(e));
                    this.releaseMemory();

                    // let's retry after the finalization and GC
                    rasters = this.getDataRasters();
                    return rasters[0].getSubRaster(params);
                }
            }
            catch (Throwable t)
            {
                String reason = this.composeExceptionReason(t);
                log.error(reason, t);
            }

            String message = Logging.getMessage("generic.CannotCreateRaster", this.getDataSource());
            log.error(message);
            throw new WWRuntimeException(message);
        }
    }

    public DataRaster getSubRaster(int width, int height, Sector sector, AVList params)
    {
        if (null == params)
            params = new AVListImpl();

        params.setValue(AVKey.WIDTH, width);
        params.setValue(AVKey.HEIGHT, height);
        params.setValue(AVKey.SECTOR, sector);

        return this.getSubRaster(params);
    }

    protected void releaseMemory()
    {
        if (this.rasterCache != null)
            this.rasterCache.clear();

        System.runFinalization();

        System.gc();

        Thread.yield();
    }

    protected String composeExceptionReason(Throwable t)
    {
        StringBuffer sb = new StringBuffer();

        if (null != this.dataSource)
            sb.append(this.dataSource).append(" : ");

        sb.append(WWUtil.extractExceptionReason(t));

        return sb.toString();
    }

    protected long getSizeInBytes(DataRaster[] rasters)
    {
        long totalBytes = 0L;

        if (rasters != null)
        {
            for (DataRaster raster : rasters)
            {
                if (raster != null && raster instanceof Cacheable)
                    totalBytes += ((Cacheable) raster).getSizeInBytes();
            }
        }

        return totalBytes;
    }

    protected static void disposeRasters(DataRaster[] rasters)
    {
        if (rasters != null)
        {
            for (DataRaster raster : rasters)
            {
                raster.dispose();
            }
        }
    }

    private static class CacheListener implements MemoryCache.CacheListener
    {
        private Object key;

        private CacheListener(Object key)
        {
            this.key = key;
        }

        public void entryRemoved(Object key, Object clientObject)
        {
            if (key != this.key)
                return;

            if (clientObject == null || !(clientObject instanceof DataRaster[]))
            {
                String message = MessageFormat.format("Cannot dispose {0}", clientObject);
                log.warn(message);
                return;
            }

            try
            {
                disposeRasters((DataRaster[]) clientObject);
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("generic.ExceptionWhileDisposing", clientObject);
                log.error(message, e);
            }
        }

        public void removalException(Throwable t, Object key, Object clientObject)
        {
            String reason = t.getMessage();
            reason = (WWUtil.isEmpty(reason) && null != t.getCause()) ? t.getCause().getMessage() : reason;
            String msg = Logging.getMessage("BasicMemoryCache.ExceptionFromRemovalListener", reason);
            log.info(msg);
        }
    }

    protected static long getTotalUsedMemory()
    {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory());
    }
}
