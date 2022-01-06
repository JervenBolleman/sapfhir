/*
 * Copyright (C) 2020 Jerven Bolleman <jerven.bolleman@sib.swiss>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package swiss.sib.swissprot.sapfhir.statements;

import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.of;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.empty;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.filter;

import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.nodeIriFromIRI;
import io.github.vgteam.handlegraph4j.EdgeHandle;
import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.NodeSequence;
import io.github.vgteam.handlegraph4j.PathGraph;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import swiss.sib.swissprot.sapfhir.values.HandleGraphValueFactory;
import swiss.sib.swissprot.sapfhir.values.NodeIRI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import swiss.sib.swissprot.handlegraph4jrdf.VG;
import swiss.sib.swissprot.sapfhir.values.SequenceLiteral;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class NodeRelatedStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>
        implements StatementProvider {

    private final PathHandleGraphSail<P, S, N, E> sail;
    private final HandleGraphValueFactory<P, S, N, E> vf;

    public NodeRelatedStatementProvider(PathHandleGraphSail<P, S, N, E> sail, HandleGraphValueFactory<P, S, N, E> vf) {
        this.sail = sail;
        this.vf = vf;
    }

    private static final Set<IRI> linkPredicates = Set.of(VG.linksForwardToForward,
            VG.linksForwardToReverse,
            VG.linksReverseToForward,
            VG.linksReverseToReverse,
            VG.links);
    private static final Set<IRI> nodeRelatedPredicates = Set.of(VG.linksForwardToForward,
            VG.linksForwardToReverse,
            VG.linksReverseToForward,
            VG.linksReverseToReverse,
            VG.links,
            RDF.TYPE,
            RDF.VALUE);

    @Override
    public boolean predicateMightReturnValues(IRI iri) {
        return iri == null || nodeRelatedPredicates.contains(iri);
    }

    @Override
    public boolean objectMightReturnValues(Value val) {
        if (val == null) {
            return true;
        } else if (VG.Node.equals(val)) {
            return true;
        } else if (val instanceof Literal) {
            Literal lit = (Literal) val;
            return (XSD.STRING.equals(lit.getDatatype()));
        }
        return false;
    }

    @Override
    public AutoClosedIterator<Statement> getStatements(Resource subject, IRI predicate, Value object) {
        if (subject instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        NodeIRI<N> nodeSubject = nodeIriFromIRI((IRI) subject, sail);
        if (nodeSubject != null) {
            return generateTriplesForKnownNode(nodeSubject, predicate, object);
        } else if (subject == null && object == null) {
            return generateTriplesForAllNodes(predicate, object);
        } else if (object instanceof Literal && (predicate == null || RDF.VALUE.equals(predicate))) {
            return getNodeTriplesForKnownSequence(object, RDF.VALUE);
        } else if (object instanceof IRI) {
            NodeIRI<N> nodeObject = nodeIriFromIRI((IRI) object, sail);
            if (nodeObject != null) {
                sail.pathGraph().followEdgesToWardsTheRight(nodeObject.node());
            } else if (VG.Node.equals(object)) {
                AutoClosedIterator<N> nodes = sail.pathGraph().nodes();
                return map(nodes, (n) -> {
                    var ni = new NodeIRI<>(n.id(), sail);
                    return vf.createStatement(ni, RDF.TYPE, VG.Node);
                });
            }
        }
        return AutoClosedIterator.empty();
    }

    private AutoClosedIterator<Statement> generateTriplesForAllNodes(IRI predicate, Value object) {
        var nodeWithSequence = sail.pathGraph().nodesWithTheirSequence();

        //All nodes
        var to = map(nodeWithSequence, (n) -> nodeSequenceToTriples(n, predicate, object));
        var nodes = flatMap(to);
        if (predicate == null || linkPredicates.contains(predicate)) {
            var edges = sail.pathGraph().edges();
            var edgeStatements = edgesToStatements(predicate, edges);
            var i = of(nodes, edgeStatements);
            var m = map(i, AutoClosedIterator::from);
            return flatMap(m);
//            return Stream.concat(nodes, edgeStatements);
        }
        return nodes;
    }

    private AutoClosedIterator<Statement> generateTriplesForKnownNode(NodeIRI<N> nodeSubject, IRI predicate, Value object) {
        N node = nodeSubject.node();
        var typeValue = nodeToTriples(node, predicate, object);

        if ((predicate == null || linkPredicates.contains(predicate))
                && ((object instanceof IRI)
                || object == null)) {
            NodeIRI<N> nodeObject = nodeIriFromIRI((IRI) object, sail);
            var linksForNode = linksForNode(node, predicate, nodeObject);
            var typesAndLinks = of(typeValue, linksForNode);
            return flatMap(typesAndLinks);
        }
        return typeValue;
    }

    private AutoClosedIterator<Statement> getNodeTriplesForKnownSequence(Value object, IRI predicate) {
        Literal lit = (Literal) object;
        if ((lit.getDatatype() == null || lit.getDatatype() == XSD.STRING)
                && lit.getLanguage().isEmpty()) {
            String label = lit.getLabel();
            if (Sequence.stringCanBeDNASequence(label)) {
                byte[] bytes = label.getBytes(StandardCharsets.US_ASCII);
                Sequence seq = SequenceType.fromByteArray(bytes);
                var nodesWithSequence = sail.pathGraph().nodesWithSequence(seq);
                Function<N, AutoClosedIterator<Statement>> name
                        = n -> nodeToTriples(n, predicate, object);
                var map = map(nodesWithSequence, name);
                return flatMap(map);
            }
        }
        return empty();
    }

    private AutoClosedIterator<Statement> nodeSequenceToTriples(NodeSequence<N> ns, IRI predicate, Value object) {
        
        NodeIRI<N> nodeSubject = new NodeIRI<>(sail.pathGraph().asLong(ns.node()), sail);
        Supplier<Literal> p = () -> new SequenceLiteral<N, E>(ns.sequence());
        return nodeIriToTriples(predicate, object, nodeSubject, p);
    }

	private AutoClosedIterator<Statement> nodeIriToTriples(IRI predicate, Value object, NodeIRI<N> nodeSubject,
			Supplier<Literal> p) {
		if (RDF.TYPE.equals(predicate) && (object == null || VG.Node.equals(object))) {
        	return of(nodeTypeStatement(nodeSubject));
        } else if (RDF.VALUE.equals(predicate)) {
        	Literal sequence = p.get();
        	if (object == null || sequence.equals(object)) {
        		return of(vf.createStatement(nodeSubject, RDF.VALUE, sequence));
        	} else
        		return empty();
        	
        }
        Statement[] statements = new Statement[2];
        if (object == null) {
            statements[0] = nodeTypeStatement(nodeSubject);
        }
        Literal sequence = p.get();
        Statement nodeValueStatement = vf.createStatement(nodeSubject, RDF.VALUE, sequence);
        statements[1] = nodeValueStatement;
        
        var i = of(statements[0], statements[1]);
        var f = filter(i, Objects::nonNull);
        return StatementProvider.filter(object, f);
	}

    private AutoClosedIterator<Statement> nodeToTriples(N node, IRI predicate, Value object) {
        NodeIRI<N> nodeSubject = new NodeIRI<>(sail.pathGraph().asLong(node), sail);
        Supplier<Literal> p = () -> vf.createSequenceLiteral(node, sail.pathGraph());
        return nodeIriToTriples(predicate, object, nodeSubject, p);
    }

    private Statement nodeTypeStatement(NodeIRI<N> nodeSubject) {
        return vf.createStatement(nodeSubject, RDF.TYPE, VG.Node);
    }

    private AutoClosedIterator<Statement> linksForNode(N node, IRI predicate, NodeIRI<N> object) {
        PathGraph<P, S, N, E> pg = sail.pathGraph();
        AutoClosedIterator<E> asStream = pg.followEdgesToWardsTheLeft(node);
        if (object != null) {
            Predicate<E> rightMatches = e -> pg.asLong(e.right()) == object.id();
            asStream = filter(asStream, rightMatches);
        }
        return edgesToStatements(predicate, asStream);
    }

    private AutoClosedIterator<Statement> edgesToStatements(IRI predicate, AutoClosedIterator<E> asStream) {
        if (VG.linksForwardToForward.equals(predicate)) {
            return map(asStream, this::forwardToForward);
        } else if (VG.linksForwardToReverse.equals(predicate)) {
            return map(asStream, this::forwardToReverse);
        } else if (VG.linksReverseToReverse.equals(predicate)) {
            return map(asStream, this::reverseToReverse);
        } else if (VG.linksReverseToForward.equals(predicate)) {
            return map(asStream, this::reverseToForward);
        } else if (VG.links.equals(predicate)) {
            return map(asStream, this::links);
        } else {
            var map = map(asStream, e -> {
                var i = Arrays.asList(
                        forwardToForward(e),
                        forwardToReverse(e),
                        reverseToReverse(e),
                        reverseToForward(e),
                        links(e)).iterator();
                return from(i);
            });
            return filter(flatMap(map),
                    Objects::nonNull);
        }
    }

    private Statement links(E edge) {
        return vf.createStatement(new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail), VG.links, new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail));
    }

    private Statement forwardToForward(E edge) {
        if (!sail.pathGraph().isReverseNodeHandle(edge.left()) && !sail.pathGraph().isReverseNodeHandle(edge.right())) {
            return vf.createStatement(new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail), VG.linksForwardToForward, new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail));
        } else {
            return null;
        }
    }

    private Statement forwardToReverse(E edge) {
        if (!sail.pathGraph().isReverseNodeHandle(edge.left()) && sail.pathGraph().isReverseNodeHandle(edge.right())) {
            return vf.createStatement(new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail), VG.linksForwardToReverse, new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail));
        } else {
            return null;
        }
    }

    private Statement reverseToReverse(E edge) {
        if (sail.pathGraph().isReverseNodeHandle(edge.left()) && sail.pathGraph().isReverseNodeHandle(edge.right())) {
            return vf.createStatement(new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail), VG.linksReverseToReverse, new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail));
        } else {
            return null;
        }
    }

    private Statement reverseToForward(E edge) {
        if (sail.pathGraph().isReverseNodeHandle(edge.left()) && !sail.pathGraph().isReverseNodeHandle(edge.right())) {
            return vf.createStatement(new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail), VG.linksReverseToForward, new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail));
        } else {
            return null;
        }
    }

    @Override
    public double estimatePredicateCardinality(IRI predicate) {
        if (predicate == null) {
            return sail.pathGraph().nodeCount()
                    + sail.pathGraph().edgeCount();
        } else if (RDF.VALUE.equals(predicate)) {
            return 10_000; // We really prefer to go linear over all sequences
        } else if (RDF.TYPE.equals(predicate)) {
            return sail.pathGraph().nodeCount();
        } else if (linkPredicates.contains(predicate)) {
            return sail.pathGraph().edgeCount();
        } else {
            return 0;
        }
    }

    @Override
    public double estimateObjectCardinality(Value obj) {
        if (obj == null || VG.Node.equals(obj)) {
            return sail.pathGraph().nodeCount();
        } else if (obj instanceof Literal) {
            Literal lit = (Literal) obj;
            if ((XSD.STRING.equals(lit.getDatatype()))) {
                if (lit.stringValue().length() == 1) {
                    // About 60% of sequences are length one, and
                    // fast to retrieve.
                    return sail.pathGraph().nodeCount() * 0.6;
                } else {
                    return sail.pathGraph().nodeCount() * 0.4;
                }
            };
        }
        return 0;
    }

}
