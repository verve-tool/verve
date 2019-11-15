/*
 * Created by Ibrahim Althomali on 12/15/2017
 * 
 ********************************************/
package verve;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import org.opencv.core.Core;




public class Workbench
{
	static String webpagesPath = "/Users/redecheck/stvr-20-sites-and-orignal";
	static String reportsPath = "/Users/redecheck/reports-new-20-and-original";
	static String reportsOriginalFailuresWithWrongRange = "/Users/redecheck/reports-original-experiment";
	static boolean compareAndExtractTwoRedecheckOutputs = false;
	static Scanner reader = new Scanner(System.in);  
	static ArrayList<Webpage> subsetPages = new ArrayList<Webpage>(); //for testing just specific sites
	static ArrayList<Webpage> allPages;
	static ArrayList<Webpage> allPagesOriginalFailuresWithWrongRange;
	static boolean featureHiddenDetection = false; //detect hidden collision elements behind another collision element with background color. 
	public static void main(String[] args) throws IOException
	{
		if(compareAndExtractTwoRedecheckOutputs)
		{
			reportsPath = "/Users/redecheck/reportsStepSize1Complete";
		}
		nu.pattern.OpenCV.loadShared(); //opencv alternativ is nu.pattern.OpenCV.loadShared();

		Assist.createOutputDirectory();
        String fileName = Assist.outDirectory + File.separator + "ProgramRuntime.csv";
        
        PrintWriter writer = null;
        String runTimeCSV ="";
        long startTime = System.nanoTime();
        Assist.startTime();
		InputManager inM = new InputManager(webpagesPath);
		InputManager inMCompare = new InputManager(webpagesPath);
		inM.findFile("fault-report.txt", new File(reportsPath));
		inM.parseRedecheckOutputFiles();		
		allPages = inM.webpages;
		removeSiteWithZeroFailures(allPages);
		//PrintPages(allPages);
		Categorizer inv = new Categorizer(reportsPath, featureHiddenDetection);
		//new File("." + File.separator +Assist.date).mkdir();
		if(compareAndExtractTwoRedecheckOutputs)
		{
			inMCompare.findFile("fault-report.txt", new File(reportsOriginalFailuresWithWrongRange));
			inMCompare.parseRedecheckOutputFiles();	
			allPagesOriginalFailuresWithWrongRange = inMCompare.webpages;
			removeSiteWithZeroFailures(allPagesOriginalFailuresWithWrongRange);
			//PrintPages(allPagesOriginalFailuresWithWrongRange);
			ManuallyClassifiyiedFailuresOnlyPages(allPages,allPagesOriginalFailuresWithWrongRange);
			inv.setWebpages(allPagesOriginalFailuresWithWrongRange); //All pages found by input manager
		}
		else
		{
			inv.setWebpages(allPages); //All pages found by input manager
		}



//		inv.setWebpages(allPages); //All pages found by input manager
		String site = "pocket";
		String page = "index.html";
		
	//	addPage(site, page);
	//	firstRun();
	//	PrintPages(subsetPages);
	//  inv.setWebpages(subsetPages); //Subset of pages found by input manager

		Assist.endTime();
		runTimeCSV = runTimeCSV + Assist.getDurationMilli() + ",";
		Assist.startTime();
		inv.lookForNOI2();
		Assist.endTime();
		runTimeCSV = runTimeCSV + Assist.getDurationMilli() + ",";
		Assist.startTime();
		inv.outputHTMLSite();
		Assist.endTime();
		runTimeCSV = runTimeCSV + Assist.getDurationMilli() + ",";
		Assist.startTime();
		inv.writeReport();
		Assist.endTime();
		runTimeCSV = runTimeCSV + Assist.getDurationMilli() + ",";
		inv.dm.shutdown(); //close browser
		Assist.reader.close(); 
		long endTime = System.nanoTime();
		runTimeCSV = runTimeCSV + ((endTime - startTime)/1000000);
        try 
        {
        		writer = new PrintWriter(fileName);
            writer.println("Input Millisec,Verify Millisec,Output HTML Millisec,Output CSV Millesec,Program Total Runtime Millesec");
    			writer.println(runTimeCSV);
        }catch (IOException e) 
        {    
            System.out.println("could not save runtime report to file ("+fileName+")... exiting");
            e.printStackTrace();
            System.exit(0);
        }
		writer.close();
		System.out.println("END");
	}
	private static void removeSite(String removeSiteName)
	{
		for(int i =0; i < allPages.size(); i++)
		{
			if(allPages.get(i).siteName.equals(removeSiteName))
			{
				allPages.remove(i);
				break;
			}
		}
	}
	private static void removeSiteWithZeroFailures(ArrayList<Webpage> allPages)
	{
		for(int i =0; i < allPages.size(); i++)
		{
			if(allPages.get(i).Failures.size()==0)
			{
				allPages.remove(i);
				i=0;
			}
		}
	}
	public static void addPage(String site, String page) {
		for(int i=0; i<allPages.size(); i++)
		{
			Webpage tmpPage = allPages.get(i);
			if(tmpPage.siteName.toLowerCase().equals(site.toLowerCase()) && tmpPage.wPageName.toLowerCase().equals(page.toLowerCase()))
			{
				subsetPages.add(tmpPage);
				i=allPages.size(); //finds the first instance of Webpage with the same name
			}
		}
	}
	public static void firstRun() {
		ArrayList<String> runs = new ArrayList<String>();
		for(int i=0; i<allPages.size(); i++)
		{
			Webpage tmpPage = allPages.get(i);
			if(runs.isEmpty() || !runs.contains(tmpPage.siteName))
			{
				subsetPages.add(tmpPage);
				runs.add(tmpPage.siteName);
			}
		}
	}
	public static void lastRun() {
		ArrayList<String> runs = new ArrayList<String>();
		for(int i=allPages.size()-1; i>=0; i--)
		{
			Webpage tmpPage = allPages.get(i);
			if(runs.isEmpty() || !runs.contains(tmpPage.siteName))
			{
				subsetPages.add(tmpPage);
				runs.add(tmpPage.siteName);
			}
		}
	}
	public static void PrintPages(ArrayList<Webpage> pages) {
		System.out.println("Total Sites: " + pages.size());
		System.out.println("---------------------------------");
		for(Webpage wp: pages)
		{
			System.out.println("Site   :" + wp.siteName);
			System.out.println("Page   :" + wp.wPageName);
			System.out.println("Path   :" + wp.wPagePath);
			System.out.println("Run    :" + wp.uniqueRunName);
			System.out.println("Faults :" + wp.Failures.size());
			System.out.println("---------------------------------");
		}
	}
	public static void ManuallyClassifiyiedFailuresOnlyPages(ArrayList<Webpage> allPages,ArrayList<Webpage> targetPages) {
		for(Webpage wp:allPages)
		{
			for(Webpage wpTarget: targetPages) //find same website
			{
				if(wpTarget.siteName.equals(wp.siteName))
				{
					for(int x=0; x < wp.Failures.size();x++) //update the max value
					{
						for(Failure targetF:wpTarget.Failures)
						{
							if(wp.Failures.get(x).xpaths.get(0).equals(targetF.xpaths.get(0)) && 
									wp.Failures.get(x).xpaths.get(1).equals(targetF.xpaths.get(1)))
							{
								if(wp.Failures.get(x).viewMin == targetF.viewMin)
								{
									//System.out.println("Failure ID: "+targetF.ID+ " viewMax changed from " + targetF.viewMax + " to " + wp.Failures.get(x).viewMax);
									targetF.viewMax = wp.Failures.get(x).viewMax;	
									targetF.alreadyMatchedViewportRange = true;
									wp.Failures.get(x).alreadyMatchedViewportRange =true;
									targetF.setCaptureView();
								}
								else if (!targetF.alreadyMatchedViewportRange && !wp.Failures.get(x).alreadyMatchedViewportRange)
								{
									//System.out.println("------ Failure ID: "+targetF.ID+ " viewMin " + targetF.viewMin + " does not match step 1 viewMin "  +wp.Failures.get(x).viewMin + " the max would have changed from " + targetF.viewMax + " to " +wp.Failures.get(x).viewMax);
								}
							}
						}
					}						
				}
			}
		}
	}

}
