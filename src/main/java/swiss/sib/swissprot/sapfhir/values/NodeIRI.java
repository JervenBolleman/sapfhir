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
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <N> the type of NodeHandle
 */
public class NodeIRI<N extends NodeHandle> implements IRI {
	private static final long serialVersionUID = 1;

    private final long nodeId;
    private final PathHandleGraphSail<?, ?, N, ?> graph;

    public NodeIRI(long nodeId, PathHandleGraphSail<?, ?, N, ?> graph) {
        this.nodeId = nodeId;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getNodeNameSpace();
    }

    @Override
    public String getLocalName() {
        return Long.toString(Math.abs(nodeId));
    }

    @Override
    public String stringValue() {
        return getNamespace() + Math.abs(nodeId);
    }

    @Override
    public String toString() {
        return stringValue();
    }

    public long id() {
        return nodeId;
    }

    public N node() {
        return graph.pathGraph().fromLong(nodeId);
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
        if (obj instanceof NodeIRI<?>) {
            final NodeIRI<?> other = (NodeIRI<?>) obj;

            if (!Objects.equals(this.graph, other.graph)) {
                return false;
            }
            var pg = this.graph.pathGraph();
            var thisNode = pg.fromLong(this.nodeId);
            var thatNode = pg.fromLong(other.nodeId);
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
