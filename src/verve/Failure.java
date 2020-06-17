package verve;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.opencv.core.Mat;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

public class Failure
{
	int viewMin;
	int viewMax;
	int savedViewMin;
	int savedViewMax;
	int captureView;
	boolean NOI = true;
	boolean ignored = false;
	boolean falsePositive = false;
	boolean overlapping = false;
	boolean protruding = false;
	boolean segregated = false;
	boolean wrapped = false;
	boolean bestEffort = false;
	boolean printFlagR = false; //flag to print recategorization images of right side
	boolean printFlagL = false; //flag to print recategorization images of left side
	boolean printFlagT = false; //flag to print recategorization images of top side
	boolean printFlagB = false; //flag to print recategorization images of bottom side
	boolean HTMLParentRelationship = false; //is there a HTML parent child relationship between the two xpaths.
	
	int detectedMin = -1;
	int detectedMax = -1;
	
	//String euclideanClassification = "Not classified using distance"; //for small-range if using euclidean calculations;

	String type = null;
	
	String chiClassification;
	String bhaClassification;
	String corClassification;
	String kldClassification;
	String altClassification;
	String intClassification;
	String classificationMethod;
	String smallrangeThisConstraints = ""; //ReDeCheck output as reasoning for reporting the small range failure.
	String smallrangePrevConstraints = ""; // includes range
	String smallrangeNextConstraints = ""; // includes range
	Failure minOracle; //Hold information about correct layout at min-1 for small-range failures. NOT A FAILURE
	Failure maxOracle; //Hold information about correct layout at max+1 for small-range failures. NOT A FAILURE
	Histogram histogram;
	Histogram prevDiff;
	Histogram nextDiff;
	int euclideanDistanceTL; // for small-range fialures. 
	int euclideanDistanceTR; // for small-range fialures. 
	int euclideanDistanceBL; // for small-range fialures. 
	int euclideanDistanceBR; // for small-range fialures. 
	ArrayList<Integer> rectangleEuclideanDistance = new ArrayList<Integer>();
	int euclideanDistanceRect1;
	int euclideanDistanceRect2;

	int viewMaxWidth;
	int viewMaxHeight;
	int ID;
	int FirstImageScrollOffsetX;
	int FirstImageScrollOffsetY;
	int IndexOfLastElementInLine; //Indicates the index of the last element in line for wrapped failures.
	boolean alreadyMatchedViewportRange =false; //used to match two different Redecheck output (step1 and original using binarysearch)
	ArrayList<Results> histogramResults = new ArrayList<Results>();

	BufferedImage screenshotP;
	String priorCat = "";
	ArrayList<String> xpaths; //xpaths of web elements
	ArrayList<Integer> problemXpathID;
	ArrayList<WebElement> wbElements; //DOM web elements
	ArrayList<BufferedImage> imgs; 
	ArrayList<Rectangle> rectangles;//DOM Rectangles of web elements (Maybe changed from original to fit to visible coordinates)
	ArrayList<Rectangle> orignalRectangles;//Original DOM Rectangles of web elements to use for printing and assessments
	ArrayList<String> notes; //notes or log if needed
	ArrayList<String> titles; //titles for notes or logs if needed
	
	HashMap<Integer, String> DOMCheckResults; // results of checking the DOM for this failures
	
	ArrayList<Area> protrudingArea;
	Area overlappedArea;
	Area segregatedArea;
	Area wrappedArea;
	Area maxAOC;

	//OpenCV Histogram
	ArrayList<Mat> histogramMats;


	Dimension reachRightDim = null;
	Dimension reachLeftDim = null;
	Dimension reachTopDim = null;
	Dimension reachBottomDim = null;
	//for viewport protrusion best effort (protruding element)
	float percentWidthRight = 0;
	float percentWidthLeft = 0;
	float percentHeightTop = 0;
	float percentHeightBottom = 0;

