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

package gov.nasa.worldwindx.applications.worldwindow.features;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.data.DataStoreProducer;
import gov.nasa.worldwind.data.TiledElevationProducer;
import gov.nasa.worldwind.data.TiledImageProducer;
import gov.nasa.worldwind.data.WWDotNetLayerSetConverter;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwindx.applications.worldwindow.core.Constants;
import gov.nasa.worldwindx.applications.worldwindow.core.Controller;
import gov.nasa.worldwindx.applications.worldwindow.core.Registry;
import gov.nasa.worldwindx.applications.worldwindow.core.WWMenu;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.xml.xpath.XPath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dcollins
 * @version $Id: ImportedDataDialog.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class ImportedDataDialog extends AbstractFeatureDialog implements NetworkActivitySignal.NetworkUser
{
    protected FileStore fileStore;
    protected ImportedDataPanel dataConfigPanel;
    protected Thread importThread;

    public ImportedDataDialog(Registry registry)
    {
        super("Import Imagery and Elevations...", Constants.FEATURE_IMPORT_IMAGERY, null, registry);
    }

    @Override
    public void initialize(Controller controller)
    {
        super.initialize(controller);

        this.dialog = this.getJDialog();

        WWMenu fileMenu = (WWMenu) this.getController().getRegisteredObject(Constants.FILE_MENU);
        if (fileMenu != null)
            fileMenu.addMenu(this.getFeatureID());

        this.fileStore = WorldWind.getDataFileStore();

        this.layoutComponents();
        this.loadPreviouslyImportedData();
    }

    public boolean hasNetworkActivity()
    {
        return this.importThread != null && this.importThread.isAlive();
    }

    @Override
    public boolean isTwoState()
    {
        return true;
    }

    @Override
    public boolean isOn()
    {
        return this.dialog != null && this.dialog.isVisible();
    }

    protected void loadPreviouslyImportedData()
    {
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                loadImportedDataFromFileStore(fileStore, dataConfigPanel);
            }
        });
        t.start();
    }

    protected void importFromFile()
    {
        JFileChooser fc = this.getController().getFileChooser();

        fc.setDialogTitle("Import File");
        fc.setMultiSelectionEnabled(false);
        ImportableDataFilter filter = new ImportableDataFilter();
        fc.addChoosableFileFilter(filter);

        int retVal = fc.showDialog(this.getController().getFrame(), "Import");

        if (retVal != JFileChooser.APPROVE_OPTION)
            return;

        final File file = fc.getSelectedFile();
        if (file == null) // This should never happen, but we check anyway.
            return;

        fc.removeChoosableFileFilter(filter);
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("");

        this.importThread = new Thread(new Runnable()
        {
            public void run()
            {
                getController().getNetworkActivitySignal().addNetworkUser(ImportedDataDialog.this);

                try
                {
                    Document dataConfig = null;

                    try
                    {
                        // Import the file into a form usable by WorldWind components.
                        dataConfig = importDataFromFile(ImportedDataDialog.this.dialog, file, fileStore);
                    }
                    catch (Exception e)
                    {
                        final String message = e.getMessage();

                        // Show a message dialog indicating that the import failed, and why.
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                JOptionPane.showMessageDialog(ImportedDataDialog.this.dialog, message, "Import Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }

                    if (dataConfig != null)
                    {
                        addImportedData(dataConfig, null, dataConfigPanel);
                    }
                }
                finally
                {
                    controller.getNetworkActivitySignal().removeNetworkUser(ImportedDataDialog.this);
                }
            }
        });

        this.importThread.start();
    }

    protected void layoutComponents()
    {
        this.setTitle("Import Data");

        this.dataConfigPanel = new ImportedDataPanel("Currently Imported Data", this.getController());

        this.setTaskComponent(this.dataConfigPanel);
        this.setLocation(SwingConstants.CENTER, SwingConstants.CENTER);
        this.getJDialog().setResizable(true);

        JButton importButton = new JButton("Import...");
        importButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                importFromFile();
            }
        });
        this.insertLeftDialogComponent(importButton);

        this.dialog.setPreferredSize(new Dimension(400, 400));
        this.dialog.validate();
        this.dialog.pack();
    }

    protected static void addImportedData(final Document dataConfig, final AVList params,
        final ImportedDataPanel panel)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addImportedData(dataConfig, params, panel);
                }
            });
        }
        else
        {
            panel.addImportedData(dataConfig.getDocumentElement(), params);
        }
    }

    //**************************************************************//
    //********************  Loading Previously Imported Data  ******//
    //**************************************************************//

    protected static void loadImportedDataFromDirectory(File dir, ImportedDataPanel panel)
    {
        String[] names = WWIO.listDescendantFilenames(dir, new DataConfigurationFilter(), false);
        if (names == null || names.length == 0)
            return;

        for (String filename : names)
        {
            Document doc = null;

            try
            {
                File dataConfigFile = new File(dir, filename);
                doc = WWXML.openDocument(dataConfigFile);
                doc = DataConfigurationUtils.convertToStandardDataConfigDocument(doc);
            }
            catch (WWRuntimeException e)
            {
                e.printStackTrace();
            }

            if (doc == null)
                continue;

            // This data configuration came from an existing file from disk, therefore we cannot guarantee that the
            // current version of WorldWind's data importers produced it. This data configuration file may have been
            // created by a previous version of WorldWind, or by another program. Set fallback values for any missing
            // parameters that WorldWind needs to construct a Layer or ElevationModel from this data configuration.
            AVList params = new AVListImpl();
            setFallbackParams(doc, filename, params);

            // Add the data configuraiton to the ImportedDataPanel.
            addImportedData(doc, params, panel);
        }
    }

    protected static void loadImportedDataFromFileStore(FileStore fileStore, ImportedDataPanel panel)
    {
        for (File file : fileStore.getLocations())
        {
            if (!file.exists())
                continue;

            if (!fileStore.isInstallLocation(file.getPath()))
                continue;

            loadImportedDataFromDirectory(file, panel);
        }
    }

    protected static void setFallbackParams(Document dataConfig, String filename, AVList params)
    {
        XPath xpath = WWXML.makeXPath();
        Element domElement = dataConfig.getDocumentElement();

        // If the data configuration document doesn't define a cache name, then compute one using the file's path
        // relative to its file cache directory.
        String s = WWXML.getText(domElement, "DataCacheName", xpath);
        if (s == null || s.length() == 0)
            DataConfigurationUtils.getDataConfigCacheName(filename, params);

        // If the data configuration document doesn't define the data's extreme elevations, provide default values using
        // the minimum and maximum elevations of Earth.
        String type = DataConfigurationUtils.getDataConfigType(domElement);
        if (type.equalsIgnoreCase("ElevationModel"))
        {
            if (WWXML.getDouble(domElement, "ExtremeElevations/@min", xpath) == null)
                params.setValue(AVKey.ELEVATION_MIN, Earth.ELEVATION_MIN);
            if (WWXML.getDouble(domElement, "ExtremeElevations/@max", xpath) == null)
                params.setValue(AVKey.ELEVATION_MAX, Earth.ELEVATION_MAX);
        }
    }

    //**************************************************************//
    //********************  Importing Data From File  **************//
    //**************************************************************//

    protected static Document importDataFromFile(Component parentComponent, File file, FileStore fileStore)
        throws Exception
    {
        // Create a DataStoreProducer which is capable of processing the file.
        final DataStoreProducer producer = createDataStoreProducerFromFile(file);
        if (producer == null)
        {
            throw new IllegalArgumentException("Unrecognized file type");
        }

        // Create a ProgressMonitor that will provide feedback on how
        final ProgressMonitor progressMonitor = new ProgressMonitor(parentComponent,
            "Importing " + file.getName(), null, 0, 100);

        final AtomicInteger progress = new AtomicInteger(0);

        // Configure the ProgressMonitor to receive progress events from the DataStoreProducer. This stops sending
        // progress events when the user clicks the "Cancel" button, ensuring that the ProgressMonitor does not
        PropertyChangeListener progressListener = new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent evt)
            {
                if (progressMonitor.isCanceled())
                    return;

                if (evt.getPropertyName().equals(AVKey.PROGRESS))
                    progress.set((int) (100 * (Double) evt.getNewValue()));
            }
        };
        producer.addPropertyChangeListener(progressListener);
        progressMonitor.setProgress(0);

        // Configure a timer to check if the user has clicked the ProgressMonitor's "Cancel" button. If so, stop
        // production as soon as possible. This just stops the production from completing; it doesn't clean up any state
        // changes made during production,
        java.util.Timer progressTimer = new java.util.Timer();
        progressTimer.schedule(new TimerTask()
        {
            public void run()
            {
                progressMonitor.setProgress(progress.get());

                if (progressMonitor.isCanceled())
                {
                    producer.stopProduction();
                    this.cancel();
                }
            }
        }, progressMonitor.getMillisToDecideToPopup(), 100L);

        Document doc = null;
        try
        {
            // Import the file into the specified FileStore.
            doc = createDataStoreFromFile(file, fileStore, producer);

            // The user clicked the ProgressMonitor's "Cancel" button. Revert any change made during production, and
            // discard the returned DataConfiguration reference.
            if (progressMonitor.isCanceled())
            {
                doc = null;
                producer.removeProductionState();
            }
        }
        finally
        {
            // Remove the progress event listener from the DataStoreProducer. stop the progress timer, and signify to the
            // ProgressMonitor that we're done.
            producer.removePropertyChangeListener(progressListener);
            progressMonitor.close();
            progressTimer.cancel();
        }

        return doc;
    }

    protected static Document createDataStoreFromFile(File file, FileStore fileStore,
        DataStoreProducer producer) throws Exception
    {
        File importLocation = DataImportUtil.getDefaultImportLocation(fileStore);
        if (importLocation == null)
        {
            String message = Logging.getMessage("generic.NoDefaultImportLocation");
            log.error(message);
            return null;
        }

        // Create the production parameters. These parameters instruct the DataStoreProducer where to import the cached
        // data, and what name to put in the data configuration document.
        AVList params = new AVListImpl();
        params.setValue(AVKey.DATASET_NAME, file.getName());
        params.setValue(AVKey.DATA_CACHE_NAME, file.getName());
        params.setValue(AVKey.FILE_STORE_LOCATION, importLocation.getAbsolutePath());
        producer.setStoreParameters(params);

        // Use the specified file as the the production data source.
        producer.offerDataSource(file, null);

        try
        {
            // Convert the file to a form usable by WorldWind components, according to the specified DataStoreProducer.
            // This throws an exception if production fails for any reason.
            producer.startProduction();
        }
        catch (Exception e)
        {
            // Exception attempting to convert the file. Revert any change made during production.
            producer.removeProductionState();
            throw e;
        }

        // Return the DataConfiguration from the production results. Since production sucessfully completed, the
        // DataStoreProducer should contain a DataConfiguration in the production results. We test the production
        // results anyway.
        Iterable results = producer.getProductionResults();
        if (results != null && results.iterator() != null && results.iterator().hasNext())
        {
            Object o = results.iterator().next();
            if (o != null && o instanceof Document)
            {
                return (Document) o;
            }
        }

        return null;
    }

    //**************************************************************//
    //********************  Utility Methods  ***********************//
    //**************************************************************//

    protected static DataStoreProducer createDataStoreProducerFromFile(File file)
    {
        if (file == null)
            return null;

        DataStoreProducer producer = null;

        AVList params = new AVListImpl();
        if (DataImportUtil.isDataRaster(file, params))
        {
            if (AVKey.ELEVATION.equals(params.getStringValue(AVKey.PIXEL_FORMAT)))
                producer = new TiledElevationProducer();
            else if (AVKey.IMAGE.equals(params.getStringValue(AVKey.PIXEL_FORMAT)))
                producer = new TiledImageProducer();
        }
        else if (DataImportUtil.isWWDotNetLayerSet(file))
            producer = new WWDotNetLayerSetConverter();

        return producer;
    }

    protected static class ImportableDataFilter extends javax.swing.filechooser.FileFilter
    {
        public ImportableDataFilter()
        {
        }

        public boolean accept(File file)
        {
            if (file == null || file.isDirectory())
                return true;

            if (DataImportUtil.isDataRaster(file, null))
                return true;
            else if (DataImportUtil.isWWDotNetLayerSet(file))
                return true;

            return false;
        }

        public String getDescription()
        {
            return "Supported Images/Elevations";
        }
    }
}
