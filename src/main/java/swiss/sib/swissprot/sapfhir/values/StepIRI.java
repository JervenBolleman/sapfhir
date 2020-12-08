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

/**
 *
 * @author jbollema
 * @param <P> the type of PathHandle
 */
public class StepIRI<P extends PathHandle> implements IRI {

    private final P path;
    private final long rank;
    private final PathHandleGraphSail graph;

    public StepIRI(P pathId, long rank, PathHandleGraphSail graph) {
        this.path = pathId;
        this.rank = rank;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getPathNameSpace(path) + "/step/";
    }

    @Override
    public String getLocalName() {
        return Long.toString(rank);
    }

    @Override
    public String stringValue() {
        return getNamespace() + '/' + getLocalName();
    }

    public long rank() {
        return rank;
    }

    public P path() {
        return path;
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
        if (obj instanceof StepIRI<?>) {
            final StepIRI<?> other = (StepIRI<?>) obj;
            if (this.rank != other.rank()) {
                return false;
            }
            if (!Objects.equals(this.path, other.path())) {
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
