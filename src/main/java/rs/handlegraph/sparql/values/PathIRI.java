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
public class PathIRI implements IRI {
    private final long graphId;
    private final RsHandleGraphSail graph;
    
       public PathIRI(long graphId, RsHandleGraphSail graph) {
        this.graphId = graphId;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getGraphNameSpace(graphId);
    }

    @Override
    public String getLocalName() {
        return Long.toString(graphId);
    }

    @Override
    public String stringValue() {
        return getNamespace() + graphId;
    }
}
