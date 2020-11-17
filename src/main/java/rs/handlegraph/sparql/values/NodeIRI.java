/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rs.handlegraph.sparql.values;

import org.eclipse.rdf4j.model.IRI;
import rs.handlegraph.sparql.RsHandleGraphSail;

/**
 *
 * @author jbollema
 */
public class NodeIRI implements IRI {

    private final long nodeId;
    private final RsHandleGraphSail graph;

    public NodeIRI(long nodeId, RsHandleGraphSail graph) {
        this.nodeId = nodeId;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getNodeNameSpace(nodeId);
    }

    @Override
    public String getLocalName() {
        return Long.toString(nodeId);
    }

    @Override
    public String stringValue() {
        return getNamespace() + nodeId;
    }

}
