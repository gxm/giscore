/****************************************************************************************
 *  GeometryBag.java
 *
 *  Created: Feb 23, 2009
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
package org.mitre.giscore.geometry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.Angle;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;

/**
 * This is a simple typed collection of geometries. The methods aggregate answers
 * from the child geometries.
 *
 * @author DRAND
 */
public class GeometryBag extends Geometry implements Collection<Geometry> {
	private static final long serialVersionUID = 1L;

	private final List<Geometry> geometries = new ArrayList<Geometry>();

	/**
	 * Empty ctor for object io.
	 */
	public GeometryBag() {
		//
	}

	/**
	 * Ctor
	 * @param geometries List of Geometry objects to include in the bag
	 * @throws IllegalArgumentException error if geometries is null.
	 */
	public GeometryBag(List<Geometry> geometries) {
		if (geometries == null) {
			throw new IllegalArgumentException(
					"geometries should never be null");
		}
		if (!geometries.isEmpty()) {
			this.geometries.addAll(geometries);
		}
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#containerOf(org.mitre.giscore.geometry.Geometry)
	 */
	@Override
	public boolean containerOf(Geometry otherGeom) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#computeBoundingBox()
	 */
	protected void computeBoundingBox() {
		Geodetic2DBounds rval = null;
		for(Geometry geo : geometries) {
			Geodetic2DBounds bounds = geo.getBoundingBox();
			if (rval == null) {
				rval = new Geodetic2DBounds(bounds);
			} else {
				rval.include(bounds);
			}
		}
		if (rval != null)
			bbox = rval;
		else
			bbox = new Geodetic2DBounds();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#getCenter()
	 */
	@Override
    @NonNull
	public Geodetic2DPoint getCenter() {
		double lat = 0.0;
		double lon = 0.0;
		double count = 0;
		for(Geometry geo : geometries) {
			Geodetic2DPoint cen = geo.getCenter();
      lat += cen.getLatitudeAsDegrees();
      lon += cen.getLongitudeAsDegrees();
			count++;
		}
		// Compute Averages
		lat = lat / count;
		lon = lon / count;
		return new Geodetic2DPoint(new Longitude(lon, Angle.DEGREES),
				new Latitude(lat, Angle.DEGREES));
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#getNumParts()
	 */
	@Override
	public int getNumParts() {
		int total = 0;
		for(Geometry geo : geometries) {
			total += geo.getNumParts();
		}
		return total;
	}

	@Override
    @NonNull
	public Geometry getPart(int i) {
		return geometries.get(i);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#getNumPoints()
	 */
	@Override
	public int getNumPoints() {
		int total = 0;
		for(Geometry geo : geometries) {
			total += geo.getNumPoints();
		}
		return total;
	}

	@Override
    @NonNull
	public List<Point> getPoints() {
		List<Point> rval = new ArrayList<Point>();
		for(Geometry geo : geometries) {
			rval.addAll(geo.getPoints());
		}
		return Collections.unmodifiableList(rval);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#is3D()
	 */
	@Override
	public boolean is3D() {
		for(Geometry geo : geometries) {
			if (geo.is3D) return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#toString()
	 */
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.VisitableGeometry#accept(org.mitre.giscore.output.StreamVisitorBase)
	 */
	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	public boolean add(Geometry e) {
		return geometries.add(e);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends Geometry> c) {
		return geometries.addAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		geometries.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		return geometries.contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		return geometries.containsAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		return geometries.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#iterator()
	 */
    @NonNull
	public Iterator<Geometry> iterator() {
		return geometries.iterator();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		return geometries.remove(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
		return geometries.removeAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
		return geometries.retainAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#size()
	 */
	public int size() {
		return geometries.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray()
	 */
    @NonNull
	public Object[] toArray() {
		return geometries.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray(T[])
	 */
    @NonNull    
	public <T> T[] toArray(T[] a) {
		return geometries.toArray(a);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException, IllegalAccessException {
		super.readData(in);
		List<Geometry> list = (List<Geometry>) in.readObjectCollection();
		if (!geometries.isEmpty()) geometries.clear();
		if (list != null && !list.isEmpty())
			geometries.addAll(list);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeObjectCollection(geometries);
	}
}