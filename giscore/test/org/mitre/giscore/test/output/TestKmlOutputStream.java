/****************************************************************************************
 *  TestKmlOutputStream.java
 *
 *  Created: Feb 4, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.test.output;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.XmlOutputStreamBase;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.test.TestGISBase;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.net.URI;

/**
 * Test the output stream
 * 
 * @author DRAND
 * 
 */
public class TestKmlOutputStream extends TestGISBase {

    private boolean autoDelete = !Boolean.getBoolean("keepTempFiles");

    @Test
	public void testSimpleCase() throws Exception {
		doTest(getStream("7084.kml"));
	}
	
	/**
	 * Note, this test fails due to some sort of issue with geodesy, but the
	 * actual output kml is fine.
	 * @throws Exception
	 */
	@Test
	public void testCase2() throws Exception {
		doTest(getStream("KML_sample1.kml"));
	}
	
	@Test
	public void testCase3() throws Exception {
		doTest(getStream("schema_example.kml"));
	}

	@Test
	public void testRingOutput() throws Exception {
		File file = createTemp("testRingz", ".kml");
		OutputStream fs = null;
		try {
			fs = new FileOutputStream(file);
			IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fs);
			os.write(new DocumentStart(DocumentType.KML));
			os.write(new ContainerStart(IKml.DOCUMENT));
			//Feature firstFeature = null;
			for (int i = 0; i < 5; i++) {
				Point cp = getRandomPoint();
				Feature f = new Feature();
				//if (firstFeature == null) firstFeature = f;
				List<Point> pts = new ArrayList<Point>();
				pts.add(getRingPoint(cp, 0, 5, .3, .4));
				pts.add(getRingPoint(cp, 1, 5, .3, .4));
				pts.add(getRingPoint(cp, 2, 5, .3, .4));
				pts.add(getRingPoint(cp, 3, 5, .3, .4));
				pts.add(getRingPoint(cp, 4, 5, .3, .4));
				pts.add(pts.get(0));
				LinearRing ring = new LinearRing(pts);
				f.setGeometry(ring);
				os.write(f);
			}
			os.close();

			/*
			KmlReader reader = new KmlReader(file);
            List<IGISObject> objs = reader.readAll();
            // imported features should be DocumentStart, Container, followed by Features
            assertEquals(8, objs.size());
            checkApproximatelyEquals(firstFeature, objs.get(2));
            */
		} finally {
			IOUtils.closeQuietly(fs);
			if (autoDelete && file.exists()) file.delete();
		}
	}

	@Test
	public void testPolyOutput() throws Exception {
		File file = createTemp("testPolys", ".kml");
		OutputStream fs = null;
		try {
			fs = new FileOutputStream(file);
			IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fs);
			os.write(new DocumentStart(DocumentType.KML));
			os.write(new ContainerStart(IKml.DOCUMENT));
			Schema schema = new Schema();
			SimpleField id = new SimpleField("testid");
			id.setLength(10);
			schema.put(id);
			SimpleField date = new SimpleField("today", SimpleField.Type.STRING);
			schema.put(date);
			os.write(schema);
			Feature firstFeature = null;
			for (int i = 0; i < 5; i++) {
				Point cp = getRandomPoint(25.0); // Center of outer poly
				Feature f = new Feature();
				if (firstFeature == null) firstFeature = f;
				f.putData(id, "id " + i);
				f.putData(date, new Date().toString());
				f.setSchema(schema.getId());
				List<Point> pts = new ArrayList<Point>();
				for (int k = 0; k < 5; k++) {
					pts.add(getRingPoint(cp, k, 5, 1.0, 2.0));
				}
				pts.add(pts.get(0));
				LinearRing outerRing = new LinearRing(pts);
				List<LinearRing> innerRings = new ArrayList<LinearRing>();
				for (int j = 0; j < 4; j++) {
					pts = new ArrayList<Point>();
					Point ircp = getRingPoint(cp, j, 4, .5, 1.0);
					for (int k = 0; k < 5; k++) {
						pts.add(getRingPoint(ircp, k, 5, .24, .2));
					}
					pts.add(pts.get(0));
					innerRings.add(new LinearRing(pts));
				}
				Polygon p = new Polygon(outerRing, innerRings);
				f.setGeometry(p);
				os.write(f);
			}
			os.close();

			KmlReader reader = new KmlReader(file);
            List<IGISObject> objs = reader.readAll();
            // imported features should be DocumentStart, Container, Schema, followed by Features
            assertEquals(9, objs.size());
            checkApproximatelyEquals(firstFeature, objs.get(3));
		} finally {
			IOUtils.closeQuietly(fs);
			if (autoDelete && file.exists()) file.delete();
		}
	}

    @Test
    public void testKmz() throws IOException, XMLStreamException {
        File file = createTemp("test", ".kmz");
        ZipOutputStream zoS = null;
        try {
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream boS = new BufferedOutputStream(os);
            // Create the doc.kml file inside of a zip entry
            zoS = new ZipOutputStream(boS);
            ZipEntry zEnt = new ZipEntry("doc.kml");
            zoS.putNextEntry(zEnt);
            KmlOutputStream kos = new KmlOutputStream(zoS);
            kos.write(new DocumentStart(DocumentType.KML));
            Feature f = new Feature();
            f.setGeometry(new Point(42.504733587704, -71.238861602674));
            f.setName("test");
            f.setDescription("this is a test placemark");
            kos.write(f);
            try {
                kos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            IOUtils.closeQuietly(zoS);
            zoS = null;
            KmlReader reader = new KmlReader(file);
            List<IGISObject> objs = reader.readAll();
            // imported features should be DocumentStart followed by Feature 
            assertEquals(2, objs.size());
            checkApproximatelyEquals(f, objs.get(1));
        } finally {
            IOUtils.closeQuietly(zoS);
            if (autoDelete && file.exists()) file.delete();
        }
    }

    public void doTest(InputStream fs) throws Exception {
        File temp = null;
        try {
            IGISInputStream is = GISFactory.getInputStream(DocumentType.KML, fs);
		    temp = createTemp("test", ".kml");
            OutputStream fos = new FileOutputStream(temp);
            IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fos);
            List<IGISObject> elements = new ArrayList<IGISObject>();
            IGISObject current;
            while ((current = is.read()) != null) {
                os.write(current);
                elements.add(current);
            }

            is.close();
            fs.close();

            os.close();
            fos.close();

            // Test for equivalence
            fs = new FileInputStream(temp);
            is = GISFactory.getInputStream(DocumentType.KML, fs);
            int index = 0;
            while ((current = is.read()) != null) {
                checkApproximatelyEquals(elements.get(index++), current);
            }
            is.close();
        } finally {
            IOUtils.closeQuietly(fs);
            if (temp != null && autoDelete && temp.exists()) temp.delete();
        }
    }
	
	/**
	 * For most objects they need to be exactly the same, but for some we can 
	 * approximate equality
	 * 
	 * @param source expected feature object
	 * @param test actual feature object
	 */
	public static void checkApproximatelyEquals(IGISObject source, IGISObject test) {
		if (source instanceof Feature && test instanceof Feature) {
			Feature sf = (Feature) source;
			Feature tf = (Feature) test;
			
			boolean ae = sf.approximatelyEquals(tf);
			
			if (! ae) {		
				System.err.println("Expected: " + source);
				System.err.println("Actual: " + test);
				fail("Found unequal objects");
			}
		} else {
			assertEquals(source, test);
		}
	}

    private InputStream getStream(String filename) throws FileNotFoundException {
        File file = new File("test/org/mitre/giscore/test/input/" + filename);
        if (file.exists()) return new FileInputStream(file);
        System.out.println("File does not exist: " + file);
        return getClass().getResourceAsStream(filename);
    }

    @Test
	public void testMultiGeometries() throws IOException, XMLStreamException {
        File out = new File("testOutput/testMultiGeometries.kml");
        KmlOutputStream os = new KmlOutputStream(new FileOutputStream(out),
                XmlOutputStreamBase.ISO_8859_1);
        try {
            List<Feature> feats = getMultiGeometries();
            os.write(new DocumentStart(DocumentType.KML));
            os.write(new ContainerStart(IKml.DOCUMENT));
            for (Feature f : feats) {
                os.write(f);
            }
        } finally {
            os.close();
        }
    }

}
