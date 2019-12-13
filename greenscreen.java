

//Dirshe Salat

import java.awt.image.*;
import java.awt.*;
import java.io.*;
import javax.imageio.*;
import java.util.*;

import org.bytedeco.ffmpeg.*;
import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.*;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.Java2DFrameConverter;

/*
 * This will allow us to read input from a video file, and write output to
 * a seperate video file. This one is going to recolor the file by replacing red -> blue,
 * blue -> green, green -> red
 */


public class GreenScreenHW 
{
	// Adjust as needed
	static int 	WIDTH = 1280;
	static int	HEIGHT = 720;


	//Things needed to draw output with
	static		FFmpegFrameRecorder 		recorder;
	static		File				outfile;
	static		Frame				outframe;
	static		BufferedImage			outimage;
	static		Java2DFrameConverter		outconverter;

	//These are needed for input
	static		File				infile, infileSecond;
	static		FFmpegFrameGrabber		grabber, grabberSecond;
	static		Frame				inframe, inframeSecond;
	static		BufferedImage			inimage, inimageSecond;
	static		Java2DFrameConverter		inconverter, inconverterSecond;
	static		int				NUMFRAMES, NUMFRAMESSECOND;


	// Arrays for the individual pixels (input)
	static 		int 				inR[][] = new int[WIDTH][HEIGHT];
	static 		int 				inG[][] = new int[WIDTH][HEIGHT];
	static 		int 				inB[][] = new int[WIDTH][HEIGHT];
	
	
	// Arrays for the individual pixels (output)
	static 		int 				outR[][] = new int[WIDTH][HEIGHT];
	static 		int 				outG[][] = new int[WIDTH][HEIGHT];
	static 		int 				outB[][] = new int[WIDTH][HEIGHT];
	
	
	
	static 		int 				inRFinal[][] = new int[WIDTH][HEIGHT];
	static 		int 				inGFinal[][] = new int[WIDTH][HEIGHT];
	static 		int 				inBFinal[][] = new int[WIDTH][HEIGHT];
	
	// Loop stuff
	static		int				i, j, x, y;

	public static void main (String args[]) throws Exception, IOException
	{
		if (args.length != 3)
		{
			System.out.println ("Usage: java demo <1st input file name> <2nd input file name> <output file name>");
			System.exit(0);
		}

		// Output file
		infile = new File(args[0]);
		infileSecond = new File(args[1]);
		outfile = new File (args[2]);
		
		// Set everything up
		initrecorder();
		initgrabber();


		// Get the number of frames in the input video
		// We have to subtract two because this library is poorly written.
		// AKA, you get a Null Pointer Exception otherwise
		NUMFRAMES = grabber.getLengthInFrames()-2;

		System.out.printf ("Analyzing....please wait: \n");


		// Start grabbing frames
		// NEVER START AT ZERO. This implementation starts at 1.
		// WHO STARTS COUNTING AT 1? It's terrible.
		for (i=1; i<NUMFRAMES; i++)
		{
			System.out.printf ("%d ", i);
			
			// Occasionally, this will throw an exception,
			// but we've never really figured out why.
			try
			{
				// Method for simplicity
				fillin (i);
				fillingSecond (i);

				// Modify the output colors just to show it's doing something
				for (x=0; x<WIDTH; x++)
					for (y=0; y<HEIGHT; y++)
					{
						if((inR[x][y] < 115) && (inG[x][y] > 150) && (inB[x][y] < 20))
						{
							outR[x][y] = inRFinal[x][y];
							outG[x][y] = inGFinal[x][y];
							outB[x][y] = inBFinal[x][y];
						}
						else
						{
							outR[x][y] = inR[x][y];
							outG[x][y] = inG[x][y];
							outB[x][y] = inB[x][y];
						}
					}
				drawframe();
			}
			catch (Exception e)
			{
				System.out.printf ("-");
			}

			cleararrays();
		}
		System.out.printf ("done!\n");

		recorder.stop();
	}

	// Initialize the frame grabber
	public static void initgrabber() throws Exception
	{
		// Where we "draw" the image before putting it in a frame.
		outimage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

		// Create the input grabber
		grabber = new FFmpegFrameGrabber(infile);
		grabberSecond = new FFmpegFrameGrabber(infileSecond);

		// Just like the recorder, we start it here.
		grabber.start();
		grabberSecond.start();
		
		// Construct the converters
		outconverter = new Java2DFrameConverter();
	}


	// Initialize the frame recorder
	public static void initrecorder() throws Exception, IOException
	{ 
		// Create the recorder for the animation
		// Args are filename, width, height
		recorder = new FFmpegFrameRecorder (outfile, WIDTH, HEIGHT);

		// Codec. This is the only one that seems to work?
		recorder.setVideoCodec (AV_CODEC_ID_WMV2);
	
		// Frame rate. Right now, 30
		recorder.setFrameRate(30);

		// Not entirely sure what these do. Have to do with quality,
		// but I don't know the specifics.
		recorder.setVideoOption ("preset", "ultrafast");

		// A decent bitrate. Could probably scale it back if needed
		recorder.setVideoBitrate(35000000);

		// Start the recorder
		recorder.start();

		// Set up the frame converter
		inconverter = new Java2DFrameConverter();
		inconverterSecond = new Java2DFrameConverter();
	}

	public static void fillin (int num) throws Exception
	{
		int	incolor;
		
		try
		{
			
			grabber.setFrameNumber(num);
			inframe = grabber.grab();
			inimage = inconverter.convert(inframe);
			
		
			for (x = 0; x<WIDTH; x++)
			{
				for (y = 0; y < HEIGHT; y++)
				{
					incolor = inimage.getRGB(x,y);
					
					inR[x][y] = (incolor >> 16) & 0xff;
					inG[x][y] = (incolor >> 8)  & 0xff;
					inB[x][y] = (incolor)       & 0xff;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void fillingSecond (int num) throws Exception
	{
		int ColorIn;
		
		try
		{
			grabberSecond.setFrameNumber(num);
			inframeSecond = grabberSecond.grab();
			inimageSecond = inconverterSecond.convert(inframeSecond);
			
			for (x = 0; x<WIDTH; x++)
			{
				for (y = 0; y < HEIGHT; y++)
				{
					ColorIn = inimageSecond.getRGB(x,y);
					
					
					inRFinal[x][y] = (ColorIn >> 16) & 0xff;
					inGFinal[x][y] = (ColorIn >> 8)  & 0xff;
					inBFinal[x][y] = (ColorIn)       & 0xff;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void cleararrays()
	{
		for (x = 0; x < WIDTH; x++)
		{
			for (y = 0; y < HEIGHT; y++)
			{
				inR[x][y] = inG[x][y] = inB[x][y] = inRFinal[x][y] = inGFinal[x][y] = inBFinal[x][y] =
				outR[x][y] = outG[x][y] = outB[x][y] = 0;
			}
		}
	}

	public static void drawframe () throws Exception, IOException
	{
		int 	x, y;

		for (x=0; x<WIDTH; x++)
		{
			for (y=0; y<HEIGHT; y++)
			{
				outimage.setRGB(x, y, outR[x][y] << 16 | outG[x][y] << 8 | outB[x][y]);
			}
		}

		// Write the buffer to a frame
		outframe = outconverter.convert(outimage);
		
		// THIS was hard to find. You need the second parameter or the colors are in 
		// some weird AGBR(?) packed format, with the red channel ignored completely.

		// It writes the frame into the video
		recorder.record(outframe, AV_PIX_FMT_ARGB);
	}
}

