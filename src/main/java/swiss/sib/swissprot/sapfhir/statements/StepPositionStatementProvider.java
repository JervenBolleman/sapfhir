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
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.map;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.of;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.filter;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.pathIriFromIri;
import static swiss.sib.swissprot.sapfhir.values.StepPositionIRI.POSITION;

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
import io.github.jervenbolleman.handlegraph4j.PathGraph;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4jrdf.FALDO;
import swiss.sib.swissprot.handlegraph4jrdf.VG;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import swiss.sib.swissprot.sapfhir.values.PathIRI;
import swiss.sib.swissprot.sapfhir.values.StepBeginPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepEndPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepPositionIRI;

/**
 * Provide the steps and their paths statements
 * 
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P>  the type of PathHandle
 * @param <S>  the type of StepHandle
 * @param <E>  the type of EdgeHandle
 * @param <N>  the type of NodeHandle
 * @param sail the sail that this provides statements from
 */
public record StepPositionStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>(
		PathHandleGraphSail<P, S, N, E> sail) implements StatementProvider {

	private static final Set<IRI> types = Set.of(FALDO.Position, FALDO.ExactPosition);
	private static final Set<IRI> predicates = Set.of(RDF.TYPE, FALDO.position, FALDO.reference);

	@Override
	public boolean predicateMightReturnValues(IRI iri) {
		return iri == null || predicates.contains(iri);
	}

	@Override
	public boolean objectMightReturnValues(Value val) {
		if (val == null) {
			return true;
		} else if (val instanceof Literal lit) {
			return (XMLDatatypeUtil.isNumericDatatype(lit.getDatatype()));
		} else if (val instanceof PathIRI) {
			return true;
		} else if (val instanceof IRI iri) {
			return types.contains(iri) || pathIriFromIri(iri, sail) != null;
		}
		return false;
	}

	@Override
	public AutoClosedIterator<Statement> getStatements(Resource subject, IRI predicate, Value object) {
		if (subject == null && (object == null || !(object instanceof BNode))) {
			var pg = sail.pathGraph();
			var paths = pg.paths();
			if (!paths.hasNext()) {
				return empty();
			}
			var flattened = new PositionMaintainingStepIRIGenerator(paths, pg);
			var known = map(flattened, be -> concat(knownSubject(be.begin(), predicate, object),
					knownSubject(be.end(), predicate, object)));
			return flatMap(known);
		} else if (subject instanceof IRI subjectIri) {
			return knownSubject(subjectIri, predicate, object);
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubject(IRI subject, IRI predicate, Value object) {
		StepPositionIRI<P, S> stepSubject = beginOrEndIriFromIri((IRI) subject);
		// If null it is not a Step IRI and therefore can't match the values here.
		if (stepSubject == null) {
			return empty();
		} else if (RDF.TYPE.equals(predicate)) {
			return knownSubjectTypeStatement(stepSubject, object);
		} else if (FALDO.reference.equals(predicate)) {
			return knownSubjectReferenceStatements(stepSubject, object);
		} else if (FALDO.position.equals(predicate)) {
			return knownSubjectPositionStatements(stepSubject, object);
		} else if (predicate == null) {
			var typeStatement = knownSubjectTypeStatement(stepSubject, object);
			var referenceStatements = knownSubjectReferenceStatements(stepSubject, object);
			var positionStatements = knownSubjectPositionStatements(stepSubject, object);
			return concat(typeStatement, concat(referenceStatements, positionStatements));
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectTypeStatement(StepPositionIRI<P, S> subject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return empty();
		}
		UnsafeStatement position = new UnsafeStatement(subject, RDF.TYPE, FALDO.Position);
		UnsafeStatement exactPosition = new UnsafeStatement(subject, RDF.TYPE, FALDO.ExactPosition);
		if (object == null) {
			return of(position, exactPosition);
		} else if (FALDO.Position.equals(object)) {
			return of(position);
		} else if (FALDO.ExactPosition.equals(object)) {
			return of(exactPosition);
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectReferenceStatements(StepPositionIRI<P, S> subject, Value object) {
		if (object instanceof Literal || object instanceof BNode) {
			return empty();
		}
		PathIRI<P> pathIRI = new PathIRI<>(subject.path(), sail);
		if (object == null || pathIRI.equals(object)) {
			return of(new UnsafeStatement(subject, VG.path, pathIRI));
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectPositionStatements(StepPositionIRI<P, S> subject, Value object) {
		if (object instanceof IRI || object instanceof BNode) {
			return empty();
		}
		AutoClosedIterator<Statement> stream;
		if (subject instanceof StepBeginPositionIRI) {
			long position = ((StepBeginPositionIRI<?, ?>) subject).getBeginPosition();
			var l = sail.getValueFactory().createLiteral(position);
			stream = of(new UnsafeStatement(subject, FALDO.position, l));
		} else if (subject instanceof StepEndPositionIRI) {
			long position = ((StepEndPositionIRI<?, ?>) subject).getEndPosition();
			var l = sail.getValueFactory().createLiteral(position);
			stream = of(new UnsafeStatement(subject, FALDO.position, l));
		} else {
			return empty();
		}
		return filter(object, stream);
	}

	private StepPositionIRI<P, S> beginOrEndIriFromIri(IRI iri) {
		if (iri == null) {
			return null;
		} else if (iri instanceof StepPositionIRI spi) {
			return spi;
		}
		String stringValue = iri.stringValue();
		int lastIndexOf = stringValue.lastIndexOf(POSITION);
		if (lastIndexOf > 0) {
			String pathPart = stringValue.substring(0, lastIndexOf);
			P p = sail.pathFromIriString(pathPart);
			return positionToStep(pathPart, lastIndexOf, p);
		}
		return null;
	}

	private StepPositionIRI<P, S> positionToStep(String pathPart, int lastIndexOf, P p) {
		try {
			int pathPartStart = lastIndexOf + POSITION.length();
			String positionPart = pathPart.substring(pathPartStart);
			long position = Long.parseLong(positionPart);
			if (p != null) {
				PathGraph<P, S, N, E> pathGraph = sail.pathGraph();
				S begin = pathGraph.stepOfPathByBeginPosition(p, position);
				if (begin != null) {
					long rank = pathGraph.rankOfStep(begin);
					return new StepBeginPositionIRI<>(p, rank, sail);
				}
				S end = pathGraph.stepOfPathByEndPosition(p, position);
				if (end != null) {
					long rank = pathGraph.rankOfStep(end);
					return new StepEndPositionIRI<>(p, rank, sail);
				}
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return null;
	}

	private record StepBeginAndEndIris<P extends PathHandle, S extends StepHandle>(StepBeginPositionIRI<P, S> begin,
			StepEndPositionIRI<P, S> end) {

	}

	private class PositionMaintainingStepIRIGenerator implements AutoClosedIterator<StepBeginAndEndIris<P, S>> {

		private final AutoClosedIterator<P> paths;
		private final PathGraph<P, S, N, E> pg;
		P path;
		AutoClosedIterator<S> steps;
		long beginPosition = 0;
		long rank = 0;

		public PositionMaintainingStepIRIGenerator(AutoClosedIterator<P> paths, PathGraph<P, S, N, E> pg) {
			this.paths = paths;
			this.pg = pg;
			this.path = paths.next();
			this.steps = pg.stepsOf(path);
		}

		@Override
		public void close() {
			steps.close();
			paths.close();
		}

		@Override
		public boolean hasNext() {
			if (steps.hasNext()) {
				return true;
			}
			while (paths.hasNext()) {
				path = paths.next();
				steps.close();
				steps = pg.stepsOf(path);
				if (steps.hasNext()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public StepBeginAndEndIris<P, S> next() {
			S s = steps.next();
			int seqln = pg.sequenceLengthOf(pg.nodeOfStep(s));

			var b = new StepBeginPositionIRI<>(path, rank, sail, beginPosition);
			long endPosition = beginPosition + seqln;
			var e = new StepEndPositionIRI<>(path, rank, sail, endPosition);
			beginPosition = endPosition + 1;
			rank++;
			return new StepBeginAndEndIris<>(b, e);
		}
	}

	@Override
	public double estimatePredicateCardinality(IRI predicate) {
		if (predicate == null || predicates.contains(predicate)) {
			return sail.pathGraph().stepCount() * 2;
		} else {
			return 0;
		}
	}
}
