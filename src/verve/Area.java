package verve;
import java.util.ArrayList;

import org.openqa.selenium.Rectangle;

public class Area {
	ArrayList <TargetArea> targetAreas;
	Rectangle area;
	int viewMaxWidth;
	int viewMaxHeight;
	public Area(Rectangle area, int viewMaxWidth, int viewMaxHeight) 
	{
		this.viewMaxWidth = viewMaxWidth;
		this.viewMaxHeight = viewMaxHeight;
		targetAreas = new ArrayList <TargetArea>();
		this.area = area;
		targetAreas = fitToView(area);
	}
	public boolean checkSides()
	{
		for(TargetArea targetArea: targetAreas)
		{
			if(targetArea.checkSides())
			{
				return true;
			}
		}
		return false;
	}
	public ArrayList<TargetArea> fitToView(Rectangle rec) //cut the rectangle in to viewable portions
	{
		ArrayList<TargetArea> results = new ArrayList<TargetArea>();
		ArrayList<TargetArea> resultsW = new ArrayList<TargetArea>();
		int areaSize = rec.height * rec.width;
		if(rec.width > viewMaxWidth)
		{
			int portion = rec.width / viewMaxWidth;
			for(int i=0; i < portion; i++)
			{
				resultsW.add(new TargetArea(new Rectangle((rec.x + (viewMaxWidth*i)), rec.y, rec.height, viewMaxWidth)));
			}
			if((portion * viewMaxWidth) < rec.width)
			{
				int widthLeftOver = rec.width - (portion * viewMaxWidth);
				resultsW.add(new TargetArea(new Rectangle((rec.x + (viewMaxWidth*portion)), rec.y, rec.height, widthLeftOver)));
			}

		}
		else
		{
			resultsW.add(new TargetArea(rec));
		}
		if(rec.height > viewMaxHeight)
		{
			if(!resultsW.isEmpty())
			{
				for(TargetArea tA : resultsW)
				{
					int portion = rec.height / viewMaxHeight;
					for(int i=0; i < portion; i++)
					{
						results.add(new TargetArea(new Rectangle(tA.area.x, (tA.area.y + (viewMaxHeight*i)), viewMaxHeight, tA.area.width)));
					}
					if((portion * viewMaxHeight) < rec.height)
					{
						int heightLeftOver = rec.height - (portion * viewMaxHeight);
						results.add(new TargetArea(new Rectangle(tA.area.x , (tA.area.y + (viewMaxHeight*portion)), heightLeftOver, rec.width)));
					}
				}
			}
		}
		else
		{
			int newArea = 0;
			for(TargetArea tA : resultsW)
			{
				newArea = newArea + (tA.area.height * tA.area.width);
			}
			if(newArea != areaSize)
			{
				System.out.println("Rectangle slicing error: (Original Area, New Area) (" + areaSize + "," + newArea + ")");
				System.exit(5);
			}
			return resultsW;
		}
		int newArea = 0;
		for(TargetArea tA : results)
		{
			newArea = newArea + (tA.area.height * tA.area.width);
		}
		if(newArea != areaSize)
		{
			System.out.println("Rectangle slicing error: (Original Area, New Area) (" + areaSize + "," + newArea + ")");
			System.exit(5);
		}
		return results;
	}

}
