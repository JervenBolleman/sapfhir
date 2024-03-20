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

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;

import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;

/**
 * An IRI that hides a node, allows comparing in node space instead of string
 * space
 * 
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <N>   the type of NodeHandle
 * @param id    the id of the node in the graph
 * @param graph that the node is present in
 */
public record NodeIRI<N extends NodeHandle>(long id, PathHandleGraphSail<?, ?, N, ?> graph) implements IRI {
	private static final long serialVersionUID = 1;

	@Override
	public String getNamespace() {
		return graph.getNodeNameSpace();
	}

	@Override
	public String getLocalName() {
		return Long.toString(Math.abs(id));
	}

	@Override
	public String stringValue() {
		return getNamespace() + Math.abs(id);
	}

	@Override
	public String toString() {
		return stringValue();
	}

	/**
	 * The node hiding in this IRI
	 * 
	 * @return a node
	 */
	public N node() {
		return graph.pathGraph().fromLong(id);
	}

	@Override
	public int hashCode() {
		return stringValue().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof NodeIRI<?> other) {
			if (!Objects.equals(this.graph, other.graph)) {
				return false;
			}
			var pg = this.graph.pathGraph();
			var thisNode = pg.fromLong(this.id);
			var thatNode = pg.fromLong(other.id);
			var thisIsRev = pg.isReverseNodeHandle(thisNode);
			var thatIsRev = pg.isReverseNodeHandle(thatNode);
			if (thisIsRev == thatIsRev) {
				return pg.equalNodes(thisNode, thatNode);
			} else {
				var flipThat = pg.flip(thatNode);
				return pg.equalNodes(thisNode, flipThat);
			}
		} else if (obj instanceof IRI) {
			return stringValue().equals(((IRI) obj).stringValue());
		}
		return true;
	}
}
