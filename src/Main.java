import java.awt.image.RasterFormatException;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.sql.SQLOutput;
import java.util.List;

public
class Main
{
    private static String NIODATAFILE = "data.txt";
    private static String NIOBINDATAFILE = "data.bin";
    private static String NIOBINCOPYFILE = "data2.bin";
    private static String NIOBINCOPYTOFILE = "data3.bin";

    public static
    void main(String[] args)
    {

    }


    static
    {


    }

    public static
    void ExploringPipes()
    {
        try
        {
            //  The data can only move one way, however, the two channels in the pipe
            //  allow for a 'write' into the pipe and a read from the pipe...
            //  If I had two different pipes, then I could pass information between two
            //  executing pieces of code...
            //  But, how would you connect the running threads to the pipes?  The pipes would
            //  need to be advertised, or part of the defined objects...???

            Pipe pipe = Pipe.open();

            Runnable writer = new Runnable()
            {
                @Override
                public
                void run()
                {
                    try
                    {
                        Pipe.SinkChannel sinkChannel = pipe.sink();
                        ByteBuffer sinkBuffer = ByteBuffer.allocate(56);
                        for(int i=0;i<10;i++)
                        {
                            String currentTime = "The time is " + System.currentTimeMillis();
                            sinkBuffer.put(currentTime.getBytes());
                            sinkBuffer.flip();

                            while(sinkBuffer.hasRemaining())
                            {
                                sinkChannel.write(sinkBuffer);
                            }
                            sinkBuffer.flip();
                            Thread.sleep(100);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            Runnable reader = new Runnable()
            {
                @Override
                public
                void run()
                {
                    try
                    {
                        Pipe.SourceChannel sourceChannel = pipe.source();
                        ByteBuffer sourceBuffer = ByteBuffer.allocate(56);
                        for(int i=0;i<10;i++)
                        {
                            int bytesRead = sourceChannel.read(sourceBuffer);
                            sourceBuffer.flip();
                            byte[] sourceInput = new byte[bytesRead];
                            sourceBuffer.get(sourceInput);
                            sourceBuffer.flip();
                            System.out.println("time string =>" + new String(sourceInput));
                            Thread.sleep(100);
                        }
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }


                }
            };

            new Thread(writer).start();
            new Thread(reader).start();

        }
        catch(IOException i)
        {
            i.printStackTrace();
        }
    }

    public static
    void NIOMoreReadWriteExploration()
    {
        //  Create a file output stream (writing to disk)
        //  Create a channel to this IO destination
        //  Associate the channel to a ByteBuffer (extends Buffer)
        //  Map a byte array to the ByteBuffer, and it becomes a view...
        //  For File Output & File Input, the ByteBuffer is a one-way buffer.

        try(FileOutputStream binFile = new FileOutputStream(NIOBINDATAFILE);
            FileChannel channel = binFile.getChannel())
        {
            byte[] output = "Testing the efficacy of the buffer-channel write method".getBytes();
            byte[] New = "\n".getBytes();
            byte[] cam = "Cameron Beeler is the author.".getBytes();

            ByteBuffer buf = ByteBuffer.allocate(100);
            buf .put(output)
                    .put(New)
                    .putInt(12345)
                    .put(New)
                    .putInt(-98765)
                    .put(New)
                    .put(cam);

            int outputSize = output.length;
            int pos1 = outputSize-1;
            int newSize = New.length;
            int pos2 = pos1+newSize;
            int varIntSize = 4;
            int pos3 = pos2 + varIntSize;
            int pos4 = pos3 + newSize;
            int pos5 = pos4 + varIntSize;


            buf.flip();
            channel.write(buf);
            buf.flip();
            channel.position(0);

            RandomAccessFile rafFile = new RandomAccessFile(NIOBINDATAFILE, "rwd");
            FileChannel originChannel = rafFile.getChannel();

            RandomAccessFile raf = new RandomAccessFile(NIOBINCOPYFILE, "rwd");
            FileChannel destFile = raf.getChannel();
            long bytesTransferred = destFile.transferFrom(originChannel, 0, originChannel.size());

            RandomAccessFile dup = new RandomAccessFile(NIOBINCOPYTOFILE, "rwd");
            FileChannel dupFile = dup.getChannel();

            originChannel.position(0);
            originChannel.transferTo(0, originChannel.size(), dupFile);

/*            ByteBuffer reading = ByteBuffer.allocate(100);
            destFile.read(reading);
            reading.flip();
            byte[] input = new byte[output.length];
            byte[] cr = new byte[New.length];
            byte[] cwb = new byte[cam.length];
            int nbr1 = 0;
            int nbr2 = 0;

            reading.get(input);
            System.out.print(new String(input));
            reading.get(cr);
            System.out.print(new String(cr));
            nbr1 = reading.getInt();
            System.out.print(nbr1);
            reading.get(cr);
            System.out.print(new String(cr));
            nbr2 = reading.getInt();
            System.out.print(nbr2);
            reading.get(cwb);
            System.out.println(new String(cwb));
*/

        }
        catch (IOException i)
        {
            i.printStackTrace();
        }
        catch (NonReadableChannelException n)
        {
            n.printStackTrace();
        }
    }


    public static
    void NIOExploration()
    {
        try(FileOutputStream binFile = new FileOutputStream(NIOBINDATAFILE);
            FileChannel binChannel = binFile.getChannel())
        {
            byte[] hw  = "Testing the efficacy of the buffer-channel write method".getBytes();
            byte[] New = "\nNew\n".getBytes();

// the wrap() method actually maps the byte array to the buffer, changes in either effect both
//  Alternately, we can use the '.allocate() method to creata a buffer of size int...'
//  Default position at 0, no marks...
//  The buffer is NOT mapped to the actual source...adding more content and writing again, will append
//      to the previous content in the destination medium.  As you would expect.
//
//      Buffer reuse is allowed, but a bit complicated...
//      It might make more sense to create a new buffer of the datatype you are manipulating and
//      then writing / reading from/to that buffer to/from the source.


            ByteBuffer buffer = ByteBuffer.allocate(hw.length);
            buffer.put(hw); //  add hw contents into buffer
            buffer.flip();  //  return to position 0 in buffer
            int byteSize = binChannel.write(buffer); // this writes the buffer to the file...
            buffer.flip();  //  return to position 0 in buffer


            ByteBuffer newBuffer = ByteBuffer.allocate((New.length));
            newBuffer.put(New); //  place contents in buffer
            newBuffer.flip();   //  return to position 0 in newBuffer
            int newByteSize = binChannel.write(newBuffer); // this writes the buffer to the file...
            newBuffer.flip();   //  return to position 0 in newBuffer

            ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
            intBuffer.putInt(12345);
            intBuffer.flip(); // reset intBuffer to position 0
            binChannel.write(intBuffer);
            intBuffer.flip(); // reset intBuffer to position 0

            binChannel.write(newBuffer);
            newBuffer.flip();   //  return to position 0 in newBuffer

            intBuffer.putInt(-98765);
            intBuffer.flip(); // reset intBuffer to position 0
            binChannel.write(intBuffer);
            intBuffer.flip(); // reset the buffer to the 0 position...

            RandomAccessFile randomAccessFile = new RandomAccessFile(NIOBINDATAFILE, "rwd");
            FileChannel      rafchannel       = randomAccessFile.getChannel();

//            buffer.flip();        may not need this as the last buffer activity was a flip()
            rafchannel.position(0);
            rafchannel.read(buffer);
            buffer.flip();
            if (buffer.hasArray())
            {
                System.out.print(new String(buffer.array()));

            }
            buffer.flip();  //  return buffer to position 0

            rafchannel.read(newBuffer);
            System.out.print(new String(New));
            newBuffer.flip();   //  return to position 0 in newBuffer


            rafchannel.read(intBuffer);
            intBuffer.flip(); // reset the buffer to the 0 position...
            System.out.print(intBuffer.getInt());
            intBuffer.flip(); // reset the buffer to the 0 position...

            rafchannel.read(newBuffer);
            System.out.print(new String(New));
            newBuffer.flip();   //  return to position 0 in newBuffer

            rafchannel.read(intBuffer);
            intBuffer.flip(); // reset the buffer to the 0 position...
            System.out.print(intBuffer.getInt());
            intBuffer.flip(); // reset the buffer to the 0 position...

            //relative read

            rafchannel.close();
            randomAccessFile.close();
        }
        catch (IOException i)
        {
            i.printStackTrace();
        }
    }


    public static
    void NIOBinaryRead()
    {
        String test = "Crazy NIO buffer sh*t";
        byte input[] = null;

        try (RandomAccessFile raf = new RandomAccessFile(NIOBINDATAFILE, "rwd"))
        {
            long numBytesRead=0;
            FileChannel fileChannel = raf.getChannel();
            byte[] data = "Hello World!".getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer = ByteBuffer.allocate((data.length + test.length()));
            buffer.put(data);
            buffer.put(test.getBytes());
            buffer.flip();
            numBytesRead = fileChannel.write(buffer);
            buffer.flip();

//            numBytesRead = fileChannel.write(buffer);
//            buffer = ByteBuffer.allocate(data.length);
//            buffer.put(data);
//            numBytesRead = fileChannel.write(buffer);
//            buffer.flip();
//            numBytesRead = fileChannel.read(buffer);
            buffer = ByteBuffer.allocate((data.length + test.length()));
            buffer.get(input);

            fileChannel.read(buffer);
            buffer.get();
            System.out.println("outputBytes =   " + new String(data));

        }
        catch(IOException i)
        {
            i.printStackTrace();
        }


    }
    public static
    void IORandomAccessfileRead()
    {
        try(RandomAccessFile raf = new RandomAccessFile(NIOBINDATAFILE, "rwd"))
        {
            byte[] b = new byte[12];
            raf.read(b);
            long one=0;
            long two = 0;
            one = raf.readInt();
            two = raf.readInt();
            System.out.println("String = " + new String(b) + ", and #1: " + one + ", and #2: " + two );

        }
        catch(IOException i)
        {
            i.printStackTrace();
        }

    }

    public static
    void NIOBinaryWrite()
    {
        try(FileOutputStream binFile = new FileOutputStream(NIOBINDATAFILE);
            FileChannel binChannel = binFile.getChannel())
        {

            byte[] hw = "Hello World!".getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(hw); // the wrap method actually maps the byte array to the buffer,
            // changes in either effect both
            int bytesize = binChannel.write(buffer);
            System.out.println("bytes written " + bytesize);

            ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
            intBuffer.putInt(245);
            intBuffer.flip();
            bytesize = binChannel.write(intBuffer);
            System.out.println("bytes written " + bytesize);

            intBuffer.flip(); // return to the 0 position
            intBuffer.putInt(-975); // write the data
            intBuffer.flip(); // return to the zero position
            bytesize = binChannel.write(intBuffer);
            System.out.println("bytes written " + bytesize);
        }
        catch (IOException i)
        {
            i.printStackTrace();
        }
    }

    public static
    void basicNIOReadWrite()
    {
        try
        {
//            FileInputStream data = new FileInputStream(NIODATAFILE);
//            FileChannel channel = data.getChannel();

            Path            path    = FileSystems.getDefault().getPath(NIODATAFILE);
            String filedata=null;
            filedata = "Line 1";
            filedata += "\nLine 2";
            filedata += "\nLine 3";
            filedata += "\nLine 4";
            filedata += "\nLine 5";
            filedata += "\nLine 6";
            filedata += "\nLine 7";
            filedata += "\nLine 8";
            filedata += "\nLine 9";
            Files.write(path, filedata.toString().getBytes("UTF-8"));
            List<String>    lines   = Files.readAllLines(path);
            int i=1;
            for(String s:lines)
            {
                System.out.println("Line #" + i + ": " + s); i++;
            }
        }
        catch(IOException i)
        {
            System.out.println(i.getMessage());
        }

    }

}

//            byte[] b = new byte[hw.length];
//            byte[] a = new byte[New.length];
//
//            int x=0;
//            int y=0;
//
//            randomAccessFile.read(b);
//            System.out.print(new String(b));
//            randomAccessFile.read(a);
//            System.out.print(new String(a));
//            x = randomAccessFile.readInt();
//            System.out.print(x);
//            randomAccessFile.read(a);
//            System.out.print(new String(a));
//            y = randomAccessFile.readInt();
//            System.out.print(y);


/*
            ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
            intBuffer.putInt(245);

            intBuffer.flip();
            System.out.println("buffer position: " + buffer.position());

            bytesize = binChannel.write(intBuffer);
            System.out.println("bytes written " + bytesize);
            System.out.println("buffer position: " + buffer.position());

            intBuffer.flip(); // return to the 0 position
            intBuffer.putInt(-975); // write the data
            System.out.println("buffer position: " + buffer.position());

            intBuffer.flip(); // return to the zero position
            bytesize = binChannel.write(intBuffer);
            System.out.println("buffer position: " + buffer.position());

            System.out.println("bytes written " + bytesize);
*/
/*
            System.out.println("intBuffer position->" + intBuffer.position());
            binChannel.write(intBuffer);
            intBuffer.clear();
            System.out.println("intBuffer position->" + intBuffer.position());
            binChannel.write(buffer);
            intBuffer.putInt(two);
            System.out.println("intBuffer position->" + intBuffer.position());
            System.out.println("buffer pointer position" + buffer.position());

*/
