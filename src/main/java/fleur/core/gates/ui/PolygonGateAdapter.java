/*
 * ------------------------------------------------------------------------
 *  Copyright 2016 by Aaron Hart
 *  Email: Aaron.Hart@gmail.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 * ---------------------------------------------------------------------
 *
 * Created on December 14, 2016 by Aaron Hart
 */
package fleur.core.gates.ui;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import org.jfree.chart.annotations.XYLineAnnotation;

import fleur.core.plots.FCSChartPanel;
import fleur.core.ui.GateNameEditor;
import fleur.core.ui.LookAndFeel;
import fleur.core.utils.ChartUtils;

public class PolygonGateAdapter extends MouseInputAdapter {
  private FCSChartPanel panel;
  private ArrayList<Point2D> vertices = new ArrayList<>();
  private ArrayList<XYLineAnnotation> segments;
  private Point2D anchorPoint;
  private XYLineAnnotation anchorSegment;

  public PolygonGateAdapter(FCSChartPanel panel) {
    this.panel = panel;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    Point2D v = ChartUtils.getPlotCoordinates(e, panel);
    if (SwingUtilities.isLeftMouseButton(e)) {
      // add the next segment
      anchorPoint = v;
      vertices.add(v);
      updateTemporaryAnnotation();
    }
    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
      XYLineAnnotation closingSegment = new XYLineAnnotation(anchorPoint.getX(), anchorPoint.getY(),
          vertices.get(0).getX(), vertices.get(0).getY());
      segments.add(closingSegment);
      panel.addTemporaryAnnotation(closingSegment);

      // Finish the polygon and ask for a name
      int pointCount = vertices.size() * 2;
      double[] polygon = new double[pointCount];
      for (int i = 0; i < pointCount; i++) {
        polygon[i] = vertices.get(i / 2).getX();
        polygon[i + 1] = vertices.get(i / 2).getY();
        i++;//Sonar warning but wont fix, I think.
      }
      // Pop a gate editor dialog
      GateNameEditor dialog = new GateNameEditor();
      dialog.setVisible(true);
      // On Close...
      if (dialog.isOK()) {
        PolygonGateAnnotation finalPolygon = new PolygonGateAnnotation(dialog.getGateName(),
            panel.getDomainAxisName(), panel.getRangeAxisName(), polygon,
            LookAndFeel.DEFAULT_STROKE, LookAndFeel.DEFAULT_GATE_COLOR);
        panel.createGateAnnotation(finalPolygon);
      }
      dialog.dispose();

      // remove the anchor point && cleanup segments.
      vertices.clear();
      panel.removeTemporaryAnnotation(anchorSegment);
      anchorSegment = null;
      anchorPoint = null;
      segments.forEach(panel::removeTemporaryAnnotation);//TODO:if this is broken, maybe sonar was wrong?
      segments = null;
      panel.activateGateSelectButton();
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if (anchorSegment != null) {
      panel.removeTemporaryAnnotation(anchorSegment);
    }
    if (anchorPoint != null) {
      Point2D p = ChartUtils.getPlotCoordinates(e, panel);
      anchorSegment =
          new XYLineAnnotation(anchorPoint.getX(), anchorPoint.getY(), p.getX(), p.getY());
      panel.addTemporaryAnnotation(anchorSegment);
    }
  }

  private void updateTemporaryAnnotation() {
    Point2D previousVertex = null;
    if (segments != null) {
      segments.stream().forEach(panel::removeTemporaryAnnotation);
    }
    segments = new ArrayList<>();
    for (Point2D v : vertices) {
      if (previousVertex == null) {
        previousVertex = v;
      } else {
        segments.add(
            new XYLineAnnotation(previousVertex.getX(), previousVertex.getY(), v.getX(), v.getY()));
        previousVertex = v;
      }
    }
    segments.stream().forEach(panel::addTemporaryAnnotation);
  }
}
