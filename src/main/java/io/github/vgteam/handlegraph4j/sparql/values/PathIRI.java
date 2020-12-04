/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.vgteam.handlegraph4j.sparql.values;

import io.github.vgteam.handlegraph4j.PathHandle;
import org.eclipse.rdf4j.model.IRI;
import io.github.vgteam.handlegraph4j.sparql.PathHandleGraphSail;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 *
 * @author jbollema
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

    @Override
    public String stringValue() {
        return graph.getPathNameSpace(pathId);
    }
}
