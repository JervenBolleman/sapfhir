/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.vgteam.handlegraph4j.sparql.values;

import io.github.vgteam.handlegraph4j.PathGraph;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import org.eclipse.rdf4j.model.IRI;
import io.github.vgteam.handlegraph4j.sparql.PathHandleGraphSail;

/**
 *
 * @author jbollema
 */
public class StepEndPositionIRI<P extends PathHandle, S extends StepHandle> implements IRI {

    private final P pathId;
    private final long rank;
    private final PathHandleGraphSail<P, S, ?, ?> graph;

    public StepEndPositionIRI(P pathId, long rank, PathHandleGraphSail<P, S, ?, ?> graph) {
        this.pathId = pathId;
        this.rank = rank;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getPathNameSpace(pathId) + "/postion/";
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
}