	long startTime;
	long endTime;
	long duration;
	public void startTime()
	{
		startTime = System.nanoTime();
	}
	public void endTime()
	{
		endTime = System.nanoTime();
		duration = endTime - startTime;
	}
	public long getDurationNano()
	{
		return duration;
	}
	public long getDurationMilli()
	{
		return duration/1000000;
	}
	public Failure()
	{
		xpaths = new ArrayList<String>();
		wbElements = new ArrayList<WebElement>();
		imgs = new ArrayList<BufferedImage>();
		rectangles = new ArrayList<Rectangle>();
		orignalRectangles = new ArrayList<Rectangle>();
		notes = new ArrayList<String>();
		titles = new ArrayList<String>();
		problemXpathID = new ArrayList<Integer>();
		DOMCheckResults = new HashMap<Integer, String>();
	}
	public void classifyAllHistograms() {
		int methodNumber = 0;
		for(String methodName : histogramResults.get(0).methods) {
			if(methodName.equals("Correlation")) {
				maximumClassification(methodNumber);
			}else if(methodName.equals("Intersection")) {
				baseClassify(methodNumber);
			}else if (methodName.equals("Kullback-Leibler Divergence")) {
				minimumAbsoluteValueClassification(methodNumber);
			}else{
				minimumClassification(methodNumber);
			}
			methodNumber++;
		}
		classifyUsingMethod(Assist.useHistogramMethod);
	}
	private void classifyUsingMethod(String methodName)
	{
		String result = "";
		if(methodName.equals("Correlation")) {
			classificationMethod = "Correlation";
			result = corClassification;
		}else if(methodName.equals("Chi-Square")) {
			classificationMethod = "Chi-Square";
			result = chiClassification;
		}else if(methodName.equals("Intersection")) {
			classificationMethod = "Intersection";
			result = intClassification;
		}else if(methodName.equals("Bhattacharyya distance")) {
			classificationMethod = "Bhattacharyya distance";
			result = bhaClassification;
		}else if(methodName.equals("Altenative Chi-Square")) {
			classificationMethod = "Altenative Chi-Square";
			result = altClassification;
		}else if(methodName.equals("Kullback-Leibler Divergence")) {
			classificationMethod = "Kullback-Leibler Divergence";
			result = kldClassification;
		}
		if(result.equals("TP")) {
			setFalsePositive(false);
		}else if(result.equals("FP")){
			setFalsePositive(true);
		}
	}
	public String getClassificationForMethod(int methodNumber)
	{
		String methodName = histogramResults.get(0).methods.get(methodNumber);
		if(methodName.equals("Correlation")) {
			return corClassification;
		}else if(methodName.equals("Chi-Square")) {
			return chiClassification;
		}else if(methodName.equals("Intersection")) {
			return intClassification;
		}else if(methodName.equals("Bhattacharyya distance")) {
			return bhaClassification;
		}else if(methodName.equals("Altenative Chi-Square")) {
			return altClassification;
		}else if(methodName.equals("Kullback-Leibler Divergence")) {
			return kldClassification;
		}
		return "Unkown Method";
	}
	private void baseClassify(int method) { //Intersection using MAX 
		double threshold = getThreshold(method);
		for(Results compared : histogramResults) {
			double closerHistogram = Double.parseDouble(Assist.decimalFormat.format(Math.max(compared.minOracle.get(method),compared.maxOracle.get(method)))); 
			double base = Double.parseDouble(Assist.decimalFormat.format(compared.base.get(method)));
			double value = (base-closerHistogram) / base;
			if( value >= threshold){
				//System.out.println("Base: " + Double.parseDouble(Assist.decimalFormat.format(compared.base.get(method))) + " Oracle: " + closerHistogram + " Change: " +value);
				setMethodClassification(method, "TP");
				return;
			}
		}
		setMethodClassification(method, "FP");
	}
	private void maximumClassification(int method){ //Correlation
		double threshold = getThreshold(method);
		for(Results compared : histogramResults) {
			if(Double.parseDouble(Assist.decimalFormat.format(Math.max(compared.minOracle.get(method),compared.maxOracle.get(method)))) < threshold){
				setMethodClassification(method, "TP");
				return;
			}
		}
		setMethodClassification(method, "FP");
	}
	private void minimumClassification(int method) { //Bhattacharyya, Chi-Square, Kullback-Leibler-Divergence, Alternative-Chi-Square
		double threshold = getThreshold(method);
		for(Results compared : histogramResults) {
			if(Double.parseDouble(Assist.decimalFormat.format(Math.min(compared.minOracle.get(method),compared.maxOracle.get(method)))) >= threshold){
				setMethodClassification(method, "TP");
				return;
			}
		}
		setMethodClassification(method, "FP");
	}
	private void minimumAbsoluteValueClassification(int method) { //Kullback-Leibler-Divergence
		double threshold = getThreshold(method);
		for(Results compared : histogramResults) {
			if(Double.parseDouble(Assist.decimalFormat.format(Math.min(Math.abs(compared.minOracle.get(method)),Math.abs(compared.maxOracle.get(method))))) >= threshold){
				setMethodClassification(method, "TP");
				return;
			}
		}
		setMethodClassification(method, "FP");
	}
	private double getThreshold(int method) {
		String methodName = histogramResults.get(0).methods.get(method);
		if(methodName.equals("Correlation")) {
			return Assist.corThreshold;
		}else if(methodName.equals("Chi-Square")) {
			return Assist.chiThreshold;
		}else if(methodName.equals("Intersection")) {
			return Assist.intThreshold;
		}else if(methodName.equals("Bhattacharyya distance")) {
			return Assist.bhaThreshold;
		}else if(methodName.equals("Altenative Chi-Square")) {
			return Assist.altThreshold;
		}else if(methodName.equals("Kullback-Leibler Divergence")) {
			return Assist.kldThreshold;
		}
		return -99;
	}

