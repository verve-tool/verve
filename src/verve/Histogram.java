package verve;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Histogram {
	BufferedImage image;
	int sampling = 1;
	//ArrayList<ColorBin> colorBins;
	HashMap<Integer, Float> colorBins; //Key is RGB
	int startW = 0;
	int endW;
	int startH = 0;
	int endH;
	int totalPixels = 0;
	float totalDifference = 0;
	public Histogram(BufferedImage image) {
		this.image = image;
		if(image != null) {
		endW = image.getWidth();
		endH = image.getHeight();
		//sampling = (int) (0.25*image.getTileWidth());
		}
	}
	public void addPixelColor(int RGB){
		Float value = colorBins.get(RGB);
		if(value == null) {
			colorBins.put(RGB, (float) 1);
		}else {
			colorBins.put(RGB, value++);
		}
	}
	public void extractHistogram() {
		totalPixels = 0;
		colorBins = new HashMap<Integer, Float>();
		for(int w = startW; w < endW; w+=sampling) {
			for(int h = startH; h< endH; h+=sampling) {
				addPixelColor(image.getRGB(w, h));
				totalPixels++;
			}
		}
	}
	public Histogram difference(Histogram other) {
		Histogram differences = new Histogram(null);
		differences.colorBins = new HashMap<Integer, Float>();
		for(Map.Entry<Integer, Float> entry : colorBins.entrySet()) {
			Float otherRGBCount = other.colorBins.get(entry.getKey());
			if(otherRGBCount == null) {
				differences.colorBins.put(entry.getKey(), entry.getValue().floatValue()/totalPixels);
				differences.totalDifference = differences.totalDifference + entry.getValue().floatValue()/totalPixels;
			}else {
				int diff = Math.abs(otherRGBCount.intValue() - entry.getValue().intValue());
				if(diff != 0) {
					differences.colorBins.put(entry.getKey(), (diff/Math.max(entry.getValue().floatValue(), otherRGBCount.floatValue())));
					differences.totalDifference = differences.totalDifference + (diff/Math.max(entry.getValue().floatValue(), otherRGBCount.floatValue()));
				}
			}
		}
		for(Map.Entry<Integer, Float> entry : other.colorBins.entrySet()) {
			Float thisRGBCount = colorBins.get(entry.getKey());
			if(thisRGBCount == null) {
				differences.colorBins.put(entry.getKey(), entry.getValue().floatValue()/other.totalPixels);
				differences.totalDifference = differences.totalDifference + (entry.getValue().floatValue()/other.totalPixels);
			}
		}
		return differences;
	}
	
}
