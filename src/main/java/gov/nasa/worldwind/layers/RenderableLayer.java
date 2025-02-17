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
package gov.nasa.worldwind.layers;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.Locatable;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.event.MessageListener;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The <code>RenderableLayer</code> class manages a collection of {@link gov.nasa.worldwind.render.Renderable} objects
 * for rendering, picking, and disposal.
 *
 * @author tag
 * @version $Id: RenderableLayer.java 3435 2015-10-13 10:32:43Z dcollins $
 * @see gov.nasa.worldwind.render.Renderable
 */
@Slf4j
public class RenderableLayer extends AbstractLayer
{
    protected Collection<Renderable> renderables = new ConcurrentLinkedQueue<Renderable>();
    protected Iterable<Renderable> renderablesOverride;
    protected PickSupport pickSupport = new PickSupport();

    /** Creates a new <code>RenderableLayer</code> with a null <code>delegateOwner</code> */
    public RenderableLayer()
    {
    }

    /**
     * Adds the specified <code>renderable</code> to the end of this layer's internal collection. If this layer's
     * internal collection has been overridden with a call to {@link #setRenderables(Iterable)}, this will throw an
     * exception.
     * <p>
     * If the <code>renderable</code> implements {@link gov.nasa.worldwind.avlist.AVList}, the layer forwards its
     * property change events to the layer's property change listeners. Any property change listeners the layer attaches
     * to the <code>renderable</code> are removed in {@link #removeRenderable(gov.nasa.worldwind.render.Renderable)},
     * {@link #removeAllRenderables()}, or {@link #dispose()}.
     *
     * @param renderable Renderable to add.
     *
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     * @throws IllegalStateException    If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void addRenderable(Renderable renderable)
    {
        if (renderable == null)
        {
            String msg = Logging.getMessage("nullValue.RenderableIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        this.renderables.add(renderable);

        // Attach the layer as a property change listener of the renderable. This forwards property change events from
        // the renderable to the SceneController.
        if (renderable instanceof AVList)
            ((AVList) renderable).addPropertyChangeListener(this);
    }

    /**
     * Inserts the specified <code>renderable</code> at the specified <code>index</code> in this layer's internal
     * collection. If this layer's internal collection has been overridden with a call to {@link
     * #setRenderables(Iterable)}, this will throw an exception.
     * <p>
     * If the <code>renderable</code> implements {@link gov.nasa.worldwind.avlist.AVList}, the layer forwards its
     * property change events to the layer's property change listeners. Any property change listeners the layer attaches
     * to the <code>renderable</code> are removed in {@link #removeRenderable(gov.nasa.worldwind.render.Renderable)},
     * {@link #removeAllRenderables()}, or {@link #dispose()}.
     *
     * @param index      the index at which to insert the specified renderable.
     * @param renderable Renderable to insert.
     *
     * @throws IllegalArgumentException If <code>renderable</code> is null, if the <code>index</code> is less than zero,
     *                                  or if the <code>index</code> is greater than the number of renderables in this
     *                                  layer.
     * @throws IllegalStateException    If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void addRenderable(int index, Renderable renderable)
    {
        if (renderable == null)
        {
            String msg = Logging.getMessage("nullValue.RenderableIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        if (index < 0 || index > this.renderables.size())
        {
            String msg = Logging.getMessage("generic.indexOutOfRange", index);
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // The renderables are contained in a ConcurrentLinkedQueue, which does not support element insertion. Make a
        // shallow copy of the queue, insert into the copy, then replace the queue contents with the copy. This process
        // maintains the element order, with the new renderabable inserted in the specified index.
        ArrayList<Renderable> copy = new ArrayList<Renderable>(this.renderables);
        copy.add(index, renderable);
        this.renderables.clear();
        this.renderables.addAll(copy);

        // Attach the layer as a property change listener of the renderable. This forwards property change events from
        // the renderable to the SceneController.
        if (renderable instanceof AVList)
            ((AVList) renderable).addPropertyChangeListener(this);
    }

    /**
     * Adds the contents of the specified <code>renderables</code> to this layer's internal collection. If this layer's
     * internal collection has been overriden with a call to {@link #setRenderables(Iterable)}, this will throw an
     * exception.
     * <p>
     * If any of the <code>renderables</code> implement {@link gov.nasa.worldwind.avlist.AVList}, the layer forwards
     * their property change events to the layer's property change listeners. Any property change listeners the layer
     * attaches to the <code>renderable</code> are removed in {@link #removeRenderable(gov.nasa.worldwind.render.Renderable)},
     * {@link #removeAllRenderables()}, or {@link #dispose()}.
     *
     * @param renderables Renderables to add.
     *
     * @throws IllegalArgumentException If <code>renderables</code> is null.
     * @throws IllegalStateException    If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void addRenderables(Iterable<? extends Renderable> renderables)
    {
        if (renderables == null)
        {
            String msg = Logging.getMessage("nullValue.IterableIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        for (Renderable renderable : renderables)
        {
            // Internal list of renderables does not accept null values.
            if (renderable != null)
                this.renderables.add(renderable);

            // Attach the layer as a property change listener of the renderable. This forwards property change events
            // from the renderable to the SceneController.
            if (renderable instanceof AVList)
                ((AVList) renderable).addPropertyChangeListener(this);
        }
    }

    /**
     * Removes the specified <code>renderable</code> from this layer's internal collection, if it exists. If this
     * layer's internal collection has been overridden with a call to {@link #setRenderables(Iterable)}, this will throw
     * an exception.
     * <p>
     * If the <code>renderable</code> implements {@link gov.nasa.worldwind.avlist.AVList}, this stops forwarding the its
     * property change events to the layer's property change listeners. Any property change listeners the layer attached
     * to the <code>renderable</code> in {@link #addRenderable(gov.nasa.worldwind.render.Renderable)} or {@link
     * #addRenderables(Iterable)} are removed.
     *
     * @param renderable Renderable to remove.
     *
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     * @throws IllegalStateException    If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void removeRenderable(Renderable renderable)
    {
        if (renderable == null)
        {
            String msg = Logging.getMessage("nullValue.RenderableIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        this.renderables.remove(renderable);

        // Remove the layer as a property change listener of the renderable. This prevents the renderable from keeping a
        // dangling reference to the layer.
        if (renderable instanceof AVList)
            ((AVList) renderable).removePropertyChangeListener(this);
    }

    /**
     * Clears the contents of this layer's internal Renderable collection. If this layer's internal collection has been
     * overriden with a call to {@link #setRenderables(Iterable)}, this will throw an exception.
     * <p>
     * If any of the <code>renderables</code> implement {@link gov.nasa.worldwind.avlist.AVList}, this stops forwarding
     * their property change events to the layer's property change listeners. Any property change listeners the layer
     * attached to the <code>renderables</code> in {@link #addRenderable(gov.nasa.worldwind.render.Renderable)} or
     * {@link #addRenderables(Iterable)} are removed.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void removeAllRenderables()
    {
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        this.clearRenderables();
    }

    protected void clearRenderables()
    {
        if (this.renderables != null && this.renderables.size() > 0)
        {
            // Remove the layer as property change listener of any renderables. This prevents the renderables from
            // keeping a dangling references to the layer.
            for (Renderable renderable : this.renderables)
            {
                if (renderable instanceof AVList)
                    ((AVList) renderable).removePropertyChangeListener(this);
            }

            this.renderables.clear();
        }
    }

    public int getNumRenderables()
    {
        if (this.renderablesOverride != null)
        {
            int size = 0;
            //noinspection UnusedDeclaration
            for (Renderable r : this.renderablesOverride)
            {
                ++size;
            }

            return size;
        }
        else
        {
            return this.renderables.size();
        }
    }

    /**
     * Returns the Iterable of Renderables currently in use by this layer. If the caller has specified a custom Iterable
     * via {@link #setRenderables(Iterable)}, this will returns a reference to that Iterable. If the caller passed
     * <code>setRenderables</code> a null parameter, or if <code>setRenderables</code> has not been called, this returns
     * a view of this layer's internal collection of Renderables.
     *
     * @return Iterable of currently active Renderables.
     */
    public Iterable<Renderable> getRenderables()
    {
        return this.getActiveRenderables();
    }

