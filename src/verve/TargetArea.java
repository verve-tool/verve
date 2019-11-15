package verve;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.openqa.selenium.Rectangle;

public class TargetArea {

	ArrayList<BufferedImage> targetImgs; //images of the area.
	Rectangle area;
	public TargetArea(Rectangle area) 
	{
		targetImgs = new ArrayList <BufferedImage>();
		this.area = area;
	}
//	public boolean imgCollisionCheck()
//	{
//		BufferedImage background = targetImgs.get(3);
//		BufferedImage firstOnlyOpaque = targetImgs.get(1);
//		BufferedImage secondOnlyOpaque = targetImgs.get(2);
//
//
//			for (int i = 0; i < background.getHeight(); i++) 
//		    {
//		        for (int j = 0; j < background.getWidth(); j++) 
//		        {
//		            int rgb1 = background.getRGB(j, i);
//		            int rgb2 = secondOnlyOpaque.getRGB(j, i);
//		            int rgb3 = firstOnlyOpaque.getRGB(j, i);
//		            if(rgb1 != rgb2 && rgb2 != rgb3 && rgb3 != rgb1)//If all three image pixels are different
//		            {
//		            	return true;
//		            }
//		        }
//		    }
//
//
//
//		return false;
//		
//	}
	public boolean checkAreaOfImgs()
	{
		BufferedImage background = targetImgs.get(2);
		BufferedImage firstOnlyOpaque = targetImgs.get(0);
		BufferedImage secondOnlyOpaque = targetImgs.get(1);
		if((background.getWidth() == secondOnlyOpaque.getWidth()) &&
				(secondOnlyOpaque.getWidth() == firstOnlyOpaque.getWidth()) &&
				(background.getHeight() ==  secondOnlyOpaque.getHeight()) &&
				(secondOnlyOpaque.getHeight() == firstOnlyOpaque.getHeight()))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public boolean checkSides()
	{
		if((area.getWidth() > 0 ) && (area.getHeight() > 0))
		{
			return false;
		}
		return true;

	}
	public boolean pixelCheck(boolean HTMLParentRelationship, boolean collision,boolean featureHiddenDetection)
	{
		BufferedImage background = targetImgs.get(0);
		BufferedImage firstOnlyOpaque = targetImgs.get(1);
		BufferedImage secondOnlyOpaque = targetImgs.get(2);
		BufferedImage BothOpaque = null;
		if(!HTMLParentRelationship && collision)
			 BothOpaque = targetImgs.get(3);

		for (int i = 0; i < background.getHeight(); i++) 
	    {
	        for (int j = 0; j < background.getWidth(); j++) 
	        {
	            int rgb1 = background.getRGB(j, i);
	            int rgb2 = secondOnlyOpaque.getRGB(j, i);
	            int rgb3 = firstOnlyOpaque.getRGB(j, i);
	            int rgb4 = 0;
	            if(!HTMLParentRelationship && collision)
	            		rgb4 = BothOpaque.getRGB(j, i);
	            if(rgb1 != rgb2 && rgb1 != rgb3)
	            {
	            		return true;
	            }
	            if(collision & featureHiddenDetection)
	            {
	            		
	            		if(rgb1 != rgb2)
		            {
		            		if(HTMLParentRelationship)
		            		{
		            			if(rgb2 != rgb3)
		            			{
		            				System.out.println("Special Case Found - Element Hidden");
		            				return true;
		            			}
		            		}
		            		else
		            		{
		            			if(rgb2 != rgb4)
		            			{
		            				System.out.println("Special Case Found - Element Hidden");
		            				return true;
		            			}
		            		}
		            }
		            else if(rgb1 != rgb3)
		            {
		            		if(!HTMLParentRelationship)
		            			if(rgb3 != rgb4)
		            			{
		            				System.out.println("Special Case Found - Element Hidden");
		            				return true;
		            			}
		            }
	            }
	        }
	    }


		return false;
	}
	public boolean anyChange()
	{
		BufferedImage background = targetImgs.get(0);
		BufferedImage withElement = targetImgs.get(1);


			for (int i = 0; i < background.getHeight(); i++) 
		    {
		        for (int j = 0; j < background.getWidth(); j++) 
		        {
		            int rgb1 = background.getRGB(j, i);
		            int rgb2 = withElement.getRGB(j, i);

		            if(rgb1 != rgb2)
		            {
		            	return true;
		            }
		        }
		    }
		return false;
	}


}
