package verve;

import java.util.ArrayList;

public class Results {
	ArrayList<String> methods;
	ArrayList<Double> base;
	ArrayList<Double> minOracle;
	ArrayList<Double> maxOracle;

	Results(){
		methods = new ArrayList<String>();
		base = new ArrayList<Double>();
		minOracle = new ArrayList<Double>();
		maxOracle = new ArrayList<Double>();
	}
}
