/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rs.handlegraph.sparql.values;

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
import rs.handlegraph.sparql.RsHandleGraphSail;

/**
 *
 * @author jbollema
 */
public class RsHandleGraphValueFactory implements ValueFactory {

    private final RsHandleGraphSail graph;

    public RsHandleGraphValueFactory(RsHandleGraphSail graph) {
        this.graph = graph;
    }

    @Override
    public IRI createIRI(String iri) {
        return getInstance().createIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        if (graph.hasPathNameSpace(namespace)) {
            try {
                long l = Long.valueOf(localName);
                return new PathIRI(l, graph);
            } catch (NumberFormatException e) {
                return getInstance().createIRI(namespace, localName);
            }
        } else if (graph.hasNodeNameSpace(namespace)) {
            try {
                long l = Long.valueOf(localName);
                return new NodeIRI(l, graph);
            } catch (NumberFormatException e) {
                return getInstance().createIRI(namespace, localName);
            }
        } else if (graph.matchesAStepInPathNameSpace(namespace)) {
            try {
                long pathId = graph.extractPathId(namespace);
                long stepId = Long.valueOf(localName);
                return new StepIRI(pathId, stepId, graph);
            } catch (NumberFormatException e) {
                return getInstance().createIRI(namespace, localName);
            }
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        return getInstance().createStatement(subject, predicate, object);
    }

    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        return getInstance().createStatement(subject, predicate, object, context);
    }
}
