/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.vgteam.handlegraph4j.sparql.values;

import io.github.vgteam.handlegraph4j.NodeHandle;
import org.eclipse.rdf4j.model.IRI;
import io.github.vgteam.handlegraph4j.sparql.PathHandleGraphSail;

/**
 *
 * @author jbollema
 */
public class NodeIRI<N extends NodeHandle> implements IRI {

    private final long nodeId;
    private final PathHandleGraphSail<?,?,N,?> graph;

    public NodeIRI(long nodeId, PathHandleGraphSail<?,?,N,?> graph) {
        this.nodeId = nodeId;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getNodeNameSpace();
    }

    @Override
    public String getLocalName() {
        return Long.toString(nodeId);
    }

    @Override
    public String stringValue() {
        return getNamespace() + nodeId;
    }

    public long id() {
        return nodeId;
    }
    
    public N node(){
        return graph.pathGraph().fromLong(nodeId);
    }
}
