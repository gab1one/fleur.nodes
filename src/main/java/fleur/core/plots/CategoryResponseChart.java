/*
 * ------------------------------------------------------------------------ Copyright 2016 by Aaron
 * Hart Email: Aaron.Hart@gmail.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License, Version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, see <http://www.gnu.org/licenses>.
 * ---------------------------------------------------------------------
 *
 * Created on December 14, 2016 by Aaron Hart
 */
package fleur.core.plots;

import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.ui.RectangleAnchor;

import com.google.common.primitives.Doubles;

import fleur.core.data.FCSFrame;
import fleur.core.data.Histogram1D;
import fleur.core.transforms.AbstractTransform;
import fleur.core.utils.PlotUtils;

public class CategoryResponseChart {

  private AbstractTransform transform;
  private String axisName;

  public CategoryResponseChart(String domainName, AbstractTransform domainTransform) {
    this.transform = domainTransform;
    this.axisName = domainName;
  }

  public JFreeChart createChart(List<FCSFrame> cytFrames) {

    CategoryXYZDataSet categoryData = new CategoryXYZDataSet();
    double zMin = Double.MAX_VALUE;
    double zMax = 1;
    
    for (int i=0;i<cytFrames.size();i++) {

      double[] tData = transform.transform(cytFrames.get(i).getDimension(axisName).getData());
      Histogram1D hist = new Histogram1D(tData, transform.getMinTranformedValue(), transform.getMaxTransformedValue(), ChartingDefaults.BIN_COUNT);
      
      double[] x = hist.getNonZeroX();
      double[] y = new double[x.length];
      for (int j = 0; j < y.length; j++) {
        y[j] = i;
      }
      double[] z = hist.getNonZeroY();

      double currentZMin = Doubles.min(z);
      double currentZMax = Doubles.max(z);
      categoryData.addCategoricalSeries(cytFrames.get(i).getDisplayName(), x, z);
      if (currentZMin < zMin) {
        zMin = currentZMin;
      } else if (currentZMax > zMax) {
        zMax = currentZMax;
      }
    }

    ValueAxis domainAxis = PlotUtils.createAxis(axisName, transform);
    NumberAxis rangeAxis = new CategoricalNumberAxis("", categoryData.getLabelMap());
    // Renderer configuration
    XYBlockRenderer renderer = new XYBlockRenderer();
    double xWidth = (transform.getMaxTransformedValue() - transform.getMinTranformedValue())
        / ChartingDefaults.BIN_COUNT;
    renderer.setBlockWidth(xWidth);
    renderer.setBlockHeight(0.5);
    renderer.setBlockAnchor(RectangleAnchor.LEFT);

    PaintScale paintScale = PlotUtils.createPaintScale(zMax, ChartingDefaults.DEFAULT_COLOR_SCHEME);
    renderer.setPaintScale(paintScale);

    // Add to panel.
    XYPlot responsePlot = new XYPlot(categoryData, domainAxis, rangeAxis, renderer);
    JFreeChart chart = new JFreeChart(responsePlot);
    chart.removeLegend();
    return chart;
  }
}
