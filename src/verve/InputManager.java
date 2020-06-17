package verve;
/*
 * Created by Ibrahim Althomali on 28/11/2017
 * 
 ********************************************/


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;



public class InputManager
{
	ArrayList<File>	inputFiles;
	ArrayList<Webpage>	webpages;
	String localSitesDirectory;
	int RedecheckRuns;
	public InputManager(String localSitesDirectory)
	{
		this.localSitesDirectory = localSitesDirectory;
		inputFiles = new ArrayList<File>();
		webpages = new ArrayList<Webpage>();
	}
	public void findFile(String name,File file)
    {
        if (file.isDirectory())
        {
            File[] subFiles = file.listFiles();
            if(subFiles!=null)
            for (File fileInSubFiles : subFiles)
            {
                findFile(name,fileInSubFiles);
            }
        }else if (name.equalsIgnoreCase(file.getName()))
        {
        	inputFiles.add(file);
        }
    }
	public boolean inList(String[] list, String text) {
		for (String item:list) {
			if(item.toLowerCase().equals(text.toLowerCase())) 
				return true;
		}
		return false;
	}
	public String replaceXPathSVG(String xpath) {
		String[] tags = xpath.trim().split("/");
		boolean addNotation = false;
		String newXPath = "";
		for(String tag : tags) {
			if(tag.trim().equals("")) {
				continue;
			}
			newXPath += "/";
			if(tag.contains("svg")) {
				addNotation = true;
			}
			if(addNotation) {
				newXPath +="*[local-name() = '"+tag+"']";
			}else {
				newXPath += tag;
			}
			
		}
		return newXPath.trim();
		
	}
	public void parseRedecheckOutputFiles()
	{
		String[] ignoredSites = {"StumbleUpon"};
		int count = 1;
		for(File f: inputFiles)
		{
			Webpage wp = new Webpage();
			int from = f.getParentFile().getParentFile().getPath().lastIndexOf(File.separator);
			wp.siteName = f.getParentFile().getParentFile().getPath().substring(from+1);
			if(!inList(ignoredSites, wp.siteName) ) //exclude StumbleUpon since it changed
			{
				from = f.getParentFile().getPath().lastIndexOf(File.separator);
				wp.uniqueRunName = f.getParentFile().getPath().substring(from+1);
				//from = f.getPath().lastIndexOf(File.separator);
				wp.wPageName = "index.html";
				wp.wPagePath = localSitesDirectory+File.separator + wp.siteName + File.separator + wp.wPageName;
				try 
				{
					String line;
					BufferedReader bReader = new BufferedReader(new FileReader(f));
					while((line = bReader.readLine()) != null)
					{
						String lineFragments [] = line.split(" ");
						
						if(line.contains("SMALL RANGE")) {
							Failure lf = new Failure();
							lf.type = "small-range";

							line = bReader.readLine();
							lineFragments = line.split(" ");
							lf.addXpath(replaceXPathSVG(lineFragments[1]));
							lf.addXpath(replaceXPathSVG(lineFragments[3]));
							lf.viewMin = Integer.parseInt(lineFragments[lineFragments.length-6].trim());
							lf.viewMax = Integer.parseInt(lineFragments[lineFragments.length-4].trim());
							lf.smallrangeThisConstraints = lineFragments[lineFragments.length-1].trim();
							
							line = bReader.readLine();
							lineFragments = line.split(" ");
							lf.smallrangePrevConstraints = lineFragments[lineFragments.length-6].trim()+ "-" + lineFragments[lineFragments.length-4].trim() + " " +lineFragments[lineFragments.length-1].trim();

							line = bReader.readLine();
							lineFragments = line.split(" ");
							lf.smallrangeNextConstraints = lineFragments[lineFragments.length-6].trim()+ "-" + lineFragments[lineFragments.length-4].trim() + " " +lineFragments[lineFragments.length-1].trim();

//							System.out.println(wp.siteName);
//							System.out.println("Small Range ("+lf.viewMin+"-"+lf.viewMax+"): "+ lf.xpaths.get(0) + " ------ " + lf.xpaths.get(1));
//							System.out.println("This : "+ lf.smallrangeThisConstraints);
//							System.out.println("Prev : "+ lf.smallrangePrevConstraints);
//							System.out.println("Next : "+ lf.smallrangeNextConstraints);
							
							//setup two objects to contain information about correct layout - NOT A FAILURE
							lf.minOracle = new Failure();
							lf.minOracle.type = "small-range";
							lf.minOracle.ID = lf.ID;
							lf.minOracle.viewMin = lf.viewMin-1;
							lf.minOracle.viewMax = lf.viewMin-1;
							lf.minOracle.addXpath(lf.xpaths.get(0));
							lf.minOracle.addXpath(lf.xpaths.get(1));
							lf.minOracle.setCaptureView();
							
							lf.maxOracle = new Failure(); 
							lf.maxOracle.type = "small-range";
							lf.maxOracle.ID = lf.ID;
							lf.maxOracle.viewMin = lf.viewMax+1;
							lf.maxOracle.viewMax = lf.viewMax+1;
							lf.maxOracle.addXpath(lf.xpaths.get(0));
							lf.maxOracle.addXpath(lf.xpaths.get(1));
							lf.maxOracle.setCaptureView();
							if(!Assist.injectedFailure | ( Assist.injectedFailure & lf.viewMin== Assist.injectedFailureRangeMin && lf.viewMax == Assist.injectedFailureRangeMax)) {
							lf.setCaptureView();
							lf.ID = count;
							count++;
							wp.Failures.add(lf);
							
							addToSiteCaptureList(wp, lf);
							addToSiteCaptureList(wp, lf.minOracle);
							addToSiteCaptureList(wp, lf.maxOracle);
							}
							
						}
						else if(line.contains("WRAPPING")) {
							Failure lf = new Failure();
							lf.type = "wrapping";
							lf.ID = count;
							count++;
							lf.viewMin = Integer.parseInt(lineFragments[lineFragments.length-3].trim());
							lf.viewMax = Integer.parseInt(lineFragments[lineFragments.length-1].trim().substring(0, lineFragments[lineFragments.length-1].trim().length() - 1));
							line = bReader.readLine();
							lineFragments = line.split(" ");
							lf.addXpath(replaceXPathSVG(lineFragments[0])); //wrapped element + //workaround patch for Firefox to get the element
							line = bReader.readLine();
							lineFragments = line.split(" ");
							for(int i=0; i < lineFragments.length; i++) {
								if(!lineFragments[i].trim().equals(lf.xpaths.get(0)) && lineFragments[i].trim().contains("/")){
									lf.addXpath(replaceXPathSVG(lineFragments[i])); //workaround patch for Firefox to get the element
								}
							}
				
							lf.setCaptureView();
							wp.Failures.add(lf);

						}
						else	 if(line.contains("ARE OVERLAPPING") && !wp.siteName.equals("Ninite") )
						{
							Failure lf = new Failure();
							lf.type = "collision";
							lf.ID = count;
							count++;
							if(lineFragments[1].trim().contains(lineFragments[3].trim()+"/"))
							{
								lf.addXpath(replaceXPathSVG(lineFragments[3]));
								lf.addXpath(replaceXPathSVG(lineFragments[1]));
							}
							else
							{
								lf.addXpath(replaceXPathSVG(lineFragments[1]));
								lf.addXpath(replaceXPathSVG(lineFragments[3]));
							}
							if(lineFragments[1].trim().contains(lineFragments[3].trim()+"/") || lineFragments[3].trim().contains(lineFragments[1].trim()+"/"))
							{
								lf.HTMLParentRelationship = true;
								lf.titles.add("HTML ancestor-descendant relationship:"); 
								lf.notes.add("Yes");
							}
							lf.viewMin = Integer.parseInt(lineFragments[lineFragments.length-3].trim());
							lf.viewMax = Integer.parseInt(lineFragments[lineFragments.length-1].trim());
							lf.setCaptureView();
							wp.Failures.add(lf);
						}
						else if(line.contains("OVERFLOWED ITS PARENT") && !wp.siteName.equals("Ninite") )
						{
							Failure lf = new Failure();
							lf.type = "protrusion";
							lf.ID = count;
							count++;
							String overflowedElement = lineFragments[0].trim();
							lf.viewMin = Integer.parseInt(lineFragments[lineFragments.length-3].trim());
							lf.viewMax = Integer.parseInt(lineFragments[lineFragments.length-1].trim());
							line = bReader.readLine();
							lineFragments = line.split(",");
							String parent = "";
							if(!lineFragments[0].trim().equals(overflowedElement))
							{
								parent = lineFragments[0].trim();
								lf.addXpath(replaceXPathSVG(lineFragments[0]));
								lf.addXpath(replaceXPathSVG(overflowedElement));
								if(overflowedElement.length() <= lineFragments[1].length() && lineFragments[0].trim().contains(overflowedElement+"/"))
								{
									System.out.println("Input Warning for ID " +lf.ID);
									lf.titles.add("Input Warning:");
									lf.notes.add(overflowedElement + " was reported as overflowing its descendant element in the HTML structure. Will not work using this prototype.");
								}
							}
							else if(!lineFragments[1].trim().equals(overflowedElement) && !wp.siteName.equals("Ninite"))
							{
								parent = lineFragments[1].trim();
								lf.addXpath(replaceXPathSVG(lineFragments[1]));
								lf.addXpath(replaceXPathSVG(overflowedElement));
								if(overflowedElement.length() <= lineFragments[1].length() && lineFragments[1].trim().contains(overflowedElement+"/"))
								{
									System.out.println("Input Warning for ID " +lf.ID);
									lf.titles.add("Input Warning:");
									lf.notes.add(overflowedElement + " was reported as overflowing its descendant element in the HTML structure. Will not work using this prototype.");
								}
							}
							if(overflowedElement.contains(parent.trim()+"/")|| parent.trim().contains(overflowedElement.trim()+"/"))
							{
								lf.HTMLParentRelationship = true;
								lf.titles.add("HTML ancestor-descendant relationship:"); 
								lf.notes.add("Yes");
							}
							lf.setCaptureView();
							wp.Failures.add(lf);
						}
						else if(line.contains("overflowed the viewport"))
						{
							Failure lf = new Failure();
							lf.type = "viewport";
							lf.ID = count;
							count++;
							lf.HTMLParentRelationship = true;
							lf.titles.add("HTML ancestor-descendant relationship:"); 
							lf.notes.add("Yes");
							lf.addXpath("/HTML/BODY");
							lf.addXpath(replaceXPathSVG(lineFragments[0]));
							lf.viewMin = Integer.parseInt(lineFragments[lineFragments.length-3].trim());
							lf.viewMax = Integer.parseInt(lineFragments[lineFragments.length-1].trim());
							lf.setCaptureView();
							wp.Failures.add(lf);
						}
						/**/
					}
					
					bReader.close();
				} catch (IOException e) 
				{
					System.out.println("problem reading input file ("+ f.getPath()+") exiting..");
					e.printStackTrace();
					System.exit(0);
				}
				webpages.add(wp);
			}
		}
	}
	public void addToSiteCaptureList(Webpage wp, Failure lf) {
		if(wp.viewports.contains(lf.captureView)) {
			int imageIndex = wp.viewports.indexOf(lf.captureView);
			wp.pendingFailures.get(imageIndex).add(lf);
		}else{
			wp.viewports.add(lf.captureView);
			ArrayList<Failure> list = new ArrayList<Failure>();
			list.add(lf);
			wp.pendingFailures.add(list);
			wp.captured.add(false);
		}
	}
//	public void parseInputFiles()
//	{
//		int count = 1;
//		for(File f: inputFiles)
//		{
//			Webpage wp = new Webpage();
//			int from = f.getParentFile().getParentFile().getPath().lastIndexOf(File.separator);
//			wp.siteName = f.getParentFile().getParentFile().getPath().substring(from+1);
//			from = f.getParentFile().getPath().lastIndexOf(File.separator);
//			wp.uniqueRunName = f.getParentFile().getPath().substring(from+1);
//			//from = f.getPath().lastIndexOf(File.separator);
//			wp.wPageName = "index.html";
//			wp.wPagePath = localSitesDirectory+File.separator + wp.siteName + File.separator + wp.wPageName;
//			try 
//			{
//				String line;
//				BufferedReader bReader = new BufferedReader(new FileReader(f));
//				while((line = bReader.readLine()) != null)
//				{
//					Failure lf = new Failure();
//					if(line.toLowerCase().equals("wrapping"))
//					{
//						lf.type = "wrapping";
//						lf.ID = 0;
//						lf.addXpath(replaceXPathSVG(bReader.readLine()));
//						int pathCount = Integer.parseInt(bReader.readLine().trim());
//						for(int i=0; i < pathCount; i++)
//						{
//							lf.addXpath(replaceXPathSVG(bReader.readLine()));
//						}
//						lf.viewMin = Integer.parseInt(bReader.readLine().trim());
//						lf.viewMax = Integer.parseInt(bReader.readLine().trim());
//						lf.setCaptureView();
//					}
//					else if(line.toLowerCase().equals("viewport"))
//					{
//						lf.type = "viewport";
//						lf.ID = count;
//						count++;
//					}
//					else if(line.toLowerCase().equals("collision"))
//					{
//						lf.type = "collision";
//						lf.ID = count;
//						count++;
//					}
//					else if(line.toLowerCase().equals("protrusion"))
//					{
//						lf.type = "protrusion";
//						lf.ID = count;
//						count++;
//					}
//					else if(line.toLowerCase().equals("small-range"))
//					{
//						lf.type = "small-range";
//						lf.ID = 0;
//					}
//					if(line.toLowerCase().equals("viewport") || line.toLowerCase().equals("collision") || line.toLowerCase().equals("protrusion") || line.toLowerCase().equals("small-range"))
//					{
//						lf.addXpath(replaceXPathSVG(bReader.readLine()));
//						lf.addXpath(replaceXPathSVG(bReader.readLine()));
//						lf.viewMin = Integer.parseInt(bReader.readLine().trim());
//						lf.viewMax = Integer.parseInt(bReader.readLine().trim());
//						lf.setCaptureView();
//					}
//					wp.Failures.add(lf);
//				}
//				bReader.close();
//			} catch (IOException e) 
//			{
//				System.out.println("problem reading input file ("+ f.getPath()+") exiting..");
//				e.printStackTrace();
//				System.exit(0);
//			}
//			webpages.add(wp);
//		}
//	}
	public ArrayList<Webpage> getWebpages() {
		return webpages;
	}
	public void print()
	{
		for(Webpage wp : webpages )
		{
			System.out.println(wp);
		}
	}
	
}
