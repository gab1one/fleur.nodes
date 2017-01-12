package tests.java.inflor.integration;

import java.util.ArrayList;

import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.fcs.FCSFileReader;
import main.java.inflor.core.gates.RangeGate;
import main.java.inflor.core.gates.RectangleGate;

public class RangeGateCalculation {
  static final int numFiles = 1;
  ArrayList<FCSFrame> dataSet = new ArrayList<FCSFrame>();

  public static void main(String[] args) throws Exception {
    String bigPath = "src/io/landysh/inflor/tests/extData/20mbFCS3.fcs";

    RangeGate rangeGate = new RangeGate("Foo", new String[] {"FSC-A", "SSC-A"},
        new double[] {40, 000, 60, 000}, new double[] {5000, 10000});

    FCSFrame data = FCSFileReader.read(bigPath);
    // gate.evaluateParallel(data);
    long start = System.currentTimeMillis();
    for (int i = 0; i < numFiles; i++) {
      rangeGate.evaluate(data);
    }
    long end = System.currentTimeMillis();
    System.out.println("Millis: " + (end - start));

    RectangleGate rectGate = new RectangleGate("", "FSC-A", 40000, 60000, "SSC-A", 5000, 10000);
    start = System.currentTimeMillis();
    for (int i = 0; i < numFiles; i++) {
      rectGate.evaluate(data);
    }
    end = System.currentTimeMillis();
    System.out.println("Millis rectangle: " + (end - start));
  }
}