package verve;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Rectangle;


/**
 * @author ibrahim
 *
 */
public class htmlout {
	int count = 0;
	String html ="";
	String infoTemplateS = "<!DOCTYPE html>\r\n" + 
			"<html>\r\n" + 
			"<head>\r\n" + 
			"<style> \r\n" + 
			"p {\r\n" + 
			"	padding: 0px;\r\n" + 
			"    margin: 0px;\r\n" + 
			"}"+
			"section {\r\n" + 
			"    border: 2px solid;\r\n" + 
			"    padding: 2px; \r\n" + 
			"    width: 100%;\r\n" + 
			"    background-color: lightgreen;\r\n" +  
			"}\r\n" + 
			"</style>\r\n" + 
			"</head>\r\n" + 
			"<body>\r\n" + 
			"\r\n";
	String infoTemplateE =  
			"</body>\r\n" + 
					"</html>\r\n" ;
	String lineS = "<p>";
	String lineE = "</p>\r\n";
	String boldS = "<b>";
	String boldE = "</b>";
	String mainThreeTemplate = "<!DOCTYPE html>\r\n" + 
			"<html>\r\n" + 
			"<frameset rows=\"85%,15%\">\r\n" + 
			"<frameset cols=\"33%,33%,33%\">\r\n" + 
			"<frame src=\"$left\" name=\"left\">\r\n" + 
			"<frame src=\"$middle\" name=\"middle\">\r\n" + 
			"<frame src=\"$right\" name=\"right\">\r\n" + 
			"</frameset>\r\n" + 
			"<frame src=\"$top\" name=\"top\">\r\n" + 
			"</frameset>\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"</html>";
	String mainTwoTemplate = "<!DOCTYPE html>\r\n" + 
			"<html>\r\n" + 
			"<frameset rows=\"85%,15%\">\r\n" + 
			"<frameset cols=\"50%,50%\">\r\n" + 
			"<frame src=\"$left\" name=\"left\">\r\n" + 
			"<frame src=\"$right\" name=\"right\">\r\n" + 
			"</frameset>\r\n" + 
			"<frame src=\"$top\" name=\"top\">\r\n" + 
			"</frameset>\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"</html>";
	String mainSingleTemplate = "<!DOCTYPE html>\r\n" + 
			"<html>\r\n" + 
			"<frameset rows=\"36%,64%\">\r\n" + 
			"<frame src=\"$bottom\" name=\"bottom\">\r\n" + 
			"<frame src=\"$top\" name=\"top\">\r\n" + 
			"</frameset>\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"</html>";
	String mainSingleInsideTemplate = "<!DOCTYPE html>\r\n" + 
			"<html>\r\n" + 
			"<frameset rows=\"85%,15%\">\r\n" + 
			"<frame src=\"$bottom\" name=\"bottom\">\r\n" + 
			"<frame src=\"$top\" name=\"top\">\r\n" + 
			"</frameset>\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"</html>";
	String gallaryhtml = "<!DOCTYPE html>\r\n" + 
			"<html>\r\n" + 
			"<head>\r\n" + 
			"<style>\r\n" + 
			"p {\r\n" + 
			"	padding: 0px;\r\n" + 
			"    margin: 0px;\r\n" + 
			"}"+
			"div.gallery {\r\n" + 
			"    margin: 5px;\r\n" + 
			"    border: 1px solid #ccc;\r\n" + 
			"    float: left;\r\n" + 
			"    width: 280px;\r\n" + 
			"}\r\n" + 
			"\r\n" + 
			"div.gallery:hover {\r\n" + 
			"    border: 1px solid #777;\r\n" + 
			"}\r\n" + 
			"\r\n" + 
			"div.gallery img {\r\n" + 
			"    max-width: 100%;\r\n" + 
			"    max-height: 100%;\r\n" + 
			"}\r\n" + 
			"\r\n" + 
			"div.desc {\r\n" + 
			"    padding: 15px;\r\n" + 
			"    text-align: center;\r\n" + 
			"}\r\n" + 
			"</style>\r\n" + 
			"</head>\r\n" + 
			"<body>\r\n" + 
			"\r\n" + 
			"$gallary\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"</body>\r\n" + 
			"</html>";
	String gallary = "\r\n" + 
			"<div class=\"gallery\">\r\n" + 
			"  <a target=\"top\" href=\"$site\">\r\n" + 
			"    <img src=\"$pic\" alt=\"$pic\" width=\"300\" height=\"200\">\r\n" + 
			"  </a>\r\n" + 
			"  <div class=\"desc\">$description</div>\r\n" + 
			"</div>";
	public void newHTML()
	{
		html = infoTemplateS;
	}
	public String outputHTML() 
	{
		return html + infoTemplateS;
	}
	public String bold(String line)
	{
		return boldS + line + boldE;
	}
	public String paragraph(String line)
	{
		return lineS + line + lineE;
	}
	public String section(String line)
	{
		return "<section>" + line + "</section>";
	}

