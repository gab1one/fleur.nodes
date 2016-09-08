package io.landysh.inflor.java.core.plots;

import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import io.landysh.inflor.java.core.dataStructures.ColumnStore;
import io.landysh.inflor.java.core.utils.AbstractDisplayTransform;
import io.landysh.inflor.java.core.utils.FCSUtils;

public class FCSPlotFactory {
	private static final int xBins = 512;
	private static final int yBins = 512;
	private PlotSpec plotSpec;
	private XYDataset dataset;

	public FCSPlotFactory(){
		
	}
	
	public FCSPlotFactory(PlotSpec spec){
		this.setPlotSpec(spec);
	}
	
	public FCSPlotFactory(PlotSpec spec, XYDataset dataset){
		this.setPlotSpec(spec);
	}


	public PlotSpec getPlotSpec() {
		return plotSpec;
	}

	public void setPlotSpec(PlotSpec plotSpec) {
		this.plotSpec = plotSpec;
	}
	
	public void setData(ColumnStore columnData) {
		DefaultXYZDataset dataset = new DefaultXYZDataset();
	    double[] domainData = columnData.getColumn(plotSpec.getDomainAxisName());
	    double[] rangeData =  columnData.getColumn(plotSpec.getRangeAxisName());
	    
	    double[] transformedDomainData = plotSpec.getDomainTransform().transform(rangeData); 

	    this.dataset = dataset;
	}
}