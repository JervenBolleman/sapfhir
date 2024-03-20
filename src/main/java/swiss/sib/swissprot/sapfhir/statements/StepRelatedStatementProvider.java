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
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.of;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.filter;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.pathIriFromIri;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.stepIriFromIri;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

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
import swiss.sib.swissprot.sapfhir.values.HandleGraphValueFactory;
import swiss.sib.swissprot.sapfhir.values.NodeIRI;
import swiss.sib.swissprot.sapfhir.values.PathIRI;
import swiss.sib.swissprot.sapfhir.values.StepBeginPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepEndPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepIRI;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public class StepRelatedStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>
		implements StatementProvider {

	/**
	 * Value factory for literals and iris
	 */
	private final HandleGraphValueFactory<P, S, N, E> vf;

	/**
	 * backing sail
	 */
	private final PathHandleGraphSail<P, S, N, E> sail;

	public StepRelatedStatementProvider(PathHandleGraphSail<P, S, N, E> sail, HandleGraphValueFactory<P, S, N, E> vf) {
		this.vf = vf;
		this.sail = sail;
	}

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
		} else if (val instanceof IRI) {
			IRI iri = (IRI) val;
			if (stepAssociatedTypes.contains(iri)) {
				return true;
			} else if (iri.stringValue().contains("/step/")) {
				// TODO: Test if it can be a StepBegin Or StepEnd by pattern
				return true;
			} else if (pathIriFromIri((IRI) val, sail) != null) {
				return true;
			}
		} else if (val instanceof Literal) {
			Literal lit = (Literal) val;
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
			return AutoClosedIterator.empty();
		}
	}

	private static final IRI[] knownPredicates = new IRI[] { RDF.TYPE, VG.rank, VG.node, VG.reverseOfNode, VG.path,
			FALDO.begin, FALDO.end };
	private final List<BiFunction<StepIRI<P>, Value, AutoClosedIterator<Statement>>> knownPredicateFunctions = List.of(
			this::knownSubjectTypeStatement, this::knownSubjectRankStatements, this::knownSubjectNodeStatements,
			this::knownSubjectReverseNodeStatements, this::knownSubjectPathStatements,
			this::knownSubjectBeginStatements, this::knownSubjectEndStatements);

	private AutoClosedIterator<Statement> knownSubject(IRI subject, IRI predicate, Value object) {
		StepIRI<P> stepSubject = stepIriFromIri((IRI) subject, sail);
		// If null it is not a Step IRI and therefore can't match the values here.
		if (stepSubject == null) {
			return AutoClosedIterator.empty();
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
		for (int i = 0; i < knownPredicates.length; i++) {
			IRI knownPredicate = knownPredicates[i];
			if (knownPredicate.equals(predicate)) {
				return knownPredicateFunctions.get(i).apply(stepSubject, object);
			}
		}
		return AutoClosedIterator.empty();
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
			return AutoClosedIterator.empty();
		}
		if (VG.Step.equals(object)) {
			return of(new HandleGraphValueFactory.UnsafeStatement(subject, RDF.TYPE, VG.Step));
		} else if (FALDO.Region.equals(object)) {
			return of(new HandleGraphValueFactory.UnsafeStatement(subject, RDF.TYPE, FALDO.Region));
		} else {
			AutoClosedIterator<Statement> stream = concat(of(new HandleGraphValueFactory.UnsafeStatement(subject, RDF.TYPE, VG.Step)),
					of(new HandleGraphValueFactory.UnsafeStatement(subject, RDF.TYPE, FALDO.Region)));
			return filter(object, stream);
		}
	}

	private AutoClosedIterator<Statement> knownSubjectRankStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof IRI || object instanceof BNode) {
			return AutoClosedIterator.empty();
		}
		AutoClosedIterator<Statement> stream = AutoClosedIterator
				.of(new HandleGraphValueFactory.UnsafeStatement(stepSubject, VG.rank, vf.createLiteral(stepSubject.rank())));
		return filter(object, stream);
	}

	private AutoClosedIterator<Statement> knownSubjectNodeStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return AutoClosedIterator.empty();
		}
		S step = sail.pathGraph().stepByRankAndPath(stepSubject.path(), stepSubject.rank());
		N node = sail.pathGraph().nodeOfStep(step);
		if (sail.pathGraph().isReverseNodeHandle(node)) {
			return AutoClosedIterator.empty();
		}
		NodeIRI<N> nodeIRI = new NodeIRI<>(node.id(), sail);
		AutoClosedIterator<Statement> stream = AutoClosedIterator.of(new HandleGraphValueFactory.UnsafeStatement(stepSubject, VG.node, nodeIRI));
		return filter(object, stream);
	}

	private AutoClosedIterator<Statement> knownSubjectReverseNodeStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return AutoClosedIterator.empty();
		}
		S step = sail.pathGraph().stepByRankAndPath(stepSubject.path(), stepSubject.rank());
		N node = sail.pathGraph().nodeOfStep(step);
		if (!sail.pathGraph().isReverseNodeHandle(node)) {
			return AutoClosedIterator.empty();
		}
		NodeIRI<N> nodeIRI = new NodeIRI<>(node.id(), sail);
		AutoClosedIterator<Statement> stream = AutoClosedIterator
				.of(new HandleGraphValueFactory.UnsafeStatement(stepSubject, VG.reverseOfNode, nodeIRI));
		return filter(object, stream);
	}

	private AutoClosedIterator<Statement> knownSubjectPathStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return AutoClosedIterator.empty();
		}
		PathIRI<P> path = new PathIRI<>(stepSubject.path(), sail);
		AutoClosedIterator<Statement> stream = AutoClosedIterator.of(new HandleGraphValueFactory.UnsafeStatement(stepSubject, VG.path, path));
		return filter(object, stream);
	}

	private AutoClosedIterator<Statement> knownSubjectEndStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return AutoClosedIterator.empty();
		}
		long rank = stepSubject.rank();
		P path = stepSubject.path();
		var beginIRI = new StepEndPositionIRI<>(path, rank, sail);
		AutoClosedIterator<Statement> stream = AutoClosedIterator.of(new HandleGraphValueFactory.UnsafeStatement(stepSubject, FALDO.begin, beginIRI));
		return filter(object, stream);
	}

	private AutoClosedIterator<Statement> knownSubjectBeginStatements(StepIRI<P> stepSubject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return AutoClosedIterator.empty();
		}
		long rank = stepSubject.rank();
		P path = stepSubject.path();
		var beginIRI = new StepBeginPositionIRI<>(path, rank, sail);
		AutoClosedIterator<Statement>  stream = AutoClosedIterator.of(new HandleGraphValueFactory.UnsafeStatement(stepSubject, FALDO.begin, beginIRI));
		return filter(object, stream);
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
