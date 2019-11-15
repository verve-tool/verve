package verve;
/*
 * Created by Ibrahim Althomali on 12/17/2017
 * 
 ********************************************/


import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.openqa.selenium.WebElement;


public class Webpage 
{
	String siteName;
	String wPageName;
	String wPagePath;
	String uniqueRunName;
	ArrayList<String>	modHTMLPaths; 
	ArrayList<Failure>	Failures;
	
	//three arrays to contain the viewport, screenshot, and related failures for each viewport reuse the same layout image.
	ArrayList<Integer> viewports = new ArrayList<Integer>();; 
	ArrayList<BufferedImage> screenshots = new ArrayList<BufferedImage>();
	ArrayList<Boolean> captured = new ArrayList<Boolean>();
	ArrayList<ArrayList<Failure>> pendingFailures = new ArrayList<ArrayList<Failure>>();
	ArrayList<HistogramDifference> histogramsDifferences = new ArrayList<HistogramDifference>();
	
	public Webpage(String wPagePath)
	{
		this.wPagePath = wPagePath;
		modHTMLPaths = new ArrayList<String>();
		Failures = new ArrayList<Failure>();
	}
	public Webpage()
	{
		modHTMLPaths = new ArrayList<String>();
		Failures = new ArrayList<Failure>();
	}

}
