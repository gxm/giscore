package org.mitre.giscore.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.shapefile.ShapefileInputStream;
import org.mitre.giscore.output.kml.KmzOutputStream;

import javax.xml.stream.XMLStreamException;
import java.awt.Color;
import java.io.*;

/**
 * Simple Shape file-to-KML Converter. Convert arbitrary shape files to KMZ files.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 1/4/11 5:19 PM
 */
public class Shape2Kml {

	private boolean verbose;
	private String labelName; // "ROUTE" [in101503.shp], "FULLNAME", LNAME [interstates.shp]

	public void outputKml(File shapefile) throws IOException, XMLStreamException {
		// InputStream is = new FileInputStream(shapefile);
		// if (shapefile.getName().endsWith(".zip") ) is = new ZipInputStream(is);
		String filename = shapefile.getName();
		String baseName;
		int ind = filename.lastIndexOf('.');
		if (ind > 0) baseName = filename.substring(0, ind);
		else baseName = "out";
		File temp;
		while (true) {
			temp = new File(baseName + ".kmz");
			if (!temp.exists()) break;
			baseName += '~';
		}
		System.out.println("Input: " + shapefile);
		System.out.println("Output: " + temp);
		if (labelName != null)
			System.out.println("Label: " + labelName);
		KmzOutputStream kmzos = new KmzOutputStream(new FileOutputStream(temp));
		int count = 0;
		int outCount = 0;
		int labelCount = 0;

		IGISInputStream sis = null;
		boolean needContainerEnd = false;
		try {
			/*
			 IAcceptSchema accepter = new IAcceptSchema() {
			 public boolean accept(Schema schema) {
				return true; // schema.get("today") != null;
			 }
			 };
			*/
			// note: if use InputStream in constructor cannot use .dbf file and no extended data
			sis = new ShapefileInputStream(shapefile, null); // accepter);
			// sis = GISFactory.getInputStream(DocumentType.Shapefile, shapefile, (IAcceptSchema)null);
			/*
			 // this preserves the extended schema data
			 String shapefileName = shapefile.getName();
			 int ind = shapefileName.lastIndexOf('.');
			 if (ind > 0) shapefileName = shapefileName.substring(0,ind); // strip off .shp extension
			 sis = new SingleShapefileInputHandler(shapefile.getParentFile(), shapefileName);
			*/
			Style style = new Style("style");
			style.setPolyStyle(null, false, null);
			double width = filename.startsWith("STATE") ? 1 : filename.startsWith("US_Highway") ? 2 : 3;
			style.setLineStyle(new Color(196, 161, 45, 255), width); // matches GE road color #c4a12d
			// TODO: can add objects to temp cache for first ~50 objects after which insert
			// a hide-child style and apply to the container to suppress large # items in GE menu list
			needContainerEnd = true;
			ContainerStart cs = new ContainerStart(IKml.DOCUMENT);
			cs.addStyle(style);
			kmzos.write(cs);
			boolean otherFlag = false;
			SimpleField sf = (labelName != null) ? new SimpleField(labelName) : null;
			final boolean hasLineStyle = style.hasLineStyle();
			Class lastGeom = null;
			IGISObject ob;
			while ((ob = sis.read()) != null) {
				if (ob instanceof Feature) {
					Feature f = (Feature) ob;
					Geometry geo = f.getGeometry();
					if (geo == null) {
						// null geometry
						continue;
					}
					count++; // featureCount
					Class geomClass = geo.getClass();
					if (lastGeom != geomClass) {
						// all feature geometries in shape file should be the same
						System.out.println("Geometry: " + geomClass);
						lastGeom = geomClass;
					}
					if (verbose && count <= 2) {
						// verbose debugging for testing
						// need to dump metadata to identify which field has label name for feature
						System.out.println(ToStringBuilder.reflectionToString(ob, ToStringStyle.MULTI_LINE_STYLE));
					}
					if (geo instanceof LinearRing) {
						// convert LinearRings to LineStrings / better icon support in Google Earth
						// Google Earth 5.x/6.x shows LinearRing placemarks in Menu panel with Point icon - confusing U/I
						// see http://code.google.com/p/kml-samples/issues/detail?id=369
						LinearRing lr = (LinearRing) geo;
						Line line = new Line(geo.getPoints());
						line.setAltitudeMode(lr.getAltitudeMode());
						line.setTessellate(true);
						// note: no need to check extrude property: not set on shape files
						f.setGeometry(line);
						if (hasLineStyle) f.setStyleUrl("#style");
						geo = line;
						// if (ringCount == 0)...
					} else if (geo instanceof Line || geo instanceof MultiLine) {
						if (hasLineStyle) f.setStyleUrl("#style");
						GeometryBase base = (GeometryBase) geo;
						//if (geo instanceof Line)
						base.setTessellate(true);
						// need to tessellate each Line in MultiLine
						// final int numPoints = geo.getNumPoints();
						// if (numPoints < 25) continue;
						// System.out.println("points=" + numPoints);
					} else if (hasPolygon(geo)) {
						f.setStyleUrl("#style");
					} else if (!otherFlag && !(geo instanceof Point) && !(geo instanceof MultiPoint)) {
						// may need to handle MultiPolygon, MultiLinearRings, etc.
						System.out.println("WARN: other geometry: " + geo.getClass());
						otherFlag = true;
					}

					// add geo filter rules here
					/*
					Geodetic2DBounds bbox = geo.getBoundingBox();
					if (bbox != null && bbox.getWestLon().inDegrees() < -125) {
						continue; // skip west of CONUS
					}
					*/
					/*
					if (outCount++ >= 13000) {
						System.out.println("*** count=" + count);
						break;
					}
					*/
					if (sf != null) {
						Object name = f.removeData(sf); // f.getData(sf);
						if (name != null) {
							f.setName(StringUtils.trimToNull(String.valueOf(name)));
							// setup alternate field for name
							if (f.getName() != null) labelCount++;
							//else System.out.println("XXX: no name.1"); // debug
						}
						//else if (outCount < 50) System.out.println("XXX: no name.2: " + f.getFields()); // debug
					}
					kmzos.write(f);
				} else if (ob.getClass() == Schema.class) {
					System.out.println(ToStringBuilder.reflectionToString(
						ob, ToStringStyle.MULTI_LINE_STYLE));
					if (labelName == null) {
						Schema s = (Schema) ob;
						for (SimpleField field : s.getFields()) {
							String name = field.getName();
							if (name.toLowerCase().endsWith("name")) {
								labelName = name;
								sf = field;
								System.out.println("Label: " + labelName);
								break;
							}
						}
					}
				} else {
					System.out.println("WARN: other type: " + ob.getClass());
					if (verbose) {
						System.out.println(ToStringBuilder.reflectionToString(
							ob, ToStringStyle.MULTI_LINE_STYLE));
					}
				}
			}
		} finally {
			if (sis != null) sis.close();
			if (needContainerEnd) kmzos.write(new ContainerEnd());
			kmzos.close();
		}

		System.out.println("Total feature count: " + count);
		System.out.println("Features with names: " + labelCount);
	}

