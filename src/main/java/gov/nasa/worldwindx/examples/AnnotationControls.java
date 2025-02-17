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
package gov.nasa.worldwindx.examples;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.AnnotationLayoutManager;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.render.WWIcon;
import gov.nasa.worldwind.util.BasicDragger;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwindx.examples.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;

/**
 * Illustrates how to use a WorldWind <code>{@link Annotation}</code> with an <code>{@link
 * AnnotationLayoutManager}</code> to display an Annotation with a simple embedded user interface. The custom Annotation
 * layouts illustrated here can be found in the following example classes: <ul> <li><code>{@link
 * AudioPlayerAnnotation}</code></li> <li><code>{@link SlideShowAnnotation}</code></li> </ul>
 *
 * @author dcollins
 * @version $Id: AnnotationControls.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class AnnotationControls extends ApplicationTemplate
{
    protected final static String AUDIO = "Audio";
    protected final static String IMAGES = "Images";

    protected final static String ICON_AUDIO = "gov/nasa/worldwindx/examples/images/audioicon-64.png";
    protected final static String ICON_IMAGES = "gov/nasa/worldwindx/examples/images/imageicon-64.png";

    protected final static String AUDIO_PATH_MUSIC = "gov/nasa/worldwindx/examples/data/spacemusic.au";

    protected final static String IMAGE_PATH_MT_ST_HELENS = "gov/nasa/worldwindx/examples/images/MountStHelens.jpg";
    protected final static String IMAGE_PATH_THE_NUT = "gov/nasa/worldwindx/examples/images/the_nut.jpg";
    protected final static String IMAGE_PATH_IRELAND = "gov/nasa/worldwindx/examples/images/ireland.jpg";
    protected final static String IMAGE_PATH_NEW_ZEALAND = "gov/nasa/worldwindx/examples/images/new_zealand.gif";

    public static class AppFrame extends ApplicationTemplate.AppFrame implements SelectListener
    {
        protected IconLayer iconLayer;
        protected WWIcon highlit;
        protected RenderableLayer contentLayer;
        protected BasicDragger dragger;

        public AppFrame()
        {
            this.iconLayer = createIconLayer();
            this.contentLayer = new RenderableLayer();
            insertBeforePlacenames(this.getWwd(), this.iconLayer);
            insertBeforePlacenames(this.getWwd(), this.contentLayer);

            this.getWwd().addSelectListener(this);
            this.dragger = new BasicDragger(this.getWwd());
        }

        public IconLayer getIconLayer()
        {
            return this.iconLayer;
        }

        public RenderableLayer getContentLayer()
        {
            return this.contentLayer;
        }

        @SuppressWarnings( {"StringEquality"})
        public void selected(SelectEvent e)
        {
            if (e == null)
                return;

            PickedObject topPickedObject = e.getTopPickedObject();

            if (e.getEventAction() == SelectEvent.LEFT_PRESS)
            {
                if (topPickedObject != null && topPickedObject.getObject() instanceof WWIcon)
                {
                    WWIcon selected = (WWIcon) topPickedObject.getObject();
                    this.highlight(selected);
                }
                else
                {
                    this.highlight(null);
                }
            }
            else if (e.getEventAction() == SelectEvent.LEFT_DOUBLE_CLICK)
            {
                if (topPickedObject != null && topPickedObject.getObject() instanceof WWIcon)
                {
                    WWIcon selected = (WWIcon) topPickedObject.getObject();
                    this.highlight(selected);
                    this.openResource(selected);
                }
            }
            else if (e.getEventAction() == SelectEvent.DRAG || e.getEventAction() == SelectEvent.DRAG_END)
            {
                this.dragger.selected(e);
            }
        }

        public void highlight(WWIcon icon)
        {
            if (this.highlit == icon)
                return;

            if (this.highlit != null)
            {
                this.highlit.setHighlighted(false);
                this.highlit = null;
            }

            if (icon != null)
            {
                this.highlit = icon;
                this.highlit.setHighlighted(true);
            }

            this.getWwd().redraw();
        }

        protected void closeResource(ContentAnnotation content)
        {
            if (content == null)
                return;

            content.detach();
        }

        protected void openResource(WWIcon icon)
        {
            if (icon == null)
                return;

            ContentAnnotation content = this.createContent(icon.getPosition(), icon);

            if (content != null)
            {
                content.attach();
            }
        }

        protected ContentAnnotation createContent(Position position, AVList params)
        {
            return createContentAnnotation(this, position, params);
        }
    }

    public static class ContentAnnotation implements ActionListener
    {
        protected AppFrame appFrame;
        protected DialogAnnotation annnotation;
        protected DialogAnnotationController controller;

        public ContentAnnotation(AppFrame appFrame, DialogAnnotation annnotation, DialogAnnotationController controller)
        {
            this.appFrame = appFrame;
            this.annnotation = annnotation;
            this.annnotation.addActionListener(this);
            this.controller = controller;
        }

        public AppFrame getAppFrame()
        {
            return this.appFrame;
        }

        public DialogAnnotation getAnnotation()
        {
            return this.annnotation;
        }

        public DialogAnnotationController getController()
        {
            return this.controller;
        }

        @SuppressWarnings( {"StringEquality"})
        public void actionPerformed(ActionEvent e)
        {
            if (e == null)
                return;

            if (e.getActionCommand() == AVKey.CLOSE)
            {
                this.getAppFrame().closeResource(this);
            }
        }

        public void detach()
        {
            this.getController().setEnabled(false);

            RenderableLayer layer = this.getAppFrame().getContentLayer();
            layer.removeRenderable(this.getAnnotation());
        }

        public void attach()
        {
            this.getController().setEnabled(true);

            RenderableLayer layer = this.appFrame.getContentLayer();
            layer.removeRenderable(this.getAnnotation());
            layer.addRenderable(this.getAnnotation());
        }
    }

    public static class AudioContentAnnotation extends ContentAnnotation
    {
        protected Clip clip;
        protected Object source;
        protected Thread readThread;

        public AudioContentAnnotation(AppFrame appFrame, AudioPlayerAnnotation annnotation,
            AudioPlayerAnnotationController controller, Object source)
        {
            super(appFrame, annnotation, controller);
            this.source = source;
            this.retrieveAndSetClip(source);
        }

        public Object getSource()
        {
            return this.source;
        }

        public void detach()
        {
            super.detach();

            // Stop any threads or timers the controller may be actively running.
            AudioPlayerAnnotationController controller = (AudioPlayerAnnotationController) this.getController();
            if (controller != null)
            {
                this.stopController(controller);
            }

            // Stop any threads that may be reading the audio source.
            this.stopClipRetrieval();
        }

        @SuppressWarnings( {"StringEquality"})
        protected void stopController(AudioPlayerAnnotationController controller)
        {
            String status = controller.getClipStatus();
            if (status == AVKey.PLAY)
            {
                controller.stopClip();
            }
        }

        protected void retrieveAndSetClip(Object source)
        {
            this.startClipRetrieval(source);
        }

        protected void doRetrieveAndSetClip(final Object source)
        {
            javax.swing.SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    getAnnotation().setBusy(true);
                    appFrame.getWwd().redraw();
                }
            });

            final Clip clip = this.readClip(source);

            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    AudioPlayerAnnotationController controller = (AudioPlayerAnnotationController) getController();
                    if (controller != null)
                    {
                        controller.setClip(clip);
                    }

                    AudioPlayerAnnotation annotation = (AudioPlayerAnnotation) getAnnotation();
                    if (annotation != null)
                    {
                        if (clip == null)
                        {
                            annotation.getTitleLabel().setText(createErrorTitle(source.toString()));
                        }
                    }

                    getAnnotation().setBusy(false);
                    appFrame.getWwd().redraw();
                }
            });
        }

        protected Clip readClip(Object source)
        {
            InputStream stream = null;
            try
            {
                stream = WWIO.openStream(source);
                return openAudioStream(stream);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                WWIO.closeStream(stream, source.toString());
            }

            return null;
        }

        protected void startClipRetrieval(final Object source)
        {
            this.readThread = new Thread(new Runnable()
            {
                public void run()
                {
                    doRetrieveAndSetClip(source);
                }
            });
            this.readThread.start();
        }

        protected void stopClipRetrieval()
        {
            if (this.readThread != null)
            {
                if (this.readThread.isAlive())
                {
                    this.readThread.interrupt();
                }
            }

            this.readThread = null;
        }
    }

    public static class ImageContentAnnotation extends ContentAnnotation
    {
        public ImageContentAnnotation(AppFrame appFrame, SlideShowAnnotation annnotation,
            SlideShowAnnotationController controller)
        {
            super(appFrame, annnotation, controller);
        }

        public void detach()
        {
            super.detach();

            // Stop any threads or timers the controller may be actively running.
            SlideShowAnnotationController controller = (SlideShowAnnotationController) this.getController();
            if (controller != null)
            {
                this.stopController(controller);
            }
        }

        @SuppressWarnings( {"StringEquality"})
        protected void stopController(SlideShowAnnotationController controller)
        {
            String state = controller.getState();
            if (state == AVKey.PLAY)
            {
                controller.stopSlideShow();
            }

            controller.stopRetrievalTasks();
        }
    }

    public static IconLayer createIconLayer()
    {
        IconLayer layer = new IconLayer();
        layer.setPickEnabled(true);

        WWIcon icon = createIcon(AUDIO, Position.fromDegrees(28.533513, -81.375789, 0),
            "Music from the Java Sound demo", AUDIO_PATH_MUSIC);
        layer.addIcon(icon);

        icon = createIcon(IMAGES, Position.fromDegrees(46.1912, -122.1944, 0), "",
            java.util.Arrays.asList(IMAGE_PATH_MT_ST_HELENS));
        layer.addIcon(icon);

        icon = createIcon(IMAGES, Position.fromDegrees(-12, -70, 0), "",
            java.util.Arrays.asList(IMAGE_PATH_IRELAND, IMAGE_PATH_NEW_ZEALAND, IMAGE_PATH_THE_NUT));
        layer.addIcon(icon);

        return layer;
    }

    public static WWIcon createIcon(Object type, Position position, String title, Object data)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (title == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (data == null)
        {
            String message = Logging.getMessage("nullValue.DataSetIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        String iconPath = (type == AUDIO) ? ICON_AUDIO : ICON_IMAGES;

        UserFacingIcon icon = new UserFacingIcon(iconPath, position);
        icon.setSize(new java.awt.Dimension(64, 64));
        icon.setValue(AVKey.DATA_TYPE, type);
        icon.setValue(AVKey.TITLE, title);
        icon.setValue(AVKey.URL, data);
        return icon;
    }

    @SuppressWarnings( {"StringEquality"})
    public static ContentAnnotation createContentAnnotation(AppFrame appFrame, Position position, AVList params)
    {
        if (appFrame == null)
        {
            String message = "AppFrameIsNull";
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        String type = params.getStringValue(AVKey.DATA_TYPE);
        String title = params.getStringValue(AVKey.TITLE);
        Object source = params.getValue(AVKey.URL);

        if (type == AUDIO)
        {
            return createAudioAnnotation(appFrame, position, title, source);
        }
        else if (type == IMAGES)
        {
            return createImageAnnotation(appFrame, position, title, (Iterable) source);
        }

        return null;
    }

    public static ContentAnnotation createAudioAnnotation(AppFrame appFrame, Position position, String title,
        Object source)
    {
        if (appFrame == null)
        {
            String message = "AppFrameIsNull";
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (title == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (source == null)
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        AudioPlayerAnnotation annotation = new AudioPlayerAnnotation(position);
        annotation.setAlwaysOnTop(true);
        annotation.getTitleLabel().setText(title);

        AudioPlayerAnnotationController controller = new AudioPlayerAnnotationController(appFrame.getWwd(), annotation);

        return new AudioContentAnnotation(appFrame, annotation, controller, source);
    }

    @SuppressWarnings( {"TypeParameterExplicitlyExtendsObject"})
    public static ContentAnnotation createImageAnnotation(AppFrame appFrame, Position position, String title,
        Iterable sources)
    {
        if (appFrame == null)
        {
            String message = "AppFrameIsNull";
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (title == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        if (sources == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        SlideShowAnnotation annotation = new SlideShowAnnotation(position);
        annotation.setAlwaysOnTop(true);
        annotation.getTitleLabel().setText(title);

        SlideShowAnnotationController controller = new SlideShowAnnotationController(appFrame.getWwd(), annotation,
            sources);

        return new ImageContentAnnotation(appFrame, annotation, controller);
    }

    public static Clip openAudioStream(InputStream stream) throws Exception
    {
        if (stream == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        Clip clip = null;

        AudioInputStream ais = null;
        try
        {
            // AudioSystem.getAudioInputStream requires that InputStreams for audio clip resources support the
            // mark/reset functionality. Streams opened to class-path resources do not support this functionality, so
            // we provide this functionality by wrapping the specified InputStream in a BufferedInputStream, which
            // always supports mark/reset.
            ais = AudioSystem.getAudioInputStream(WWIO.getBufferedInputStream(stream));
            AudioFormat format = ais.getFormat();

            // Code taken from Java Sound demo at
            // http://java.sun.com/products/java-media/sound/samples/JavaSoundDemo
            //
            // We can't yet open the device for ALAW/ULAW playback, convert ALAW/ULAW to PCM.
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) ||
                (format.getEncoding() == AudioFormat.Encoding.ALAW))
            {
                AudioFormat tmp = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(),
                    format.getSampleSizeInBits() * 2,
                    format.getChannels(),
                    format.getFrameSize() * 2,
                    format.getFrameRate(),
                    true);
                ais = AudioSystem.getAudioInputStream(tmp, ais);
                format = tmp;
            }

            DataLine.Info info = new DataLine.Info(
                Clip.class,
                ais.getFormat(),
                ((int) ais.getFrameLength() * format.getFrameSize()));

            clip = (Clip) AudioSystem.getLine(info);
            clip.open(ais);
        }
        finally
        {
            if (ais != null)
            {
                ais.close();
            }
        }

        return clip;
    }

    public static String createErrorTitle(String path)
    {
        if (path == null)
        {
            String message = Logging.getMessage("nullValue.PathIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Cannot open the resource at <i>").append(path).append("</i>");
        return sb.toString();
    }

    public static String createTitle(Iterable sources)
    {
        if (sources == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        StringBuilder sb = new StringBuilder();

        java.util.Iterator iter = sources.iterator();
        while (iter.hasNext())
        {
            Object o = iter.next();

            sb.append(o);

            if (iter.hasNext())
            {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("WorldWind Annotation Controls", AppFrame.class);
    }
}
