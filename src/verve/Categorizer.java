package verve;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.opencv.core.Core;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Range;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;



public class Categorizer
{
	ArrayList<Webpage> webpages;
	DriverManager dm;
	String reportsPath;
	boolean featureHiddenDetection;
//	int[] pauseList = new int [] {167,168,169,170,180,188,190,192,193,194,195,196,197,198,199,200,221,223,229,282,283,298,303,308,318,319,439};
//	int[] pauseList = new int [] {439};

	public Categorizer(String reportsPath, boolean featureHiddenDetection)
	{
		dm = new DriverManager("firefox");
		this.reportsPath = reportsPath;
		this.featureHiddenDetection = featureHiddenDetection;

	}
	public void lookForNOI2()
	{
		BufferedImage screenshot;		
		for(Webpage wp: webpages)
		{
			for(Failure lf : wp.Failures)
			{
				System.out.println("Classifying " +" Failure ID: " + lf.ID + " Webpage: " +wp.siteName + " Type: "+ lf.type + " Range: " + lf.viewMin + "-" + lf.viewMax);
				if(lf.type.equals("small-range")) {
					if(!dm.getURL().contains(wp.wPagePath.replace("\\", "/"))) //check if web site is already loaded
					{
						dm.navigate("file://" + wp.wPagePath);
					}
					lf.startTime();

					String imagePathPrefix = Assist.outDirectory + File.separator  + "Compare_ID_" + lf.ID + "_" + lf.type + "_" + wp.siteName + "_"+ lf.viewMin + "_" + lf.viewMax + "_capture_"; // + lf.captureView +"_1.png";

					//screenshot two oracle locations and failure location
					getImageFor(wp, lf.maxOracle);
					getImageFor(wp, lf);
					if(lf.type.equals("small-range"))
						getImageFor(wp, lf.minOracle);


					lf.findAreasOfConcern();
					lf.createImagesRightAndLeftOfElements();
					lf.maxOracle.findAreasOfConcern();
					lf.maxOracle.createImagesRightAndLeftOfElements();

					lf.minOracle.findAreasOfConcern();
					lf.minOracle.createImagesRightAndLeftOfElements();


					lf.maxOracle.highlightElements("ID_" + lf.ID + "_" + wp.siteName + "_" + lf.type + "_" + lf.viewMin + "_to_" + lf.viewMax + "_capture_" + lf.maxOracle.captureView  +".png");
					lf.highlightElements("ID_" + lf.ID + "_" + wp.siteName + "_" + lf.type + "_" + lf.viewMin + "_to_" + lf.viewMax + "_capture_"+ lf.captureView  +".png");
					if(lf.type.equals("small-range"))
						lf.minOracle.highlightElements("ID_" + lf.ID + "_" + wp.siteName + "_" + lf.type + "_" + lf.viewMin + "_to_" + lf.viewMax + "_capture_"+ lf.minOracle.captureView  +".png");

					lf.writeImages(wp.siteName);

					//opencv
					prepareImagesAndHistograms(lf, imagePathPrefix);
					prepareImagesAndHistograms(lf.maxOracle, imagePathPrefix);
					prepareImagesAndHistograms(lf.minOracle, imagePathPrefix);

					for(int i = 1; i < lf.imgs.size(); i++) {
						histogramCompareOpenCV(lf, lf.histogramMats.get(i), lf.minOracle.histogramMats.get(i), lf.maxOracle.histogramMats.get(i));
					}

					lf.classifyAllHistograms();
				}
				else if(lf.type.equals("wrapping")) {
					if(!dm.getURL().contains(wp.wPagePath.replace("\\", "/"))) //check if web site is already loaded
					{
						dm.navigate("file://" + wp.wPagePath);
					}
					dm.setViewport(lf.captureView);
					lf.viewMaxWidth = dm.getWindowWidth();
					lf.viewMaxHeight = dm.getWindowHeight();
					lf.startTime();
//					if(wp.siteName.equals("Ninite")) {
//						System.out.println("waiting");
//						try {
//							TimeUnit.SECONDS.sleep(1);
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						System.out.println("finished waiting");
//					}
					findWebElementsAddRectangles(lf);
					//exampleScreenshots(wp, lf); //For paper
					//Old way of wrapping
					String originalOpacity1 = dm.getOpacity(lf.wbElements.get(0)); //first element index is wrapped. store original opacity.
					dm.scroll(lf.rectangles.get(0));

					lf.FirstImageScrollOffsetX = dm.scrollX;
					lf.FirstImageScrollOffsetY = dm.scrollY;
					lf.imgs.add(dm.screenshot());
					dm.setOpacity(lf.wbElements.get(0), "0");
					lf.imgs.add(dm.screenshot());
					dm.setOpacity(lf.wbElements.get(0), originalOpacity1);
					lf.findAreasOfConcern();
					lf.findFalsePositive();
					if(lf.falsePositive || lf.ignored)
					{
//						if(lf.ignored)
//							System.out.println("Wrapping: IGNORED Before checking images");
//						else
//							System.out.println("Wrapping: FP Before checking images");

						try {
							lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
						} catch (IOException e) {
							e.printStackTrace();
						}
						lf.endTime();
						continue;
					}
					for(TargetArea targetArea: lf.wrappedArea.targetAreas)
					{
						dm.scroll(targetArea.area);

						dm.setOpacity(lf.wbElements.get(0), "0");
						screenshot = dm.screenshot();
						targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
						dm.setOpacity(lf.wbElements.get(0), originalOpacity1);
						screenshot = dm.screenshot();
						targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
						if(targetArea.anyChange())
						{
							lf.NOI = false;
							//							try 
							//							{
							//								lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
							//							} catch (IOException e) {
							//								e.printStackTrace();
							//							}
							break;
						}

					}
					try 
					{
						lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(lf.type.equals("protrusion") || lf.type.equals("viewport") || lf.type.equals("collision"))
				{
					if(!dm.getURL().contains(wp.wPagePath.replace("\\", "/"))) //check if web site is already loaded
					{
						dm.navigate("file://" + wp.wPagePath);
					}
					dm.setViewport(lf.captureView);

					lf.viewMaxWidth = dm.getWindowWidth();
					lf.viewMaxHeight = dm.getWindowHeight();
					lf.startTime();
					findWebElementsAddRectangles(lf);

					String old1 = dm.getOpacity(lf.wbElements.get(0));
					String old2 = dm.getOpacity(lf.wbElements.get(1));
					//exampleScreenshots(wp, lf); // For paper
					if(lf.rectangles.size() > 1)
					{
						dm.scroll(lf.rectangles.get(1));
						lf.FirstImageScrollOffsetX = dm.scrollX;
						lf.FirstImageScrollOffsetY = dm.scrollY;
						lf.imgs.add(dm.screenshot());
						dm.setOpacity(lf.wbElements.get(1), "0");
						lf.imgs.add(dm.screenshot());
						dm.setOpacity(lf.wbElements.get(1), old2);
					}
					else
					{
						BufferedImage ss = dm.screenshot();
						lf.imgs.add(ss);
						lf.imgs.add(ss);
						lf.endTime();
						continue;
					}


					if(lf.xpaths.get(0).equals(lf.xpaths.get(1)))
					{
						lf.titles.add("xpath error:");
						lf.notes.add("The program was asked to compare the same element to self");
						lf.setIgnore(true);
						try 
						{
							lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
							System.out.println("Something went wrong... the program was asked to compare the same element to self");
						} catch (IOException e) {
							e.printStackTrace();
						}
						lf.endTime();
						continue;
					}


					lf.findAreasOfConcern();
					if(lf.ignored || lf.falsePositive)
					{

						try {
							lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
						} catch (IOException e) {
							e.printStackTrace();
						}
						if(lf.bestEffort)
						{
							finalReach(lf, wp.siteName);
							//finalReachImproved(wp, lf);
						}
						lf.endTime();
						continue;
					}
					if(lf.type.equals("collision"))
					{
						if(lf.segregated)
						{
							try {
								lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						if(lf.overlapping)
						{
							//System.out.println("Area Rect: X("+ lf.overlappedArea.area.x + "," + lf.overlappedArea.area.y + ")Y  W("+ lf.overlappedArea.area.width + "," + lf.overlappedArea.area.height + ")H");
							for(TargetArea targetArea: lf.overlappedArea.targetAreas)
							{
								//System.out.println("Targer Area Rect: X("+ targetArea.area.x + "," + targetArea.area.y + ")Y  W("+ targetArea.area.width + "," + targetArea.area.height + ")H");
								dm.scroll(targetArea.area);

								if(lf.HTMLParentRelationship)
								{
									//									if(lf.type.equals("protrusion") || lf.type.equals("collision"))
									//									{
									//										System.out.println("HTML relationship ------------- " + lf.type);
									//									}
									dm.setOpacity(lf.wbElements.get(0), "0");
									dm.setOpacity(lf.wbElements.get(1), "0");
									screenshot = dm.screenshot();
									targetArea.targetImgs.add(Assist.getSubImage(screenshot,targetArea.area,dm.scrollX,dm.scrollY, lf)); //background image meaning both elements not visible
									dm.setOpacity(lf.wbElements.get(0), old1);
									screenshot = dm.screenshot();
									targetArea.targetImgs.add(Assist.getSubImage(screenshot,targetArea.area,dm.scrollX,dm.scrollY, lf)); //parent only visible
									dm.setOpacity(lf.wbElements.get(1), old2);
									screenshot = dm.screenshot();
									targetArea.targetImgs.add(Assist.getSubImage(screenshot,targetArea.area,dm.scrollX,dm.scrollY, lf)); //both elements visible
								}
								else //we dont know which one is on top so display each element in isolation.
								{
									//Change both elements
									dm.setOpacity(lf.wbElements.get(0), "0");
									dm.setOpacity(lf.wbElements.get(1), "0");
									screenshot = dm.screenshot();
									targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
									dm.setOpacity(lf.wbElements.get(0), old1);
									dm.setOpacity(lf.wbElements.get(1), old2);

									//Change first element
									dm.setOpacity(lf.wbElements.get(0), "0");
									screenshot = dm.screenshot();
									targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
									dm.setOpacity(lf.wbElements.get(0), old1);

									//Change second element
									dm.setOpacity(lf.wbElements.get(1), "0");
									screenshot = dm.screenshot();
									targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
									dm.setOpacity(lf.wbElements.get(1), old2);

									//Both images rendered to make sure the final version shows both image changes
									screenshot = dm.screenshot();
									targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
								}








								if(targetArea.checkAreaOfImgs())
								{
									//										if(targetArea.imgCollisionCheck())
									if(targetArea.pixelCheck(lf.HTMLParentRelationship,true,featureHiddenDetection))
									{
										lf.NOI = false;
										try 
										{
											lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
										} catch (IOException e) {
											e.printStackTrace();
										}
										break;
									}
								}
								else
								{
									lf.titles.add("Collision Size Error:");
									lf.notes.add("Image dimensions are not the same and cannot compare... ignoring failure..");
									lf.setIgnore(true);
									try 
									{
										lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
										System.out.println("Image dimensions are not the same and cannot compare... ignoring failure..");
									} catch (IOException e) {
										e.printStackTrace();
									}
									break;
								}
							}
						}
						try 
						{
							lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					else if((lf.type.equals("protrusion") || lf.type.equals("viewport")))
					{

						boolean outsideChange = false;
						boolean insideChange = false;




						if(lf.segregated)
						{
							for(TargetArea targetArea: lf.segregatedArea.targetAreas)
							{
								dm.scroll(targetArea.area);
								dm.setOpacity(lf.wbElements.get(1), "0");
								screenshot = dm.screenshot();
								targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
								dm.setOpacity(lf.wbElements.get(1), old2);
								screenshot = dm.screenshot();
								targetArea.targetImgs.add(Assist.getSubImage(screenshot, targetArea.area,dm.scrollX,dm.scrollY,lf));
								outsideChange = targetArea.anyChange();
								if(outsideChange)
								{
									lf.NOI = false;
									try 
									{
										lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
									} catch (IOException e) {
										e.printStackTrace();
									}
									break;
								}
							}		
						}
						else if(lf.protruding)
						{
							//check if the parent changes the area where both meet
							for(TargetArea insideTargetArea: lf.overlappedArea.targetAreas)
							{

								dm.scroll(insideTargetArea.area);


								if(lf.HTMLParentRelationship)
								{
									//									if(lf.type.equals("protrusion") || lf.type.equals("collision"))
									//									{
									//										System.out.println("HTML relationship ------------- " + lf.type);
									//									}
									dm.setOpacity(lf.wbElements.get(1), "0");
									screenshot = dm.screenshot();
									insideTargetArea.targetImgs.add(Assist.getSubImage(screenshot,insideTargetArea.area,dm.scrollX,dm.scrollY, lf)); //parent only visible
									dm.setOpacity(lf.wbElements.get(0), "0");
									screenshot = dm.screenshot();
									insideTargetArea.targetImgs.add(Assist.getSubImage(screenshot,insideTargetArea.area,dm.scrollX,dm.scrollY, lf)); //background image meaning both elements not visible
									dm.setOpacity(lf.wbElements.get(0), old1);
									dm.setOpacity(lf.wbElements.get(1), old2);
									screenshot = dm.screenshot();
									insideTargetArea.targetImgs.add(Assist.getSubImage(screenshot,insideTargetArea.area,dm.scrollX,dm.scrollY, lf)); //both elements visible
								}
								else //we dont know which one is on top so display each element in isolation.
								{
									//Change second element
									dm.setOpacity(lf.wbElements.get(1), "0");
									BufferedImage second = dm.screenshot();

									//Change both elements
									dm.setOpacity(lf.wbElements.get(0), "0");
									BufferedImage background = dm.screenshot();
									dm.setOpacity(lf.wbElements.get(1), old2);

									//Change first element
									BufferedImage first = dm.screenshot();
									dm.setOpacity(lf.wbElements.get(0), old1);
									
									insideTargetArea.targetImgs.add(Assist.getSubImage(background, insideTargetArea.area,dm.scrollX,dm.scrollY,lf));
									insideTargetArea.targetImgs.add(Assist.getSubImage(first, insideTargetArea.area,dm.scrollX,dm.scrollY,lf));
									insideTargetArea.targetImgs.add(Assist.getSubImage(second, insideTargetArea.area,dm.scrollX,dm.scrollY,lf));

								}


								insideChange = insideTargetArea.pixelCheck(lf.HTMLParentRelationship, false, featureHiddenDetection);  
								for(Area area : lf.protrudingArea)
								{
									for(TargetArea outsideTargetArea: area.targetAreas)
									{
										dm.scroll(outsideTargetArea.area);
										dm.setOpacity(lf.wbElements.get(1), "0");
										screenshot = dm.screenshot();
										outsideTargetArea.targetImgs.add(Assist.getSubImage(screenshot, outsideTargetArea.area,dm.scrollX,dm.scrollY,lf));
										dm.setOpacity(lf.wbElements.get(1), old2);
										screenshot = dm.screenshot();
										outsideTargetArea.targetImgs.add(Assist.getSubImage(screenshot, outsideTargetArea.area,dm.scrollX,dm.scrollY,lf));

										outsideChange = outsideTargetArea.anyChange();
										if(outsideChange && insideChange)
										{
											lf.NOI = false;
											break;
										}
									}
									if((outsideChange && insideChange) || (lf.ignored))
									{
										break;
									}

								}
								lf.titles.add("Inside Change Note:");
								if(insideChange)
								{
									lf.notes.add("Yes - Overlapping pixels");
								}
								else
								{
									lf.notes.add("NO - Overlapping pixels");
								}

								lf.titles.add("Outside Change Note:");
								if(outsideChange)
								{
									lf.notes.add("Yes - There was a change between plain background and after adding the protruding child");
								}
								else
								{
									lf.notes.add("NO - There was no change between plain background and after adding the protruding child");
								}
								if(outsideChange && insideChange)
								{
									break;
								}

							}

						}
						try 
						{
							lf.saveImages(wp.siteName,dm.scrollX,dm.scrollY);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if(lf.bestEffort)
					{
						finalReach(lf, wp.siteName);
						//finalReachImproved(wp, lf);
					}



				}
				lf.endTime();
			}
		}
		SaveToFileSmallRangeResult();

	}
	private void finalReachImproved(Webpage wp, Failure lf) {
		if((lf.falsePositive || lf.NOI || lf.ignored) && lf.type.equals("viewport")) //if it is true positive then there is no need to check. 
		{
			lf.unreachableDimensions(dm);
			lf.titles.add("ReCategorization Attempt:");
			lf.notes.add("This failure is a candidate for ReCat");

			for(int xpathID: lf.problemXpathID)
			{			
				if(lf.percentHeightBottom != 0 || lf.percentHeightTop != 0 || lf.percentWidthLeft != 0 || lf.percentWidthRight != 0)
				{
					//System.out.println("Final Reach...");

					//					System.out.println("Fault Type: " + lf.type);
					//					System.out.println("xpath 0   : " + lf.xpaths.get(0));
					//					System.out.println("xpath 1   : " + lf.xpaths.get(1));
					//					System.out.println("Problem xpath count  : " + lf.problemXpathID.size());


					dm.setViewport(lf.viewMax+1); //oracles is bigger width

					lf.viewMaxWidth = dm.getWindowWidth();
					lf.viewMaxHeight = dm.getWindowHeight();

					WebElement wb = dm.getWebElem(lf.xpaths.get(xpathID));
					String opacity = dm.getOpacity(wb);
					WebElement body = dm.getWebElem("/HTML/BODY");
					String wbStyle = wb.getAttribute("style");
					String bodyStyle = body.getAttribute("style");
					Rectangle wbRectangle = new Rectangle(wb.getLocation(),wb.getSize());
					//System.out.println("Oracle full rectangle X:" + wbRectangle.x + " Y:"+ wbRectangle.y + " Width:"+ wbRectangle.width + " height:" + wbRectangle.height);

					if(lf.percentWidthLeft != 0){
						int width = Math.round(wb.getSize().width * lf.percentWidthLeft);
						Dimension dimension = new Dimension(width, wb.getSize().height);
						Rectangle leftR = new Rectangle(wb.getLocation(),dimension);
						lf.printFlagL = true;
						if(reach(wp, lf, wb, opacity, leftR , "L")) // if true positive found no need for further reach.
							return;
					}
					if(lf.percentWidthRight != 0) {
						int width = Math.round(wb.getSize().width * lf.percentWidthRight);
						Dimension dimension = new Dimension(width, wb.getSize().height);
						Point point = new Point((wb.getLocation().x+(wb.getSize().width-width)), wb.getLocation().y);
						Rectangle rightR = new Rectangle(point,dimension);
						lf.printFlagR = true;
						if(reach(wp, lf, wb, opacity, rightR, "R")) // true positive found no need for further reach.
							return;
					}
					if(lf.percentHeightTop != 0){
						int height = Math.round(wb.getSize().height * lf.percentHeightTop);
						Dimension dimension = new Dimension(wb.getSize().width, height);
						Rectangle topR = new Rectangle(wb.getLocation(),dimension);
						lf.printFlagT = true;
						if(reach(wp, lf, wb, opacity, topR, "T")) // true positive found no need for further reach.
							return;
					}					
					if(lf.percentHeightBottom != 0){
						int height = Math.round(wb.getSize().height * lf.percentHeightBottom);
						Dimension dimension = new Dimension(wb.getSize().width, height);
						Point point = new Point(wb.getLocation().x, (wb.getLocation().y+(wb.getSize().height-height)));
						Rectangle bottomR = new Rectangle(point,dimension);
						lf.printFlagB = true;
						if(reach(wp, lf, wb, opacity, bottomR, "B")) // true positive found no need for further reach.
							return;
					}

				}
			}
		}else
		{
			lf.bestEffort = false; //There was no attempt to move the elements since it is already a TP.
			lf.titles.add("Unscrollable Area Not Needed:");
			if(lf.type.equals("viewport"))
			{
				lf.notes.add("This failure had some content unreachable by scrolling but it was successufully classified without programatically moving the elements.");
			}else
			{
				lf.notes.add("This failure had some content unreachable by scrolling but it is not a viewport failure hence it was ignored.");
			}
		}

	}
	public boolean reach(Webpage wp, Failure lf, WebElement wb, String opacity, Rectangle rec, String locationFlagLetter) {
		boolean foundTP =false;
		BufferedImage screenshot;
		System.out.println("Scrolling to rectangle X:" + rec.x + " Y:"+ rec.y + " Width:"+ rec.width + " height:" + rec.height);
		dm.scroll(rec);
		Rectangle fullRec = wb.getRect();
		TargetArea ta = new TargetArea(rec);
		if(dm.cantReachX == 0 && dm.cantReachY == 0 && fullRec.x >= 0 && fullRec.y >= 0)
		{
			System.out.println("Reaching...");
			dm.scroll(ta.area);
			dm.setOpacity(wb, "0");
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			dm.setOpacity(wb, opacity);
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			if(ta.anyChange())
			{

				lf.newCat("TP");
				lf.saveRecatImages(screenshot, dm, wp.siteName, ta, "TP", locationFlagLetter);
				foundTP = true;
			}
			else
			{
				lf.newCat("NOI");
				lf.saveRecatImages(screenshot, dm, wp.siteName, ta, "NOI", locationFlagLetter);
			}

		}
		else
		{
			System.out.println("Reach cannot be done at oracle");
			System.out.println("Reach Failed X:"+ fullRec.x + " Y:"+ fullRec.y +" Scroll-X:" +dm.cantReachX + " Scroll-Y:"+ dm.cantReachY);
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail partial rectangle:");
			lf.notes.add("At oracle the partial element coordinates XYHW(" + rec.x + "," + rec.y + "," + rec.height + "," + rec.width + ") "); 
			lf.titles.add("ReCat fail full rectangle:");
			lf.notes.add("At oracle the full element coordinates XYHW(" + fullRec.x + "," + fullRec.y + "," + fullRec.height + "," + fullRec.width + ") "); 
			if(rec.height > 0 && rec.width > 0 && lf.falsePositive)
			{
				lf.newCat("FP");
				try {
					lf.saveImages(wp.siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return foundTP;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, wp.siteName, ta, "FailedReach", locationFlagLetter);
		}
		return foundTP;
	}
//	public void featureCompareOpenCV(Failure lf, String failureImagePath, String minOracleImagePath,
//			String maxOracleImagePath) {
//		int maxOracleValue = compareFeature(failureImagePath,maxOracleImagePath);
//		lf.titles.add("Feature Difference : ");
//		if(lf.type.equals("wrapping")) {
//			if(maxOracleValue > Assist.threshold ) {
//				lf.setFalsePositive(false);
//			}else {
//				lf.setFalsePositive(true);
//			}
//			lf.notes.add("Next : " +  maxOracleValue);
//		}
//		if(lf.type.equals("small-range")) {
//			int minOracleValue = compareFeature(failureImagePath,minOracleImagePath);
//			if(Math.min(minOracleValue, maxOracleValue)  > Assist.threshold ) {
//				lf.setFalsePositive(false);
//			}else {
//				lf.setFalsePositive(true);
//			}
//			lf.notes.add("Prev -- Next : (" + minOracleValue + ") , (" + maxOracleValue+")");
//		}
//	}
	/**
	 * Compare that two images is similar using feature mapping  
	 * @author minikim
	 * @param filename1 - the first image
	 * @param filename2 - the second image
	 * @return integer - count that has the similarity within images 
	 */
	public int compareFeature(String filename1, String filename2) {
		int retVal = 0;

		// Load images to compare
		Mat img1 = Imgcodecs.imread(filename1, Imgcodecs.CV_LOAD_IMAGE_COLOR);
		Mat img2 = Imgcodecs.imread(filename2, Imgcodecs.CV_LOAD_IMAGE_COLOR);

		// Declare key point of images
		MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
		MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
		Mat descriptors1 = new Mat();
		Mat descriptors2 = new Mat();

		// Definition of ORB key point detector and descriptor extractors
		FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB); 
		DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

		// Detect key points
		detector.detect(img1, keypoints1);
		detector.detect(img2, keypoints2);

		// Extract descriptors
		extractor.compute(img1, keypoints1, descriptors1);
		extractor.compute(img2, keypoints2, descriptors2);

		// Definition of descriptor matcher
		DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

		// Match points of two images
		MatOfDMatch matches = new MatOfDMatch();
		//  System.out.println("Type of Image1= " + descriptors1.type() + ", Type of Image2= " + descriptors2.type());
		//  System.out.println("Cols of Image1= " + descriptors1.cols() + ", Cols of Image2= " + descriptors2.cols());

		// Avoid to assertion failed
		// Assertion failed (type == src2.type() && src1.cols == src2.cols && (type == CV_32F || type == CV_8U)
		if (descriptors2.cols() == descriptors1.cols()) {
			matcher.match(descriptors1, descriptors2 ,matches);

			// Check matches of key points
			DMatch[] match = matches.toArray();
			double max_dist = 0; double min_dist = 100;

			for (int i = 0; i < descriptors1.rows(); i++) { 
				double dist = match[i].distance;
				if( dist < min_dist ) min_dist = dist;
				if( dist > max_dist ) max_dist = dist;
			}
			System.out.println("max_dist=" + max_dist + ", min_dist=" + min_dist);

			// Extract good images (distances are under 10)
			for (int i = 0; i < descriptors1.rows(); i++) {
				//	    if (match[i].distance <= 10) {
				if (match[i].distance > 20) {
					retVal++;
				}
			}
			System.out.println("matching count=" + retVal);
		}

		return retVal;
	}
	public void prepareImagesAndHistograms(Failure lf, String imagePathPrefix) {
		int hBins = 180, sBins = 256;
		int[] histSize = { hBins, sBins };
		// hue varies from 0 to 179, saturation from 0 to 255
		float[] ranges = { 0, 180, 0, 256 };
		// Use the 0-th and 1-st channels
		int[] channels = { 0, 1 };

		//Initialise array and skip first element to match index of failure images lf.imgs
		lf.histogramMats = new ArrayList<Mat>();
		lf.histogramMats.add(null);

		for(int i = 1; i < lf.imgs.size(); i++) {
			Mat imgMat = Imgcodecs.imread(imagePathPrefix+ lf.captureView +"_"+i+".png");
			if (imgMat.empty()) {
				System.err.println("Cannot read the image:" + imagePathPrefix+ lf.captureView +"_"+i+".png");
				System.exit(0);
			}

			Mat hsvMat = new Mat();
			Mat histogramMat = new Mat();
			Imgproc.cvtColor( imgMat, hsvMat, Imgproc.COLOR_BGR2HSV );
			List<Mat> hsvMatList = Arrays.asList(hsvMat);
			Imgproc.calcHist(hsvMatList, new MatOfInt(channels), new Mat(), histogramMat, new MatOfInt(histSize), new MatOfFloat(ranges), false);
			Core.normalize(histogramMat, histogramMat, 0, 1, Core.NORM_MINMAX);
			lf.histogramMats.add(histogramMat);
		}
	}
	public void histogramCompareOpenCV(Failure lf, Mat baseHistogram, Mat minOracleHistogram, Mat maxOracleHistogram) {

		Results result = new Results();
		for( int compareMethod = 0; compareMethod < 6; compareMethod++ ) {
			double perfectResult = Imgproc.compareHist( baseHistogram, baseHistogram, compareMethod );
			double minResult = Imgproc.compareHist( baseHistogram, minOracleHistogram, compareMethod );
			double maxResult = Imgproc.compareHist( baseHistogram, maxOracleHistogram, compareMethod );

			String CompareMethodName = ""; //https://docs.opencv.org/2.4/doc/tutorials/imgproc/histograms/histogram_comparison/histogram_comparison.html
			if(compareMethod == 0)
				CompareMethodName = "Correlation";
			if(compareMethod == 1)
				CompareMethodName = "Chi-Square";
			if(compareMethod == 2)
				CompareMethodName = "Intersection";
			if(compareMethod == 3)
				CompareMethodName = "Bhattacharyya distance";
			if(compareMethod == 4)
				CompareMethodName = "Altenative Chi-Square";
			if(compareMethod == 5)
				CompareMethodName = "Kullback-Leibler Divergence";

			result.methods.add(CompareMethodName);
			result.base.add(perfectResult);
			result.maxOracle.add(maxResult);
			result.minOracle.add(minResult);
		}
		lf.histogramResults.add(result);
	}
//	public void histogramCompareOpenCV(Failure lf, String failureImagePath1, String minOracleImagePath1,
//			String maxOracleImagePath1, String failureImagePath2, String minOracleImagePath2, String maxOracleImagePath2) {
//
//		Mat base1Mat = Imgcodecs.imread(failureImagePath1);
//		Mat minOracle1Mat = Imgcodecs.imread(minOracleImagePath1);
//		Mat maxOracle1Mat = Imgcodecs.imread(maxOracleImagePath1);
//
//		Mat base2Mat = Imgcodecs.imread(failureImagePath2);
//		Mat minOracle2Mat = Imgcodecs.imread(minOracleImagePath2);
//		Mat maxOracle2Mat = Imgcodecs.imread(maxOracleImagePath2);
//
//		if (base1Mat.empty() || maxOracle1Mat.empty() ) {
//			System.err.println("Cannot read the images base1:" + base1Mat.empty() + " maxOracle1:"+ maxOracle1Mat.empty());
//			System.exit(0);
//		}
//
//		if (lf.type.equals("small-range") &&  (base2Mat.empty() || maxOracle2Mat.empty() || minOracle1Mat.empty() || minOracle2Mat.empty())) {
//			System.err.println("Cannot read the images base2:"+ base2Mat.empty() + " maxOracle2:"+maxOracle2Mat.empty() + " minOracle1:"+minOracle1Mat.empty() + " minOracle2:"+minOracle2Mat.empty());
//			System.exit(0);
//		}
//
//		Mat hsvBase = new Mat(), hsvTest1 = new Mat(), hsvTest2 = new Mat();
//		Imgproc.cvtColor( base1Mat, hsvBase, Imgproc.COLOR_BGR2HSV );
//		if(lf.type.equals("small-range"))
//			Imgproc.cvtColor( minOracle1Mat, hsvTest1, Imgproc.COLOR_BGR2HSV );
//		Imgproc.cvtColor( maxOracle1Mat, hsvTest2, Imgproc.COLOR_BGR2HSV );
//		//Mat hsvHalfDown = hsvBase.submat( new Range( hsvBase.rows()/2, hsvBase.rows() - 1 ), new Range( 0, hsvBase.cols() - 1 ) );
//		//int hBins = 50, sBins = 60;
//		int hBins = 180, sBins = 256;
//		int[] histSize = { hBins, sBins };
//		// hue varies from 0 to 179, saturation from 0 to 255
//		float[] ranges = { 0, 180, 0, 256 };
//		// Use the 0-th and 1-st channels
//		int[] channels = { 0, 1 };
//		Mat histBase = new Mat(), histTest1 = new Mat(), histTest2 = new Mat();
//		List<Mat> hsvBaseList = Arrays.asList(hsvBase);
//		Imgproc.calcHist(hsvBaseList, new MatOfInt(channels), new Mat(), histBase, new MatOfInt(histSize), new MatOfFloat(ranges), false);
//		Core.normalize(histBase, histBase, 0, 1, Core.NORM_MINMAX);
//		//List<Mat> hsvHalfDownList = Arrays.asList(hsvHalfDown);
//		//Imgproc.calcHist(hsvHalfDownList, new MatOfInt(channels), new Mat(), histHalfDown, new MatOfInt(histSize), new MatOfFloat(ranges), false);
//		//Core.normalize(histHalfDown, histHalfDown, 0, 1, Core.NORM_MINMAX);
//		if(lf.type.equals("small-range")) {
//			List<Mat> hsvTest1List = Arrays.asList(hsvTest1);
//			Imgproc.calcHist(hsvTest1List, new MatOfInt(channels), new Mat(), histTest1, new MatOfInt(histSize), new MatOfFloat(ranges), false);
//			Core.normalize(histTest1, histTest1, 0, 1, Core.NORM_MINMAX);
//		}
//		List<Mat> hsvTest2List = Arrays.asList(hsvTest2);
//		Imgproc.calcHist(hsvTest2List, new MatOfInt(channels), new Mat(), histTest2, new MatOfInt(histSize), new MatOfFloat(ranges), false);
//		Core.normalize(histTest2, histTest2, 0, 1, Core.NORM_MINMAX);
//		boolean truePositive = false;
//		for( int compareMethod = 0; compareMethod < 4; compareMethod++ ) {
//			double baseBase = Imgproc.compareHist( histBase, histBase, compareMethod );
//			//double baseHalf = Imgproc.compareHist( histBase, histHalfDown, compareMethod );
//			double baseTest2 = Imgproc.compareHist( histBase, histTest2, compareMethod );
//			String CompareMethodName = ""; //https://docs.opencv.org/2.4/doc/tutorials/imgproc/histograms/histogram_comparison/histogram_comparison.html
//			if(compareMethod == 0)
//				CompareMethodName = "Correlation";
//			if(compareMethod == 1)
//				CompareMethodName = "Chi-Square ";
//			if(compareMethod == 2)
//				CompareMethodName = "Intersection";
//			if(compareMethod == 3)
//				CompareMethodName = "Bhattacharyya distance";
//			lf.titles.add("Method " + CompareMethodName + ":");
//			if(lf.type.equals("wrapping")) {
//				if(compareMethod == 3 && baseTest2 > Assist.threshold ) {
//					lf.setFalsePositive(false);
//				}else {
//					lf.setFalsePositive(true);
//				}
//				lf.notes.add("Perfect / Next : " + baseBase + " / " + baseTest2);
//			}
//			if(lf.type.equals("small-range")) {
//				double baseTest1 = Imgproc.compareHist( histBase, histTest1, compareMethod );
//				if(compareMethod == 3 && Math.min(baseTest1, baseTest2)  > Assist.threshold ) {
//					lf.setFalsePositive(false);
//					truePositive = true;
//				}
//				lf.notes.add("Perfect / Prev / Next : " + baseBase + " / " + baseTest1 + " / " + baseTest2);
//			}
//		}
//		if(lf.type.equals("small-range") && !truePositive) { //try other image
//			hsvBase = new Mat();
//			hsvTest1 = new Mat();
//			hsvTest2 = new Mat();
//			Imgproc.cvtColor( base2Mat, hsvBase, Imgproc.COLOR_BGR2HSV );
//			if(lf.type.equals("small-range"))
//				Imgproc.cvtColor( minOracle2Mat, hsvTest1, Imgproc.COLOR_BGR2HSV );
//			Imgproc.cvtColor( maxOracle2Mat, hsvTest2, Imgproc.COLOR_BGR2HSV );
//			histBase = new Mat();
//			histTest1 = new Mat();
//			histTest2 = new Mat();
//			hsvBaseList = Arrays.asList(hsvBase);
//			Imgproc.calcHist(hsvBaseList, new MatOfInt(channels), new Mat(), histBase, new MatOfInt(histSize), new MatOfFloat(ranges), false);
//			Core.normalize(histBase, histBase, 0, 1, Core.NORM_MINMAX);
//			List<Mat> hsvTest1List = Arrays.asList(hsvTest1);
//			Imgproc.calcHist(hsvTest1List, new MatOfInt(channels), new Mat(), histTest1, new MatOfInt(histSize), new MatOfFloat(ranges), false);
//			Core.normalize(histTest1, histTest1, 0, 1, Core.NORM_MINMAX);
//			hsvTest2List = Arrays.asList(hsvTest2);
//			Imgproc.calcHist(hsvTest2List, new MatOfInt(channels), new Mat(), histTest2, new MatOfInt(histSize), new MatOfFloat(ranges), false);
//			Core.normalize(histTest2, histTest2, 0, 1, Core.NORM_MINMAX);
//			for( int compareMethod = 0; compareMethod < 4; compareMethod++ ) {
//				double baseBase = Imgproc.compareHist( histBase, histBase, compareMethod );
//				//double baseHalf = Imgproc.compareHist( histBase, histHalfDown, compareMethod );
//				double baseTest2 = Imgproc.compareHist( histBase, histTest2, compareMethod );
//				String CompareMethodName = ""; //https://docs.opencv.org/2.4/doc/tutorials/imgproc/histograms/histogram_comparison/histogram_comparison.html
//				if(compareMethod == 0)
//					CompareMethodName = "Correlation";
//				if(compareMethod == 1)
//					CompareMethodName = "Chi-Square";
//				if(compareMethod == 2)
//					CompareMethodName = "Intersection";
//				if(compareMethod == 3)
//					CompareMethodName = "Bhattacharyya distance";
//
//				lf.titles.add("Second Element Method: " + CompareMethodName + ":");
//				double baseTest1 = Imgproc.compareHist( histBase, histTest1, compareMethod );
//				if(compareMethod == 3 && Math.min(baseTest1, baseTest2)  > Assist.threshold ) {
//					lf.setFalsePositive(false);
//				}else { //try other oracle
//					lf.setFalsePositive(true);
//				}
//				lf.notes.add("Perfect / Prev / Next : " + baseBase + " / " + baseTest1 + " / " + baseTest2);
//
//			}
//		}
//	}
	public void SaveToFileSmallRangeResult() {
		try 
		{
			String heading = "UID,Failure Type,Webpage,Range,Base,elementOneAndRightMin,elementOneAndRightMax,Base,elementOneAndLeftMin,elementOneAndLeftMax,Base,elementTwoAndRightMin,elementTwoAndRightMax,Base,elementTwoAndLeftMin,elementTwoAndLeftMax,Classification";

			PrintWriter writerCor;
			writerCor = new PrintWriter(Assist.outDirectory + File.separator + "Correlation" + ".csv");
			writerCor.println(heading);

			PrintWriter writerChi;
			writerChi = new PrintWriter(Assist.outDirectory + File.separator + "Chi-Square" + ".csv");
			writerChi.println(heading);

			PrintWriter writerInt;
			writerInt = new PrintWriter(Assist.outDirectory + File.separator + "Intersection" + ".csv");
			writerInt.println(heading);

			PrintWriter writerBha;
			writerBha = new PrintWriter(Assist.outDirectory + File.separator + "Bhattacharyya" + ".csv");
			writerBha.println(heading);

			PrintWriter writerM4;
			writerM4 = new PrintWriter(Assist.outDirectory + File.separator + "Alternative-Chi-Square" + ".csv");
			writerM4.println(heading);

			PrintWriter writerM5;
			writerM5 = new PrintWriter(Assist.outDirectory + File.separator + "Kullback-Leibler-Divergence" + ".csv");
			writerM5.println(heading);

			for(Webpage wp: webpages)
			{
				for(Failure lf : wp.Failures)
				{
					if(lf.type.equals("small-range")) {
						String result = "";
						if(lf.ignored)
						{
							result = "Ignored";

						}else if(lf.NOI)
						{
							result = "NOI";

						}else if(lf.falsePositive)
						{
							result = "FP";

						}else
						{
							result = "TP";
						}
						writeHistogramResultToFile(writerCor, wp, lf, 0);
						writeHistogramResultToFile(writerChi, wp, lf, 1);
						writeHistogramResultToFile(writerInt, wp, lf, 2);
						writeHistogramResultToFile(writerBha, wp, lf, 3);
						writeHistogramResultToFile(writerM4, wp, lf, 4);
						writeHistogramResultToFile(writerM5, wp, lf, 5);
					}
				}
			}
			writerCor.close();
			writerChi.close();
			writerInt.close();
			writerBha.close();
			writerM4.close();
			writerM5.close();
		}catch (IOException e) 
		{    
			System.out.println("could not save small-range results to file... exiting");
			e.printStackTrace();
			System.exit(0);
		}
	}


	public void writeHistogramResultToFile(PrintWriter writer, Webpage wp, Failure lf, int method) {
		
		writer.println(lf.ID +"," + lf.type + "," + wp.siteName + "," + lf.viewMin + "px-" + lf.viewMax + "px" 
				+ "," + 	Assist.decimalFormat.format(lf.histogramResults.get(0).base.get(method)) + "," + 	Assist.decimalFormat.format(lf.histogramResults.get(0).minOracle.get(method))+ "," + 	Assist.decimalFormat.format(lf.histogramResults.get(0).maxOracle.get(method)) 
				+ "," + 	Assist.decimalFormat.format(lf.histogramResults.get(1).base.get(method)) + "," + 	Assist.decimalFormat.format(lf.histogramResults.get(1).minOracle.get(method))+ "," + 	Assist.decimalFormat.format(lf.histogramResults.get(1).maxOracle.get(method))
				+ "," + 	Assist.decimalFormat.format(lf.histogramResults.get(2).base.get(method)) + "," + 	Assist.decimalFormat.format(lf.histogramResults.get(2).minOracle.get(method))+ "," + 	Assist.decimalFormat.format(lf.histogramResults.get(2).maxOracle.get(method))
				+ "," + 	Assist.decimalFormat.format(lf.histogramResults.get(3).base.get(method)) + "," + 	Assist.decimalFormat.format(lf.histogramResults.get(3).minOracle.get(method))+ "," + 	Assist.decimalFormat.format(lf.histogramResults.get(3).maxOracle.get(method))
				+ "," + lf.getClassificationForMethod(method));
	}
//	public void oldSmallRangeDetection(Webpage wp, Failure lf) {
//		getImageFor(wp, lf.minOracle);
//		getImageFor(wp, lf.maxOracle);
//		getImageFor(wp, lf);
//		lf.minOracle.highlightElements("ID_" + lf.ID + "-" + wp.siteName + "-" + lf.type + "-capture-"+ lf.minOracle.captureView  +".png");
//		lf.maxOracle.highlightElements("ID_" + lf.ID + "-" + wp.siteName + "-" + lf.type + "-capture-"+ lf.maxOracle.captureView  +".png");
//		lf.highlightElements("ID_" + lf.ID + "-" + wp.siteName + "-" + lf.type + "-capture-"+ lf.captureView  +".png");
//
//		//Histogram approach
//		lf.histogram = new Histogram(lf.imgs.get(0));
//		lf.histogram.extractHistogram();
//		lf.minOracle.histogram = new Histogram(lf.minOracle.imgs.get(0));
//		lf.minOracle.histogram.extractHistogram();
//		lf.maxOracle.histogram = new Histogram(lf.maxOracle.imgs.get(0));
//		lf.maxOracle.histogram.extractHistogram();
//		for(HistogramDifference diff: wp.histogramsDifferences) {//save on recalculating histogram differences
//			if(diff.isFor(lf.minOracle.captureView, lf.captureView)) {
//				lf.prevDiff = diff.difference;
//				System.out.println("Found Difference for: " + lf.minOracle.captureView + "-"+ lf.captureView);
//			}
//			if(diff.isFor(lf.captureView, lf.maxOracle.captureView)) {
//				lf.nextDiff = diff.difference;
//				System.out.println("Found Difference for: " + lf.captureView + "-"+  lf.maxOracle.captureView);
//			}
//		}
//		if(lf.prevDiff == null) {
//			System.out.println("Creating Difference for: " + lf.minOracle.captureView + "-"+ lf.captureView);
//			lf.prevDiff = lf.minOracle.histogram.difference(lf.histogram);
//			wp.histogramsDifferences.add(new HistogramDifference(lf.minOracle.captureView,lf.captureView,lf.prevDiff));
//			System.out.println("Created");
//		}
//		if(lf.nextDiff == null) {
//			System.out.println("Creating Difference for: " + lf.maxOracle.captureView + "-"+ lf.captureView);
//			lf.nextDiff = lf.maxOracle.histogram.difference(lf.histogram);
//			wp.histogramsDifferences.add(new HistogramDifference(lf.captureView, lf.maxOracle.captureView,lf.nextDiff));
//			System.out.println("Created");
//		}
//		if(Math.min(lf.prevDiff.totalDifference, lf.nextDiff.totalDifference) > Assist.threshold ) {
//			lf.setFalsePositive(false);
//		}else {
//			lf.setFalsePositive(true);
//		}
//
//		//distance between both elements
//		//							euclideanDistance(lf);
//		//							euclideanDistance(lf.belowMinLayout);
//		//							euclideanDistance(lf.aboveMaxLayout);
//		//							if((Math.abs(lf.euclideanDistanceTL - lf.belowMinLayout.euclideanDistanceTL)<= smallrangeThreshold
//		//									|| Math.abs(lf.euclideanDistanceTL - lf.aboveMaxLayout.euclideanDistanceTL)<= smallrangeThreshold)
//		//									&& (Math.abs(lf.euclideanDistanceTR - lf.belowMinLayout.euclideanDistanceTR)<= smallrangeThreshold
//		//									|| Math.abs(lf.euclideanDistanceTR - lf.aboveMaxLayout.euclideanDistanceTR)<= smallrangeThreshold)
//		//									&& (Math.abs(lf.euclideanDistanceBL - lf.belowMinLayout.euclideanDistanceBL)<= smallrangeThreshold
//		//									|| Math.abs(lf.euclideanDistanceBL - lf.aboveMaxLayout.euclideanDistanceBL)<= smallrangeThreshold)
//		//									&& (Math.abs(lf.euclideanDistanceBR - lf.belowMinLayout.euclideanDistanceBR)<= smallrangeThreshold
//		//									|| Math.abs(lf.euclideanDistanceBR - lf.aboveMaxLayout.euclideanDistanceBR)<= smallrangeThreshold)) {
//		//								lf.setFalsePositive(true);
//		//							}else {
//		//								lf.setFalsePositive(false);
//		//							}
//
//
//		//distance using max value only
//		//							for(int i=0; i < lf.orignalRectangles.size(); i++) {
//		//								int firstED = euclideanRectangleDistance(lf.orignalRectangles.get(i),lf.belowMinLayout.orignalRectangles.get(i));
//		//								int secondED = euclideanRectangleDistance(lf.orignalRectangles.get(i),lf.aboveMaxLayout.orignalRectangles.get(i));
//		//								lf.belowMinLayout.rectangleEuclideanDistance.add(firstED);
//		//								lf.aboveMaxLayout.rectangleEuclideanDistance.add(secondED);
//		//							}
//		//							if((lf.belowMinLayout.rectangleEuclideanDistance.get(0) <= Assist.smallrangeThreshold
//		//									&& lf.belowMinLayout.rectangleEuclideanDistance.get(1) <= Assist.smallrangeThreshold)
//		//									|| (lf.aboveMaxLayout.rectangleEuclideanDistance.get(0) <= Assist.smallrangeThreshold
//		//									&& lf.aboveMaxLayout.rectangleEuclideanDistance.get(1) <= Assist.smallrangeThreshold)) {
//		//								lf.setFalsePositive(true);
//		//								}else {
//		//									lf.setFalsePositive(false);
//		//								}
//	}
	public void euclideanDistance(Failure lf) {
		Rectangle firstRect = lf.orignalRectangles.get(0);
		Rectangle secondRect = lf.orignalRectangles.get(1);
		lf.euclideanDistanceTL = Math.abs(firstRect.x-secondRect.x) + Math.abs(firstRect.y-secondRect.y);
		lf.euclideanDistanceTR = Math.abs((firstRect.x+firstRect.width)-(secondRect.x+secondRect.width)) + Math.abs(firstRect.y-secondRect.y);
		lf.euclideanDistanceBL = Math.abs(firstRect.x-secondRect.x) + Math.abs((firstRect.y+firstRect.height)-(secondRect.y+secondRect.height));
		lf.euclideanDistanceBR = Math.abs((firstRect.x+firstRect.width)-(secondRect.x+secondRect.width)) + Math.abs((firstRect.y+firstRect.height)-(secondRect.y+secondRect.height));
	}
	public int euclideanRectangleDistance(Rectangle firstRect, Rectangle secondRect ) {
		int eucX1 = Math.abs(firstRect.x-secondRect.x);
		int eucX2 = Math.abs((firstRect.x+firstRect.width)-(secondRect.x+secondRect.width));
		int eucY1 = Math.abs(firstRect.y-secondRect.y);
		int eucY2 =  Math.abs((firstRect.y+firstRect.height)-(secondRect.y+secondRect.height));
		//		int euclideanDistanceTL = Math.abs(firstRect.x-secondRect.x) + Math.abs(firstRect.y-secondRect.y);
		//		int euclideanDistanceTR = Math.abs((firstRect.x+firstRect.width)-(secondRect.x+secondRect.width)) + Math.abs(firstRect.y-secondRect.y);
		//		int euclideanDistanceBL = Math.abs(firstRect.x-secondRect.x) + Math.abs((firstRect.y+firstRect.height)-(secondRect.y+secondRect.height));
		//		int euclideanDistanceBR = Math.abs((firstRect.x+firstRect.width)-(secondRect.x+secondRect.width)) + Math.abs((firstRect.y+firstRect.height)-(secondRect.y+secondRect.height));
		//		return eucX1+eucX2+eucY1+eucY2;
		return Math.max(eucX1, Math.max(eucX2, Math.max(eucY1, eucY2)));
	}
	public void getImageFor(Webpage wp, Failure lf) {
		int index = wp.viewports.indexOf(lf.captureView);
		//lf.imgs.add(wp.screenshots.get(index));
		//System.out.println(wp.siteName + "   Viewport: " + lf.captureView + "   For ID:" + lf.ID);
		if(wp.captured.get(index).booleanValue() == false) {
			//System.out.println("capturing");
			dm.setViewport(lf.captureView);
			ArrayList<Failure> listOfFailures = wp.pendingFailures.get(index);
			int width = dm.getWindowWidth();
			int height = dm.getWindowHeight();

//			if(wp.siteName.equals("Ninite")) {
//				System.out.println("waiting");
//				try {
//					TimeUnit.SECONDS.sleep(1);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.out.println("finished waiting");
//			}

			String fileName = "page_screenshot_" + wp.siteName + "_" +lf.type + "_capture_"+ lf.captureView  +".png";
			BufferedImage screenshot = dm.saveFullScreenshot(fileName);
			//wp.screenshots.add(screenshot);
			for(Failure failure : listOfFailures) {
				failure.viewMaxWidth = width;
				failure.viewMaxHeight = height;
				findWebElementsAddRectangles(failure);
				failure.imgs.add(screenshot);
			}
			wp.captured.set(index, true);
		}else {
			//System.out.println("No need to capture");
		}

	}

	public void exampleScreenshotsForWebpage(Webpage wp, Failure lf) {
		if(!dm.getURL().contains(wp.wPagePath.replace("\\", "/"))) //check if web site is already loaded
		{
			dm.navigate("file://" + wp.wPagePath);
		}

		



		lf.captureView = lf.viewMax+1;
		capture(lf, "-max-plus-one");
		
		lf.captureView = lf.viewMax;
		capture(lf, "-max");
		
		lf.captureView = (lf.viewMin+lf.viewMax)/2;
		capture(lf, "-mid");
		
		lf.captureView = lf.viewMin;
		capture(lf, "-min");
		
		lf.captureView = lf.viewMin-1;
		capture(lf, "-min-minus-one");
		
//		lf.captureView = lf.viewMin;
//		dm.setViewport(lf.captureView);
//		lf.viewMaxWidth = dm.getWindowWidth();
//		lf.viewMaxHeight = dm.getWindowHeight();
//		findWebElementsAddRectangles(lf);
//		String fileName = "FS-ID-" +lf.ID + "-min.png";
//		dm.saveFullScreenshot(fileName);
//		if(lf.rectangles.size() > 1)
//		{
//			dm.scroll(lf.rectangles.get(1));
//		}
//		fileName = "VS-ID-" +lf.ID + "-min.png";
//		dm.saveViewportScreenshot(fileName);
//		lf.wbElements.clear();
//		lf.rectangles.clear();
//		
//		
//		
//		lf.captureView = (lf.viewMin+lf.viewMax)/2;
//		dm.setViewport(lf.captureView);
//		lf.viewMaxWidth = dm.getWindowWidth();
//		lf.viewMaxHeight = dm.getWindowHeight();
//		findWebElementsAddRectangles(lf);
//		fileName = "FS-ID-" +lf.ID + "-mid.png";
//		dm.saveFullScreenshot(fileName);
//		if(lf.rectangles.size() > 1)
//		{
//			dm.scroll(lf.rectangles.get(1));
//		}
//		fileName = "VS-ID-" +lf.ID + "-mid.png";
//		dm.saveViewportScreenshot(fileName);
//		lf.wbElements.clear();
//		lf.rectangles.clear();
//		
//		
//		
//		
//		lf.captureView = lf.viewMax;
//		dm.setViewport(lf.captureView);
//		lf.viewMaxWidth = dm.getWindowWidth();
//		lf.viewMaxHeight = dm.getWindowHeight();
//		findWebElementsAddRectangles(lf);
//		fileName = "FS-ID-" +lf.ID + "-max.png";
//		dm.saveFullScreenshot(fileName);
//		if(lf.rectangles.size() > 1)
//		{
//			if(lf.type.equals("wrapping"))
//				dm.scroll(lf.rectangles.get(0));
//			else
//				dm.scroll(lf.rectangles.get(1));
//		}
//		fileName = "VS-ID-" +lf.ID + "-max.png";
//		dm.saveViewportScreenshot(fileName);
//		lf.wbElements.clear();
//		lf.rectangles.clear();
//		
//		
//		
//		lf.captureView = lf.viewMax+1;
//		dm.setViewport(lf.captureView);
//		lf.viewMaxWidth = dm.getWindowWidth();
//		lf.viewMaxHeight = dm.getWindowHeight();
//		findWebElementsAddRectangles(lf);
//		fileName = "FS-ID-" +lf.ID + "-max-plus-one.png";
//		dm.saveFullScreenshot(fileName);
//		if(lf.rectangles.size() > 1)
//		{
//			dm.scroll(lf.rectangles.get(1));
//		}
//		fileName = "VS-ID-" +lf.ID + "-max-plus-one.png";
//		dm.saveViewportScreenshot(fileName);
//		lf.wbElements.clear();
//		lf.rectangles.clear();
//		
//		
//
//		lf.captureView = lf.viewMin-1;
//		dm.setViewport(lf.captureView);
//		lf.viewMaxWidth = dm.getWindowWidth();
//		lf.viewMaxHeight = dm.getWindowHeight();
//		findWebElementsAddRectangles(lf);
//		fileName = "FS-ID-" +lf.ID + "-min-minus-one.png";
//		dm.saveFullScreenshot(fileName);
//		if(lf.rectangles.size() > 1)
//		{
//			dm.scroll(lf.rectangles.get(1));
//		}
//		fileName = "VS-ID-" +lf.ID + "-min-minus-one.png";
//		dm.saveViewportScreenshot(fileName);
//		lf.wbElements.clear();
//		lf.rectangles.clear();

}
	public void capture(Failure lf, String postfix) {
		dm.setViewport(lf.captureView);
		lf.viewMaxWidth = dm.getWindowWidth();
		lf.viewMaxHeight = dm.getWindowHeight();
		findWebElementsAddRectangles(lf);
		String fileName = "FS-ID-" +lf.ID + postfix+ ".png";
		BufferedImage screenshot = dm.saveFullScreenshot(fileName);
		Graphics2D g = screenshot.createGraphics();
		for(int i =1; i < lf.rectangles.size(); i++)
		{
			Assist.highlightSingleElement(0, 0, screenshot, g, lf.rectangles.get(i), Color.yellow);
		}
		Assist.highlightSingleElement(0, 0, screenshot, g, lf.rectangles.get(0), Color.red);
		int minX = 0, minY=0,maxX=0,maxY=0;
		for(int i =0; i < lf.rectangles.size(); i++)
		{
			Rectangle currentRec = lf.rectangles.get(i);
			if(i==0) {
				minX = ((currentRec.x < 0)? 0:currentRec.x);
				minY = ((currentRec.y < 0)? 0 : currentRec.y);
				maxX = ((currentRec.x + currentRec.getWidth()) < screenshot.getWidth())?(currentRec.x +currentRec.getWidth()):(screenshot.getWidth() - currentRec.x);
				maxY = ((currentRec.y + currentRec.getHeight()) < screenshot.getHeight())?(currentRec.y + currentRec.getHeight()):(screenshot.getHeight() - currentRec.y);
			}else {
				minX = Math.min(minX, (currentRec.x < 0? 0:currentRec.x));
				minY = Math.min(minY, (currentRec.y < 0? 0 : currentRec.y));
				maxX = Math.max(maxX, ((currentRec.x + currentRec.getWidth()) < screenshot.getWidth())?(currentRec.x +currentRec.getWidth()):(screenshot.getWidth() - currentRec.x));
				maxY = Math.max(maxY, ((currentRec.y + currentRec.getHeight()) < screenshot.getHeight())?(currentRec.y + currentRec.getHeight()):(screenshot.getHeight() - currentRec.y));
			}
		}
		try {
			ImageIO.write(screenshot.getSubimage(minX, minY, maxX-minX, maxY-minY), "png", new File(Assist.outDirectory + File.separator  +  "ID-"+lf.ID+ postfix+ ".png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lf.wbElements.clear();
		lf.rectangles.clear();
	}
	public void exampleScreenshots(Webpage wp, Failure lf) {

			lf.captureView = lf.viewMin;
			dm.setViewport(lf.captureView);
			String fileName = "full-screenshot-" + wp.siteName + "-ID-" +lf.ID + "-" + lf.type + "-range-" + lf.viewMin + "-to-" + lf.viewMax + "-capture-"+ lf.captureView  +".png";
			dm.saveFullScreenshot(fileName);
			if(lf.rectangles.size() > 1)
			{
				dm.scroll(lf.rectangles.get(1));
			}
			fileName = "window-screenshot-" + wp.siteName + "-ID-" +lf.ID + "-" + lf.type + "-range-" + lf.viewMin + "-to-" + lf.viewMax + "-capture-"+ lf.captureView  +".png";
			dm.saveViewportScreenshot(fileName);

			lf.captureView = (lf.viewMin+lf.viewMax)/2;
			dm.setViewport(lf.captureView);
			fileName = "full-screenshot-" + wp.siteName + "-ID-" +lf.ID + "-" + lf.type + "-range-" + lf.viewMin + "-to-" + lf.viewMax + "-capture-"+ lf.captureView  +".png";
			dm.saveFullScreenshot(fileName);
			if(lf.rectangles.size() > 1)
			{
				dm.scroll(lf.rectangles.get(1));
			}
			fileName = "window-screenshot-" + wp.siteName + "-ID-" +lf.ID + "-" + lf.type + "-range-" + lf.viewMin + "-to-" + lf.viewMax + "-capture-"+ lf.captureView  +".png";
			dm.saveViewportScreenshot(fileName);

			lf.captureView = lf.viewMax;
			dm.setViewport(lf.captureView);
			fileName = "full-screenshot-" + wp.siteName + "-ID-" +lf.ID + "-" + lf.type + "-range-" + lf.viewMin + "-to-" + lf.viewMax + "-capture-"+ lf.captureView  +".png";
			dm.saveFullScreenshot(fileName);
			if(lf.rectangles.size() > 1)
			{
				dm.scroll(lf.rectangles.get(1));
			}
			fileName = "window-screenshot-" + wp.siteName + "-ID-" +lf.ID + "-" + lf.type + "-range-" + lf.viewMin + "-to-" + lf.viewMax + "-capture-"+ lf.captureView  +".png";
			dm.saveViewportScreenshot(fileName);

			lf.captureView = lf.viewMax+1;
			dm.setViewport(lf.captureView);
			fileName = "full-screenshot-" + wp.siteName + "-ID-" +lf.ID + "-" + lf.type + "-range-" + lf.viewMin + "-to-" + lf.viewMax + "-capture-"+ lf.captureView  +".png";
			dm.saveFullScreenshot(fileName);
			if(lf.rectangles.size() > 1)
			{
				dm.scroll(lf.rectangles.get(1));
			}
			fileName = "window-screenshot-" + wp.siteName + "-ID-" +lf.ID + "-" + lf.type + "-range-" + lf.viewMin + "-to-" + lf.viewMax + "-capture-"+ lf.captureView  +".png";
			dm.saveViewportScreenshot(fileName);

			lf.setCaptureView();
			dm.setViewport(lf.captureView);
	}

	public void finalReach(Failure lf, String siteName)
	{
		if(lf.bestEffort)//reach only if there was an attempt to categorize without modifying the location of elements.
		{
			if((lf.falsePositive || lf.NOI || lf.ignored) && lf.type.equals("viewport")) //if it is true positive then there is no need to check. 
			{
				//System.out.println("Final Reach");
				lf.unreachableDimensions(dm);
				lf.titles.add("ReCategorization Attempt:");
				lf.notes.add("This failure is a candidate for ReCat");
				for(int xpathID: lf.problemXpathID)
				{			
					if(lf.reachRightDim != null || lf.reachBottomDim != null || lf.reachLeftDim != null || lf.reachTopDim != null)
					{
						//						System.out.println("ID        : " + lf.ID);
						//						System.out.println("Fault Type: " + lf.type);
						//						System.out.println("xpath 0   : " + lf.xpaths.get(0));
						//						System.out.println("xpath 1   : " + lf.xpaths.get(1));
						//						System.out.println("Problem xpath current: " + lf.xpaths.get(xpathID));
						//						System.out.println("Problem xpath count  : " + lf.problemXpathID.size());

						WebElement wb = dm.getWebElem(lf.xpaths.get(xpathID));
						WebElement body = dm.getWebElem("/HTML/BODY");
						String wbStyle = wb.getAttribute("style");
						String bodyStyle = body.getAttribute("style");
						Rectangle wbRectangle = new Rectangle(wb.getLocation(),wb.getSize());

						lf.titles.add("ReCat xpath:");
						lf.notes.add(lf.xpaths.get(xpathID));
						lf.titles.add("ReCat Original Rectangle:");
						lf.notes.add("Original XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + wbRectangle.height + "," + wbRectangle.width + ") ");



						if(lf.reachRightDim != null && (lf.falsePositive || lf.NOI || lf.ignored))
						{
							lf.titles.add("ReCat Target Rectangle:");
							lf.notes.add("Target XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + lf.reachRightDim.height + "," + lf.reachRightDim.width + ") ");
							finalReachRightImproved(lf, siteName, wb, body, wbRectangle);
							dm.setStyle(wb, wbStyle);
							dm.setStyle(body, bodyStyle);
						}
						if(lf.reachBottomDim != null && (lf.falsePositive || lf.NOI || lf.ignored))
						{
							lf.titles.add("ReCat Target Rectangle:");
							lf.notes.add("Target XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + lf.reachBottomDim.height + "," + lf.reachBottomDim.width + ") ");
							finalReachBottom(lf, siteName, wb, body, wbRectangle);
							dm.setStyle(wb, wbStyle);
							dm.setStyle(body, bodyStyle);
						}
						if(lf.reachLeftDim != null && (lf.falsePositive || lf.NOI || lf.ignored))
						{
							lf.titles.add("ReCat Target Rectangle:");
							lf.notes.add("Target XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + lf.reachLeftDim.height + "," + lf.reachLeftDim.width + ") ");
							finalReachLeftImproved(lf, siteName, wb, body, wbRectangle);
							dm.setStyle(wb, wbStyle);
							dm.setStyle(body, bodyStyle);
							//Assist.pause("Left ID: " + lf.ID);
						}
						if(lf.reachTopDim != null && (lf.falsePositive || lf.NOI || lf.ignored))
						{
							lf.titles.add("ReCat Target Rectangle:");
							lf.notes.add("Target XYHW(" + wbRectangle.x + "," + wbRectangle.y + "," + lf.reachTopDim.height + "," + lf.reachTopDim.width + ") ");
							finalReachTop(lf, siteName, wb, body, wbRectangle);
							dm.setStyle(wb, wbStyle);
							dm.setStyle(body, bodyStyle);
							//Assist.pause("Top ID: " + lf.ID);
						}
					}
				}
			}else
			{
				lf.bestEffort = false; //There was no attempt to move the elements since it is already a TP.
				lf.titles.add("Unscrollable Area Not Needed:");
				if(lf.type.equals("viewport"))
				{
					lf.notes.add("This failure had some content unreachable by scrolling but it was successufully classified as TP without programatically moving the elements.");
				}else
				{
					lf.notes.add("This failure had some content unreachable by scrolling but it is not a viewport failure hence it was ignored.");
				}
			}
		}
	}
	public void finalReachTop(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String bodyMarginTop = dm.getMarginTop(body);					
		String wbMarginTop = dm.getMarginTop(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbTop = dm.getTop(wb);
		String opacity = dm.getOpacity(wb);
		BufferedImage screenshot;

		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		lf.titles.add("ReCat Criteria:");		
		lf.notes.add("Cant reach Top by("+lf.reachTopDim.height+")");
		Rectangle topR = renewTopRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginTop.substring(0, wbMarginTop.length()-2)) + lf.reachTopDim.height)+"px";
		String wbMoveByTop;

		if(wbTop.substring(wbTop.length()-2, wbTop.length()).equals("px"))
		{
			wbMoveByTop = Integer.toString(Integer.parseInt(wbTop.substring(0, wbTop.length()-2)) + lf.reachTopDim.height)+"px";
		}
		else
		{
			wbMoveByTop = lf.reachTopDim.height + "px";
		}
		//		if(!wbPosition.equals("static"))
		//		{
		//			dm.setMarginTop(body, wbMoveByTop);
		//			topR = renewTopRectangle(lf, wb);
		//			lf.titles.add("ReCat Move:");
		//			lf.notes.add("Body moved by ("+ wbMoveByTop +") new coordinates XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") ");
		//		}
		if(topR.y < 0)
		{
			if(wbPosition.equals("static"))
			{
				dm.setMarginTop(wb, wbMoveBy);
				topR = renewTopRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") ");
			}
			else
			{
				dm.setTop(wb, wbMoveByTop);
				topR = renewTopRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveByTop+") new coordinates XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") ");
			}
		}	
		TargetArea ta = new TargetArea(topR);
		if(topR.y == 0 && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
			//System.out.println("Reach Top Passed.");
			dm.scroll(ta.area);
			dm.setOpacity(wb, "0");
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			dm.setOpacity(wb, opacity);
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			lf.printFlagT = true;
			if(ta.anyChange())
			{

				lf.newCat("TP");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "T");
			}
			else
			{
				lf.newCat("NOI");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "T");
			}

		}
		else
		{
			System.out.println("Reach Top Failed by : " +topR.y);
			System.out.println("----------------------------FAILED-------FAILED---------------------------");
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") "); 
			if(topR.height > 0 && topR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "T");
		}

	}
	public void finalReachLeft(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String bodyMarginLeft = dm.getMarginLeft(body);					
		String wbMarginLeft = dm.getMarginLeft(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbLeft = dm.getLeft(wb);
		String opacity = dm.getOpacity(wb);
		BufferedImage screenshot;

		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		lf.titles.add("ReCat Criteria:");		
		lf.notes.add("Cant reach Left by("+lf.reachLeftDim.width+")");
		Rectangle LeftR = renewLeftRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginLeft.substring(0, wbMarginLeft.length()-2)) + lf.reachLeftDim.width)+"px";
		String wbMoveByLeft;

		if(wbLeft.substring(wbLeft.length()-2, wbLeft.length()).equals("px"))
		{
			wbMoveByLeft = Integer.toString(Integer.parseInt(wbLeft.substring(0, wbLeft.length()-2)) + lf.reachLeftDim.width)+"px";
		}
		else
		{
			wbMoveByLeft = lf.reachLeftDim.width + "px";
		}
		//		if(!wbPosition.equals("static"))
		//		{
		//			dm.setMarginLeft(body, wbMoveByLeft);
		//			LeftR = renewLeftRectangle(lf, wb);
		//			lf.titles.add("ReCat Move:");
		//			lf.notes.add("Body moved by ("+ wbMoveByLeft +") new coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") ");
		//		}
		if(LeftR.x < 0)
		{
			if(wbPosition.equals("static"))
			{
				dm.setMarginLeft(wb, wbMoveBy);
				LeftR = renewLeftRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") ");
			}
			else
			{
				dm.setLeft(wb, wbMoveByLeft);
				LeftR = renewLeftRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveByLeft+") new coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") ");
			}
		}	
		TargetArea ta = new TargetArea(LeftR);
		if(LeftR.x == 0 && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
			dm.scroll(ta.area);
			dm.setOpacity(wb, "0");
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			dm.setOpacity(wb, opacity);
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			lf.printFlagL = true;
			if(ta.anyChange())
			{

				lf.newCat("TP");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "L");
			}
			else
			{
				lf.newCat("NOI");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "L");
			}

		}
		else
		{
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") "); 
			if(LeftR.height > 0 && LeftR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "L");
		}

	}
	public void finalReachLeftImproved(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String bodyMarginLeft = dm.getMarginLeft(body);					
		String wbMarginLeft = dm.getMarginLeft(wb);
		String wbMarginRight = dm.getMarginRight(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbLeft = dm.getLeft(wb);
		String opacity = dm.getOpacity(wb);
		BufferedImage screenshot;

		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		lf.titles.add("ReCat Criteria:");		
		lf.notes.add("Cant reach Left by("+lf.reachLeftDim.width+")");
		Rectangle LeftR = renewLeftRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginLeft.substring(0, wbMarginLeft.length()-2)) + lf.reachLeftDim.width)+"px";
		String wbMoveByLeft;

