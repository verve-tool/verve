package verve;

public class HistogramDifference {
	int viewport1;
	int viewport2;
	Histogram difference;
	public HistogramDifference(int viewport1, int viewport2) {
		this.viewport1 = Math.min(viewport1, viewport2);
		this.viewport2 = Math.max(viewport1, viewport2);
	}
	public HistogramDifference(int viewport1, int viewport2, Histogram diff) {
		this.viewport1 = Math.min(viewport1, viewport2);
		this.viewport2 = Math.max(viewport1, viewport2);
		this.difference = diff;
	}
	public boolean isFor(int viewport1, int viewport2) {
		return (this.viewport1 == Math.min(viewport1, viewport2) && (this.viewport2 == Math.max(viewport1, viewport2)));
	}

}
