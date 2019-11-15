package verve;
/*
 * Created by Ibrahim Althomali on 29/11/2017
 * 
 ********************************************/


import org.openqa.selenium.Rectangle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class Assist
{
	static int viewportPatingWait = 200;
	static int defaultMilliWaitLoad = 200;
	static int defaultMilliWaitSettle = 200;
	final static String date = getDateTime();
	static Scanner reader = new Scanner(System.in);  
	static int smallrangeThreshold = 100;
	static String useHistogramMethod = "Intersection";
	static double corThreshold =  1;
	static double chiThreshold =  1.85;
	static double kldThreshold =  2;
	static double intThreshold =  0.22;
	static double altThreshold =  1.2;
	static double bhaThreshold =  0.23;
	static DecimalFormat decimalFormat = new DecimalFormat("#.##");
	final static float dash1[] = {15.0f};
	final static float dash2[] = {10.0f};
	final static float dash3[] = {5.0f};
    final static BasicStroke solid =
            new BasicStroke(2.5f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER);
    final static BasicStroke dashed1 =
        new BasicStroke(2.5f,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f, dash1, 0.0f);
    final static BasicStroke dashed2 =
            new BasicStroke(2.5f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, dash2, 0.0f);
    final static BasicStroke dashed3 =
            new BasicStroke(2.5f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, dash2, 0.0f);
	static long startTime;
	static long endTime;
	static long duration;
	static String navWebDirectory = "/"+  "Thirty-Runs" +"/"+Assist.date+"/"; 
	static String outDirectory = "." + File.separator + "Thirty-Runs" +File.separator + Assist.date;
	static public void waitToLoad() {
		try { //timeout for elements to settle
			TimeUnit.MILLISECONDS.sleep(defaultMilliWaitLoad);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	static public void waitToSettle() {
		try { //timeout for elements to settle
			TimeUnit.MILLISECONDS.sleep(defaultMilliWaitSettle);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	static public void createOutputDirectory() {
		new File(outDirectory).mkdirs();
	}
	static public void startTime()
	{
		startTime = System.nanoTime();
	}
	static public void endTime()
	{
		endTime = System.nanoTime();
		duration = endTime - startTime;
	}
	static public long getDurationNano()
	{
		return duration;
	}
	static public long getDurationMilli()
	{
		return duration/1000000;
	}
	final static String getDateTime()  
	{  
	    DateFormat df = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");  
	    //df.setTimeZone(TimeZone.getTimeZone("PST"));  
	    return df.format(new Date());  
	} 
	static public BufferedImage getSubImage(BufferedImage img, Rectangle rectangle, int offsetX, int offsetY, Failure lf)
	{
		if (lf.ignored == false)
		{
			int newX = rectangle.getX() - offsetX;
			int newY = rectangle.getY() - offsetY;
			int newW = rectangle.getWidth();
			int newH = rectangle.getHeight();
			if (newX < 0 || newY < 0 || newX + newW > img.getWidth() || newY + newH > img.getHeight())
			{
				String note = "Changing coordinates XYHW(" + newX + "," + newY + "," + newH + "," + newW + ") ";
				if (newX < 0)
				{
					newW = newW + newX;
					newX = 0;
				}
				if (newY < 0)
				{
					newH = newH + newY;
					newY = 0;
				}
				if (newX + newW > img.getWidth())
				{
					newW = newW - ((newX + newW) - img.getWidth());
					// newW = img.getWidth() - newX;
				}
				if (newY + newH > img.getHeight())
				{
					newH = newH - ((newY + newH) - img.getHeight());
					// newH = img.getHeight() - newY;
				}
				note = note + "to XYHW(" + newX + "," + newY + "," + newH + "," + newW + ") ";
				//System.out.println(note);
				lf.titles.add("Rectangle (SubImage) Size Warning:");
				lf.notes.add(note);
			}
			BufferedImage partialImg = null;
			try
			{
				partialImg = img.getSubimage(newX, newY, newW, newH);
			} catch (Exception RasterFormatException)
			{
				RasterFormatException.printStackTrace();
				String note = "Original Image: Width: " + img.getWidth() + " Height: " + img.getHeight() + "\n";
				note = note + "Sub Image     : Width: " + rectangle.getWidth() + " Height: " + rectangle.getHeight()
						+ "  X: " + rectangle.getX() + " Y: " + rectangle.getY() + "\n";
				note = note + "Sub Image new : Width: " + newW + " Height: " + newH + "  X: " + newX + " Y: " + newY
						+ "\n";
				note = note + "offsetX: " + offsetX + " offsetY: " + offsetY + "\n";
				lf.titles.add("Image or Subimage Size Error:");
				//System.out.println(note);
				lf.notes.add(note);
				lf.ignored = true;
			}
			return partialImg;
		} else
		{
			return null;
		}
	}
	public static BufferedImage copyImage(BufferedImage source){
	    BufferedImage img = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
	    Graphics2D g = img.createGraphics();
	    g.drawImage(source, 0, 0, null);
	    g.dispose();
	    return img;
	}
	public double changeInImage(BufferedImage buffImgOne, BufferedImage buffImgTwo)
	{
		double allPixelsDiff = 0;
		if (buffImgOne.getWidth() != buffImgTwo.getWidth() || buffImgOne.getHeight() != buffImgTwo.getHeight())
		{
			System.out.println("Image dimensions are not the same and cannot compare... exiting");
			System.exit(1);
		} else
		{

			for (int i = 0; i < buffImgOne.getHeight(); i++)
			{
				for (int j = 0; j < buffImgOne.getWidth(); j++)
				{
					int rgb1 = buffImgOne.getRGB(j, i);
					int rgb2 = buffImgTwo.getRGB(j, i);
					int r1 = (rgb1 >> 16) & 0xff;
					int g1 = (rgb1 >> 8) & 0xff;
					int b1 = (rgb1) & 0xff;
					int r2 = (rgb2 >> 16) & 0xff;
					int g2 = (rgb2 >> 8) & 0xff;
					int b2 = (rgb2) & 0xff;
					int redDiff = Math.abs(r1 - r2); // Change in red
					int greenDiff = Math.abs(g1 - g2);// Change in green
					int blueDiff = Math.abs(b1 - b2);// Change in blue
					double allColorDiff = (redDiff + greenDiff + blueDiff) / 3; // Make sure result is between 0 - 255
					double result = allColorDiff / 255;
					allPixelsDiff += result;
				}
			}
		}
		return (allPixelsDiff / (buffImgOne.getHeight() * buffImgOne.getWidth()));
	}

	public static Boolean intersectingRec(Rectangle a, Rectangle b)
	{
		java.awt.Rectangle c = new java.awt.Rectangle(a.x, a.y, a.width, a.height);
		java.awt.Rectangle d = new java.awt.Rectangle(b.x, b.y, b.width, b.height);
		return c.intersects(d);
	}

	public static Rectangle intrscRec(Rectangle a, Rectangle b)
	{
		java.awt.Rectangle c = new java.awt.Rectangle(a.x, a.y, a.width, a.height);
		java.awt.Rectangle d = new java.awt.Rectangle(b.x, b.y, b.width, b.height);
		java.awt.Rectangle z = c.intersection(d);
		Rectangle r = new Rectangle(z.x, z.y, z.height, z.width); // Notice order of W & H
		return r;

	}
	public static void highlightSingleElement(int offsetX, int offsetY, BufferedImage img, Graphics2D g, Rectangle rectangle, Color color) {
		Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, rectangle, img);
		g.setColor(Color.BLACK);
		g.setStroke(Assist.solid);
		g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); 
		g.setColor(color);
		g.setStroke(Assist.dashed2);
		g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); 
	}
	public static Rectangle cutRectangleToVisibleArea(int offsetX, int offsetY, Rectangle rectangle, BufferedImage img) {
		int newX = rectangle.getX() - offsetX;
		int newY = rectangle.getY() - offsetY;
		int newW = rectangle.getWidth();
		int newH = rectangle.getHeight();
		if(newX < 0 || newY < 0 || newX + newW > img.getWidth() || newY + newH > img.getHeight())
		{
			if(newX < 0)
			{
				newW = newW + newX;
				newX = 0;
			}
			if(newY < 0)
			{
				newH = newH + newY;
				newY = 0;
			}
			if(newX + newW > img.getWidth())
			{
				newW = newW - ((newX + newW) - img.getWidth());
				//				newW = img.getWidth() - newX;
			}
			if(newY + newH > img.getHeight())
			{
				newH = newH - ((newY + newH) - img.getHeight());
				//				newH = img.getHeight() - newY;
			}

		}
		Rectangle r = new Rectangle(newX,newY,newH,newW);
		return r;
	}
	static public void pause(String print)
	{
		System.out.println("Pause: " + print);
		reader.nextLine(); 
	}

}
