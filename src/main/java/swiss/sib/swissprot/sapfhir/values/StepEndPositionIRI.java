/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.sapfhir.values;

import io.github.vgteam.handlegraph4j.PathGraph;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import org.eclipse.rdf4j.model.IRI;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import java.util.Objects;

/**
 *
 * @author jbollema
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 */
public class StepEndPositionIRI<P extends PathHandle, S extends StepHandle> implements StepPositionIRI<P, S> {

    private final P pathId;
    private final long rank;
    final PathHandleGraphSail<P, S, ?, ?> graph;

    public StepEndPositionIRI(P pathId, long rank, PathHandleGraphSail<P, S, ?, ?> graph) {
        this.pathId = pathId;
        this.rank = rank;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getPathNameSpace(pathId) + StepPositionIRI.POSITION;
    }

    @Override
    public String getLocalName() {
        PathGraph<P, S, ?, ?> pathGraph = graph.pathGraph();
        S step = pathGraph.stepByRankAndPath(pathId, rank);
        return Long.toString(pathGraph.endPositionOfStep(step));
    }

    @Override
    public String stringValue() {
        return getNamespace() + '/' + getLocalName();
    }

    @Override
    public String toString() {
        return stringValue();
    }

    @Override
    public int hashCode() {
        return stringValue().hashCode();
    }

    private boolean equalsBeginStep(Object obj) {
        StepBeginPositionIRI<?, ?> other = (StepBeginPositionIRI<?, ?>) obj;
        if (!Objects.equals(this.path(), other.path())) {
            return false;
        }
        if (!Objects.equals(this.graph, other.graph)) {
            return false;
        }
        if (this.rank == other.rank()) {
            return false;
        } else {
            StepBeginPositionIRI<P, S> other2 = (StepBeginPositionIRI<P, S>) obj;
            long ourposition = getEndPosition(this);
            long thatPosition = getBeginPosition(other2);
            return ourposition == thatPosition;

        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof StepBeginPositionIRI<?, ?>) {
            return equalsBeginStep(obj);
        } else if (obj instanceof StepEndPositionIRI<?, ?>) {
            return equalsEndStep(obj);
        } else if (obj instanceof IRI) {
            return stringValue().equals(((IRI) obj).stringValue());
        }
        return true;
    }

    private boolean equalsEndStep(Object obj) {
        StepEndPositionIRI<?, ?> other = (StepEndPositionIRI<?, ?>) obj;
        if (!Objects.equals(this.path(), other.path())) {
            return false;
        }
        if (!Objects.equals(this.graph, other.graph)) {
            return false;
        }
        if (this.rank == other.rank()) {
            return true;
        } else {
            StepEndPositionIRI<P, S> other2 = (StepEndPositionIRI<P, S>) obj;
            long ourposition = getEndPosition(this);
            long thatPosition = getEndPosition(other2);
            return ourposition == thatPosition;
        }
    }

    private long getBeginPosition(StepBeginPositionIRI<P, S> stepBeginIri) {
        PathGraph<P, S, ?, ?> pg = graph.pathGraph();
        S s = pg.stepByRankAndPath(stepBeginIri.path(), stepBeginIri.rank());
        return graph.pathGraph().beginPositionOfStep(s);
    }

    private long getEndPosition(StepEndPositionIRI<P, S> stepEndIri) {
        PathGraph<P, S, ?, ?> pg = graph.pathGraph();
        S s = pg.stepByRankAndPath(stepEndIri.path(), stepEndIri.rank());
        return graph.pathGraph().endPositionOfStep(s);
    }

    public P path() {
        return pathId;
    }

    public long rank() {
        return rank;
    }
}
