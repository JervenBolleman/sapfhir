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
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.of;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.pathIriFromIri;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.stepIriFromIri;

import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import io.github.jervenbolleman.handlegraph4j.EdgeHandle;
import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4jrdf.FALDO;
import swiss.sib.swissprot.handlegraph4jrdf.VG;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import swiss.sib.swissprot.sapfhir.values.NodeIRI;
import swiss.sib.swissprot.sapfhir.values.PathIRI;
import swiss.sib.swissprot.sapfhir.values.StepBeginPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepEndPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepIRI;

/**
 * Provides the statements related to steps.
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P>  the type of PathHandle
 * @param <S>  the type of StepHandle
 * @param <E>  the type of EdgeHandle
 * @param <N>  the type of NodeHandle
 * @param sail the sail that this provides statements from
 */
public record StepRelatedStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>(
		PathHandleGraphSail<P, S, N, E> sail) implements StatementProvider {

	private static final Set<IRI> stepAssociatedTypes = Set.of(FALDO.Region, VG.Step);

	private static final Set<IRI> stepAssociatedPredicates = Set.of(RDF.TYPE, VG.rank, VG.path, VG.node,
			VG.reverseOfNode, FALDO.begin, FALDO.end);

	@Override
	public boolean predicateMightReturnValues(IRI iri) {
		return iri == null || stepAssociatedPredicates.contains(iri);
	}

	@Override
	public boolean objectMightReturnValues(Value val) {
		if (val instanceof StepBeginPositionIRI || val instanceof StepEndPositionIRI) {
			return true;
		} else if (val instanceof IRI iri) {
			if (stepAssociatedTypes.contains(iri)) {
				return true;
			} else if (pathIriFromIri(iri, sail) != null) {
				return true;
			} else if (stepIriFromIri(iri, sail) != null) {
				return true;
			} else if (StatementProvider.nodeIriFromIri(iri, sail) != null) {
				return true;
			} else if (iri.stringValue().contains("/step/")) {
				// TODO: Test if it can be a StepBegin Or StepEnd by pattern
				return true;
			}
		} else if (val instanceof Literal lit) {
			return (XMLDatatypeUtil.isNumericDatatype(lit.getDatatype()));
		}
		return val == null;
	}

	@Override
	public AutoClosedIterator<Statement> getStatements(Resource subject, IRI predicate, Value object) {
		if (subject == null && (object == null || object instanceof IRI)) {
			AutoClosedIterator<S> steps = sail.pathGraph().steps();
			var map = AutoClosedIterator.map(steps, s -> {
				P path = sail.pathGraph().pathOfStep(s);
				long rank = sail.pathGraph().rankOfStep(s);
				StepIRI<P> stepIRI = new StepIRI<>(path, rank, sail);
				return getStatements(stepIRI, predicate, object);
			});
			return AutoClosedIterator.flatMap(map);
		} else if (subject instanceof IRI) {
			return knownSubject((IRI) subject, predicate, object);
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubject(IRI subject, IRI predicate, Value object) {
		StepIRI<P> stepSubject = stepIriFromIri((IRI) subject, sail);
		// If null it is not a Step IRI and therefore can't match the values here.
		if (stepSubject == null) {
			return empty();
		} else if (predicate == null) {
			var typeStatement = knownSubjectTypeStatement(stepSubject, object);
			var rankStatements = knownSubjectRankStatements(stepSubject, object);
			var nodeStatements = knownSubjectNodeStatements(stepSubject, object);
			var pathStatements = knownSubjectPathStatements(stepSubject, object);
			var reverseNodeStatements = knownSubjectReverseNodeStatements(stepSubject, object);
			return concat(concat(concat(typeStatement, rankStatements), pathStatements),
					concat(nodeStatements, reverseNodeStatements));
		} else {
			AutoClosedIterator<Statement> found = findByIdentity(predicate, object, stepSubject);
			if (found != null) {
				return found;
			} else {
				return findByEquals(predicate, object, stepSubject);
			}
		}
	}

	private AutoClosedIterator<Statement> findByEquals(IRI predicate, Value object, StepIRI<P> stepSubject) {
		if (RDF.TYPE.equals(predicate)) {
			return knownSubjectTypeStatement(stepSubject, object);
		} else if (VG.rank.equals(predicate)) {
			return knownSubjectRankStatements(stepSubject, object);
		} else if (VG.node.equals(predicate)) {
			return knownSubjectNodeStatements(stepSubject, object);
		} else if (VG.reverseOfNode.equals(predicate)) {
			return knownSubjectReverseNodeStatements(stepSubject, object);
		} else if (VG.path.equals(predicate)) {
			return knownSubjectPathStatements(stepSubject, object);
		} else if (FALDO.begin.equals(predicate)) {
			return knownSubjectBeginStatements(stepSubject, object);
		} else if (FALDO.end.equals(predicate)) {
			return knownSubjectEndStatements(stepSubject, object);
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> findByIdentity(IRI predicate, Value object, StepIRI<P> stepSubject) {
		if (RDF.TYPE == predicate) {
			return knownSubjectTypeStatement(stepSubject, object);
		} else if (VG.rank == predicate) {
			return knownSubjectRankStatements(stepSubject, object);
		} else if (VG.node == predicate) {
			return knownSubjectNodeStatements(stepSubject, object);
		} else if (VG.reverseOfNode == predicate) {
			return knownSubjectReverseNodeStatements(stepSubject, object);
		} else if (VG.path == predicate) {
			return knownSubjectPathStatements(stepSubject, object);
		} else if (FALDO.begin == predicate) {
			return knownSubjectBeginStatements(stepSubject, object);
		} else if (FALDO.end == predicate) {
			return knownSubjectEndStatements(stepSubject, object);
		} else {
			return null;
		}
	}

	private AutoClosedIterator<Statement> knownSubjectTypeStatement(StepIRI<P> subject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return empty();
		}
		if (VG.Step.equals(object)) {
			return of(new UnsafeStatement(subject, RDF.TYPE, VG.Step));
		} else if (FALDO.Region.equals(object)) {
			return of(new UnsafeStatement(subject, RDF.TYPE, FALDO.Region));
		} else if (object == null) {
			return of(new UnsafeStatement(subject, RDF.TYPE, VG.Step),
					new UnsafeStatement(subject, RDF.TYPE, FALDO.Region));
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectRankStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof IRI || object instanceof BNode) {
			return empty();
		}
		Literal rankLit = sail.getValueFactory().createLiteral(stepSubject.rank());
		if (object == null || rankLit.equals(object)) {
			return of(new UnsafeStatement(stepSubject, VG.rank, rankLit));
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectNodeStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return empty();
		}
		S step = sail.pathGraph().stepByRankAndPath(stepSubject.path(), stepSubject.rank());
		N node = sail.pathGraph().nodeOfStep(step);
		if (sail.pathGraph().isReverseNodeHandle(node)) {
			return empty();
		}
		NodeIRI<N> nodeIRI = new NodeIRI<>(node.id(), sail);
		if (object == null || nodeIRI.equals(object)) {
			return of(new UnsafeStatement(stepSubject, VG.node, nodeIRI));
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectReverseNodeStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return empty();
		}
		S step = sail.pathGraph().stepByRankAndPath(stepSubject.path(), stepSubject.rank());
		N node = sail.pathGraph().nodeOfStep(step);
		if (!sail.pathGraph().isReverseNodeHandle(node)) {
			return empty();
		}
		NodeIRI<N> nodeIRI = new NodeIRI<>(node.id(), sail);
		if (object == null || nodeIRI.equals(object)) {
			return of(new UnsafeStatement(stepSubject, VG.reverseOfNode, nodeIRI));
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectPathStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return empty();
		}
		PathIRI<P> path = new PathIRI<>(stepSubject.path(), sail);
		if (object == null || path.equals(object)) {
			return of(new UnsafeStatement(stepSubject, VG.path, path));
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectEndStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return empty();
		}
		long rank = stepSubject.rank();
		P path = stepSubject.path();
		var beginIRI = new StepEndPositionIRI<>(path, rank, sail);
		if (object == null || beginIRI.equals(object)) {
			return of(new UnsafeStatement(stepSubject, FALDO.end, beginIRI));
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectBeginStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return empty();
		}
		long rank = stepSubject.rank();
		P path = stepSubject.path();
		var beginIRI = new StepBeginPositionIRI<>(path, rank, sail);
		if (object == null || beginIRI.equals(object)) {
			return of(new UnsafeStatement(stepSubject, FALDO.begin, beginIRI));
		} else {
			return empty();
		}
	}

	@Override
	public double estimatePredicateCardinality(IRI predicate) {
		if (predicate == null) {
			return sail.pathGraph().stepCount() * 5;
		} else if (FALDO.begin.equals(predicate) || FALDO.end.equals(predicate)) {
			return sail.pathGraph().stepCount() * 4;
		} else if (stepAssociatedPredicates.contains(predicate)) {
			return sail.pathGraph().stepCount();
		} else {
			return 0;
		}
	}
}
