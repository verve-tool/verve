package verve;
/*
 * Created by Ibrahim Althomali on 12/17/2017
 * 
 ********************************************/


import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import org.openqa.selenium.Rectangle;
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
	public void domFilter(DriverManager dm) { //determine the true failure range if any.
		for(Failure lf : Failures)
		{
			if(lf.type.equals("small-range"))
				continue;
			System.out.println("DOM Filter - Failure ID: " + lf.ID + " Webpage: " + siteName + " Type: "+ lf.type + " Original Range: " + lf.viewMin + "px-" + lf.viewMax + "px");
			if(domCheck(dm, lf, lf.viewMin)) {
				lf.detectedMin = lf.viewMin;
				lf.detectedMax = lf.viewMin;
				System.out.println("Min - New Range: " + lf.detectedMin + "px-" + lf.detectedMax + "px");
			}
			if(domCheck(dm, lf, ((lf.viewMin+lf.viewMax)/2))) {
				if(lf.detectedMin == -1) 
					lf.detectedMin = ((lf.viewMin+lf.viewMax)/2);
				lf.detectedMax = ((lf.viewMin+lf.viewMax)/2);
				System.out.println("Mid - New Range: " + lf.detectedMin + "px-" + lf.detectedMax + "px");
			}
			if(domCheck(dm, lf, lf.viewMax)) {
				if(lf.detectedMin == -1) 
					lf.detectedMin = lf.viewMax;
				lf.detectedMax = lf.viewMax;
				System.out.println("Max - New Range: " + lf.detectedMin + "px-" + lf.detectedMax + "px");
			}
			if(lf.detectedMin == -1 && lf.detectedMax == -1){ //FP because all three inspection points were FP
				System.out.println("Result: FP");
				lf.setFalsePositive(true);
			}else if(lf.detectedMin == -1 || lf.detectedMax == -1){ //ERROR exit program
				System.out.println("ERROR: Range should be in a different state");
				System.exit(0);
			}else { //
				int trySize = lf.detectedMin-1;
				boolean endCondition = false;
				while(trySize >= Assist.startWindowSize && !endCondition) {
					if(domCheck(dm, lf, trySize)) {
						lf.detectedMin = trySize;
						trySize--;
					}else {
						endCondition = true;
					}	
				}
				trySize = lf.detectedMax+1;
				endCondition = false;
				while(trySize <= Assist.endWindowSize && !endCondition) {
					if(domCheck(dm, lf, trySize)) {
						lf.detectedMax = trySize;
						trySize++;
					}else {
						endCondition = true;
					}	
				}
				lf.savedViewMax = lf.viewMax;
				lf.savedViewMin = lf.viewMin;
				if(lf.detectedMin != -1 && lf.detectedMax != -1 && (lf.viewMin != lf.detectedMin || lf.viewMax != lf.detectedMax)) {
					System.out.println("Corrected Range: " + lf.detectedMin + "px-" + lf.detectedMax + "px");
					lf.viewMin = lf.detectedMin;
					lf.viewMax = lf.detectedMax;
				}
				if(lf.detectedMin == Assist.startWindowSize && lf.detectedMax == Assist.endWindowSize){ //FP because all three inspection points were FP and spans eniter testing range
					System.out.println("Result: FP - Spans Entire Testing Range");
					lf.setFalsePositive(true);
				}else if(lf.type.equals("wrapping") && lf.detectedMax == Assist.endWindowSize){ //FP because it does not have a non-failure upper range
					System.out.println("Result: FP - does not have unwrapped upper end");
					lf.setFalsePositive(true);
				}
			}
		}
	}
	public boolean domCheck(DriverManager dm, Failure lf, int windowSize) {
		//System.out.println("Viewport " + windowSize);
		if(!dm.getURL().contains(wPagePath.replace("\\", "/"))) //check if web site is already loaded
		{
			dm.navigate("file://" + wPagePath);
		}
		dm.setViewport(windowSize);
		ArrayList<Rectangle> rectangles = new ArrayList<Rectangle>();
		Assist.waitToSettle();
		if(lf.type.equals("wrapping") && lf.xpaths.size() < 3) { // if only two elements are reported in a wrapped RLF.
			//System.out.println(lf.type + " Only two elements are reported and a row should have at least two element plus an additional wrapped element ");
			return false;
		}
		for(int i =0; i < lf.xpaths.size(); i++)
		{
			WebElement wb = dm.getWebElem(lf.xpaths.get(i));
			if(wb == null) {
				//System.out.println(lf.type + " ------------ failure at "+ windowSize +"px: Could not find the element " + lf.xpaths.get(i));
				return false;
			}
			if(windowSize == lf.viewMin || windowSize == lf.viewMax) {
				//System.out.println("Element " +(i+1) + ": X1,X2 - Y1,Y2     " + wb.getRect().x + "," + (wb.getRect().x + wb.getRect().width) + " - " + wb.getRect().y + "," + (wb.getRect().y + wb.getRect().height));
			}
			if(wb.getSize().width<=0 ||wb.getSize().height <= 0) {
				//System.out.println(lf.type + " ------------ failure at "+ windowSize +"px: Element width or height is 0 or less " + lf.xpaths.get(i));
				return false;
			}
			if(dm.getOpacity(wb).equals("0") ) {
				//System.out.println(lf.type + " ------------ failure at "+ windowSize +"px: Element is not displayed " + lf.xpaths.get(i));
				return false;
			}
			if(dm.getVisiblity(wb).equals("hidden")) {
				//System.out.println(lf.type + " ------------ failure at "+ windowSize +"px: Element visiblity is hidden " + lf.xpaths.get(i));
				return false;
			}
			Rectangle rectangle = new Rectangle(wb.getLocation(),wb.getSize());
			rectangles.add(rectangle);
		}
		if(lf.type.equals("wrapping")) {
			return isWrapping(rectangles);
		}else if(lf.type.equals("collision")) {
			return isCollision(rectangles);
		}else if(lf.type.equals("protrusion") || lf.type.equals("viewport")) {
			return isProtruding(rectangles);
		}

		System.out.println(lf.type +" is not supprted by domCheck method");
		System.exit(0);
		return false;
	}
	public boolean isCollision(ArrayList<Rectangle> rectangles) {
		if(Assist.intersectingRec(rectangles.get(0), rectangles.get(1))) {
			Rectangle overlapRec = Assist.intrscRec(rectangles.get(0), rectangles.get(1));
			if(overlapRec.width > Assist.tolerance && overlapRec.height > Assist.tolerance)
			{
				return true;
			}
		}
		return false;
	}
	public boolean isFirstAboveSecond(Rectangle first, Rectangle second) {
		return (first.y+first.height) <= second.y; 
	}
	public boolean isFirstLeftOfSecond(Rectangle first, Rectangle second) {
		return (first.x+first.width) <= second.x; 
	}
	public boolean inRow(ArrayList<Rectangle> rectangles) {
		ArrayList<Rectangle> orderedList = new ArrayList<Rectangle>();
		boolean allInARow = true;
		for(int i = 0; i < rectangles.size(); i++) { //if all elements are in a row - non above the other
			Rectangle first = rectangles.get(i);
			for(int x = 0; x < rectangles.size(); x++) {
				if(x != i) { // do not check the same rectangle against it self
					Rectangle second = rectangles.get(x);
					if(isFirstAboveSecond(first, second)) {
						allInARow = false;
						break;
					}
				}
			}
			if(!allInARow)
				break;
		}
		if(allInARow) {
			//System.out.println("No element is above the other - no wrapping");
			return true;
		}
		else
			return false;
	}
	public boolean isWrapping(ArrayList<Rectangle> rectangles) {
		ArrayList<Rectangle> orderedList = new ArrayList<Rectangle>();
		Rectangle wrapped = rectangles.get(0);
		Rectangle rowElement = rectangles.get(1);
		if(inRow(rectangles)) {
			//System.out.println("No element is above the other - no wrapping");
			return false;
		}
		for(int i = 1; i < rectangles.size(); i++) {//------ Ordered list of row elements
			Rectangle rect = rectangles.get(i);
			if(orderedList.isEmpty()) {
				orderedList.add(rect);
			}else { //find position to insert 
				boolean added = false;
				for(int x =0; x< orderedList.size(); x++) {
					Rectangle orederedRect = orderedList.get(x);
					if((rect.x+rect.width) < (orederedRect.x+orederedRect.width)) {
						added = true;
						orderedList.add(x, rect);
						break;
					}
				}
				if(!added) {
					orderedList.add(orderedList.size(), rect);
				}
			}
		}
		for(int i = 0; i < orderedList.size()-1; i++) { //(the rest of row elements) do not form a row - as-in not left of each other
		if(!isFirstLeftOfSecond(orderedList.get(i), orderedList.get(i+1))) {
			//System.out.println("---------------------------------------");
			//System.out.println("Element " +(i+1) + ": X1,X2 - Y1,Y2     " + orderedList.get(i).x + "," + (orderedList.get(i).x + orderedList.get(i).width) + " - " + orderedList.get(i).y + "," + (orderedList.get(i).y + orderedList.get(i).height));
			//System.out.println("Element " +(i+2) + ": X1,X2 - Y1,Y2     " + orderedList.get(i+1).x + "," + (orderedList.get(i+1).x + orderedList.get(i+1).width) + " - " + orderedList.get(i+1).y + "," + (orderedList.get(i+1).y + orderedList.get(i+1).height));
			//System.out.println("row elements are not left of each other");
			//System.out.println("---------------------------------------");
			return false;
		}
		}
		for(int i = 1; i < rectangles.size(); i++) {// if not bellow any row element then return false
			Rectangle rowElem = rectangles.get(i);
			if(!isFirstAboveSecond(rowElem, wrapped)) {
				//System.out.println("---------------------------------------");
				//System.out.println("Wrapped " + ": X1,X2 - Y1,Y2     " + wrapped.x + "," + (wrapped.x + wrapped.width) + " - " + wrapped.y + "," + (wrapped.y + wrapped.height));
				//System.out.println("RowElem " + ": X1,X2 - Y1,Y2     " + rowElem.x + "," + (rowElem.x + rowElem.width) + " - " + rowElem.y + "," + (rowElem.y + rowElem.height));
				//System.out.println("Wrapped element not bellow all other elements");
				//System.out.println("---------------------------------------");
				return false;
			}
		}
		
		return true;
	}
	public boolean isProtruding(ArrayList<Rectangle> rectangles) {
		if(!isCollision(rectangles)) //separated
			return true;
		java.awt.Rectangle child;
		java.awt.Rectangle parent;
		parent = new java.awt.Rectangle(rectangles.get(0).getX(), rectangles.get(0).getY(), rectangles.get(0).getWidth(), rectangles.get(0).getHeight());
		child = new java.awt.Rectangle(rectangles.get(1).getX(), rectangles.get(1).getY(), rectangles.get(1).getWidth(), rectangles.get(1).getHeight());
		java.awt.Rectangle []protrudingRecAWT = SwingUtilities.computeDifference(child, parent);
		if(protrudingRecAWT.length > 0)
		{
			for(int i = 0; i < protrudingRecAWT.length; i++)
			{
				if(protrudingRecAWT[i].height > Assist.tolerance && protrudingRecAWT[i].width > Assist.tolerance )
				{
					return true;
				}
			}
		}
		return false;
	}


}
