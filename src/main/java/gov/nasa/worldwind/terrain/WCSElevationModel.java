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

package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.gml.GMLRectifiedGrid;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capabilities;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100DescribeCoverage;
import gov.nasa.worldwind.retrieve.HTTPRetriever;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.util.TileUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * @author tag
 * @version $Id: WCSElevationModel.java 2154 2014-07-17 21:32:34Z pabercrombie $
 */
@Slf4j
public class WCSElevationModel extends BasicElevationModel
{
    public WCSElevationModel(Element domElement, AVList params)
    {
        super(wcsGetParamsFromDocument(domElement, params));
    }

    public WCSElevationModel(WCS100Capabilities caps, AVList params)
    {
        super(wcsGetParamsFromCapsDoc(caps, params));
    }

    /**
     * Create a new elevation model from a serialized restorable state string.
     *
     * @param restorableStateInXml XML string in WorldWind restorable state format.
     *
     * @see #getRestorableState()
     */
    public WCSElevationModel(String restorableStateInXml)
    {
        super(wcsRestorableStateToParams(restorableStateInXml));

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(restorableStateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", restorableStateInXml);
            log.error(message);
            throw new IllegalArgumentException(message, e);
        }

        this.doRestoreState(rs, null);
    }

    protected static AVList wcsGetParamsFromDocument(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        DataConfigurationUtils.getWCSConfigParams(domElement, params);
        BasicElevationModel.getBasicElevationModelConfigParams(domElement, params);
        wcsSetFallbacks(params);

        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(params.getStringValue(AVKey.WCS_VERSION), params));

