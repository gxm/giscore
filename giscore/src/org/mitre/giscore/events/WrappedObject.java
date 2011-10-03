/***************************************************************************
 * (C) Copyright MITRE Corporation 2008
 *
 * The program is provided "as is" without any warranty express or implied,
 * including the warranty of non-infringement and the implied warranties of
 * merchantability and fitness for a particular purpose.  The Copyright
 * owner will not be liable for any damages suffered by you as a result of
 * using the Program.  In no event will the Copyright owner be liable for
 * any special, indirect or consequential damages or lost profits even if
 * the Copyright owner has been advised of the possibility of their
 * occurrence.
 *
 ***************************************************************************/
package org.mitre.giscore.events;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.giscore.utils.IDataSerializable;

import java.io.IOException;
import java.io.Serializable;

/**
 * In reading a GISStream sometimes elements are sometimes out of order and a
 * straight-forward processing could encounter errors so such elements that
 * require special handling are wrapped in a <code>WrappedObject</code>.
 * Without explicit handling these are treated as <code>Comment</code>
 * objects and generally ignored or passed around without any processing.
 *
 * @author Jason Mathews, MITRE Corp.
 *         Date: Nov 4, 2009 8:54:09 AM
 */
public class WrappedObject extends Comment implements IDataSerializable, Serializable {

    private static final long serialVersionUID = 1L;

    private IGISObject wrappedObject;

    /**
     * Empty constructor for data IO
     */
    public WrappedObject() {
        super();
    }

    public WrappedObject(IGISObject obj) {
        wrappedObject = obj;
    }

    /**
     * This returns the textual data within the <code>Comment</code>.
     *
     * @return the text of this comment
     */
    public String getText() {
        StringBuilder sb = new StringBuilder()
                .append("[Comment: ");
        if (wrappedObject != null)
            sb.append('\n')
                    .append(ToStringBuilder.reflectionToString(wrappedObject, ToStringStyle.SHORT_PREFIX_STYLE));
        return sb.append(']').toString();
    }

    /**
     * Get wrapped IGISObject instance that this element includes
     *
     * @return wrapped object
     */
    @Nullable
    public IGISObject getObject() {
        return wrappedObject;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    @Override
    public boolean equals(Object other) {
        if (this == other || wrappedObject == other)
            return true;
        if (!(other instanceof IGISObject)) return false;
        if (wrappedObject != null) {
            if (other instanceof WrappedObject)
                return wrappedObject.equals(((WrappedObject) other).getObject());
            else
                return wrappedObject.equals(other);
        }
        return super.equals(other);
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public int hashCode() {
        return wrappedObject != null ? wrappedObject.hashCode() : super.hashCode();
    }

    /*
      * (non-Javadoc)
      *
      * @see org.mitre.giscore.events.BaseStart#readData(org.mitre.giscore.utils.
      * SimpleObjectInputStream)
      */
    public void readData(SimpleObjectInputStream in) throws IOException,
            ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        wrappedObject = (IGISObject) in.readObject();
    }

    /*
      * (non-Javadoc)
      *
      * @see
      * org.mitre.giscore.events.BaseStart#writeData(java.io.DataOutputStream)
      */
    public void writeData(SimpleObjectOutputStream out) throws IOException {
        if (wrappedObject instanceof IDataSerializable)
            out.writeObject((IDataSerializable) wrappedObject);
    }

}
