/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.sapfhir.values;

import io.github.vgteam.handlegraph4j.PathHandle;
import org.eclipse.rdf4j.model.IRI;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import java.util.Objects;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 *
 * @author jbollema
 * @param <P> the type of PathHandle
 */
public class PathIRI<P extends PathHandle> implements IRI {

    private static final ValueFactory VF = SimpleValueFactory.getInstance();
    private final P pathId;
    private final PathHandleGraphSail graph;

    public PathIRI(P pathId, PathHandleGraphSail<P, ?, ?, ?> graph) {
        this.pathId = pathId;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return VF.createIRI(graph.getPathNameSpace(pathId)).getNamespace();
    }

    @Override
    public String getLocalName() {
        return VF.createIRI(graph.getPathNameSpace(pathId)).getLocalName();
    }

    public P path() {
        return pathId;
    }

    @Override
    public String stringValue() {
        return graph.getPathNameSpace(pathId);
    }

    @Override
    public String toString() {
        return stringValue();
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
        if (obj instanceof PathIRI<?>) {
            final PathIRI<?> other = (PathIRI<?>) obj;
            if (this.pathId.equals(other.pathId)) {
                return false;
            }
            if (!Objects.equals(this.graph, other.graph)) {
                return false;
            }
            return true;
        } else if (obj instanceof IRI) {
            return stringValue().equals(((IRI) obj).stringValue());
        }
        return true;
    }
}
