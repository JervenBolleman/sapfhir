/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swiss.sib.swissprot.sapfhir.values;

import io.github.vgteam.handlegraph4j.EdgeHandle;
import io.github.vgteam.handlegraph4j.HandleGraph;
import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import static org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;

/**
 *
 * @author jbollema
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class HandleGraphValueFactory<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>> implements ValueFactory {

    private final PathHandleGraphSail<P, S, N, E> graph;

    public HandleGraphValueFactory(PathHandleGraphSail<P, S, N, E> graph) {
        this.graph = graph;
    }

    @Override
    public IRI createIRI(String iri) {
        return getInstance().createIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        if (graph.hasPathNameSpace(namespace)) {
            P p = graph.pathGraph().pathByName(namespace + localName);
            return new PathIRI<>(p, graph);
        } else if (graph.matchesNodeIriPattern(namespace)) {
            try {
                long l = Long.valueOf(localName);
                return new NodeIRI<>(l, graph);
            } catch (NumberFormatException e) {
                return getInstance().createIRI(namespace, localName);
            }
        }

        try {
            S step = graph.stepFromIriString(namespace + localName);
            if (step != null) {
                return new StepIRI(graph.pathGraph().pathOfStep(step), graph.pathGraph().rankOfStep(step), graph);
            }
        } catch (NumberFormatException e) {
            return getInstance().createIRI(namespace, localName);
        }

        return getInstance().createIRI(namespace, localName);
    }

    @Override
    public BNode createBNode() {
        return SimpleValueFactory.getInstance().createBNode();
    }

    @Override
    public BNode createBNode(String nodeID) {
        return SimpleValueFactory.getInstance().createBNode(nodeID);
    }

    @Override
    public Literal createLiteral(String label) {
        return SimpleValueFactory.getInstance().createLiteral(label);
    }

    @Override
    public Literal createLiteral(String label, String language) {
        return SimpleValueFactory.getInstance().createLiteral(label, language);
    }

    @Override
    public Literal createLiteral(String label, IRI datatype) {
        return SimpleValueFactory.getInstance().createLiteral(label, datatype);
    }

    @Override
    public Literal createLiteral(boolean value) {
        return SimpleValueFactory.getInstance().createLiteral(value);
    }

    @Override
    public Literal createLiteral(byte value) {
        return SimpleValueFactory.getInstance().createLiteral(value);
    }

    @Override
    public Literal createLiteral(short value) {
        return getInstance().createLiteral(value);
    }

    @Override
    public Literal createLiteral(int value) {
        return getInstance().createLiteral(value);
    }

    @Override
    public Literal createLiteral(long value) {
        return getInstance().createLiteral(value);
    }

    @Override
    public Literal createLiteral(float value) {
        return getInstance().createLiteral(value);
    }

    @Override
    public Literal createLiteral(double value) {
        return getInstance().createLiteral(value);
    }

    @Override
    public Literal createLiteral(BigDecimal bigDecimal) {
        return getInstance().createLiteral(bigDecimal);
    }

    @Override
    public Literal createLiteral(BigInteger bigInteger) {
        return getInstance().createLiteral(bigInteger);
    }

    @Override
    public Literal createLiteral(XMLGregorianCalendar calendar) {
        return getInstance().createLiteral(calendar);
    }

    @Override
    public Literal createLiteral(Date date) {
        return getInstance().createLiteral(date);
    }

    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object) {
        if (subject == null || predicate == null || object == null) {
            throw new NullPointerException("somethings null"
                    + subject + ':' + predicate + ':' + object);
        }
        return getInstance().createStatement(subject, predicate, object);
    }

    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        if (subject == null || predicate == null || object == null) {
            throw new NullPointerException("somethings null"
                    + subject + ':' + predicate + ':' + object);
        }
        return getInstance().createStatement(subject, predicate, object, context);
    }

    public <N extends NodeHandle, E extends EdgeHandle<N>> Literal createSequenceLiteral(final N handle, HandleGraph<N, E> graph) {
        return new SequenceLiteralWithNodeHandle<>(graph, handle);
    }
}
