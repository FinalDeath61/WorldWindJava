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
package gov.nasa.worldwind.formats.nmea;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.tracks.*;
import gov.nasa.worldwind.util.Logging;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

// TODO: exception handling
/**
 * @author Tom Gaskins
 * @version $Id: NmeaReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Slf4j
public class NmeaReader implements Track, TrackSegment
{
    private java.util.List<Track> tracks = new java.util.ArrayList<Track>();
    private java.util.List<TrackSegment> segments =
        new java.util.ArrayList<TrackSegment>();
    private java.util.List<TrackPoint> points =
        new java.util.ArrayList<TrackPoint>();
    private String name;
    private int sentenceNumber = 0;

    public NmeaReader()
    {
        this.tracks.add(this);
        this.segments.add(this);
    }

    public java.util.List<TrackSegment> getSegments()
    {
        return this.segments;
    }

    public String getName()
    {
        return this.name;
    }

    public int getNumPoints()
    {
        return this.points.size();
    }

    public java.util.List<TrackPoint> getPoints()
    {
        return this.points;
    }

    /**
     * @param path The file spec to read.
     * @throws IllegalArgumentException if <code>path</code> is null
     * @throws java.io.IOException if a read error occurs.
     */
    public void readFile(String path) throws java.io.IOException
    {
        if (path == null)
        {
            String msg = Logging.getMessage("nullValue.PathIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.name = path;

        java.io.File file = new java.io.File(path);
        if (!file.exists())
        {
            String msg = Logging.getMessage("generic.FileNotFound", path);
            log.error(msg);
            throw new java.io.FileNotFoundException(path);
        }

        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        this.doReadStream(fis);

        if (this.tracks.isEmpty() || this.tracks.get(0).getNumPoints() == 0)
            throw new IllegalArgumentException(Logging.getMessage("formats.notNMEA", path));
//        java.nio.ByteBuffer buffer = this.doReadFile(fis);
//        this.parseBuffer(buffer);
    }

    /**
     * @param stream The stream to read from.
     * @param name The name of the stream.
     * @throws IllegalArgumentException if <code>stream</code> is null
     * @throws java.io.IOException if a read error occurs.
     */
    public void readStream(java.io.InputStream stream, String name) throws java.io.IOException
    {
        if (stream == null)
        {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.name = name != null ? name : "Un-named stream";
        this.doReadStream(stream);
    }

    public java.util.List<Track> getTracks()
    {
        return this.tracks;
    }

    public Iterator<Position> getTrackPositionIterator()
    {
        return new Iterator<Position>()
        {
            private TrackPointIterator trackPoints = new TrackPointIteratorImpl(NmeaReader.this.tracks);

            public boolean hasNext()
            {
                return this.trackPoints.hasNext();
            }

            public Position next()
            {
                return this.trackPoints.next().getPosition();
            }

            public void remove()
            {
                this.trackPoints.remove();
            }
        };
    }

    private void doReadStream(java.io.InputStream stream)
    {
        String sentence;

        try
        {
            do
            {
                sentence = this.readSentence(stream);
                if (sentence != null)
                {
                    ++this.sentenceNumber;
                    this.parseSentence(sentence);
                }
            } while (sentence != null);
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
//
//    private static final int PAGE_SIZE = 4096;
//
//    private java.nio.ByteBuffer doReadFile(java.io.FileInputStream fis) throws java.io.IOException
//    {
//        java.nio.channels.ReadableByteChannel channel = java.nio.channels.Channels.newChannel(fis);
//        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(PAGE_SIZE);
//
//        int count = 0;
//        while (count >= 0)
//        {
//            count = channel.read(buffer);
//            if (count > 0 && !buffer.hasRemaining())
//            {
//                java.nio.ByteBuffer biggerBuffer = java.nio.ByteBuffer.allocate(buffer.limit() + PAGE_SIZE);
//                biggerBuffer.put((java.nio.ByteBuffer) buffer.rewind());
//                buffer = biggerBuffer;
//            }
//        }
//
//        if (buffer != null)
//            buffer.flip();
//
//        return buffer;
//    }
//
//    private void parseBuffer(java.nio.ByteBuffer buffer)
//    {
//        while (buffer.hasRemaining())
//        {
//            byte b = buffer.get();
//            if (b == '$')
//            {
//                String sentence = this.readSentence(buffer);
//                if (sentence.length() > 0)
//                {
//                    this.parseSentence(sentence);
//                }
//            }
//        }
//    }

    private String readSentence(java.io.InputStream stream) throws java.io.IOException, InterruptedException
    {
        StringBuilder sb = null;
        boolean endOfSentence = false;

        while (!endOfSentence && !Thread.currentThread().isInterrupted())
        {
            int b = stream.read();

            if (b < 0)
                return null;
            else if (b == 0)
                Thread.sleep(200);
            else if (b == '$')
                sb = new StringBuilder(100);
            else if (b == '\r')
                endOfSentence = true;
            else if (sb != null)
                sb.append((char) b);
        }

        // TODO: check checksum
        return sb != null ? sb.toString() : null;
    }

    private String readSentence(java.nio.ByteBuffer buffer)
    {
        StringBuilder sb = new StringBuilder(100);
        boolean endOfSentence = false;
        while (!endOfSentence)
        {
            byte b = buffer.get();
            if (b == '\r')
                endOfSentence = true;
            else
                sb.append((char) b);
        }

        return sb.toString();
    }

    private void parseSentence(String sentence)
    {
        String[] words = sentence.split("[,*]");

        if (words[0].equalsIgnoreCase("GPGGA"))
            this.doTrackPoint(words);
//        else if (words[0].equalsIgnoreCase("GPRMC"))
//            this.doTrackPoint(words);
    }

    private void doTrackPoint(String[] words)
    {
        try
        {
            gov.nasa.worldwind.formats.nmea.NmeaTrackPoint point = new gov.nasa.worldwind.formats.nmea.NmeaTrackPoint(
                words);
            this.points.add(point);
        }
        catch (Exception e)
        {
            System.out.printf("Exception %s at sentence number %d for %s\n",
                e.getMessage(), this.sentenceNumber, this.name);
        }
    }
}
