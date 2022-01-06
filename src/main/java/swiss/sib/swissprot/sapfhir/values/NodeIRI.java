/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.sapfhir.values;

import io.github.vgteam.handlegraph4j.NodeHandle;
import org.eclipse.rdf4j.model.IRI;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import java.util.Objects;

/**
 *
 * @author jbollema
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