	private void setMethodClassification(int method, String classification) {
		String methodName = histogramResults.get(0).methods.get(method);
		if(methodName.equals("Correlation")) {
			corClassification = classification;
		}else if(methodName.equals("Chi-Square")) {
			chiClassification = classification;
		}else if(methodName.equals("Intersection")) {
			intClassification = classification;
		}else if(methodName.equals("Bhattacharyya distance")) {
			bhaClassification = classification;
		}else if(methodName.equals("Altenative Chi-Square")) {
			altClassification = classification;
		}else if(methodName.equals("Kullback-Leibler Divergence")) {
			kldClassification = classification;
		}
	}
	public void unreachableDimensions(DriverManager dm)
	{
		if(bestEffort && type.equals("viewport"))
		{
			if(falsePositive || NOI || ignored)
			{
				for(int xpathID: problemXpathID)
				{
					WebElement wb = dm.getWebElem(xpaths.get(xpathID));
					Rectangle wbRec = new Rectangle(wb.getLocation(),wb.getSize());
					dm.scroll(wbRec);

					if(wbRec.x < 0 || wbRec.y < 0 || dm.cantReachX > 0 || dm.cantReachY > 0)
					{
						//System.out.println("Problematic full rectangle X:" + wbRec.x + " Y:"+ wbRec.y + " Width:"+ wbRec.width + " height:" + wbRec.height);
						if(dm.cantReachX > 0)
						{
							reachRightDim = new Dimension( dm.cantReachX<viewMaxWidth?dm.cantReachX:viewMaxWidth, Math.min(viewMaxHeight,wbRec.height));
							titles.add("ReCat Criteria:");
							notes.add("Cant reach X Right by ("+dm.cantReachX+")");
							//System.out.println("Cant reach X Right by ("+dm.cantReachX+")");
							percentWidthRight = (float)dm.cantReachX/wbRec.width;
//							titles.add("ReCat Percentage Criteria:");
//							notes.add("Width Right by ("+percentWidthRight+")");
						}
						if(dm.cantReachY > 0)
						{
							reachBottomDim = new Dimension(Math.min(viewMaxWidth,wbRec.width), dm.cantReachY<viewMaxHeight?dm.cantReachY:viewMaxHeight);
							titles.add("ReCat Criteria:");
							notes.add("Cant reach Y Bottom by ("+dm.cantReachY+")");
							//System.out.println("Cant reach Y Bottom by ("+dm.cantReachY+")");
							percentHeightBottom = (float)dm.cantReachY/wbRec.height;
//							titles.add("ReCat Percentage Criteria:");
//							notes.add("Height Bottom by ("+percentHeightBottom+")");
						}
						if(wbRec.x < 0)
						{
							reachLeftDim = new Dimension(Math.min(viewMaxWidth,Math.abs(wbRec.x)), Math.min(viewMaxHeight,wbRec.height));
							titles.add("ReCat Criteria:");
							notes.add("Cant reach X Left by ("+wbRec.x+")");
							//System.out.println("Cant reach X Left by ("+wbRec.x+")");
							percentWidthLeft = (float)(wbRec.x * -1)/wbRec.width;
//							titles.add("ReCat Percentage Criteria:");
//							notes.add("Width Left by ("+percentWidthLeft+")");
						}
						if(wbRec.y < 0)
						{
							reachTopDim = new Dimension(Math.min(viewMaxWidth,wbRec.width), Math.min(viewMaxHeight,Math.abs(wbRec.y)));
							titles.add("ReCat Criteria:");
							notes.add("Cant reach Y Top by ("+wbRec.y+")");
							//System.out.println("Cant reach Y Top by ("+wbRec.y+")");
							percentHeightTop = (float)(wbRec.y * -1) / wbRec.height;
//							titles.add("ReCat Percentage Criteria:");
//							notes.add("Height Top by ("+percentHeightTop+")");
						}
					}
				}
			}
		}
	}
	public void saveRecategorizedImages(DriverManager dm, String siteName, Area area, String categorization) {
		try {
			ImageIO.write(screenshotP, "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +0+".png"));
			if(area == null)
			{
				ImageIO.write(screenshotP, "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +1+".png"));
				return;
			}
			saveImages(siteName, dm.scrollX, dm.scrollY);
			Graphics2D g = screenshotP.createGraphics();
			Rectangle r1 = cutRectangleToVisibleArea(dm.scrollX, dm.scrollY, area.area, screenshotP);
			g.setColor(Color.BLACK);
			g.setStroke(Assist.dashed1);
			g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
			g.setColor(Color.ORANGE);
			g.setStroke(Assist.dashed2);
			g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
			ImageIO.write(screenshotP, "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +1+".png"));			
			for(TargetArea TA : area.targetAreas)
			{
				for (int i=1; i <= TA.targetImgs.size(); i++)
				{
					try 
					{
						ImageIO.write(TA.targetImgs.get(i-1), "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type + "_" + siteName + "_"+ viewMin + "_" + viewMax + "_" + "Recategorized_" + i +".png"));
					} catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveRecatImages(BufferedImage screenshot, DriverManager dm, String siteName, TargetArea area, String categorization, String side) {
		try {
			if(area == null)
			{
				ImageIO.write(screenshot, "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +1+".png"));
				return;
			}
			for (int i=1; i <= area.targetImgs.size(); i++)
			{
				try 
				{
					ImageIO.write(area.targetImgs.get(i-1), "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type + "_" + siteName + "_"+ viewMin + "_" + viewMax + "_" + "Recat_Side_"+ side + i +".png"));
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
			saveImages(siteName, dm.scrollX, dm.scrollY);
			Graphics2D g = screenshot.createGraphics();
			Rectangle r1 = cutRectangleToVisibleArea(dm.scrollX, dm.scrollY, area.area, screenshot);
			g.setColor(Color.BLACK);
			g.setStroke(Assist.dashed1);
			g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
			g.setColor(Color.ORANGE);
			g.setStroke(Assist.dashed2);
			g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
			ImageIO.write(screenshot, "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type+ "_" + siteName + "_" + viewMin + "px_"+ viewMax + "px_"+categorization+"_" +1+".png"));			

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void newCat(String newCat)
	{

		if(NOI)
		{
			priorCat = "NOI";			
		}
		if(falsePositive)
		{
			priorCat = "FP";

		}
		if(ignored)
		{
			priorCat = "ignored";

		}
		NOI = false;
		ignored = false;
		falsePositive = false;

		if(newCat.equals("NOI"))
		{
			NOI = true;
		}else if(newCat.equals("FP"))
		{
			falsePositive = true;
		}else if(newCat.equals("ignored"))
		{
			ignored = true;
		}
		titles.add("ReCat Success:");
		notes.add("(" +priorCat + ") confirmed as (" + newCat +")");
	}
	public void findAreasOfConcern()
	{
		if(type.equals("wrapping")) 
			wrappedArea = wrappedArea(rectangles);

		if(type.equals("wrapping") || type.equals("small-range")) {
			maxAOC = maxRectangleArea();
			if (maxAOC.checkSides()) 
			{
				setFalsePositive(true);
				titles.add("Zero Size Error:");
				notes.add("Wrapped area height or width is zero or negative");
				return;
			}
		}else {
			protrudingArea = protrudingAreas(rectangles.get(0), rectangles.get(1));
			overlappedArea = overlappedArea(rectangles.get(0), rectangles.get(1));
			segregatedArea = segregatedArea(rectangles.get(0), rectangles.get(1));
		}
		if(protruding)
		{
			for(Area a : protrudingArea)
			{
				if (a.checkSides()) 
				{
					//setIgnore(true);
					titles.add("Zero Size Error:");
					notes.add("A protruding area height or width is zero or negative");
					protrudingArea.remove(a);
				}
			}
			if(protrudingArea.isEmpty())
			{
				setFalsePositive(true);
				return;
			}
		}
		if(overlapping)
		{
			if (overlappedArea.checkSides()) 
			{
				setFalsePositive(true);
				titles.add("Zero Size Error:");
				notes.add("Overlapped area height or width is zero or negative");
				return;
			}
		}
		if(segregated)
		{
			if (segregatedArea.checkSides()) 
			{
				setFalsePositive(true);
				titles.add("Zero Size Error:");
				notes.add("segregated area height or width is zero or negative");
				return;
			}
		}
		if(!falsePositive)
		{
			findFalsePositive();
		}


	}
	public Rectangle getLineEndRect() {
		Rectangle last = null;
		for(int i=1; i<rectangles.size(); i++) { //skip the reported wrapped element
			Rectangle current = rectangles.get(i);
			if(last == null) {
				last = current;
				IndexOfLastElementInLine = i;
			}
			else {
				if(last.x + last.width < current.x + current.width) {
					last = current;
					IndexOfLastElementInLine = i;
				}
			}

		}
		return last;
	}
	public void findFalsePositive()
	{
		if(type.equals("collision"))
		{
			if(segregated)
			{
				setFalsePositive(true);
			}
		}
		else if(type.equals("protrusion") || type.equals("viewport"))
		{
			if(overlapping == true && protruding == false)
			{
				setFalsePositive(true);
			}
		}
		else if(type.equals("wrapping") && wrapped == false) {
			setFalsePositive(true);
		}
	}

	public ArrayList<Rectangle> fitToView(Rectangle target) //cut the rectangle in to viewable portions
	{
		ArrayList<Rectangle> results = new ArrayList<Rectangle>();
		ArrayList<Rectangle> resultsW = new ArrayList<Rectangle>();
		int area = target.height * target.width;
		if(target.width > viewMaxWidth)
		{
			int portion = target.width / viewMaxWidth;
			for(int i=0; i < portion; i++)
			{
				resultsW.add(new Rectangle((target.x + (viewMaxWidth*i)), target.y, target.height, viewMaxWidth));
			}
			if((portion * viewMaxWidth) < target.width)
			{
				int widthLeftOver = target.width - (portion * viewMaxWidth);
				resultsW.add(new Rectangle((target.x + (viewMaxWidth*portion)), target.y, target.height, widthLeftOver));
			}

		}
		else
		{
			resultsW.add(target);
		}
		if(target.height > viewMaxHeight)
		{
			if(!resultsW.isEmpty())
			{
				for(Rectangle r : resultsW)
				{
					int portion = target.height / viewMaxHeight;
					for(int i=0; i < portion; i++)
					{
						results.add(new Rectangle(r.x, (r.y + (viewMaxHeight*i)), viewMaxHeight, r.width));
					}
					if((portion * viewMaxHeight) < target.height)
					{
						int heightLeftOver = target.height - (portion * viewMaxHeight);
						results.add(new Rectangle(r.x , (r.y + (viewMaxHeight*portion)), heightLeftOver, target.width));
					}
				}
			}
		}
		else
		{
			int newArea = 0;
			for(Rectangle r : resultsW)
			{
				newArea = newArea + (r.height * r.width);
			}
			if(newArea != area)
			{
				System.out.println("Rectangle slicing error: (Original Area, New Area) (" + area + "," + newArea + ")");
				System.exit(5);
			}
			return resultsW;
		}
		int newArea = 0;
		for(Rectangle r : results)
		{
			newArea = newArea + (r.height * r.width);
		}
		if(newArea != area)
		{
			System.out.println("Rectangle slicing error: (Original Area, New Area) (" + area + "," + newArea + ")");
			System.exit(5);
		}
		return results;
	}
	public ArrayList<Area> protrudingAreas(Rectangle firstR, Rectangle secondR) //returns protruding rectangles only
	{
		java.awt.Rectangle child;
		java.awt.Rectangle parent;
		parent = new java.awt.Rectangle(firstR.getX(), firstR.getY(), firstR.getWidth(), firstR.getHeight());
		child = new java.awt.Rectangle(secondR.getX(), secondR.getY(), secondR.getWidth(), secondR.getHeight());
		java.awt.Rectangle []protrudingRecAWT = SwingUtilities.computeDifference(child, parent);
		ArrayList<Area>  protrudingArea = new ArrayList<Area>();
		if(protrudingRecAWT.length > 0)
		{

			for(int i = 0; i < protrudingRecAWT.length; i++)
			{
				Rectangle r1 = new Rectangle(protrudingRecAWT[i].x, protrudingRecAWT[i].y, protrudingRecAWT[i].height, protrudingRecAWT[i].width);
				if(r1.height > 0 && r1.width > 0)
				{
					protrudingArea.add(new Area(r1, viewMaxWidth, viewMaxHeight));
				}
				else
				{
					titles.add("Protrusion excluded:");
					notes.add("AWT rectangle if Height,Width("+r1.height+","+r1.width+")");
				}
			}
			if(protrudingArea.size() > 0)
			{
				protruding = true;
				//System.out.println("----------------PROTRUDING");
			}
		}
		return protrudingArea;
	}
	public Area segregatedArea(Rectangle firstR, Rectangle secondR) //must be checked last!!!!!
	{
		if(protruding == false && overlapping == false)
		{
			segregated = true;
			//System.out.println("----------------SEGREGATED");
			if((firstR.height * firstR.width) <= (secondR.height * secondR.width))
			{
				return new Area(firstR, viewMaxWidth, viewMaxHeight);
			}
			else
			{
				return new Area(secondR, viewMaxWidth, viewMaxHeight);
			}

		}
		return null;
	}
	public Area maxRectangleArea() 
	{
		int minX1 = this.rectangles.get(0).x;
		int maxX2 = this.rectangles.get(0).x+this.rectangles.get(0).width;
		int minY1 = this.rectangles.get(0).y;
		int maxY2 = this.rectangles.get(0).y+this.rectangles.get(0).height;
		for(int i =1; i < this.rectangles.size(); i++) {
			Rectangle r = this.rectangles.get(i);
			minX1 = Math.min(minX1, r.x);
			maxX2 = Math.max(maxX2, r.x+r.width);
			minY1 = Math.min(minY1, r.y);
			maxY2 = Math.max(maxY2, r.y+r.height);
		}
		Rectangle aoc = new Rectangle(minX1, minY1, maxY2 - minY1, maxX2 - minX1);
		return new Area(aoc, viewMaxWidth, viewMaxHeight);
	}
	public boolean isFirstAboveSecond(Rectangle first, Rectangle second) {
		return (first.y+first.height) <= second.y; 
	}
	public boolean wrappedBellowAllElements(ArrayList<Rectangle> rectangles) {
		Rectangle wrapped = rectangles.get(0);
		for(int i = 1; i < rectangles.size(); i++)
			if(!isFirstAboveSecond(rectangles.get(i), wrapped))
				return false;
		return true;
	}
	public Area wrappedArea(ArrayList<Rectangle> rectangles) {
		if(wrappedBellowAllElements(rectangles)) {
			wrapped = true;
			return new Area(rectangles.get(0), viewMaxWidth, viewMaxHeight);
		}else {
			return null;
		}
			
	}
	public Area wrappedArea(Rectangle wrappedRect, Rectangle lineEndRect) 
	{
		if(wrappedRect.y+wrappedRect.height <= lineEndRect.y + lineEndRect.height)
		{
			//did not wrap
			titles.add("Wrapping excluded:"); 
			notes.add("AWT rectangle of Wrapped.y2,lineEndElement.y2("+wrappedRect.y+wrappedRect.height+","+lineEndRect.y + lineEndRect.height+")");
			return null;
		}
		if(wrappedRect.x+wrappedRect.width < lineEndRect.x + lineEndRect.width)
		{
			//System.out.println("----------------WRAPPED");
			wrapped = true;
			return new Area(wrappedRect, viewMaxWidth, viewMaxHeight);
		}
		else if (wrappedRect.x < lineEndRect.x + lineEndRect.width)
		{
			Rectangle partialWrapRect = new Rectangle(wrappedRect.x, wrappedRect.y, wrappedRect.height, lineEndRect.x + lineEndRect.width  - wrappedRect.x);
			if(partialWrapRect.width > 0 && partialWrapRect.height > 0)
			{
				wrapped = true;
				//System.out.println("----------------WRAPPED");
				return new Area(partialWrapRect, viewMaxWidth, viewMaxHeight);
			}
			else
			{
				titles.add("Wrapping excluded:");
				notes.add("AWT rectangle of Height,Width("+partialWrapRect.height+","+partialWrapRect.width+")");
			}
		} 

		return null;
	}
	public Area overlappedArea(Rectangle firstR, Rectangle secondR)
	{
		if(Assist.intersectingRec(firstR, secondR)) //check if they actually intersect
		{

			Rectangle overlapRec = Assist.intrscRec(rectangles.get(0), rectangles.get(1));
			if(overlapRec.width > 0 && overlapRec.height > 0)
			{
				overlapping = true;
				//System.out.println("----------------OVERLAPPING");
				return new Area(overlapRec, viewMaxWidth, viewMaxHeight);
			}
			else
			{
				titles.add("Overlap excluded:");
				notes.add("AWT rectangle of Height,Width("+overlapRec.height+","+overlapRec.width+")");
			}
		}
		return null;
	}
	public void addXpath(String xpath)
	{
		//		if(xpaths.isEmpty())
		//		{
		xpaths.add(xpath);
		//		}
		//		else
		//		{
		//			if(xpaths.get(0).length() > xpath.length())
		//			{
		//				xpaths.add(0, xpath);
		//			}
		//			else
		//			{
		//				xpaths.add(xpath);
		//			}
		//		}
	}
	public void setCaptureView() {
		captureView = viewMin;
	}
	public void writeImages(String sitename)
	{
		if(protruding)
		{
			for(int x = 1; x <= protrudingArea.size(); x++)
			{
				for(TargetArea TA : protrudingArea.get(x-1).targetAreas) 
				{
					for (int i=1; i <= TA.targetImgs.size(); i++)
					{
						try 
						{
							ImageIO.write(TA.targetImgs.get(i-1), "png", new File(Assist.outDirectory + File.separator + "ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_" + "protruding_" + x + "_TargetArea_"+ i +".png"));
						} catch (IOException e) 
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		if(overlapping)
		{
			for(TargetArea TA : overlappedArea.targetAreas)
			{
				for (int i=1; i <= TA.targetImgs.size(); i++)
				{
					try 
					{
						ImageIO.write(TA.targetImgs.get(i-1), "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_" + "overlapping_"+ i +".png"));
					} catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
		}
		if(segregated)
		{
			for(TargetArea TA : segregatedArea.targetAreas)
			{
				for (int i=1; i <= TA.targetImgs.size(); i++)
				{
					try 
					{
						ImageIO.write(TA.targetImgs.get(i-1), "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_" + "segregated_" + i +".png"));
					} catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
		}
		if(wrapped)
		{
			for(TargetArea TA : wrappedArea.targetAreas)
			{
				for (int i=1; i <= TA.targetImgs.size(); i++)
				{
					try 
					{
						ImageIO.write(TA.targetImgs.get(i-1), "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_" + "wrapped_" + i +".png"));
					} catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
		}
		if(type.equals("small-range"))
		{
			try {
				for(int i=1; i < imgs.size(); i++) {
					ImageIO.write(imgs.get(i), "png", new File(Assist.outDirectory + File.separator  + "Compare_ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_capture_" + captureView + "_" + i +".png"));
					ImageIO.write(maxOracle.imgs.get(i), "png", new File(Assist.outDirectory + File.separator  + "Compare_ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_capture_" + maxOracle.captureView + "_" + i +".png"));
					if(type.equals("small-range"))
						ImageIO.write(minOracle.imgs.get(i), "png", new File(Assist.outDirectory + File.separator  + "Compare_ID_" + ID + "_" + type + "_" + sitename + "_"+ viewMin + "_" + viewMax + "_capture_" + minOracle.captureView + "_" + i +".png"));
				}


			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void highlightElements(String filename) {
		int offsetX = 0;
		int offsetY = 0;
		//BufferedImage aocImg = Assist.copyImage(imgs.get(0).getSubimage(maxAOC.area.x, maxAOC.area.y, maxAOC.area.width, maxAOC.area.height));
		//createImagesHeightOfElements();

		BufferedImage img = Assist.copyImage(imgs.get(0));
		Graphics2D g = img.createGraphics();

		highlightSingleElement(offsetX, offsetY, img, g, rectangles.get(0), Color.YELLOW);
		for(int i = 1; i < rectangles.size(); i++) {
			highlightSingleElement(offsetX, offsetY, img, g, rectangles.get(i), Color.RED);
		}

		try {
			//ImageIO.write(img.getSubimage(maxAOC.area.x, maxAOC.area.y, maxAOC.area.width, maxAOC.area.height), "png", new File(Assist.outDirectory + File.separator  +  filename));
			ImageIO.write(img.getSubimage(0, Math.max(0,maxAOC.area.y), img.getWidth(), Math.min((img.getHeight()-maxAOC.area.y),maxAOC.area.height)), "png", new File(Assist.outDirectory + File.separator  +  filename));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void createImagesHeightOfElements() {
		BufferedImage aocImg = Assist.copyImage(imgs.get(0).getSubimage(0, rectangles.get(0).y, imgs.get(0).getWidth(), rectangles.get(0).height));
		imgs.add(aocImg);
		if(type.equals("small-range")) {
			aocImg = Assist.copyImage(imgs.get(0).getSubimage(0, rectangles.get(1).y, imgs.get(0).getWidth(), rectangles.get(1).height));
			imgs.add(aocImg);
		}
	}
	public void createImagesRightAndLeftOfElementsWithScrolling() {
		//first element and everything to right of it
		BufferedImage aocImg = Assist.copyImage(imgs.get(0).getSubimage(rectangles.get(0).x, rectangles.get(0).y, imgs.get(0).getWidth()-rectangles.get(0).x, rectangles.get(0).height));
		imgs.add(aocImg);
		//first element and everything to the left of it
		aocImg = Assist.copyImage(imgs.get(0).getSubimage(0, rectangles.get(0).y, rectangles.get(0).x+rectangles.get(0).getWidth(), rectangles.get(0).height));
		imgs.add(aocImg);
		if(type.equals("small-range")) {
			//second element and everything to right of it
			aocImg = Assist.copyImage(imgs.get(0).getSubimage(rectangles.get(1).x, rectangles.get(1).y, imgs.get(0).getWidth()-rectangles.get(1).x, rectangles.get(1).height));
			imgs.add(aocImg);
			//second element and everything to the left of it
			aocImg = Assist.copyImage(imgs.get(0).getSubimage(0, rectangles.get(1).y, rectangles.get(1).x+rectangles.get(1).getWidth(), rectangles.get(1).height));
			imgs.add(aocImg);
		}
	}
	public void createImagesRightAndLeftOfElements() {
		BufferedImage pageImage = imgs.get(0);
		for(Rectangle rectangle : rectangles) {
			if(rectangle.x < 0) {
				rectangle.width = rectangle.width + rectangle.x;
				rectangle.x = 0;
			}
			if(rectangle.y < 0) {
				rectangle.height =rectangle.height + rectangle.y;
				rectangle.y = 0;
			}
			if(rectangle.x+rectangle.width > pageImage.getWidth()) {
				rectangle.width = pageImage.getWidth() - rectangle.x;
			}
			if(rectangle.y+rectangle.height > pageImage.getHeight()) {
				rectangle.height = pageImage.getHeight() - rectangle.y;
			}
			//element and everything to right of it
			BufferedImage aocImg = Assist.copyImage(pageImage.getSubimage(rectangle.x, rectangle.y, pageImage.getWidth() - rectangle.x, rectangle.height));
			imgs.add(aocImg);
			//element and everything to the left of it
			aocImg = Assist.copyImage(pageImage.getSubimage(0, rectangle.y, rectangle.width + rectangle.x, rectangle.height));
			imgs.add(aocImg);
		}
	}
	public void createImagesRightLeftTopBottomOfElements() {
		BufferedImage pageImage = imgs.get(0);
		for(Rectangle rectangle : rectangles) {
			if(rectangle.x < 0) {
				rectangle.width = rectangle.width + rectangle.x;
				rectangle.x = 0;
			}
			if(rectangle.y < 0) {
				rectangle.height =rectangle.height + rectangle.y;
				rectangle.y = 0;
			}
			if(rectangle.x+rectangle.width > pageImage.getWidth()) {
				rectangle.width = pageImage.getWidth() - rectangle.x;
			}
			if(rectangle.y+rectangle.height > pageImage.getHeight()) {
				rectangle.height = pageImage.getHeight() - rectangle.y;
			}

			//element and everything to right of it
			BufferedImage aocImg = Assist.copyImage(pageImage.getSubimage(rectangle.x, rectangle.y, pageImage.getWidth() - rectangle.x, rectangle.height));
			imgs.add(aocImg);
			//element and everything to the left of it
			aocImg = Assist.copyImage(pageImage.getSubimage(0, rectangle.y, rectangle.width + rectangle.x, rectangle.height));
			imgs.add(aocImg);
			//element and everything to the top of it
			aocImg = Assist.copyImage(pageImage.getSubimage(rectangle.x, 0, rectangle.width, rectangle.y+rectangle.height));
			imgs.add(aocImg);
			//element and everything to the bottom of it
			aocImg = Assist.copyImage(pageImage.getSubimage(rectangle.x, rectangle.y, rectangle.width, pageImage.getHeight() - rectangle.y));
			imgs.add(aocImg);
		}
		
	}
	public void createImagesFullpage() {
		BufferedImage pageImage = imgs.get(0);
			//Fullpage
			BufferedImage aocImg = Assist.copyImage(pageImage.getSubimage(0, 0, pageImage.getWidth(), pageImage.getHeight()));
			imgs.add(aocImg);
		
	}
	public void createImagesLimitedRightLeftTopBottomOfElements() {
		BufferedImage pageImage = imgs.get(0);
		for(Rectangle rectangle : rectangles) {
			//element and equal size to right of it
			int width = rectangle.width * 2;
			if(rectangle.x + width > pageImage.getWidth())
				width = pageImage.getWidth()-rectangle.x;
			BufferedImage aocImg = Assist.copyImage(pageImage.getSubimage(rectangle.x, rectangle.y, width, rectangle.height));
			imgs.add(aocImg);
			//element and equal size to the left of it
			int x = rectangle.x - rectangle.width;
			if(x < 0) {
				x = 0;
				width = rectangle.x +rectangle.width;
			}
			else
				width = rectangle.width * 2;
			aocImg = Assist.copyImage(pageImage.getSubimage(x, rectangle.y, width, rectangle.height));
			imgs.add(aocImg);
			//element and equal size to the bottom of it
			int height = rectangle.height * 2;
			if(rectangle.y + height  > pageImage.getHeight())
				height = pageImage.getHeight()-rectangle.y;
			aocImg = Assist.copyImage(pageImage.getSubimage(rectangle.x, rectangle.y, rectangle.width, height));
			imgs.add(aocImg);
			//element and equal size to the top of it
			int y = rectangle.y - rectangle.height;
			if(y < 0) {
				y = 0;
				height = rectangle.y +rectangle.height;
			}
			else
				height = rectangle.height * 2;
			aocImg = Assist.copyImage(pageImage.getSubimage(rectangle.x, y, rectangle.width, height));
			imgs.add(aocImg);
		}
		
	}
	public void createImagesMaxAOC() {
		BufferedImage pageImage = imgs.get(0);
		Rectangle rectangle = maxAOC.area;
		BufferedImage aocImg = Assist.copyImage(pageImage.getSubimage(rectangle.x, rectangle.y, rectangle.getWidth(), rectangle.height));
		imgs.add(aocImg);
	}
	public void highlightSingleElement(int offsetX, int offsetY, BufferedImage img, Graphics2D g, Rectangle rectangle, Color color) {
		Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, rectangle, img);
		g.setColor(Color.BLACK);
		g.setStroke(Assist.dashed1);
		g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); 
		g.setColor(color);
		g.setStroke(Assist.dashed2);
		g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); 
	}
	public void saveImages(String site, int offsetX, int offsetY) throws IOException
	{
		offsetX = FirstImageScrollOffsetX;
		offsetY = FirstImageScrollOffsetY;
		for(int i = 0; i < imgs.size(); i++)
		{
			BufferedImage img = Assist.copyImage(imgs.get(0));
			Graphics2D g = img.createGraphics();
			if(ignored || i == 0)
			{
				Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, rectangles.get(0), img);
				g.setColor(Color.BLACK);
				g.setStroke(Assist.dashed1);
				g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
				g.setColor(Color.ORANGE);
				g.setStroke(Assist.dashed2);
				g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element

				if(type.equals("wrapping")) {
					for(int x=1; x<rectangles.size(); x++) {
						Rectangle r2 = cutRectangleToVisibleArea(offsetX, offsetY, rectangles.get(x), img);
						g.setColor(Color.BLACK);
						g.setStroke(Assist.dashed1);
						g.draw(new java.awt.Rectangle(r2.getX() , r2.getY() , r2.getWidth(), r2.getHeight())); //draw around second element
						g.setColor(Color.MAGENTA);
						g.setStroke(Assist.dashed2);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             
						g.draw(new java.awt.Rectangle(r2.getX() , r2.getY() , r2.getWidth(), r2.getHeight())); //draw around second element
					}
				}else {
					Rectangle r2 = cutRectangleToVisibleArea(offsetX, offsetY, rectangles.get(1), img);
					g.setColor(Color.BLACK);
					g.setStroke(Assist.dashed1);
					g.draw(new java.awt.Rectangle(r2.getX() , r2.getY() , r2.getWidth(), r2.getHeight())); //draw around second element
					g.setColor(Color.MAGENTA);
					g.setStroke(Assist.dashed2);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             
					g.draw(new java.awt.Rectangle(r2.getX() , r2.getY() , r2.getWidth(), r2.getHeight())); //draw around second element
				}



				g.dispose();
			}
			else
			{
				if(type.equals("small-range")) {
					for(TargetArea ta : maxAOC.targetAreas)
					{
						Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, ta.area, img);
						g.setColor(Color.BLACK);
						g.setStroke(Assist.dashed1);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
						g.setColor(Color.YELLOW);
						g.setStroke(Assist.dashed2);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
					}
				}
				if(wrapped) {
					for(TargetArea ta : wrappedArea.targetAreas)
					{
						Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, ta.area, img);
						g.setColor(Color.BLACK);
						g.setStroke(Assist.dashed1);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
						g.setColor(Color.YELLOW);
						g.setStroke(Assist.dashed2);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
					}
				}
				if(protruding && !type.equals("collision"))
				{
					for(Area a : protrudingArea)
					{
						for(TargetArea ta : a.targetAreas)
						{
							Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, ta.area, img);
							g.setColor(Color.BLACK);
							g.setStroke(Assist.dashed1);
							g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
							g.setColor(Color.GREEN);
							g.setStroke(Assist.dashed2);
							g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element

						}
					}
				}
				if(overlapping)
				{
					for(TargetArea ta : overlappedArea.targetAreas)
					{
						Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, ta.area, img);
						g.setColor(Color.BLACK);
						g.setStroke(Assist.dashed1);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
						g.setColor(Color.RED);
						g.setStroke(Assist.dashed2);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
					}
				}
				if(segregated)
				{
					for(TargetArea ta : segregatedArea.targetAreas)
					{
						Rectangle r1 = cutRectangleToVisibleArea(offsetX, offsetY, ta.area, img);
						g.setColor(Color.BLACK);
						g.setStroke(Assist.dashed1);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
						g.setColor(Color.YELLOW);
						g.setStroke(Assist.dashed2);
						g.draw(new java.awt.Rectangle(r1.getX() , r1.getY() , r1.getWidth(), r1.getHeight())); //draw around first element
					}
				}

				g.dispose();
			}

			if(ignored)
			{
				ImageIO.write(img, "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type+ "_" + site + "_" + viewMin + "px_"+ viewMax + "px_ignored_" +i+ ".png"));
			}
			else if (falsePositive)
			{
				ImageIO.write(img, "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type+ "_" + site + "_" + viewMin + "px_"+ viewMax + "px_FP_" +i+".png"));
			}
			else if(NOI)
			{
				ImageIO.write(img, "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type+ "_" + site + "_" + viewMin + "px_"+ viewMax + "px_NOI_" +i+ ".png"));

			}else
			{
				ImageIO.write(img, "png", new File(Assist.outDirectory + File.separator  + "ID_" + ID + "_" + type+ "_" + site + "_" + viewMin + "px_"+ viewMax + "px_TP_" +i+".png"));
			}
		}
	}
	public Rectangle cutRectangleToVisibleArea(int offsetX, int offsetY, Rectangle rectangle, BufferedImage img) {
		int newX = rectangle.getX() - offsetX;
		int newY = rectangle.getY() - offsetY;
		int newW = rectangle.getWidth();
		int newH = rectangle.getHeight();
		if(newX < 0 || newY < 0 || newX + newW > img.getWidth() || newY + newH > img.getHeight())
		{
			String note = "Changing coordinates XYHW(" + newX + "," + newY + "," + newH + "," + newW + ") ";
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
			note = note + "to XYHW(" + newX + "," + newY + "," + newH + "," + newW + ") ";
			//System.out.println(note);
			titles.add("Rectangle (View) Size Warning:");
			notes.add(note);
		}
		Rectangle r = new Rectangle(newX,newY,newH,newW);
		return r;
	}

	public void setFalsePositive(boolean falsePositive) {
		this.falsePositive = falsePositive;
		NOI = false;
	}
	public void setIgnore(boolean bool) {
		this.ignored = bool;
		NOI = false;
	}
}
