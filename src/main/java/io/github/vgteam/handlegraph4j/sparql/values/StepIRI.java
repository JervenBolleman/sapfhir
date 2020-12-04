/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.vgteam.handlegraph4j.sparql.values;

import io.github.vgteam.handlegraph4j.PathHandle;
import org.eclipse.rdf4j.model.IRI;
import io.github.vgteam.handlegraph4j.sparql.PathHandleGraphSail;

/**
 *
 * @author jbollema
 */
public class StepIRI<P extends PathHandle> implements IRI {
    private final P pathId;
    private final long rank;
    private final PathHandleGraphSail graph;
    
       public StepIRI(P pathId, long rank, PathHandleGraphSail graph) {
        this.pathId = pathId;
        this.rank = rank;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getPathNameSpace(pathId)+"/step/";
    }

    @Override
    public String getLocalName() {
        return Long.toString(rank);
    }

    @Override
    public String stringValue() {
        return getNamespace() + '/'+getLocalName();
    }
}
