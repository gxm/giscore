package org.mitre.giscore.test.input;

import junit.framework.TestCase;
import org.junit.Test;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.test.output.TestKmlOutputStream;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URL;
import java.awt.image.BufferedImage;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 30, 2009 1:12:51 PM
 */
public class TestKmlReader extends TestCase {

	/**
     * Test loading KMZ file with network link containing embedded KML
	 * then load the content from the NetworkLink.
     *
	 * @throws IOException if an I/O error occurs
     */
    @Test
	public void testKmzNetworkLinks() throws IOException {
		File file = new File("data/kml/kmz/dir/content.kmz");
		KmlReader reader = new KmlReader(file);
		List<IGISObject> features = reader.readAll();
		assertEquals(5, features.size());
		List<IGISObject> linkedFeatures = reader.importFromNetworkLinks();
		List<URI> networkLinks = reader.getNetworkLinks();
		assertEquals(1, networkLinks.size());
		assertEquals(2, linkedFeatures.size());
		IGISObject o = linkedFeatures.get(1);
		assertTrue(o instanceof Feature);
		Feature ptFeat = (Feature)o;
		Geometry geom = ptFeat.getGeometry();
		assertTrue(geom instanceof Point);

		// import same KMZ file as URL
		URL url = file.toURI().toURL();
		KmlReader reader2 = new KmlReader(url);
		List<IGISObject> features2 = reader2.readAll();
		List<IGISObject> linkedFeatures2 = reader2.importFromNetworkLinks();
		List<URI> networkLinks2 = reader2.getNetworkLinks();
		assertEquals(5, features2.size());
		assertEquals(1, networkLinks2.size());
		assertEquals(2, linkedFeatures2.size());
		// NetworkLinked Feature -> DocumentStart + Feature
		TestKmlOutputStream.checkApproximatelyEquals(ptFeat, linkedFeatures2.get(1));
	}

	/**
     * Test loading KMZ file with 2 levels of network links
	 * recursively loading each NetworkLink.
     *
	 * @throws IOException if an I/O error occurs
     */
    @Test
	public void testMultiLevelNetworkLinks() throws IOException {
		File file = new File("data/kml/NetworkLink/multiLevelNetworkLinks2.kmz");
		KmlReader reader = new KmlReader(file);
		List<IGISObject> objs = reader.readAll();
		assertEquals(6, objs.size());
		List<IGISObject> linkedFeatures = reader.importFromNetworkLinks();
		List<URI> networkLinks = reader.getNetworkLinks();

		assertEquals(2, networkLinks.size());
		assertEquals(9, linkedFeatures.size());
		IGISObject o = linkedFeatures.get(8);
		assertTrue(o instanceof Feature);
		Feature ptFeat = (Feature)o;
		Geometry geom = ptFeat.getGeometry();
		assertTrue(geom instanceof Point);
	}

	/**
     * Test ground overlay from KMZ file target
     */
    @Test
	public void testKmzFileOverlay() throws Exception {
		// target overlay URI -> kmzfile:/C:/projects/giscore/data/kml/GroundOverlay/etna.kmz?file=etna.jpg
		checkGroundOverlay(new KmlReader(new File("data/kml/GroundOverlay/etna.kmz")));
	}

	/**
     * Test ground overlays with KML from URL target
     */
    @Test
	public void testUrlOverlay() throws Exception {
		// target overlay URI -> file:/C:/projects/giscore/data/kml/GroundOverlay/etna.jpg
		checkGroundOverlay(new KmlReader(new File("data/kml/GroundOverlay/etna.kml").toURI().toURL()));
	}

	private void checkGroundOverlay(KmlReader reader) throws Exception {
		List<IGISObject> features = reader.readAll();
		assertEquals(2, features.size());
		IGISObject obj = features.get(1);
		assertTrue(obj instanceof GroundOverlay);
		GroundOverlay o = (GroundOverlay)obj;
		TaggedMap icon = o.getIcon();
		String href = icon != null ? icon.get(IKml.HREF) : null;
		assertNotNull(href);
		//System.out.println(href);
		UrlRef urlRef = new UrlRef(new URI(href));
		//System.out.println(urlRef);
		InputStream is = null;
		try {
			is = urlRef.getInputStream();
			BufferedImage img = ImageIO.read(is);
			assertNotNull(img);
			assertEquals(418, img.getHeight());
			assertEquals(558, img.getWidth());
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	/**
     * Test IconStyle with KML from URL target with relative URL to icon
	 * @throws Exception
	 */
    @Test
	public void testIconStyle() throws Exception {
		checkIconStyle(new KmlReader(new File("data/kml/Style/styled_placemark.kml").toURI().toURL()));
	}

	/**
     * Test IconStyle from KMZ file target with icon inside KMZ
	 * @throws Exception
	 */
    @Test
	public void testKmzIconStyle() throws Exception {
		checkIconStyle(new KmlReader(new File("data/kml/kmz/iconStyle/styled_placemark.kmz")));
	}

	private void checkIconStyle(KmlReader reader) throws Exception {
		List<IGISObject> features = new ArrayList<IGISObject>();
		try {
			IGISObject gisObj;
			while ((gisObj = reader.read()) != null) {
				features.add(gisObj);
			}
		} finally {
			reader.close();
		}
		/*
		for(Object o : features) {
			System.out.println(" >" + o.getClass().getName());
		}
		System.out.println();
        */
		assertEquals(3, features.size());
		IGISObject obj = features.get(1);
		assertTrue(obj instanceof Style);
		Style style = (Style)obj;
		assertTrue(style.hasIconStyle());
		String href = style.getIconUrl();
		assertNotNull(href);
		UrlRef urlRef = new UrlRef(new URI(href));
		InputStream is = null;
		try {
			is = urlRef.getInputStream();
			BufferedImage img = ImageIO.read(is);
			assertNotNull(img);
			assertEquals(80, img.getHeight());
			assertEquals(80, img.getWidth());
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

}