	private static boolean hasPolygon(Geometry geo) {
		if (geo instanceof Polygon) {
			// enable tessellation on polygons
			((GeometryBase) geo).setTessellate(true);
			return true;
		}
		// what about MultiPolygon ?
		// TODO: can shape file even have GeometryBag: don't think so
		if (geo instanceof GeometryBag) {
			for (Geometry geom : (GeometryBag) geo) {
				if (hasPolygon(geom)) return true;
			}
		}
		return false;
	}

	private static void usage() {
		System.out.println("Usage: Shape2Kml [-v] [-nlabelName] <shp output file>");
		System.out.println(" -nlabelName = name attribute for feature");
		System.out.println(" -v = verbose mode");
		System.exit(0);
	}

	public static void main(String args[]) throws Exception {
		String file = null;
		boolean verbose = false;
		String label = null;
		for (String arg : args) {
			// System.out.println(" " + arg);
			if ("-v".equals(arg)) verbose = true;
			else if (arg.startsWith("-n")) label = arg.substring(2);
			else if (!arg.startsWith("-")) file = arg;
			else usage();
		}
		if (file == null) {
			usage();
		}
		Shape2Kml app = new Shape2Kml();
		app.labelName = label;
		app.verbose = verbose;
		app.outputKml(new File(file));
	}

}
