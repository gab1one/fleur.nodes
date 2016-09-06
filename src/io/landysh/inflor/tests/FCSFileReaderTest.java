package io.landysh.inflor.tests;

import static org.junit.Assert.assertEquals;

import java.util.Hashtable;

import org.junit.Test;

import io.landysh.inflor.java.core.dataStructures.FCSVector;
import io.landysh.inflor.java.core.fcs.FCSFileReader;

public class FCSFileReaderTest {
	// Define Constants

	String path1 = "src/io/landysh/inflor/tests/extData/int-15_scatter_events.fcs";

	@Test
	public void testInitialization() throws Exception {
		// Setup
		final FCSFileReader r = new FCSFileReader(path1, false);

		// Test

		// Assert
		assertEquals(r.pathToFile, path1);
		assertEquals(r.beginText, (Integer) 256);
		assertEquals(r.endText, (Integer) 511);
		assertEquals(r.beginData, (Integer) 576);
		assertEquals(r.dataType, "I");
		// assertEquals( r.bitMap, new Integer[] {16,16});
		System.out.println("FCSFileReaderTest testInitialization completed (succefully or otherwise)");

	}

	@Test
	public void testReadAllData() throws Exception {
		// Setup
		final FCSFileReader r = new FCSFileReader(path1, false);
		r.readData();

		// Test
		final Hashtable<String, FCSVector> testData = r.getColumnStore().getData();

		final double[] fcs = { 400, 600, 300, 500, 600, 500, 800, 200, 300, 800, 900, 400, 200, 600, 400 };
		final double[] ssc = { 300, 300, 600, 200, 800, 500, 600, 400, 100, 200, 400, 800, 900, 700, 500 };
		
		double[] testFCS = testData.get("FCS").getData();
		double[] testSSC = testData.get("SSC").getData();

		// Assert
		for (int i = 0; i < fcs.length; i++) {
			assertEquals(fcs[i], testFCS[i], Double.MIN_VALUE);
			assertEquals(ssc[i], testSSC[i], Double.MIN_VALUE);
		}
		System.out.println("FCSFileReaderTest testReadAllData completed (succefully or otherwise)");
	}
}