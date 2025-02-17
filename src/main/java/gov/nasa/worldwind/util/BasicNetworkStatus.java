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

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Basic implementation of NetworkStatus.
 *
 * @author tag
 * @version $Id: BasicNetworkStatus.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class BasicNetworkStatus extends AVListImpl implements NetworkStatus
{
    protected static final long DEFAULT_TRY_AGAIN_INTERVAL = (long) 60e3; // seconds
    protected static final int DEFAULT_ATTEMPT_LIMIT = 7; // number of unavailable events to declare host unavailable
    protected static final long NETWORK_STATUS_REPORT_INTERVAL = (long) 120e3;
    protected static final String[] DEFAULT_NETWORK_TEST_SITES = new String[]
        {"www.nasa.gov", "worldwind.arc.nasa.gov", "google.com", "microsoft.com", "yahoo.com"};

    protected static class HostInfo
    {
        protected final long tryAgainInterval;
        protected final int attemptLimit;
        protected AtomicInteger logCount = new AtomicInteger();
        protected AtomicLong lastLogTime = new AtomicLong();

        protected HostInfo(int attemptLimit, long tryAgainInterval)
        {
            this.lastLogTime.set(System.currentTimeMillis());
            this.logCount.set(1);
            this.tryAgainInterval = tryAgainInterval;
            this.attemptLimit = attemptLimit;
        }

        protected boolean isUnavailable()
        {
            return this.logCount.get() >= this.attemptLimit;
        }

        protected boolean isTimeToTryAgain()
        {
            return System.currentTimeMillis() - this.lastLogTime.get() >= this.tryAgainInterval;
        }
    }

    // Values exposed to the application.
    private CopyOnWriteArrayList<String> networkTestSites = new CopyOnWriteArrayList<String>();
    private AtomicLong tryAgainInterval = new AtomicLong(DEFAULT_TRY_AGAIN_INTERVAL);
    private AtomicInteger attemptLimit = new AtomicInteger(DEFAULT_ATTEMPT_LIMIT);
    private boolean offlineMode;

    // Fields for determining and remembering overall network status.
    protected ConcurrentHashMap<String, HostInfo> hostMap = new ConcurrentHashMap<String, HostInfo>();
    protected AtomicLong lastUnavailableLogTime = new AtomicLong(System.currentTimeMillis());
    protected AtomicLong lastAvailableLogTime = new AtomicLong(System.currentTimeMillis() + 1);
    protected AtomicLong lastNetworkCheckTime = new AtomicLong(System.currentTimeMillis());
    protected AtomicLong lastNetworkStatusReportTime = new AtomicLong(0);
    protected AtomicBoolean lastNetworkUnavailableResult = new AtomicBoolean(false);

    public BasicNetworkStatus()
    {
        String oms = Configuration.getStringValue(AVKey.OFFLINE_MODE, "false");
        this.offlineMode = oms.startsWith("t") || oms.startsWith("T");

        this.establishNetworkTestSites();
    }

    /**
     * Determines and stores the network sites to test for public network connectivity. The sites are drawn from the
     * JVM's gov.nasa.worldwind.avkey.NetworkStatusTestSites property ({@link AVKey#NETWORK_STATUS_TEST_SITES}). If that
     * property is not defined, the sites are drawn from the same property in the WorldWind or application
     * configuration file. If the sites are not specified there, the set of sites specified in {@link
     * #DEFAULT_NETWORK_TEST_SITES} are used. To indicate an empty list in the JVM property or configuration file
     * property, specify an empty site list, "".
     */
    protected void establishNetworkTestSites()
    {
        String testSites = System.getProperty(AVKey.NETWORK_STATUS_TEST_SITES);

        if (testSites == null)
            testSites = Configuration.getStringValue(AVKey.NETWORK_STATUS_TEST_SITES);

        if (testSites == null)
        {
            this.networkTestSites.addAll(Arrays.asList(DEFAULT_NETWORK_TEST_SITES));
        }
        else
        {
            String[] sites = testSites.split(",");
            List<String> actualSites = new ArrayList<String>(sites.length);

            for (int i = 0; i < sites.length; i++)
            {
                String site = WWUtil.removeWhiteSpace(sites[i]);
                if (!WWUtil.isEmpty(site))
                    actualSites.add(site);
            }

            this.setNetworkTestSites(actualSites);
        }
    }

    /** {@inheritDoc} */
    public boolean isOfflineMode()
    {
        return offlineMode;
    }

    /** {@inheritDoc} */
    public void setOfflineMode(boolean offlineMode)
    {
        this.offlineMode = offlineMode;
    }

    /** {@inheritDoc} */
    public void setAttemptLimit(int limit)
    {
        if (limit < 1)
        {
            String message = Logging.getMessage("NetworkStatus.InvalidAttemptLimit");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.attemptLimit.set(limit);
    }

    /** {@inheritDoc} */
    public void setTryAgainInterval(long interval)
    {
        if (interval < 0)
        {
            String message = Logging.getMessage("NetworkStatus.InvalidTryAgainInterval");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.tryAgainInterval.set(interval);
    }

    /** {@inheritDoc} */
    public int getAttemptLimit()
    {
        return this.attemptLimit.get();
    }

    /** {@inheritDoc} */
    public long getTryAgainInterval()
    {
        return this.tryAgainInterval.get();
    }

    /** {@inheritDoc} */
    public List<String> getNetworkTestSites()
    {
        return new ArrayList<String>(networkTestSites);
    }

    /** {@inheritDoc} */
    public void setNetworkTestSites(List<String> networkTestSites)
    {
        this.networkTestSites.clear();

        if (networkTestSites != null)
            this.networkTestSites.addAll(networkTestSites);
    }

    /** {@inheritDoc} */
    public synchronized void logUnavailableHost(URL url)
    {
        if (this.offlineMode)
            return;

        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi != null)
        {
            if (!hi.isUnavailable())
            {
                hi.logCount.incrementAndGet();
                if (hi.isUnavailable()) // host just became unavailable
                    this.firePropertyChange(NetworkStatus.HOST_UNAVAILABLE, null, url);
            }
            hi.lastLogTime.set(System.currentTimeMillis());
        }
        else
        {
            hi = new HostInfo(this.attemptLimit.get(), this.tryAgainInterval.get());
            hi.logCount.set(1);
            if (hi.isUnavailable()) // the attempt limit may be as low as 1, so handle that case here
                this.firePropertyChange(NetworkStatus.HOST_UNAVAILABLE, null, url);
            this.hostMap.put(hostName, hi);
        }

        this.lastUnavailableLogTime.set(System.currentTimeMillis());
    }

    /** {@inheritDoc} */
    public synchronized void logAvailableHost(URL url)
    {
        if (this.offlineMode)
            return;

        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi != null)
        {
            this.hostMap.remove(hostName); // host is available again
            firePropertyChange(NetworkStatus.HOST_AVAILABLE, null, url);
        }

        this.lastAvailableLogTime.set(System.currentTimeMillis());
    }

    /** {@inheritDoc} */
    public synchronized boolean isHostUnavailable(URL url)
    {
        if (this.offlineMode)
            return true;

        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi == null)
            return false;

        if (hi.isTimeToTryAgain())
        {
            hi.logCount.set(0); // info removed from table in logAvailableHost
            return false;
        }

        return hi.isUnavailable();
    }

    /** {@inheritDoc} */
    public boolean isNetworkUnavailable()
    {
        return this.offlineMode || this.isNetworkUnavailable(10000L);
    }

    /** {@inheritDoc} */
    public synchronized boolean isNetworkUnavailable(long checkInterval)
    {
        if (this.offlineMode)
            return true;

        // If there's been success since failure, network assumed to be reachable.
        if (this.lastAvailableLogTime.get() > this.lastUnavailableLogTime.get())
        {
            this.lastNetworkUnavailableResult.set(false);
            return this.lastNetworkUnavailableResult.get();
        }

        long now = System.currentTimeMillis();

        // If there's been success recently, network assumed to be reachable.
        if (!this.lastNetworkUnavailableResult.get() && now - this.lastAvailableLogTime.get() < checkInterval)
        {
            return this.lastNetworkUnavailableResult.get();
        }

        // If query comes too soon after an earlier one that addressed the network, return the earlier result.
        if (now - this.lastNetworkCheckTime.get() < checkInterval)
        {
            return this.lastNetworkUnavailableResult.get();
        }

        this.lastNetworkCheckTime.set(now);

        if (!this.isWorldWindServerUnavailable())
        {
            this.lastNetworkUnavailableResult.set(false); // network not unreachable
            return this.lastNetworkUnavailableResult.get();
        }

        for (String testHost : networkTestSites)
        {
            if (isHostReachable(testHost))
            {
                {
                    this.lastNetworkUnavailableResult.set(false); // network not unreachable
                    return this.lastNetworkUnavailableResult.get();
                }
            }
        }

        if (now - this.lastNetworkStatusReportTime.get() > NETWORK_STATUS_REPORT_INTERVAL)
        {
            this.lastNetworkStatusReportTime.set(now);
            String message = Logging.getMessage("NetworkStatus.NetworkUnreachable");
            log.info(message);
        }

        this.lastNetworkUnavailableResult.set(true); // if no successful contact then network is unreachable
        return this.lastNetworkUnavailableResult.get();
    }

    /** {@inheritDoc} */
    public boolean isWorldWindServerUnavailable()
    {
        return this.offlineMode || !isHostReachable("worldwind.arc.nasa.gov");
    }

    /**
     * Determine if a host is reachable by attempting to resolve the host name, and then attempting to open a
     * connection using either https or http.
     *
     * @param hostName Name of the host to connect to.
     *
     * @return {@code true} if a the host is reachable, {@code false} if the host name cannot be resolved, or if opening
     *         a connection to the host fails.
     */
    protected static boolean isHostReachable(String hostName)
    {
        try
        {
            // Assume host is unreachable if we can't get its dns entry without getting an exception
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(hostName);
        }
        catch (UnknownHostException e)
        {
            String message = Logging.getMessage("NetworkStatus.UnreachableTestHost", hostName);
            log.debug(message);
            return false;
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("NetworkStatus.ExceptionTestingHost", hostName);
            log.info(message);
            return false;
        }

        // Was able to get internet address, but host still might not be reachable because the address might have been
        // cached earlier when it was available. So need to try something else.

        URLConnection connection = null;
        try
        {
            final String[] protocols = new String[] {"https://", "http://"};
            for (String protocol: protocols)
            {
                URL url = new URL(protocol + hostName);

                Proxy proxy = WWIO.configureProxy();
                if (proxy != null)
                    connection = url.openConnection(proxy);
                else
                    connection = url.openConnection();

                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
                String ct = connection.getContentType();
                if (ct != null)
                    return true;
            }
        }
        catch (IOException e)
        {
            String message = Logging.getMessage("NetworkStatus.ExceptionTestingHost", hostName);
            log.info(message);
        }
        finally
        {
            if (connection != null && connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
        }

        return false;
    }
}