    /**
     * Returns the Iterable of currently active Renderables. If the caller has specified a custom Iterable via {@link
     * #setRenderables(Iterable)}, this will returns a reference to that Iterable. If the caller passed
     * <code>setRenderables</code> a null parameter, or if <code>setRenderables</code> has not been called, this returns
     * a view of this layer's internal collection of Renderables.
     *
     * @return Iterable of currently active Renderables.
     */
    protected Iterable<Renderable> getActiveRenderables()
    {
        if (this.renderablesOverride != null)
        {
            return this.renderablesOverride;
        }
        else
        {
            // Return an unmodifiable reference to the internal list of renderables.
            // This prevents callers from changing this list and invalidating any invariants we have established.
            return java.util.Collections.unmodifiableCollection(this.renderables);
        }
    }

    /**
     * Overrides the collection of currently active Renderables with the specified <code>renderableIterable</code>. This
     * layer will maintain a reference to <code>renderableIterable</code> strictly for picking and rendering. This layer
     * will not modify the reference, or dispose of its contents. This will also clear and dispose of the internal
     * collection of Renderables, and will prevent any modification to its contents via <code>addRenderable,
     * addRenderables, removeRenderables, or dispose</code>.
     * <p>
     * Unlike {@link #addRenderable(gov.nasa.worldwind.render.Renderable)} or {@link #addRenderables(Iterable)}, this
     * does not forward any of the renderable's property change events to the layer's property change listeners. Since
     * the layer is not in control of the iIterable's contents, attaching property change listeners to the renderables
     * could cause the them to hold dangling references to the layer. If any of the renderables in the Iterable rely on
     * forwarding property change events for proper operation - such as {@link gov.nasa.worldwind.render.AbstractBrowserBalloon}
     * - use {@link #addRenderables(Iterable)} instead.
     * <p>
     * If the specified <code>renderableIterable</code> is null, this layer reverts to maintaining its internal
     * collection.
     *
     * @param renderableIterable Iterable to use instead of this layer's internal collection, or null to use this
     *                           layer's internal collection.
     */
    public void setRenderables(Iterable<Renderable> renderableIterable)
    {
        this.renderablesOverride = renderableIterable;
        // Dispose of the internal collection of Renderables.
        this.disposeRenderables();
        // Clear the internal collection of Renderables.
        this.clearRenderables();
    }

