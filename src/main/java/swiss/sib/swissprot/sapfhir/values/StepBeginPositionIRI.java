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
public class StepBeginPositionIRI<P extends PathHandle, S extends StepHandle> extends StepPositionIRI<P, S> {

    public StepBeginPositionIRI(P pathId, long rank, PathHandleGraphSail<P, S, ?, ?> graph) {
        this(pathId, rank, graph, -404);
    }

    public StepBeginPositionIRI(P pathId, long rank, PathHandleGraphSail<P, S, ?, ?> graph, long beginPosition) {
        super(beginPosition, pathId, rank, graph);
    }

    @Override
    public String getNamespace() {
        return graph.getPathNameSpace(path) + StepPositionIRI.POSITION;
    }

    @Override
    public String getLocalName() {
        PathGraph<P, S, ?, ?> pathGraph = graph.pathGraph();
        S step = pathGraph.stepByRankAndPath(path, rank);
        if (hasCachedPosition()) {
            return Long.toString(pathGraph.beginPositionOfStep(step));
        } else {
            return Long.toString(position);
        }
    }

    @Override
    public String stringValue() {
        return getNamespace() + getLocalName();
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
        if (obj instanceof StepBeginPositionIRI<?, ?>) {
            return equalsBeginStep(obj);
        } else if (obj instanceof StepEndPositionIRI<?, ?>) {
            return equalsEndStep(obj);
        } else if (obj instanceof IRI) {
            return stringValue().equals(((IRI) obj).stringValue());
        }
        return true;
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
            return true;
        } else {
            StepBeginPositionIRI<P, S> other2 = (StepBeginPositionIRI<P, S>) obj;
            long ourposition = this.getBeginPosition();
            long thatPosition = other2.getBeginPosition();
            return ourposition == thatPosition;

        }
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
            return false;
        } else {
            StepEndPositionIRI<P, S> other2 = (StepEndPositionIRI<P, S>) obj;
            long ourposition = getBeginPosition();
            long thatPosition = other2.getEndPosition();
            return ourposition == thatPosition;
        }
    }

    @Override
    public long getBeginPosition() {
        if (hasCachedPosition()) {
            return position;
        }
        PathGraph<P, S, ?, ?> pg = graph.pathGraph();
        S s = pg.stepByRankAndPath(path(), rank());
        return graph.pathGraph().beginPositionOfStep(s);
    }

    @Override
    public long getEndPosition() {
        PathGraph<P, S, ?, ?> pg = graph.pathGraph();
        S s = pg.stepByRankAndPath(path(), rank());
        return graph.pathGraph().endPositionOfStep(s);
    }
}
