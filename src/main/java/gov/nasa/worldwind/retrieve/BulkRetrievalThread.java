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

package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.event.BulkRetrievalEvent;
import gov.nasa.worldwind.event.BulkRetrievalListener;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import javax.swing.event.EventListenerList;

/**
 * Retrieves data for a {@link BulkRetrievable}.
 *
 * @author Patrick Murris
 * @version $Id: BulkRetrievalThread.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public abstract class BulkRetrievalThread extends Thread
{
    protected int RETRIEVAL_SERVICE_POLL_DELAY = 1000;

    protected final BulkRetrievable retrievable;
    protected final Sector sector;
    protected final double resolution;
    protected final Progress progress;
    protected final FileStore fileStore;
    protected EventListenerList retrievalListeners = new EventListenerList();

    /**
     * Construct a thread that attempts to download to a specified {@link FileStore} a retrievable's data for a given
     * {@link Sector} and resolution.
     * <p>
     * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create
     * a downloader that has not been started, construct a {@link gov.nasa.worldwind.terrain.BasicElevationModelBulkDownloader}.
     * <p>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param retrievable the retrievable to retrieve data for.
     * @param sector      the sector of interest.
     * @param resolution  the target resolution, provided in radians of latitude per texel.
     * @param fileStore   the file store to examine.
     * @param listener    an optional retrieval listener. May be null.
     *
     * @throws IllegalArgumentException if either the retrievable, sector or file store are null, or the resolution is
     *                                  less than or equal to zero.
     */
    public BulkRetrievalThread(BulkRetrievable retrievable, Sector sector, double resolution, FileStore fileStore,
        BulkRetrievalListener listener)
    {
        if (retrievable == null)
        {
            String msg = Logging.getMessage("nullValue.RetrievableIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (fileStore == null)
        {
            String msg = Logging.getMessage("nullValue.FileStoreIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
//
//        if (resolution <= 0)
//        {
//            String msg = Logging.getMessage("generic.ResolutionInvalid", resolution);
//            log.error(msg);
//            throw new IllegalArgumentException(msg);
//        }

        this.retrievable = retrievable;
        this.sector = sector;
        this.resolution = resolution;
        this.fileStore = fileStore;
        this.progress = new Progress();

        if (listener != null)
            this.addRetrievalListener(listener);
    }

    public abstract void run();

    /**
     * Get the {@link BulkRetrievable} instance for which this thread acts.
     *
     * @return the {@link BulkRetrievable} instance.
     */
    public BulkRetrievable getRetrievable()
    {
        return this.retrievable;
    }

    /**
     * Get the requested {@link Sector}.
     *
     * @return the requested {@link Sector}.
     */
    public Sector getSector()
    {
        return this.sector;
    }

    /**
     * Get the requested resolution.
     *
     * @return the requested resolution.
     */
    public double getResolution()
    {
        return this.resolution;
    }

    /**
     * Get the file store.
     *
     * @return the file store associated with this downloader.
     */
    public FileStore getFileStore()
    {
        return fileStore;
    }

    /**
     * Get a {@link Progress} instance providing information about this task progress.
     *
     * @return a {@link Progress} instance providing information about this task progress.
     */
    public Progress getProgress()
    {
        return this.progress;
    }

    public void addRetrievalListener(BulkRetrievalListener listener)
    {
        if (listener != null)
            this.retrievalListeners.add(BulkRetrievalListener.class, listener);
    }

    public void removeRetrievalListener(BulkRetrievalListener listener)
    {
        if (listener != null)
            this.retrievalListeners.remove(BulkRetrievalListener.class, listener);
    }

    protected boolean hasRetrievalListeners()
    {
        return this.retrievalListeners.getListenerCount() > 0;
    }

    protected void callRetrievalListeners(BulkRetrievalEvent event)
    {
        for (BulkRetrievalListener listener : this.retrievalListeners.getListeners(BulkRetrievalListener.class))
        {
            listener.eventOccurred(event);
        }
    }
}