	public String image(String line)
	{
		return "<img src=\"" + line + "\">"; 
	}
	public String heading(String line)
	{
		return "<h>" + line + "</h>";
	}
	public void addToHtml(String code)
	{
		html = html + code;
	}
	public String writeFinalHTML(String name,String code)
	{
		String fileName = name + "_" + count + ".html";
		count++;
		File newHtmlFile = new File(Assist.outDirectory  + File.separator  + fileName);
		try {
			FileUtils.writeStringToFile(newHtmlFile, code);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileName;
	}

	public String gallary(ArrayList<Webpage> webPages)
	{
		String site = "" + mainSingleTemplate;
		String first = "";
		for(Webpage wp : webPages)
		{
			for(Failure lf : wp.Failures)
			{
				String gal = "" + gallary;
				String result;
				if(lf.type.equals("small-range") ) //small-range has only one picture per viewport.
				{
					gal = gal.replace("$pic",  "ID_" + lf.ID + "_" + wp.siteName + "_" + lf.type + "_" + lf.viewMin + "_to_" + lf.viewMax + "_capture_"+ lf.captureView  +".png");
				}
				if(lf.ignored)
				{
					result = "Ignored";
					gal = gal.replace("$pic", "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_ignored_0.png");
				}
				else if(lf.falsePositive)
				{
					result = "False Positive";
					gal = gal.replace("$pic",  "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_FP_0.png");
				}
				else if(lf.NOI)
				{
					result = "NOI";
					gal = gal.replace("$pic",  "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_NOI_0.png");
				}
				else
				{
					result = "True Positive";
					gal = gal.replace("$pic",  "ID_" + lf.ID + "_" + lf.type+ "_" + wp.siteName + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_TP_0.png");
				}
				if(lf.bestEffort == true)
				{
					result = "Best Effort(" + result + ")";
					if(!lf.priorCat.equals(""))
					{
						result = result + "R";
					}
				}
				if(first.equals(""))
				{
					first = makeWebpage(lf, wp.siteName, result);
					gal = gal.replace("$site", first);
				}
				else
				{
					gal = gal.replace("$site", makeWebpage(lf, wp.siteName,result));
				}
				String desc = 
						paragraph(bold("Site   : ")+ wp.siteName)
						+ paragraph(bold("ID     : ")+ lf.ID)
						+ paragraph(bold("Type   : ")+ lf.type)
						+ paragraph(bold("Capture: ")+ Integer.toString(lf.captureView))
						+ paragraph(bold("Range  : ")+ Integer.toString(lf.viewMin) + " - " +Integer.toString(lf.viewMax)) 
						+ paragraph(bold("Result : ")+ result);
				gal = gal.replace("$description", desc);
				gallaryhtml = gallaryhtml.replace("$gallary\r\n", "$gallary\r\n " + gal);

			}
		}

		gallaryhtml = gallaryhtml.replace("$gallary", " ");
		String name = writeFinalHTML("gallary", gallaryhtml);
		site = site.replace("$bottom", name);
		site = site.replace("$top", first);
		return writeFinalHTML("index", site);
	}
	public String makeWebpage(Failure lf, String site,String result)
	{
		String desc = 
				paragraph(bold("Site   : ")+ site) 
				+ paragraph(bold("ID     : ")+ lf.ID)
				+ paragraph(bold("Type   : ")+ lf.type)
				+ paragraph(bold("Capture: ")+ Integer.toString(lf.captureView))
				+ paragraph(bold("Range  : ")+ Integer.toString(lf.viewMin) + " - " +Integer.toString(lf.viewMax)) 
				+ paragraph(bold("Result : ")+ result );
		if(lf.type.equals("small-range")) {
			desc = desc + paragraph(bold("Classification Method: ")+lf.classificationMethod);
			desc = desc + paragraph(bold("Intersection Threshold: ")+Assist.intThreshold + " (" + lf.intClassification + ")");
			desc = desc + paragraph(bold("Kullback-Leibler-Divergence Threshold: ")+Assist.kldThreshold + " (" + lf.kldClassification + ")");
			desc = desc + paragraph(bold("Altenative Chi-Square Threshold: ")+Assist.altThreshold + " (" + lf.altClassification+ ")");
			desc = desc + paragraph(bold("Chi-Square Threshold: ")+Assist.chiThreshold + " (" + lf.chiClassification + ")");
			desc = desc + paragraph(bold("Bhattacharyya-Distance Threshold: ")+Assist.bhaThreshold + " (" + lf.bhaClassification + ")");
			desc = desc + paragraph(bold("Correlation Threshold: ")+Assist.corThreshold + " (" + lf.corClassification + ")");

		}
		//		if(lf.type.equals("small-range")) {
		//		}
		for(String xpath:lf.xpaths)
			desc = desc + paragraph(bold("XPATH : ")+xpath);



		if(lf.type.equals("small-range")) {
			desc = desc 
					+ paragraph(bold("Prev : ")+ lf.smallrangePrevConstraints) 
					+ paragraph(bold("This : ")+ lf.viewMin +"-" + lf.viewMax + " " + lf.smallrangeThisConstraints) 
					+ paragraph(bold("Next : ")+ lf.smallrangeNextConstraints); 

		}



		String siteInside;
		siteInside = "" + mainTwoTemplate;
		if(lf.type.equals("small-range")) {
			siteInside = "" + mainThreeTemplate;
		}
		newHTML();
		addToHtml(section(desc));
		for(int i = 0; i < lf.notes.size(); i++)
		{
			String note = paragraph(bold(lf.titles.get(i))) + paragraph(lf.notes.get(i));
			addToHtml(section(note));
		}
		if(lf.type.equals("small-range")) {
			if(lf.minOracle.orignalRectangles.size() > 0) {
				addToHtml(section(paragraph(bold("Prev (below min) Rectangle Information..")))); 
			}
			for(int i = 0; i < lf.minOracle.notes.size(); i++)
			{
				String note = paragraph(bold(lf.minOracle.titles.get(i))) + paragraph(lf.minOracle.notes.get(i));
				addToHtml(section(note));
			}
		}
		if(lf.type.equals("small-range")) {
			if(lf.maxOracle.orignalRectangles.size() > 0) {
				addToHtml(section(paragraph(bold("Next (above max) Rectangle Information..")))); 
			}
			for(int i = 0; i < lf.maxOracle.notes.size(); i++)
			{
				String note = paragraph(bold(lf.maxOracle.titles.get(i))) + paragraph(lf.maxOracle.notes.get(i));
				addToHtml(section(note));
			}
			//			if(lf.prevDiff != null) {
			//				//			String diffHtml = paragraph(bold("Histogram Difference Prev..."));
			//				//			for(Map.Entry<Integer, Float> entry : lf.prevDiff.colorBins.entrySet()) {
			//				//				double percent = entry.getValue()*100;
			//				//				totalPercent = totalPercent + percent;
			//				////				if(percent > 1) {
			//				////					diffHtml = diffHtml + paragraph(bold(entry.getKey().toString() + " : ")+ "&#37;"+percent);
			//				////				}
			//				//			}
			//				String diffHtml = paragraph(bold("Total Difference to Prev ("+lf.minOracle.captureView+"): ") + "&#37;"+ lf.prevDiff.totalDifference );
			//				addToHtml(section(diffHtml));
			//			}
			//			if(lf.nextDiff != null) {
			//				//			String diffHtml = paragraph(bold("Histogram Difference Next..."));
			//
			//				//			for(Map.Entry<Integer, Float> entry : lf.nextDiff.colorBins.entrySet()) {
			//				//				double percent = entry.getValue()*100;
			//				//				totalPercent = totalPercent + percent;
			//				////				if(percent > 1) {
			//				////				diffHtml = diffHtml + paragraph(bold(entry.getKey().toString() + " : ")+ "&#37;"+percent);
			//				////				}
			//				//			}
			//				String diffHtml = paragraph(bold("Total Difference to Next ("+lf.maxOracle.captureView+"): ") + "&#37;"+ lf.nextDiff.totalDifference );
			//				addToHtml(section(diffHtml));
			//			}
		}
		String images = "";
		if(lf.protruding)
		{
			images = images + paragraph("Protruding..");
			for(int x = 1; x <= lf.protrudingArea.size(); x++)
				for(TargetArea TA : lf.protrudingArea.get(x-1).targetAreas) 
					for (int i=1; i <= TA.targetImgs.size(); i++)
						images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "protruding_" + x + "_TargetArea_"+ i +".png"));
		}
		if(lf.overlapping)
		{
			images = images + paragraph("Overlapping..");
			for(TargetArea TA : lf.overlappedArea.targetAreas)
				for (int i=1; i <= TA.targetImgs.size(); i++)
					images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "overlapping_"+ i +".png"));

		}
		if(lf.segregated)
		{
			images = images + paragraph("Segregated..");
			for(TargetArea TA : lf.segregatedArea.targetAreas)
				for (int i=1; i <= TA.targetImgs.size(); i++)
					images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "segregated_" + i +".png"));
		}
		if(lf.wrapped)
		{
			images = images + paragraph("wrapped..");
			for(TargetArea TA : lf.wrappedArea.targetAreas)
				for (int i=1; i <= TA.targetImgs.size(); i++)
					images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "wrapped_" + i +".png"));
		}
		//		if(lf.type.equals("wrapping") || lf.type.equals("small-range"))
		//		{
		//			images = images + paragraph("aoc..");
		//			for(TargetArea TA : lf.maxAOC.targetAreas)
		//				for (int i=0; i < TA.targetImgs.size(); i++)
		//					images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + lf.type +"_" + "_capture_" +lf.captureView + "_" +i +".png"));
		//			images = images + paragraph("minimum oracle aoc..");
		//			for(TargetArea TA : lf.minOracle.maxAOC.targetAreas)
		//				for (int i=0; i < TA.targetImgs.size(); i++)
		//					images = images + paragraph(image("ID_" + lf.minOracle.ID + "_" + lf.minOracle.type + "_" + site + "_"+ lf.minOracle.viewMin + "_" + lf.minOracle.viewMax + "_" + lf.minOracle.type +"_" + "_capture_" +lf.minOracle.captureView + "_" +i +".png"));
		//			images = images + paragraph("maximum oracle aoc..");
		//			for(TargetArea TA : lf.maxOracle.maxAOC.targetAreas)
		//				for (int i=0; i < TA.targetImgs.size(); i++)
		//					images = images + paragraph(image("ID_" + lf.maxOracle.ID + "_" + lf.maxOracle.type + "_" + site + "_"+ lf.maxOracle.viewMin + "_" + lf.maxOracle.viewMax + "_" + lf.maxOracle.type +"_" + "_capture_" +lf.maxOracle.captureView + "_" +i +".png"));
		//			
		//		}
		if(lf.type.equals("small-range"))
		{
			for (int i=1; i < lf.imgs.size(); i++) {
				images = images + paragraph("This Failure aoc..");
				images = images + paragraph(image("Compare_ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_capture_" + lf.captureView + "_" +i +".png"));
				images = images + paragraph("Previous oracle aoc..");
				images = images + paragraph(image("Compare_ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_capture_" + lf.minOracle.captureView + "_" +i +".png"));
				images = images + paragraph("Next oracle aoc..");
				images = images + paragraph(image("Compare_ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_capture_" + lf.maxOracle.captureView + "_" +i +".png"));
				images = images + paragraph("-------------------------------------");
				for(int x = 0; x<lf.histogramResults.get(0).methods.size(); x++) {
					images = images + paragraph(lf.histogramResults.get(0).methods.get(x) + " Perfect Compare Result: "+ Assist.decimalFormat.format(lf.histogramResults.get(i-1).base.get(x)));
					images = images + paragraph(lf.histogramResults.get(0).methods.get(x) + " Previous Compare Result: "+	Assist.decimalFormat.format(lf.histogramResults.get(i-1).minOracle.get(x)))
					+ paragraph(lf.histogramResults.get(0).methods.get(x) + " Next Compare Result: "+	Assist.decimalFormat.format(lf.histogramResults.get(i-1).maxOracle.get(x)));
					images = images + paragraph("-------------------------------------");
				}
			}
		}

		if(lf.printFlagT)
		{
			images = images + paragraph("Final reach top..");
			images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "Recat_Side_"+ "T" + 1 +".png"));
			images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "Recat_Side_"+ "T" + 2 +".png"));
		}
		if(lf.printFlagR)
		{
			images = images + paragraph("Final reach right..");
			images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "Recat_Side_"+ "R" + 1 +".png"));
			images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "Recat_Side_"+ "R" + 2 +".png"));
		}
		if(lf.printFlagL)
		{
			images = images + paragraph("Final reach left..");
			images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "Recat_Side_"+ "L" + 1 +".png"));
			images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "Recat_Side_"+ "L" + 2 +".png"));
		}
		if(lf.printFlagB)
		{
			images = images + paragraph("Final reach bottom..");
			images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "Recat_Side_"+ "B" + 1 +".png"));
			images = images + paragraph(image("ID_" + lf.ID + "_" + lf.type + "_" + site + "_"+ lf.viewMin + "_" + lf.viewMax + "_" + "Recat_Side_"+ "B" + 2 +".png"));
		}
		String allImages = paragraph(bold("Detailed Images:")) + images;
		addToHtml(section(allImages));
		String name = writeFinalHTML(lf.type, outputHTML());
		siteInside = siteInside.replace("$top", name);
		String category = "";
		if(lf.ignored)
		{
			category = "ignored";
		}
		else if(lf.falsePositive)
		{
			category = "FP";
		}
		else if(lf.NOI)
		{
			category = "NOI";
		}
		else
		{
			category = "TP";
		}
		if(lf.type.equals("small-range")) {
			siteInside = siteInside.replace("$left",  "ID_" + lf.ID + "_" + site + "_" + lf.type + "_" + lf.viewMin + "_to_" + lf.viewMax + "_capture_" + lf.minOracle.captureView  +".png");
			siteInside = siteInside.replace("$middle",  "ID_" + lf.ID + "_" + site + "_" + lf.type + "_" + lf.viewMin + "_to_" + lf.viewMax + "_capture_"+ lf.captureView  +".png");
			siteInside = siteInside.replace("$right",  "ID_" + lf.ID + "_" + site + "_" + lf.type + "_" + lf.viewMin + "_to_" + lf.viewMax + "_capture_" + lf.maxOracle.captureView  +".png");

		}
		//		else if(lf.type.equals("wrapping")) {
		//			siteInside = siteInside.replace("$left",  "ID_" + lf.ID + "_" + site + "_" + lf.type + "_" + lf.viewMin + "_to_" + lf.viewMax + "_capture_"+ lf.captureView  +".png");
		//			siteInside = siteInside.replace("$right",  "ID_" + lf.ID + "_" + site + "_" + lf.type + "_" + lf.viewMin + "_to_" + lf.viewMax + "_capture_" + lf.maxOracle.captureView  +".png");
		//		}
		else {
			siteInside = siteInside.replace("$left",  "ID_" + lf.ID + "_" + lf.type+ "_" + site + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_"+ category +"_0.png");
			siteInside = siteInside.replace("$right",  "ID_" + lf.ID + "_" + lf.type+ "_" + site + "_" + lf.viewMin + "px_"+ lf.viewMax + "px_"+ category +"_1.png");
		}

		name = writeFinalHTML(site, siteInside);
		return name;
	}

}
