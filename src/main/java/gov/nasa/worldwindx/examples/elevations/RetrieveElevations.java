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
package gov.nasa.worldwindx.examples.elevations;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import gov.nasa.worldwindx.examples.LayerPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Demonstrates how to retrieve the elevation of a geographic position from a local elevation model or from a WMS
 * server.
 *
 * @author garakl
 * @version $Id: RetrieveElevations.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
@Slf4j
public class RetrieveElevations extends ApplicationTemplate
{
    public static final String ACTION_COMMAND_BUTTON1 = "ActionCommand_Button1";
    public static final String ACTION_COMMAND_BUTTON2 = "ActionCommand_Button2";
    public static final String ACTION_COMMAND_BUTTON3 = "ActionCommand_Button3";
    public static final String ACTION_COMMAND_VERTICAL_EXAGGERATION = "ActionCommandVerticalExaggeration";

    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        protected ElevationsDemoController controller;
        protected LayerPanel layerPanel;

        public AppFrame()
        {
            // We add our own LayerPanel, but keep the StatusBar from ApplicationTemplate.
            super(true, false, false);
            this.controller = new ElevationsDemoController(this.getWwd());
            this.controller.frame = this;
            this.makeComponents();

            this.pack();
        }

        protected void makeComponents()
        {
            ((Component) this.getWwd()).setPreferredSize(new Dimension(1024, 768));

            JPanel panel = new JPanel(new BorderLayout());
            {
                panel.setBorder(new EmptyBorder(10, 0, 10, 0));

                JPanel controlPanel = new JPanel(new BorderLayout(0, 10));
                controlPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

                JPanel btnPanel = new JPanel(new GridLayout(5, 1, 0, 5));
                {
                    JButton btn = new JButton("Zoom to Matterhorn");
                    btn.setActionCommand(ACTION_COMMAND_BUTTON1);
                    btn.addActionListener(this.controller);
                    btnPanel.add(btn);

                    btn = new JButton("DEMO getElevations()");
                    btn.setActionCommand(ACTION_COMMAND_BUTTON2);
                    btn.addActionListener(this.controller);
                    btnPanel.add(btn);

                    btn = new JButton("DEMO getElevation()");
                    btn.setActionCommand(ACTION_COMMAND_BUTTON3);
                    btn.addActionListener(this.controller);
                    btnPanel.add(btn);
                }
                controlPanel.add(btnPanel, BorderLayout.NORTH);

                JPanel vePanel = new JPanel(new BorderLayout(0, 5));
                {
                    JLabel label = new JLabel("Vertical Exaggeration");
                    vePanel.add(label, BorderLayout.NORTH);

                    int MIN_VE = 1;
                    int MAX_VE = 8;
                    int curVe = (int) this.getWwd().getSceneController().getVerticalExaggeration();
                    curVe = curVe < MIN_VE ? MIN_VE : (curVe > MAX_VE ? MAX_VE : curVe);
                    JSlider slider = new JSlider(MIN_VE, MAX_VE, curVe);
                    slider.setMajorTickSpacing(1);
                    slider.setPaintTicks(true);
                    slider.setSnapToTicks(true);
                    Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
                    labelTable.put(1, new JLabel("1x"));
                    labelTable.put(2, new JLabel("2x"));
                    labelTable.put(4, new JLabel("4x"));
                    labelTable.put(8, new JLabel("8x"));
                    slider.setLabelTable(labelTable);
                    slider.setPaintLabels(true);
                    slider.addChangeListener(new ChangeListener()
                    {
                        public void stateChanged(ChangeEvent e)
                        {
                            double ve = ((JSlider) e.getSource()).getValue();
                            ActionEvent ae = new ActionEvent(ve, 0, ACTION_COMMAND_VERTICAL_EXAGGERATION);
                            controller.actionPerformed(ae);
                        }
                    });
                    vePanel.add(slider, BorderLayout.SOUTH);
                }
                controlPanel.add(vePanel, BorderLayout.SOUTH);

                panel.add(controlPanel, BorderLayout.SOUTH);

                this.layerPanel = new LayerPanel(this.getWwd());
                panel.add(this.layerPanel, BorderLayout.CENTER);
            }
            getContentPane().add(panel, BorderLayout.WEST);
        }
    }

    public static class ElevationsDemoController implements ActionListener
    {
        protected RetrieveElevations.AppFrame frame;
        // WorldWind stuff.
        protected WorldWindow wwd;

        public ElevationsDemoController(WorldWindow wwd)
        {
            this.wwd = wwd;
        }

        public void actionPerformed(ActionEvent e)
        {
            if (ACTION_COMMAND_BUTTON1.equalsIgnoreCase(e.getActionCommand()))
            {
                this.doActionOnButton1();
            }
            else if (ACTION_COMMAND_BUTTON2.equalsIgnoreCase(e.getActionCommand()))
            {
                this.doActionOnButton2();
            }
            else if (ACTION_COMMAND_BUTTON3.equalsIgnoreCase(e.getActionCommand()))
            {
                this.doActionOnButton3();
            }
            else if (ACTION_COMMAND_VERTICAL_EXAGGERATION.equalsIgnoreCase(e.getActionCommand()))
            {
                Double ve = (Double) e.getSource();
                this.doSetVerticalExaggeration(ve);
                this.wwd.redraw();
            }
        }

        public void doActionOnButton1()
        {
            log.info("Zooming to Matterhorn");

            View view = this.wwd.getView();

            Position matterhorn = new Position(LatLon.fromDegrees(45.9763888888889d, 7.65833333333333d), 0d);

            view.goTo(matterhorn, 5000d);
        }

        public void doActionOnButton2()
        {
            ArrayList<LatLon> latlons = new ArrayList<LatLon>();

            latlons.add(LatLon.fromDegrees(45.50d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.52d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.54d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.56d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.58d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.60d, -123.3d));

            Sector sector = Sector.fromDegrees(44d, 46d, -123d, -121d);

            double[] elevations = new double[latlons.size()];

            // request resolution of DTED2 (1degree / 3600 )
            double targetResolution = Angle.fromDegrees(1d).radians / 3600;

            double resolutionAchieved = this.wwd.getModel().getGlobe().getElevationModel().getElevations(
                sector, latlons, targetResolution, elevations);

            StringBuffer sb = new StringBuffer();
            for (double e : elevations)
            {
                sb.append("\n").append(e);
            }
            sb.append("\nresolutionAchieved = ").append(resolutionAchieved);
            sb.append(", requested resolution = ").append(targetResolution);

            log.info(sb.toString());
        }

        public void doActionOnButton3()
        {
            ArrayList<LatLon> latlons = new ArrayList<LatLon>();

            latlons.add(LatLon.fromDegrees(45.50d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.52d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.54d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.56d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.58d, -123.3d));
            latlons.add(LatLon.fromDegrees(45.60d, -123.3d));

            ElevationModel model = this.wwd.getModel().getGlobe().getElevationModel();

            StringBuffer sb = new StringBuffer();
            for (LatLon ll : latlons)
            {
                double e = model.getElevation(ll.getLatitude(), ll.getLongitude());
                sb.append("\n").append(e);
            }

            log.info(sb.toString());
        }

        public void doSetVerticalExaggeration(double ve)
        {
            this.wwd.getSceneController().setVerticalExaggeration(ve);
        }
    }

    public static void main(String[] args)
    {
        start("WorldWind Get Elevations Demo", AppFrame.class);
    }
}