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
public class StepBeginPositionIRI implements IRI {

    private final long graphId;
    private final long stepId;
    private final RsHandleGraphSail graph;

    public StepBeginPositionIRI(long graphId, long stepId, RsHandleGraphSail graph) {
        this.graphId = graphId;
        this.stepId = stepId;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getGraphNameSpace(graphId) + graphId + '/';
    }

    @Override
    public String getLocalName() {
        return Long.toString(stepId) + "#begin";
    }

    @Override
    public String stringValue() {
        return getNamespace() + graphId + '/' + stepId + "#begin";
    }
}
