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

import lombok.extern.slf4j.Slf4j;

/**
 * BufferFactory provides a general factory interface for creating instances of {@link
 * gov.nasa.worldwind.util.BufferWrapper}, without having to know the underlying data type. Once created, a
 * BufferWrapper abstracts reading and writing buffer data from the underlying data type. When BufferWrapper is combined
 * with BufferFactory, a component may create and work with buffer data in a type agnostic manner.
 * <p>
 * BufferFactory is itself abstract and defines the factory interface. It defines several implementations as static
 * inner classes, which serve the most common data types: {@link gov.nasa.worldwind.util.BufferFactory.ByteBufferFactory},
 * {@link gov.nasa.worldwind.util.BufferFactory.ShortBufferFactory}, {@link gov.nasa.worldwind.util.BufferFactory.IntBufferFactory},
 * {@link gov.nasa.worldwind.util.BufferFactory.FloatBufferFactory}, and {@link gov.nasa.worldwind.util.BufferFactory.DoubleBufferFactory}.
 *
 * @author dcollins
 * @version $Id: BufferFactory.java 1171 2013-02-11 21:45:02Z dcollins $
 * @see BufferWrapper
 */
@Slf4j
public abstract class BufferFactory
{
    private final boolean allocateDirect;

    /**
     * Constructs a new BufferFactory with the specified buffer allocation policy.
     *
     * @param allocateDirect true to allocate and return BufferWrappers backed by direct buffers, false to allocate and
     *                       return BufferWrappers backed by non-direct buffers.
     */
    protected BufferFactory(boolean allocateDirect)
    {
        this.allocateDirect = allocateDirect;
    }

    /**
     * Constructs a new BufferFactory with the default buffer allocation policy. This factory allocates and returns
     * BufferWrappers backed by direct buffers.
     */
    protected BufferFactory()
    {
        this(true);
    }

    /**
     * @return true if this factory allocates and returns BufferWrappers backed by direct buffers, and false if it
     *         allocates and return BufferWrappers backed by non-direct buffers.
     */
    public boolean isAllocateDirect()
    {
        return this.allocateDirect;
    }

    /**
     * Constructs a new BufferWrapper of the specified size.
     *
     * @param size the new buffer's size, in number of underlying data type units (bytes, shorts, ints, floats, or
     *             doubles).
     *
     * @return the new buffer.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public abstract BufferWrapper newBuffer(int size);

    /** Implementation of BufferFactory which constructs instances of {@link gov.nasa.worldwind.util.BufferWrapper.ByteBufferWrapper} */
    public static class ByteBufferFactory extends BufferFactory
    {
        /**
         * Constructs a new ByteBufferFactory with the specified buffer allocation policy.
         *
         * @param allocateDirect true to allocate and return ByteBufferWrappers backed by direct buffers, false to
         *                       allocate and return ByteufferWrappers backed by non-direct buffers.
         */
        public ByteBufferFactory(boolean allocateDirect)
        {
            super(allocateDirect);
        }

        /**
         * Constructs a new ByteBufferFactory with the default buffer allocation policy. This factory allocates and
         * returns ByteBufferWrappers backed by direct buffers.
         */
        public ByteBufferFactory()
        {
        }

        /**
         * Constructs a new ByteBufferWrapper of the specified size, backed by a {@link java.nio.ByteBuffer}.
         *
         * @param size the new buffer's size, int bytes.
         *
         * @return the new buffer.
         *
         * @throws IllegalArgumentException if size is negative.
         */
        public BufferWrapper newBuffer(int size)
        {
            if (size < 0)
            {
                String message = Logging.getMessage("generic.SizeOutOfRange", size);
                log.error(message);
                throw new IllegalArgumentException(message);
            }

            return WWBufferUtil.newByteBufferWrapper(size, this.isAllocateDirect());
        }
    }

    /** Implementation of BufferFactory which constructs instances of {@link gov.nasa.worldwind.util.BufferWrapper.ShortBufferWrapper} */
    public static class ShortBufferFactory extends BufferFactory
    {
        /**
         * Constructs a new ShortBufferFactory with the specified buffer allocation policy.
         *
         * @param allocateDirect true to allocate and return ShortBufferWrappers backed by direct buffers, false to
         *                       allocate and return ShortBufferWrappers backed by non-direct buffers.
         */
        public ShortBufferFactory(boolean allocateDirect)
        {
            super(allocateDirect);
        }

        /**
         * Constructs a new ShortBufferFactory with the default buffer allocation policy. This factory allocates and
         * returns ShortBufferWrappers backed by direct buffers.
         */
        public ShortBufferFactory()
        {
        }

