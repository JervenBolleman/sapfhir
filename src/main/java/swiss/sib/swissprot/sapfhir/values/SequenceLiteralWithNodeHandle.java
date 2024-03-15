/**
 * Copyright (c) 2020, SIB Swiss Institute of Bioinformatics
 * and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package swiss.sib.swissprot.sapfhir.values;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import io.github.jervenbolleman.handlegraph4j.EdgeHandle;
import io.github.jervenbolleman.handlegraph4j.HandleGraph;
import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class SequenceLiteralWithNodeHandle<N extends NodeHandle, E extends EdgeHandle<N>> implements Literal {
	private static final long serialVersionUID = 1;

	/**
	 * the graph
	 */
    private final HandleGraph<N, E> graph;
    
    /**
     * the handle
     */
    private final N handle;

    public SequenceLiteralWithNodeHandle(HandleGraph<N, E> graph, N handle) {
        this.graph = graph;
        this.handle = handle;
    }

    @Override
    public String getLabel() {
        return graph.sequenceOf(handle).asString();
    }

    @Override
    public Optional<String> getLanguage() {
        return Optional.empty();
    }

    @Override
    public IRI getDatatype() {
        return XSD.STRING;
    }

    @Override
    public String stringValue() {
        return getLabel();
    }

    @Override
    public byte byteValue() {
        throw new NumberFormatException("A sequence is not a number");
    }

    @Override
    public short shortValue() {
        throw new NumberFormatException("A sequence is not a number");
    }

    @Override
    public int intValue() {
        throw new NumberFormatException("A sequence is not a number");
    }

    @Override
    public long longValue() {
        throw new NumberFormatException("A sequence is not a number");
    }

    @Override
    public BigInteger integerValue() {
        throw new NumberFormatException("A sequence is not a number");
    }

    @Override
    public BigDecimal decimalValue() {
        throw new NumberFormatException("A sequence is not a number");
    }

    @Override
    public float floatValue() {
        throw new NumberFormatException("A sequence is not a number");
    }

    @Override
    public double doubleValue() {
        throw new NumberFormatException("A sequence is not a number");
    }

    @Override
    public boolean booleanValue() {
        throw new IllegalArgumentException("Sequences are not a legal boolean value: ");
    }

    @Override
    public XMLGregorianCalendar calendarValue() {
        throw new IllegalArgumentException("Sequence is not a date");
    }

    @Override
    public int hashCode() {
        return getLabel().hashCode();
    }

    /**
     * Returns the label of the literal with its language or datatype. Note that
     * this method does not escape the quoted label.
     *
     * @see
     * org.eclipse.rdf4j.rio.ntriples.NTriplesUtil#toNTriplesString(org.eclipse.rdf4j.model.Literal)
     */
    @Override
    public String toString() {
        return '"' + getLabel() + '"';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof SequenceLiteralWithNodeHandle) {
            SequenceLiteralWithNodeHandle other = (SequenceLiteralWithNodeHandle) obj;
            if (this.graph.equals(other.graph)
                    && this.handle.equals(other.handle)) {
                return true;
            }
            Sequence sequenceOfThis = graph.sequenceOf(handle);
			Sequence sequenceOfOther = other.graph.sequenceOf(other.handle);
			return sequenceOfThis
                    .equals(sequenceOfOther);
        } else if (obj instanceof Literal) {
            Literal other = (Literal) obj;
            if (other.getLanguage().isPresent()) {
                return false;
            } else if (other.getDatatype() == null
                    || XSD.STRING.equals(other.getDatatype())) {
                return getLabel().equals(other.getLabel());
            }
        }
        return false;
  
    
    }

	@Override
	public CoreDatatype getCoreDatatype() {
		return CoreDatatype.XSD.STRING;
	}
}