		if(wbLeft.substring(wbLeft.length()-2, wbLeft.length()).equals("px"))
		{
			wbMoveByLeft = Integer.toString(Integer.parseInt(wbLeft.substring(0, wbLeft.length()-2)) + lf.reachLeftDim.width)+"px";
		}
		else
		{
			wbMoveByLeft = lf.reachLeftDim.width + "px";
		}
		if(LeftR.x < 0)
		{
			if(wbPosition.equals("static"))
			{
				dm.setMarginLeft(wb, wbMoveBy);
				LeftR = renewLeftRectangle(lf, wb);
				if(LeftR.x < 0) //second attempt
				{
					dm.setMarginLeft(wb, wbMarginLeft); //reset left
					//System.out.println("Second attempt to move....");
					wbMoveBy = Integer.toString(Integer.parseInt(wbMarginRight.substring(0, wbMarginRight.length()-2)) - lf.reachLeftDim.width)+"px";
					dm.setMarginRight(wb, wbMoveBy);
					LeftR = renewLeftRectangle(lf, wb);
//					if(LeftR.x < 0) //second attempt
//						System.out.println("Second attempt FAILED");
					lf.titles.add("ReCat Move:");
					lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") ");
				}else {
					lf.titles.add("ReCat Move:");
					lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") ");
				}
			}
			else
			{
				dm.setLeft(wb, wbMoveByLeft);
				LeftR = renewLeftRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveByLeft+") new coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") ");
			}

		}	
		TargetArea ta = new TargetArea(LeftR);
		if(LeftR.x == 0 && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
			//System.out.println("Reach Left Passed.");
			dm.scroll(ta.area);
			dm.setOpacity(wb, "0");
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			dm.setOpacity(wb, opacity);
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			lf.printFlagL = true;
			if(ta.anyChange())
			{

				lf.newCat("TP");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "L");
			}
			else
			{
				lf.newCat("NOI");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "L");
			}

		}
		else
		{
			System.out.println("Reach Left Failed by : " +LeftR.x);
			System.out.println("----------------------------FAILED-------FAILED---------------------------");
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + LeftR.x + "," + LeftR.y + "," + LeftR.height + "," + LeftR.width + ") "); 
			if(LeftR.height > 0 && LeftR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "L");
		}

	}
	public void finalReachRight(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String bodyMarginLeft = dm.getMarginLeft(body);					
		String wbMarginLeft = dm.getMarginLeft(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbLeft = dm.getLeft(wb);
		String opacity = dm.getOpacity(wb);
		BufferedImage screenshot;

		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		Rectangle rightR = renewRightRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginLeft.substring(0, wbMarginLeft.length()-2)) - lf.reachRightDim.width)+"px";
		String wbMoveByLeft;
		if(wbLeft.substring(wbLeft.length()-2, wbLeft.length()).equals("px"))
		{
			wbMoveByLeft = Integer.toString(Integer.parseInt(wbLeft.substring(0, wbLeft.length()-2)) - lf.reachRightDim.width)+"px";
		}
		else
		{
			wbMoveByLeft = -lf.reachRightDim.width + "px";
		}

		if(wbPosition.equals("static"))
		{
			dm.setMarginLeft(wb, wbMoveBy);
			rightR = renewRightRectangle(lf, wb);
			lf.titles.add("ReCat Move:");
			lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") ");
		}
		else
		{
			dm.setLeft(wb, wbMoveByLeft);
			rightR = renewRightRectangle(lf, wb);
			lf.titles.add("ReCat Move:");
			lf.notes.add("Element moved by ("+wbMoveByLeft+") new coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") ");
		}
		dm.scroll(rightR);

		TargetArea ta = new TargetArea(rightR);
		if(dm.scrollX == 0 && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
			//System.out.println("Reach Right Passed.");
			dm.scroll(ta.area);
			dm.setOpacity(wb, "0");
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			dm.setOpacity(wb, opacity);
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			lf.printFlagR = true;
			if(ta.anyChange())
			{

				lf.newCat("TP");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "R");
			}
			else
			{
				lf.newCat("NOI");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "R");
			}

		}
		else
		{
			System.out.println("Reach Right Failed by : " +dm.scrollX);
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") "); 
			if(rightR.height > 0 && rightR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "R");
		}

	}
	public void finalReachRightImproved(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String bodyMarginLeft = dm.getMarginLeft(body);					
		String wbMarginLeft = dm.getMarginLeft(wb);
		String wbMarginRight = dm.getMarginRight(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbLeft = dm.getLeft(wb);
		String opacity = dm.getOpacity(wb);
		String oldStyle = dm.getStyle(wb);
		BufferedImage screenshot;

		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		Rectangle rightR = renewRightRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginLeft.substring(0, wbMarginLeft.length()-2)) - lf.reachRightDim.width)+"px";
		String wbMoveByLeft;
		if(wbLeft.substring(wbLeft.length()-2, wbLeft.length()).equals("px"))
		{
			wbMoveByLeft = Integer.toString(Integer.parseInt(wbLeft.substring(0, wbLeft.length()-2)) - lf.reachRightDim.width)+"px";
		}
		else
		{
			wbMoveByLeft = -lf.reachRightDim.width + "px";
		}
		boolean thirdAttempt = false;
		if(wbPosition.equals("static"))
		{

			dm.setMarginLeft(wb, wbMoveBy);
			rightR = renewRightRectangle(lf, wb);

			dm.scroll(rightR);
			if(dm.cantReachX == lf.reachRightDim.width) { //second attempt
//				System.out.println("Second attempt to move....");
				dm.setMarginLeft(wb, wbMarginLeft); //reset left
				String wbMoveByThirdAttempt = wbMoveBy;
				wbMoveBy = Integer.toString(Integer.parseInt(wbMarginRight.substring(0, wbMarginRight.length()-2)) + lf.reachRightDim.width)+"px";
				dm.setMarginRight(wb, wbMoveBy);
				rightR = renewRightRectangle(lf, wb);
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") ");
				dm.scroll(rightR);
				if(dm.cantReachX == lf.reachRightDim.width) { //third attempt
//					System.out.println("Second attempt failed....");
//					System.out.println("Second attempted to move ...." + wbMoveBy);
//					boolean paused = false;
//					for(int i : pauseList)
//						if(i == lf.ID) {
//							paused = true;
//							Assist.pause("Will move");
//						}
//					System.out.println("Third attempt to move....");
//					System.out.println("old Style -> "+ oldStyle);
					String newStyle =  oldStyle + "margin-left:" + wbMoveByThirdAttempt + " !important;";
					dm.setStyle(wb, newStyle);
//					System.out.println("new Style -> "+ newStyle);
					rightR = renewRightRectangle(lf, wb);
					dm.scroll(rightR);
//					if(paused)
//						Assist.pause("Move Completed");
				
				}
				}else {
				lf.titles.add("ReCat Move:");
				lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") ");
			}
		}
		else
		{
			//System.out.println("Non static move.... ("+wbPosition+") will move by " + wbMoveByLeft + " old left is " + wbLeft);
			dm.setLeft(wb, wbMoveByLeft);
			rightR = renewRightRectangle(lf, wb);
			lf.titles.add("ReCat Move:");
			lf.notes.add("Element moved by ("+wbMoveByLeft+") new coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") ");
		}
		dm.scroll(rightR);
		if(rightR.width > lf.viewMaxWidth)
			rightR.setWidth(lf.viewMaxWidth);
		TargetArea ta = new TargetArea(rightR);
		if((dm.cantReachX == 0 || dm.cantReachX <= lf.reachRightDim.width - Integer.parseInt(wbMoveBy.substring(0, wbMoveBy.length()-2))) && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
			//System.out.println("Reach Right Passed.");
			dm.scroll(ta.area);
			dm.setOpacity(wb, "0");
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			dm.setOpacity(wb, opacity);
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			lf.printFlagR = true;
			if(ta.anyChange())
			{

				lf.newCat("TP");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "R");
			}
			else
			{
				lf.newCat("NOI");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "R");
			}
			dm.setStyle(wb, oldStyle);
		}
		else
		{
			System.out.println("Reach Right Failed by : " + dm.cantReachX);
			System.out.println("----------------------------FAILED-------FAILED---------------------------------------------");

			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") "); 
			if(rightR.height > 0 && rightR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "R");
		}

	}
	public void finalReachBottom(Failure lf, String siteName, WebElement wb, WebElement body, Rectangle wbRectangle) {
		if(lf.falsePositive == false && lf.ignored == false && lf.NOI == false ) //if true positive don't reach
			return;
		String wbMarginTop = dm.getMarginTop(wb);
		String wbHeight = dm.getHeight(wb);
		String wbWidth = dm.getWidth(wb);
		String wbPosition = dm.getPosition(wb);
		String wbTop = dm.getTop(wb);
		String opacity = dm.getOpacity(wb);
		BufferedImage screenshot;

		dm.setWidth(wb, wbWidth);
		dm.setHeight(wb, wbHeight);

		Rectangle bottomR = renewBottomRectangle(lf, wb);
		String wbMoveBy = Integer.toString(Integer.parseInt(wbMarginTop.substring(0, wbMarginTop.length()-2)) - lf.reachBottomDim.height)+"px";
		String wbMoveByTop;
		if(wbTop.substring(wbTop.length()-2, wbTop.length()).equals("px"))
		{
			wbMoveByTop = Integer.toString(Integer.parseInt(wbTop.substring(0, wbTop.length()-2)) - lf.reachBottomDim.height)+"px";
		}
		else
		{
			wbMoveByTop = -lf.reachBottomDim.height + "px";
		}

		if(wbPosition.equals("static"))
		{
			dm.setMarginTop(wb, wbMoveBy);
			bottomR = renewBottomRectangle(lf, wb);
			lf.titles.add("ReCat Move:");
			lf.notes.add("Element moved by ("+wbMoveBy+") new coordinates XYHW(" + bottomR.x + "," + bottomR.y + "," + bottomR.height + "," + bottomR.width + ") ");
		}
		else
		{
			dm.setTop(wb, wbMoveByTop);
			bottomR = renewBottomRectangle(lf, wb);
			lf.titles.add("ReCat Move:");
			lf.notes.add("Element moved by ("+wbMoveByTop+") new coordinates XYHW(" + bottomR.x + "," + bottomR.y + "," + bottomR.height + "," + bottomR.width + ") ");
		}
		dm.scroll(bottomR);

		TargetArea ta = new TargetArea(bottomR);
		if(dm.scrollY == 0 && wbRectangle.width == wb.getSize().width && wbRectangle.height == wb.getSize().height)
		{
			//System.out.println("Reach Bottom Passed.");
			dm.scroll(ta.area);
			dm.setOpacity(wb, "0");
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			dm.setOpacity(wb, opacity);
			screenshot = dm.screenshot();
			ta.targetImgs.add(Assist.getSubImage(screenshot, ta.area,dm.scrollX,dm.scrollY,lf));
			lf.printFlagB = true;
			if(ta.anyChange())
			{
				lf.newCat("TP");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "TP", "B");
			}
			else
			{
				lf.newCat("NOI");
				lf.saveRecatImages(screenshot, dm, siteName, ta, "NOI", "B");
			}

		}
		else
		{
			System.out.println("Reach Bottom Failed by : " +dm.scrollY);
			System.out.println("----------------------------FAILED-------FAILED-----------------------------------------------");
			screenshot = dm.screenshot();
			lf.titles.add("ReCat fail:");
			lf.notes.add("Element coordinates XYHW(" + bottomR.x + "," + bottomR.y + "," + bottomR.height + "," + bottomR.width + ") "); 
			if(bottomR.height > 0 && bottomR.width > 0 && lf.falsePositive)
			{
				lf.newCat("NOI");
				try {
					lf.saveImages(siteName, lf.FirstImageScrollOffsetX, lf.FirstImageScrollOffsetY);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			ta = null;
			lf.saveRecatImages(screenshot, dm, siteName, ta, "FailedReach", "B");
		}

	}
	private Rectangle renewTopRectangle(Failure lf, WebElement wb)
	{
		Rectangle topR;
		topR = new Rectangle(wb.getLocation(),wb.getSize());
		//topR.setY((topR.y+topR.height)-lf.reachTopDim.height);
		topR.setHeight(lf.reachTopDim.height);
		topR.setWidth(lf.reachTopDim.width);
		lf.titles.add("ReCat Target Rectangle:");
		lf.notes.add("Target XYHW(" + topR.x + "," + topR.y + "," + topR.height + "," + topR.width + ") ");
		return topR;
	}
	private Rectangle renewBottomRectangle(Failure lf, WebElement wb)
	{
		Rectangle bottomR;
		bottomR = new Rectangle(wb.getLocation(),wb.getSize());
		bottomR.setY((bottomR.y+bottomR.height)-lf.reachBottomDim.height);
		bottomR.setHeight(lf.reachBottomDim.height);
		bottomR.setWidth(lf.reachBottomDim.width);
		lf.titles.add("ReCat Target Rectangle:");
		lf.notes.add("Target XYHW(" + bottomR.x + "," + bottomR.y + "," + bottomR.height + "," + bottomR.width + ") ");
		return bottomR;
	}
	private Rectangle renewRightRectangle(Failure lf, WebElement wb)
	{
		Rectangle rightR;
		rightR = new Rectangle(wb.getLocation(),wb.getSize());
		rightR.setX((rightR.x+rightR.width)-lf.reachRightDim.width);
		rightR.setHeight(lf.reachRightDim.height);
		rightR.setWidth(lf.reachRightDim.width);
		lf.titles.add("ReCat Target Rectangle:");
		lf.notes.add("Target XYHW(" + rightR.x + "," + rightR.y + "," + rightR.height + "," + rightR.width + ") ");
		return rightR;
	}
	private Rectangle renewLeftRectangle(Failure lf, WebElement wb)
	{
		Rectangle leftR;
		leftR = new Rectangle(wb.getLocation(),wb.getSize());
		//leftR.setY((leftR.y+leftR.height)-lf.reachLeftDim.height);
		leftR.setHeight(lf.reachLeftDim.height);
		leftR.setWidth(lf.reachLeftDim.width);
		lf.titles.add("ReCat Target Rectangle:");
		lf.notes.add("Target XYHW(" + leftR.x + "," + leftR.y + "," + leftR.height + "," + leftR.width + ") ");
		return leftR;
	}


	public void outputHTMLSite() throws IOException
	{
		for(Webpage wp: webpages)
		{
			for(Failure lf : wp.Failures)
			{
				lf.writeImages(wp.siteName);
			}
		}
		htmlout html = new htmlout();
		dm.navigate("file://" + new File(".").getCanonicalFile()+ Assist.navWebDirectory + html.gallary(webpages));
		dm.maximize();
		//		dm.shutdown();
	}
	public void writeReport()
	{
		String fileName = Assist.outDirectory + File.separator + "report.csv";
		try 
		{
			PrintWriter writer;
			writer = new PrintWriter(fileName);
			writer.println("UID,Failure,Site,Range,Categorization,Prior Cat,Best Effort,Screenshot 1,Screenshot 2,XPath 1,XPath 2,Rect 1 (X*Y*H*W),Rect 2 (X*Y*H*W),Runtime Millisec");
			for(Webpage wp: webpages)
			{
				for(Failure lf : wp.Failures)
				{
					//if(lf.type.equals("collision") || lf.type.equals("viewport") || lf.type.equals("protrusion")|| lf.type.equals("wrapping"))
					//{
					String result = "Error in reporting";
					String screenshotFile = "file:///C:/Results/"+Assist.date+"/"; 
					String screenshotFile2 = "file:///C:/Results/"+Assist.date+"/"; 
					String bestEffort = "No";
					String priorCat = lf.priorCat;
					String xpaths = "";
					String rect1;
					String rect2;
					String rectangles;
					if(lf.type.equals("wrapping")) {
						xpaths =   "," + lf.xpaths.get(0)+ "," + lf.xpaths.get(lf.IndexOfLastElementInLine);
						rect1 = "("+ lf.orignalRectangles.get(0).x+"*" + lf.orignalRectangles.get(0).y +"*" + lf.orignalRectangles.get(0).height +"*"+ lf.orignalRectangles.get(0).width +")";
						rect2 = "("+ lf.orignalRectangles.get(lf.IndexOfLastElementInLine).x+"*" + lf.orignalRectangles.get(lf.IndexOfLastElementInLine).y +"*" + lf.orignalRectangles.get(lf.IndexOfLastElementInLine).height +"*"+ lf.orignalRectangles.get(lf.IndexOfLastElementInLine).width +")";
						rectangles = "," + rect1 + "," + rect2;
					}else {
						for(String xpath:lf.xpaths)
						{
							xpaths = xpaths + "," + xpath;
						}
						rect1 = "("+ lf.orignalRectangles.get(0).x+"*" + lf.orignalRectangles.get(0).y +"*" + lf.orignalRectangles.get(0).height +"*"+ lf.orignalRectangles.get(0).width +")";
						rect2 = "("+ lf.orignalRectangles.get(1).x+"*" + lf.orignalRectangles.get(1).y +"*" + lf.orignalRectangles.get(1).height +"*"+ lf.orignalRectangles.get(1).width +")";
						rectangles = "," + rect1 + "," + rect2;
					}

					if(lf.type.equals("small-range")) {
						screenshotFile = screenshotFile + "ID_" + lf.ID + "-" + wp.siteName + "-" + lf.type + "-capture-"+ lf.captureView  +".png";
						screenshotFile2 = screenshotFile2 + "ID_" + lf.ID + "-" + wp.siteName + "-" + lf.type + "-capture-"+ lf.minOracle.captureView  +".png";
					}
					if(lf.ignored)
					{
						result = "Ignored";
						screenshotFile = screenshotFile + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_ignored_" +0+ ".png";
						screenshotFile2 = screenshotFile2 + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_ignored_" +1+ ".png";

					}else if(lf.NOI)
					{
						result = "NOI";
						screenshotFile = screenshotFile + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_NOI_" +0+ ".png";
						screenshotFile2 = screenshotFile2 + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_NOI_" +1+ ".png";

					}else if(lf.falsePositive)
					{
						result = "FP";
						screenshotFile = screenshotFile + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_FP_" +0+".png";
						screenshotFile2 = screenshotFile2 + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_FP_" +1+".png";

					}else
					{
						result = "TP";
						screenshotFile = screenshotFile + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_TP_" +0+".png";
						screenshotFile2 = screenshotFile2 + "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_TP_" +1+".png";

					}

					if(lf.bestEffort == true)
					{
						bestEffort = "Yes";
					}
					if(lf.priorCat.equals(""))
					{
						priorCat = result;
					}
					screenshotFile = "=HYPERLINK(\""+screenshotFile+"\")";
					screenshotFile2 = "=HYPERLINK(\""+screenshotFile2+"\")";
					writer.println(lf.ID + "," + lf.type + "," + wp.siteName + "," + lf.viewMin + "px-" + lf.viewMax + "px,"+result + "," + priorCat + "," + bestEffort  + "," + screenshotFile+ "," + screenshotFile2 + xpaths + rectangles+","+lf.getDurationMilli());
					//}
				}
			}
			writer.close();

		} catch (IOException e) 
		{    
			System.out.println("could not save report to file ("+fileName+")... exiting");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void printSiteThenExit(Webpage wp, Failure lf, boolean exit) {
		System.out.println("Site   : " + wp.siteName);
		System.out.println("Page   : " + wp.wPageName);
		System.out.println("Path   : " + wp.wPagePath);
		System.out.println("Run    : " + wp.uniqueRunName);
		System.out.println("Capture: " + lf.captureView);
		System.out.println("Range  : " + lf.viewMin + "-" + lf.viewMax + " px");
		System.out.println("Failure: " + lf.type);
		System.out.println("ID     : " + lf.ID);
		for(int i =0; i < lf.xpaths.size(); i++)
		{
			System.out.println("xpath  : " + lf.xpaths.get(i));
		}
		for(int i =0; i < lf.rectangles.size(); i++)
		{
			System.out.println("Rect "+ i +" : " + "xy(" + lf.rectangles.get(i).getX() + "," + lf.rectangles.get(i).getY()+ ")  hw(" + lf.rectangles.get(i).getHeight() + "," + lf.rectangles.get(i).getWidth() + ")  Area(" + (lf.rectangles.get(i).getHeight()*lf.rectangles.get(i).getWidth())+")");
		}
		if(exit)
		{
			System.out.println("Exiting..... ");
			System.exit(0);
		}
	}
	private void findWebElementsAddRectangles(Failure lf)
	{
		for(int i =0; i < lf.xpaths.size(); i++)
		{

			dm.scrollZero();
			WebElement wb = dm.getWebElem(lf.xpaths.get(i));
			lf.wbElements.add(wb);
			Rectangle r = new Rectangle(lf.wbElements.get(i).getLocation(),lf.wbElements.get(i).getSize());
			Rectangle origR = new Rectangle(lf.wbElements.get(i).getLocation(),lf.wbElements.get(i).getSize());
			lf.orignalRectangles.add(origR);
			lf.titles.add("Original Rectangle Information:");
			lf.notes.add(lf.xpaths.get(i) + "   XYHW(" + r.x + "," + r.y + "," + r.height + "," + r.width + ")");

			dm.scroll(r);

			if(r.x < 0 || r.y < 0 || dm.cantReachX > 0 || dm.cantReachY > 0)
			{
				lf.bestEffort = true;
				lf.problemXpathID.add(i);

				String note = "*Original coordinates XYHW(" + r.x + "," + r.y + "," + r.height + "," + r.width + ") ";

				r.setWidth(r.width - dm.cantReachX);
				r.setHeight(r.height - dm.cantReachY);
				if(r.x < 0)
				{
					r.setWidth(r.width + r.x);
					r.setX(0);
				}
				if(r.y < 0)
				{

					r.setHeight(r.height + r.y);
					r.setY(0);
				}
				note = note + "to XYHW(" + r.x + "," + r.y + "," + r.height + "," + r.width + ") ";
				//System.out.println(note);
				lf.titles.add("Rectangle (WebElemet) Size Warning:");
				lf.notes.add(note);
			}
			lf.rectangles.add(r);
			//			lf.rectangles.add(new Rectangle(newX,newY,newH,newW));

			if(wb == null)
			{
				lf.setIgnore(true);
				lf.titles.add("Element Not Found:");
				lf.notes.add("Timeout or Could not find... xpath " + lf.xpaths.get(i));
				System.out.println("Timeout or Could not find... xpath " + lf.xpaths.get(i));
			}
			if(r.height <= 0 || r.width <= 0)
			{
				lf.titles.add("Element Size Error:");
				lf.notes.add(lf.xpaths.get(i) + " Height: " + r.height + " , Width: " + r.width);
				System.out.println("Element Size Error: " + lf.xpaths.get(i) + " Height: " + r.height + " , Width: " + r.width);
				if(lf.type.equals("wrapping")) {
					if(i == 0) {
						lf.titles.add("Setting As false positive:");
						lf.notes.add(lf.xpaths.get(i) + " Height: " + r.height + " , Width: " + r.width);
						lf.setFalsePositive(true);
					}
					else if(lf.xpaths.size() >= 3) {
						System.out.println("Removing row element: " + lf.xpaths.get(i) + " With Height: " + r.height + " , Width: " + r.width);
						lf.titles.add("Removing Row Element:");
						lf.notes.add(lf.xpaths.get(i) + " Height: " + r.height + " , Width: " + r.width);
						lf.xpaths.remove(i);
						lf.orignalRectangles.remove(i);
						lf.wbElements.remove(i);
					}else {
						lf.setIgnore(true);
					}
				}else {
					lf.setIgnore(true);
				}
			}

		}
	}


	public void addWebpage(Webpage wp)
	{
		webpages.add(wp);
	}
	public void setWebpages(ArrayList<Webpage> webpages) {
		this.webpages = webpages;
	}
}