        return params;
    }

    protected static AVList wcsGetParamsFromCapsDoc(WCS100Capabilities caps, AVList params)
    {
        if (caps == null)
        {
            String message = Logging.getMessage("nullValue.WCSCapabilities");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ElevationModelConfigParams");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        WCS100DescribeCoverage coverage = (WCS100DescribeCoverage) params.getValue(AVKey.DOCUMENT);
        if (coverage == null)
        {
            String message = Logging.getMessage("nullValue.WCSDescribeCoverage");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        getWCSElevationModelConfigParams(caps, coverage, params);

        wcsSetFallbacks(params);
        determineNumLevels(coverage, params);

        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(caps.getVersion(), params));

        if (params.getValue(AVKey.ELEVATION_EXTREMES_FILE) == null)
        {
            // Use the default extremes file if there are at least as many levels in this new elevation model as the
            // level of the extremes file, which is level 5.
            int numLevels = (Integer) params.getValue(AVKey.NUM_LEVELS);
            if (numLevels >= 6)
                params.setValue(AVKey.ELEVATION_EXTREMES_FILE, "config/SRTM30Plus_ExtremeElevations_5.bil");
        }

        return params;
    }

    protected static void wcsSetFallbacks(AVList params)
    {
        if (params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA) == null)
        {
            Angle delta = Angle.fromDegrees(20);
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
        }

        if (params.getValue(AVKey.TILE_WIDTH) == null)
            params.setValue(AVKey.TILE_WIDTH, 150);

        if (params.getValue(AVKey.TILE_HEIGHT) == null)
            params.setValue(AVKey.TILE_HEIGHT, 150);

        if (params.getValue(AVKey.FORMAT_SUFFIX) == null)
            params.setValue(AVKey.FORMAT_SUFFIX, ".tif");

        if (params.getValue(AVKey.MISSING_DATA_SIGNAL) == null)
            params.setValue(AVKey.MISSING_DATA_SIGNAL, -9999d);

        if (params.getValue(AVKey.NUM_LEVELS) == null)
            params.setValue(AVKey.NUM_LEVELS, 18); // approximately 20 cm per pixel

        if (params.getValue(AVKey.NUM_EMPTY_LEVELS) == null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);

        if (params.getValue(AVKey.ELEVATION_MIN) == null)
            params.setValue(AVKey.ELEVATION_MIN, -11000.0);

        if (params.getValue(AVKey.ELEVATION_MAX) == null)
            params.setValue(AVKey.ELEVATION_MAX, 8850.0);
    }

    protected static void determineNumLevels(WCS100DescribeCoverage coverage, AVList params)
    {
        List<GMLRectifiedGrid> grids =
            coverage.getCoverageOfferings().get(0).getDomainSet().getSpatialDomain().getRectifiedGrids();
        if (grids.size() < 1 || grids.get(0).getOffsetVectors().size() < 2)
        {
            params.setValue(AVKey.NUM_LEVELS, 18);
            return;
        }

        double xRes = Math.abs(grids.get(0).getOffsetVectors().get(0).x);
        double yRes = Math.abs(grids.get(0).getOffsetVectors().get(1).y);
        double dataResolution = Math.min(xRes, yRes);

        int tileSize = (Integer) params.getValue(AVKey.TILE_WIDTH);
        LatLon level0Delta = (LatLon) params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA);

        double n = Math.log(level0Delta.getLatitude().degrees / (dataResolution * tileSize)) / Math.log(2);
        params.setValue(AVKey.NUM_LEVELS, (int) (Math.ceil(n) + 1));
    }

    public static AVList getWCSElevationModelConfigParams(WCS100Capabilities caps, WCS100DescribeCoverage coverage,
        AVList params)
    {
        DataConfigurationUtils.getWCSConfigParameters(caps, coverage, params); // checks for null args

        // Ensure that we found all the necessary information.
        if (params.getStringValue(AVKey.DATASET_NAME) == null)
        {
            log.warn(Logging.getMessage("WCS.NoCoverageName"));
            throw new WWRuntimeException(Logging.getMessage("WCS.NoCoverageName"));
        }

        if (params.getStringValue(AVKey.SERVICE) == null)
        {
            log.warn(Logging.getMessage("WCS.NoGetCoverageURL"));
            throw new WWRuntimeException(Logging.getMessage("WCS.NoGetCoverageURL"));
        }

        if (params.getStringValue(AVKey.DATA_CACHE_NAME) == null)
        {
            log.warn(Logging.getMessage("nullValue.DataCacheIsNull"));
            throw new WWRuntimeException(Logging.getMessage("nullValue.DataCacheIsNull"));
        }

        if (params.getStringValue(AVKey.IMAGE_FORMAT) == null)
        {
            log.error("WCS.NoImageFormats");
            throw new WWRuntimeException(Logging.getMessage("WCS.NoImageFormats"));
        }

        if (params.getValue(AVKey.SECTOR) == null)
        {
            log.error("WCS.NoLonLatEnvelope");
            throw new WWRuntimeException(Logging.getMessage("WCS.NoLonLatEnvelope"));
        }

        if (params.getStringValue(AVKey.COORDINATE_SYSTEM) == null)
        {
            String msg = Logging.getMessage("WCS.RequiredCRSNotSupported", "EPSG:4326");
            log.error(msg);
            throw new WWRuntimeException(msg);
        }

        return params;
    }

    protected static class URLBuilder implements TileUrlBuilder
    {
        protected final String layerNames;
        private final String imageFormat;
        protected final String serviceVersion;
        protected String URLTemplate = null;

        protected URLBuilder(String version, AVList params)
        {
            this.serviceVersion = version;
            this.layerNames = params.getStringValue(AVKey.COVERAGE_IDENTIFIERS);
            this.imageFormat = params.getStringValue(AVKey.IMAGE_FORMAT);
        }

        public URL getURL(gov.nasa.worldwind.util.Tile tile, String altImageFormat) throws MalformedURLException
        {
            StringBuffer sb;
            if (this.URLTemplate == null)
            {
                sb = new StringBuffer(tile.getLevel().getService());

                if (!sb.toString().toLowerCase().contains("service=wcs"))
                    sb.append("service=WCS");
                sb.append("&request=GetCoverage");
                sb.append("&version=");
                sb.append(this.serviceVersion);
                sb.append("&crs=EPSG:4326");
                sb.append("&coverage=");
                sb.append(this.layerNames);
                sb.append("&format=");
                if (altImageFormat == null)
                    sb.append(this.imageFormat);
                else
                    sb.append(altImageFormat);

                this.URLTemplate = sb.toString();
            }
            else
            {
                sb = new StringBuffer(this.URLTemplate);
            }

            sb.append("&width=");
            sb.append(tile.getWidth());
            sb.append("&height=");
            sb.append(tile.getHeight());

            Sector s = tile.getSector();
            sb.append("&bbox=");
            sb.append(s.getMinLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMinLatitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLatitude().getDegrees());

            sb.append("&"); // terminate the query string

            return new java.net.URL(sb.toString().replace(" ", "%20"));
        }
    }

    /**
     * Appends WCS elevation model configuration elements to the superclass configuration document.
     *
     * @param params configuration parameters describing this WCS basic elevation model.
     *
     * @return a WCS basic elevation model configuration document.
     */
    protected Document createConfigurationDocument(AVList params)
    {
        Document doc = super.createConfigurationDocument(params);
        if (doc == null || doc.getDocumentElement() == null)
            return doc;

        DataConfigurationUtils.createWCSLayerConfigElements(params, doc.getDocumentElement());

        return doc;
    }

    public void composeElevations(Sector sector, List<? extends LatLon> latlons, int tileWidth, double[] buffer)
        throws Exception
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (latlons == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonListIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationsBufferIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer.length < latlons.size() || tileWidth > latlons.size())
        {
            String msg = Logging.getMessage("ElevationModel.ElevationsBufferTooSmall", latlons.size());
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        WMSBasicElevationModel.ElevationCompositionTile tile = new WMSBasicElevationModel.ElevationCompositionTile(
            sector, this.getLevels().getLastLevel(),
            tileWidth, latlons.size() / tileWidth);

        this.downloadElevations(tile);
        tile.setElevations(this.readElevations(tile.getFile().toURI().toURL()), this);

        for (int i = 0; i < latlons.size(); i++)
        {
            LatLon ll = latlons.get(i);
            if (ll == null)
                continue;

            double value = this.lookupElevation(ll.getLatitude(), ll.getLongitude(), tile);

            // If an elevation at the given location is available, then write that elevation to the destination buffer.
            // Otherwise do nothing.
            if (value != this.getMissingDataSignal())
                buffer[i] = value;
        }
    }

    protected void downloadElevations(WMSBasicElevationModel.ElevationCompositionTile tile) throws Exception
    {
        URL url = tile.getResourceURL();

        Retriever retriever = new HTTPRetriever(url,
            new WMSBasicElevationModel.CompositionRetrievalPostProcessor(tile.getFile()));
        retriever.setConnectTimeout(10000);
        retriever.setReadTimeout(60000);
        retriever.call();
    }

    //**************************************************************//
    //********************  Restorable Support  ********************//
    //**************************************************************//

    @Override
    public void getRestorableStateForAVPair(String key, Object value,
        RestorableSupport rs, RestorableSupport.StateObject context)
    {
        if (value instanceof URLBuilder)
        {
            rs.addStateValueAsString(context, AVKey.WCS_VERSION, ((URLBuilder) value).serviceVersion);
        }
        else if (!(value instanceof WCS100DescribeCoverage))
        {
            // Don't pass DescribeCoverage to superclass. The DescribeCoverage parameters will already be present in the
            // parameter list, so do nothing here.
            super.getRestorableStateForAVPair(key, value, rs, context);
        }
    }

    protected static AVList wcsRestorableStateToParams(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            log.error(message);
            throw new IllegalArgumentException(message, e);
        }

        AVList params = new AVListImpl();
        wcsRestoreStateForParams(rs, null, params);
        return params;
    }

    protected static void wcsRestoreStateForParams(RestorableSupport rs, RestorableSupport.StateObject context,
        AVList params)
    {
        // Invoke the BasicElevationModel functionality.
        restoreStateForParams(rs, null, params);

        String s = rs.getStateValueAsString(context, AVKey.IMAGE_FORMAT);
        if (s != null)
            params.setValue(AVKey.IMAGE_FORMAT, s);

        s = rs.getStateValueAsString(context, AVKey.TITLE);
        if (s != null)
            params.setValue(AVKey.TITLE, s);

        s = rs.getStateValueAsString(context, AVKey.DISPLAY_NAME);
        if (s != null)
            params.setValue(AVKey.DISPLAY_NAME, s);

        RestorableSupport.adjustTitleAndDisplayName(params);

        s = rs.getStateValueAsString(context, AVKey.COVERAGE_IDENTIFIERS);
        if (s != null)
            params.setValue(AVKey.COVERAGE_IDENTIFIERS, s);

        s = rs.getStateValueAsString(context, AVKey.WCS_VERSION);
        if (s != null)
            params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(s, params));
    }
}