        /**
         * Constructs a new ShortBufferWrapper of the specified size, backed by a {@link java.nio.ShortBuffer}.
         *
         * @param size the new buffer's size, int shorts.
         *
         * @return the new buffer.
         *
         * @throws IllegalArgumentException if size is negative.
         */
        public BufferWrapper newBuffer(int size)
        {
            if (size < 0)
            {
                String message = Logging.getMessage("generic.SizeOutOfRange", size);
                log.error(message);
                throw new IllegalArgumentException(message);
            }

            return WWBufferUtil.newShortBufferWrapper(size, this.isAllocateDirect());
        }
    }

    /** Implementation of BufferFactory which constructs instances of {@link gov.nasa.worldwind.util.BufferWrapper.IntBufferWrapper} */
    public static class IntBufferFactory extends BufferFactory
    {
        /**
         * Constructs a new IntBufferFactory with the specified buffer allocation policy.
         *
         * @param allocateDirect true to allocate and return IntBufferWrappers backed by direct buffers, false to
         *                       allocate and return IntBufferWrappers backed by non-direct buffers.
         */
        public IntBufferFactory(boolean allocateDirect)
        {
            super(allocateDirect);
        }

        /**
         * Constructs a new IntBufferFactory with the default buffer allocation policy. This factory allocates and
         * returns IntBufferWrappers backed by direct buffers.
         */
        public IntBufferFactory()
        {
        }

        /**
         * Constructs a new IntBufferWrapper of the specified size, backed by a {@link java.nio.IntBuffer}.
         *
         * @param size the new buffer's size, int ints.
         *
         * @return the new buffer.
         *
         * @throws IllegalArgumentException if size is negative.
         */
        public BufferWrapper newBuffer(int size)
        {
            if (size < 0)
            {
                String message = Logging.getMessage("generic.SizeOutOfRange", size);
                log.error(message);
                throw new IllegalArgumentException(message);
            }

            return WWBufferUtil.newIntBufferWrapper(size, this.isAllocateDirect());
        }
    }

    /** Implementation of BufferFactory which constructs instances of {@link gov.nasa.worldwind.util.BufferWrapper.FloatBufferWrapper} */
    public static class FloatBufferFactory extends BufferFactory
    {
        /**
         * Constructs a new FloatBufferFactory with the specified buffer allocation policy.
         *
         * @param allocateDirect true to allocate and return FloatBufferWrappers backed by direct buffers, false to
         *                       allocate and return FloatBufferWrappers backed by non-direct buffers.
         */
        public FloatBufferFactory(boolean allocateDirect)
        {
            super(allocateDirect);
        }

        /**
         * Constructs a new FloatBufferFactory with the default buffer allocation policy. This factory allocates and
         * returns FloatBufferWrappers backed by direct buffers.
         */
        public FloatBufferFactory()
        {
        }

        /**
         * Constructs a new FloatBufferWrapper of the specified size, backed by a {@link java.nio.FloatBuffer}.
         *
         * @param size the new buffer's size, int floats.
         *
         * @return the new buffer.
         *
         * @throws IllegalArgumentException if size is negative.
         */
        public BufferWrapper newBuffer(int size)
        {
            if (size < 0)
            {
                String message = Logging.getMessage("generic.SizeOutOfRange", size);
                log.error(message);
                throw new IllegalArgumentException(message);
            }

            return WWBufferUtil.newFloatBufferWrapper(size, this.isAllocateDirect());
        }
    }

    /** Implementation of BufferFactory which constructs instances of {@link gov.nasa.worldwind.util.BufferWrapper.DoubleBufferWrapper} */
    public static class DoubleBufferFactory extends BufferFactory
    {
        /**
         * Constructs a new DoubleBufferFactory with the specified buffer allocation policy.
         *
         * @param allocateDirect true to allocate and return DoubleBufferWrappers backed by direct buffers, false to
         *                       allocate and return DoubleBufferWrappers backed by non-direct buffers.
         */
        public DoubleBufferFactory(boolean allocateDirect)
        {
            super(allocateDirect);
        }

        /**
         * Constructs a new DoubleBufferFactory with the default buffer allocation policy. This factory allocates and
         * returns DoubleBufferWrappers backed by direct buffers.
         */
        public DoubleBufferFactory()
        {
        }

        /**
         * Constructs a new DoubleBufferWrapper of the specified size, backed by a {@link java.nio.DoubleBuffer}.
         *
         * @param size the new buffer's size, int doubles.
         *
         * @return the new buffer.
         *
         * @throws IllegalArgumentException if size is negative.
         */
        public BufferWrapper newBuffer(int size)
        {
            if (size < 0)
            {
                String message = Logging.getMessage("generic.SizeOutOfRange", size);
                log.error(message);
                throw new IllegalArgumentException(message);
            }

            return WWBufferUtil.newDoubleBufferWrapper(size, this.isAllocateDirect());
        }
    }
}
