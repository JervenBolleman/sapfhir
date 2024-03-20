/**
 * Copyright (c) 2020, SIB Swiss Institute of Bioinformatics
 * and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package swiss.sib.swissprot.sapfhir.statements;

import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.concat;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.empty;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.map;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.of;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.nodeIriFromIri;

import java.nio.charset.StandardCharsets;
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

import io.github.jervenbolleman.handlegraph4j.EdgeHandle;
import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import io.github.jervenbolleman.handlegraph4j.NodeSequence;
import io.github.jervenbolleman.handlegraph4j.PathGraph;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import io.github.jervenbolleman.handlegraph4j.sequences.Sequence;
import io.github.jervenbolleman.handlegraph4j.sequences.SequenceType;
import swiss.sib.swissprot.handlegraph4jrdf.VG;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import swiss.sib.swissprot.sapfhir.values.NodeIRI;
import swiss.sib.swissprot.sapfhir.values.SequenceLiteral;

/**
 * Generate the statements associated with node objects
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P>  the type of PathHandle
 * @param <S>  the type of StepHandle
 * @param <E>  the type of EdgeHandle
 * @param <N>  the type of NodeHandle
 * @param sail the sail that this provides statements from
 */
public record NodeRelatedStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>(
		PathHandleGraphSail<P, S, N, E> sail) implements StatementProvider {

	private static final Set<IRI> linkPredicates = Set.of(VG.linksForwardToForward, VG.linksForwardToReverse,
			VG.linksReverseToForward, VG.linksReverseToReverse, VG.links);
	private static final Set<IRI> nodeRelatedPredicates = Set.of(VG.linksForwardToForward, VG.linksForwardToReverse,
			VG.linksReverseToForward, VG.linksReverseToReverse, VG.links, RDF.TYPE, RDF.VALUE);

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
		} else if (val instanceof Literal lit) {
			return (XSD.STRING.equals(lit.getDatatype()));
		}
		return false;
	}

	@Override
	public AutoClosedIterator<Statement> getStatements(Resource subject, IRI predicate, Value object) {
		if (subject instanceof BNode) {
			return empty();
		}
		NodeIRI<N> nodeSubject = nodeIriFromIri((IRI) subject, sail);
		if (nodeSubject != null) {
			return generateTriplesForKnownNode(nodeSubject, predicate, object);
		} else if (subject == null && object == null) {
			return generateTriplesForAllNodes(predicate);
		} else if (object instanceof Literal lit && (predicate == null || RDF.VALUE.equals(predicate))) {
			return getNodeTriplesForKnownSequence(lit, RDF.VALUE);
		} else if (object instanceof IRI iri) {
			NodeIRI<N> nodeObject = nodeIriFromIri(iri, sail);
			if (nodeObject != null) {
				sail.pathGraph().followEdgesToWardsTheRight(nodeObject.node());
			} else if (VG.Node.equals(iri)) {
				AutoClosedIterator<N> nodes = sail.pathGraph().nodes();
				return map(nodes, (n) -> {
					var ni = new NodeIRI<>(n.id(), sail);
					return new UnsafeStatement(ni, RDF.TYPE, VG.Node);
				});
			}
		}
		return empty();
	}

	private AutoClosedIterator<Statement> generateTriplesForAllNodes(IRI predicate) {
		var nodeWithSequence = sail.pathGraph().nodesWithTheirSequence();

		if (predicate == null) {
			// All nodes
			var to = map(nodeWithSequence, (n) -> nodeSequenceToTriples(n, predicate, null));

			var nodes = flatMap(to);
			var edges = sail.pathGraph().edges();
			var edgeStatements = edgesToStatements(predicate, edges);
			return concat(nodes, edgeStatements);
		} else if (linkPredicates.contains(predicate)) {
			var edges = sail.pathGraph().edges();
			return edgesToStatements(predicate, edges);
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> generateTriplesForKnownNode(NodeIRI<N> nodeSubject, IRI predicate,
			Value object) {
		N node = nodeSubject.node();
		var typeValue = nodeToTriples(node, predicate, object);

		if ((predicate == null || linkPredicates.contains(predicate)) && ((object instanceof IRI) || object == null)) {
			NodeIRI<N> nodeObject = nodeIriFromIri((IRI) object, sail);
			var linksForNode = linksForNode(node, predicate, nodeObject);
			var typesAndLinks = concat(typeValue, linksForNode);
			return typesAndLinks;
		}
		return typeValue;
	}

	private AutoClosedIterator<Statement> getNodeTriplesForKnownSequence(Literal lit, IRI predicate) {

		if ((lit.getDatatype() == null || lit.getDatatype() == XSD.STRING) && lit.getLanguage().isEmpty()) {
			String label = lit.getLabel();
			if (Sequence.stringCanBeDNASequence(label)) {
				byte[] bytes = label.getBytes(StandardCharsets.US_ASCII);
				Sequence seq = SequenceType.fromByteArray(bytes);
				var nodesWithSequence = sail.pathGraph().nodesWithSequence(seq);
				Function<N, AutoClosedIterator<Statement>> name = n -> nodeToTriples(n, predicate, lit);
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
				return of(new UnsafeStatement(nodeSubject, RDF.VALUE, sequence));
			} else {
				return empty();
			}
		} else if (VG.Node.equals(object)){
			return of(nodeTypeStatement(nodeSubject));
		}
		Literal seq = p.get();
		if (object == null) {
			return of(nodeTypeStatement(nodeSubject), new UnsafeStatement(nodeSubject, RDF.VALUE, seq));
		} else if (seq.equals(object)){
			return of(new UnsafeStatement(nodeSubject, RDF.VALUE, seq));
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> nodeToTriples(N node, IRI predicate, Value object) {
		NodeIRI<N> nodeSubject = new NodeIRI<>(sail.pathGraph().asLong(node), sail);
		Supplier<Literal> p = () -> sail.getValueFactory().createSequenceLiteral(node, sail.pathGraph());
		return nodeIriToTriples(predicate, object, nodeSubject, p);
	}

	private Statement nodeTypeStatement(NodeIRI<N> nodeSubject) {
		return new UnsafeStatement(nodeSubject, RDF.TYPE, VG.Node);
	}

	private AutoClosedIterator<Statement> linksForNode(N node, IRI predicate, NodeIRI<N> object) {
		PathGraph<P, S, N, E> pg = sail.pathGraph();
		AutoClosedIterator<E> leftStream = pg.followEdgesToWardsTheLeft(node);
		if (object != null) {
			Predicate<E> rightMatches = e -> pg.asLong(e.right()) == object.id();
			leftStream = filter(leftStream, rightMatches);
		}
		
//		AutoClosedIterator<E> rightStream = pg.followEdgesToWardsTheRight(node);
//		if (object != null) {
//			Predicate<E> rightMatches = e -> pg.asLong(e.right()) == object.id();
//			rightStream = filter(rightStream, rightMatches);
//		}
		return edgesToStatements(predicate, leftStream);
	}

	private AutoClosedIterator<Statement> edgesToStatements(IRI predicate, AutoClosedIterator<E> asStream) {
		if (VG.linksForwardToForward.equals(predicate)) {
			return filter(map(asStream, this::forwardToForward), Objects::nonNull);
		} else if (VG.linksForwardToReverse.equals(predicate)) {
			return filter(map(asStream, this::forwardToReverse), Objects::nonNull);
		} else if (VG.linksReverseToReverse.equals(predicate)) {
			return filter(map(asStream, this::reverseToReverse), Objects::nonNull);
		} else if (VG.linksReverseToForward.equals(predicate)) {
			return filter(map(asStream, this::reverseToForward), Objects::nonNull);
		} else if (VG.links.equals(predicate)) {
			return map(asStream, this::links);
		} else {
			return flatMap(map(asStream, this::edgeToStatements));
		}
	}

	private Statement links(E edge) {
		NodeIRI<N> left = new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail);
		NodeIRI<N> right = new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail);
		return links(left, right);
	}
	
	private Statement links(NodeIRI<N> left , NodeIRI<N> right) {
		return new UnsafeStatement(left, VG.links, right);
	}

	private AutoClosedIterator<Statement> edgeToStatements(E edge) {
		NodeIRI<N> left = new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail);
		NodeIRI<N> right = new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail);
		boolean leftIsReverse = sail.pathGraph().isReverseNodeHandle(edge.left());
		boolean rightIsReverse = sail.pathGraph().isReverseNodeHandle(edge.right());
		var links = links(left, right);
		if (!leftIsReverse && !rightIsReverse) {
			return of(links, new UnsafeStatement(left, VG.linksForwardToForward, right));
		} else if (!leftIsReverse && rightIsReverse) {
			return of(links, new UnsafeStatement(left, VG.linksForwardToReverse, right));
		} else if (leftIsReverse && rightIsReverse) {
			return of(links, new UnsafeStatement(left, VG.linksReverseToReverse, right));
		} else {
//		if (leftIsReverse && !rightIsReverse) {
			return of(links, new UnsafeStatement(left, VG.linksReverseToForward, right));
		}
	}

	private Statement forwardToForward(E edge) {
		if (!sail.pathGraph().isReverseNodeHandle(edge.left()) && !sail.pathGraph().isReverseNodeHandle(edge.right())) {
			NodeIRI<N> left = new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail);
			NodeIRI<N> right = new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail);
			return new UnsafeStatement(left, VG.linksForwardToForward, right);
		} else {
			return null;
		}
	}

	private Statement forwardToReverse(E edge) {
		if (!sail.pathGraph().isReverseNodeHandle(edge.left()) && sail.pathGraph().isReverseNodeHandle(edge.right())) {
			NodeIRI<N> left = new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail);
			NodeIRI<N> right = new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail);
			return new UnsafeStatement(left, VG.linksForwardToReverse, right);
		} else {
			return null;
		}
	}

	private Statement reverseToReverse(E edge) {
		if (sail.pathGraph().isReverseNodeHandle(edge.left()) && sail.pathGraph().isReverseNodeHandle(edge.right())) {
			NodeIRI<N> left = new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail);
			NodeIRI<N> right = new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail);
			return new UnsafeStatement(left, VG.linksReverseToReverse, right);
		} else {
			return null;
		}
	}

	private Statement reverseToForward(E edge) {
		if (sail.pathGraph().isReverseNodeHandle(edge.left()) && !sail.pathGraph().isReverseNodeHandle(edge.right())) {
			NodeIRI<N> left = new NodeIRI<>(sail.pathGraph().asLong(edge.left()), sail);
			NodeIRI<N> right = new NodeIRI<>(sail.pathGraph().asLong(edge.right()), sail);
			return new UnsafeStatement(left, VG.linksReverseToForward, right);
		} else {
			return null;
		}
	}
	
	
	@Override
	public double estimatePredicateCardinality(IRI predicate) {
		if (predicate == null) {
			return sail.pathGraph().nodeCount() + sail.pathGraph().edgeCount();
		} else if (RDF.VALUE.equals(predicate)) {
			return sail.pathGraph().nodeCount() * 10; // We really prefer to go linear over all sequences
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
			}
			;
		}
		return 0;
	}

}