    /**
     * Opacity is not applied to layers of this type because each renderable typically has its own opacity control.
     *
     * @param opacity the current opacity value, which is ignored by this layer.
     */
    @Override
    public void setOpacity(double opacity)
    {
        super.setOpacity(opacity);
    }

    /**
     * Returns the layer's opacity value, which is ignored by this layer because each of its renderables typiically has
     * its own opacity control.
     *
     * @return The layer opacity, a value between 0 and 1.
     */
    @Override
    public double getOpacity()
    {
        return super.getOpacity();
    }

    /**
     * Disposes the contents of this layer's internal Renderable collection, but does not remove any elements from that
     * collection.
     * <p>
     * If any of layer's internal Renderables implement {@link gov.nasa.worldwind.avlist.AVList}, this stops forwarding
     * their property change events to the layer's property change listeners. Any property change listeners the layer
     * attached to the <code>renderables</code> in {@link #addRenderable(gov.nasa.worldwind.render.Renderable)} or
     * {@link #addRenderables(Iterable)} are removed.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void dispose()
    {
        if (this.renderablesOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        this.disposeRenderables();
    }

    protected void disposeRenderables()
    {
        if (this.renderables != null && this.renderables.size() > 0)
        {
            for (Renderable renderable : this.renderables)
            {
                try
                {
                    // Remove the layer as a property change listener of the renderable. This prevents the renderable
                    // from keeping a dangling reference to the layer.
                    if (renderable instanceof AVList)
                        ((AVList) renderable).removePropertyChangeListener(this);

                    if (renderable instanceof Disposable)
                        ((Disposable) renderable).dispose();
                }
                catch (Exception e)
                {
                    String msg = Logging.getMessage("generic.ExceptionAttemptingToDisposeRenderable");
                    log.error(msg);
                    // continue to next renderable
                }
            }
        }

        this.renderables.clear();
    }

    protected void doPreRender(DrawContext dc)
    {
        this.doPreRender(dc, this.getActiveRenderables());
    }

    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        this.doPick(dc, this.getActiveRenderables(), pickPoint);
    }

    protected void doRender(DrawContext dc)
    {
        this.doRender(dc, this.getActiveRenderables());
    }

    protected void doPreRender(DrawContext dc, Iterable<? extends Renderable> renderables)
    {
        for (Renderable renderable : renderables)
        {
            try
            {
                // If the caller has specified their own Iterable,
                // then we cannot make any guarantees about its contents.
                if (renderable != null && renderable instanceof PreRenderable)
                    ((PreRenderable) renderable).preRender(dc);
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("generic.ExceptionWhilePrerenderingRenderable");
                log.error(msg);
                // continue to next renderable
            }
        }
    }

    protected void doPick(DrawContext dc, Iterable<? extends Renderable> renderables, java.awt.Point pickPoint)
    {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        this.pickSupport.clearPickList();
        this.pickSupport.beginPicking(dc);

        try
        {
            for (Renderable renderable : renderables)
            {
                // If the caller has specified their own Iterable,
                // then we cannot make any guarantees about its contents.
                if (renderable != null)
                {
//                    float[] inColor = new float[4];
//                    gl.glGetFloatv(GL.GL_CURRENT_COLOR, inColor, 0);
                    java.awt.Color color = dc.getUniquePickColor();
                    gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());

                    try
                    {
                        renderable.render(dc);
                    }
                    catch (Exception e)
                    {
                        String msg = Logging.getMessage("generic.ExceptionWhilePickingRenderable");
                        log.error(msg);
                        log.trace(msg, e); // show exception for this level
                        continue; // go on to next renderable
                    }
//
//                    gl.glColor4fv(inColor, 0);

                    if (renderable instanceof Locatable)
                    {
                        this.pickSupport.addPickableObject(color.getRGB(), renderable,
                            ((Locatable) renderable).getPosition(), false);
                    }
                    else
                    {
                        this.pickSupport.addPickableObject(color.getRGB(), renderable);
                    }
                }
            }

            this.pickSupport.resolvePick(dc, pickPoint, this);
        }
        finally
        {
            this.pickSupport.endPicking(dc);
        }
    }

    protected void doRender(DrawContext dc, Iterable<? extends Renderable> renderables)
    {
        for (Renderable renderable : renderables)
        {
            try
            {
                // If the caller has specified their own Iterable,
                // then we cannot make any guarantees about its contents.
                if (renderable != null)
                    renderable.render(dc);
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("generic.ExceptionWhileRenderingRenderable");
                log.error(msg, e);
                // continue to next renderable
            }
        }
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.RenderableLayer.Name");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the message to each Renderable that implements {@link MessageListener}.
     *
     * @param message The message that was received.
     */
    @Override
    public void onMessage(Message message)
    {
        for (Renderable renderable : this.renderables)
        {
            try
            {
                if (renderable instanceof MessageListener)
                    ((MessageListener) renderable).onMessage(message);
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("generic.ExceptionInvokingMessageListener");
                log.error(msg, e);
                // continue to next renderable
            }
        }
    }
}
