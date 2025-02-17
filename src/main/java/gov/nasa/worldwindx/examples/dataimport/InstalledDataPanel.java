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
package gov.nasa.worldwindx.examples.dataimport;

import gov.nasa.worldwind.Factory;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import gov.nasa.worldwindx.examples.util.ExampleUtil;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Displays UI components for a set of caller specified installed data, and manages creation of WorldWind components
 * from that data. Callers fill the panel with installed data by invoking <code>{@link
 * #addInstalledData(org.w3c.dom.Element, gov.nasa.worldwind.avlist.AVList)}</code>. This adds the UI components for a
 * specified data set (a <code>Go To</code> button, and a label description), creates a WorldWind component from the
 * DataConfiguration, and adds the component to the WorldWindow passed to the panel during construction.
 *
 * @author dcollins
 * @version $Id: InstalledDataPanel.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class InstalledDataPanel extends JPanel
{
    protected WorldWindow worldWindow;
    protected JPanel dataConfigPanel;

    /**
     * Constructs an InstalledDataPanel with the specified title and WorldWindow. Upon construction, the panel is
     * configured to accept installed data via calls to {@link #addInstalledData(org.w3c.dom.Element,
     * gov.nasa.worldwind.avlist.AVList)}.
     *
     * @param title       the panel's title, displayed in a titled border.
     * @param worldWindow the panel's WorldWindow, which any WorldWind components are added to.
     *
     * @throws IllegalArgumentException if the WorldWindow is null.
     */
    public InstalledDataPanel(String title, WorldWindow worldWindow)
    {
        if (worldWindow == null)
        {
            String message = Logging.getMessage("nullValue.WorldWindow");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.worldWindow = worldWindow;
        this.dataConfigPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        this.layoutComponents(title);
    }

    /**
     * Adds the UI components for the specified installed data to this panel, and adds the WorldWind component created
     * from the data to the WorldWindow passed to this panel during construction.
     *
     * @param domElement the document which describes a WorldWind data configuration.
     * @param params     the parameter list which overrides or extends information contained in the document.
     *
     * @throws IllegalArgumentException if the Element is null.
     */
    public void addInstalledData(final Element domElement, final AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        this.addToWorldWindow(domElement, params);

        String description = this.getDescription(domElement);
        Sector sector = this.getSector(domElement);

        Box box = Box.createHorizontalBox();
        box.add(new JButton(new GoToSectorAction(sector)));
        box.add(Box.createHorizontalStrut(10));
        box.add(new JLabel(description));

        this.dataConfigPanel.add(box);
        this.revalidate();
    }

    protected void layoutComponents(String title)
    {
        this.dataConfigPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // top, left, bottom, right
        // Put the grid in a container to prevent scroll panel from stretching its vertical spacing.
        JPanel dummyPanel = new JPanel(new BorderLayout());
        dummyPanel.add(this.dataConfigPanel, BorderLayout.NORTH);
        // Add the dummy panel to a scroll pane.
        JScrollPane scrollPane = new JScrollPane(dummyPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // top, left, bottom, right
        // Add the scroll pane to a titled panel that will resize with the main window.
        JPanel titlePanel = new JPanel(new GridLayout(0, 1, 0, 10)); // rows, cols, hgap, vgap
        titlePanel.setBorder(
            new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder(title)));
        titlePanel.add(scrollPane);

        this.setLayout(new GridLayout(0, 1, 0, 0)); // rows, cols, hgap, vgap
        this.add(titlePanel);
    }

    //**************************************************************//
    //********************  DataConfiguration Utils  ***************//
    //**************************************************************//

    protected String getDescription(Element domElement)
    {
        String displayName = DataConfigurationUtils.getDataConfigDisplayName(domElement);
        String type = DataConfigurationUtils.getDataConfigType(domElement);

        StringBuilder sb = new StringBuilder(displayName);

        if (type.equalsIgnoreCase("Layer"))
        {
            sb.append(" (Layer)");
        }
        else if (type.equalsIgnoreCase("ElevationModel"))
        {
            sb.append(" (Elevations)");
        }

        return sb.toString();
    }

    protected Sector getSector(Element domElement)
    {
        return WWXML.getSector(domElement, "Sector", null);
    }

    protected void addToWorldWindow(Element domElement, AVList params)
    {
        String type = DataConfigurationUtils.getDataConfigType(domElement);
        if (type == null)
            return;

        if (type.equalsIgnoreCase("Layer"))
        {
            this.addLayerToWorldWindow(domElement, params);
        }
        else if (type.equalsIgnoreCase("ElevationModel"))
        {
            this.addElevationModelToWorldWindow(domElement, params);
        }
    }

    protected void addLayerToWorldWindow(Element domElement, AVList params)
    {
        Layer layer = null;
        try
        {
            Factory factory = (Factory) WorldWind.createConfigurationComponent(AVKey.LAYER_FACTORY);
            layer = (Layer) factory.createFromConfigSource(domElement, params);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.CreationFromConfigurationFailed",
                DataConfigurationUtils.getDataConfigDisplayName(domElement));
            log.error(message, e);
        }

        if (layer == null)
            return;

        layer.setEnabled(true); // TODO: BasicLayerFactory creates layer which is intially disabled

        if (!this.worldWindow.getModel().getLayers().contains(layer))
            ApplicationTemplate.insertBeforePlacenames(this.worldWindow, layer);
    }

    protected void addElevationModelToWorldWindow(Element domElement, AVList params)
    {
        ElevationModel em = null;
        try
        {
            Factory factory = (Factory) WorldWind.createConfigurationComponent(AVKey.ELEVATION_MODEL_FACTORY);
            em = (ElevationModel) factory.createFromConfigSource(domElement, params);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.CreationFromConfigurationFailed",
                DataConfigurationUtils.getDataConfigDisplayName(domElement));
            log.error(message, e);
        }

        if (em == null)
            return;

        ElevationModel defaultElevationModel = this.worldWindow.getModel().getGlobe().getElevationModel();
        if (defaultElevationModel instanceof CompoundElevationModel)
        {
            if (!((CompoundElevationModel) defaultElevationModel).containsElevationModel(em))
                ((CompoundElevationModel) defaultElevationModel).addElevationModel(em);
        }
        else
        {
            CompoundElevationModel cm = new CompoundElevationModel();
            cm.addElevationModel(defaultElevationModel);
            cm.addElevationModel(em);
            this.worldWindow.getModel().getGlobe().setElevationModel(cm);
        }
    }

    //**************************************************************//
    //********************  Actions  *******************************//
    //**************************************************************//

    protected class GoToSectorAction extends AbstractAction
    {
        protected Sector sector;

        public GoToSectorAction(Sector sector)
        {
            super("Go To");
            this.sector = sector;
            this.setEnabled(this.sector != null);
        }

        public void actionPerformed(ActionEvent e)
        {
            ExampleUtil.goTo(worldWindow, this.sector);
        }
    }
}
