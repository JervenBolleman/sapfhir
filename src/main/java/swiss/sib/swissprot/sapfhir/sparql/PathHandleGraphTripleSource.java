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
package swiss.sib.swissprot.sapfhir.sparql;

import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator.map;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;

import io.github.jervenbolleman.handlegraph4j.EdgeHandle;
import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.sapfhir.statements.NodeRelatedStatementProvider;
import swiss.sib.swissprot.sapfhir.statements.PathRelatedStatementProvider;
import swiss.sib.swissprot.sapfhir.statements.StatementProvider;
import swiss.sib.swissprot.sapfhir.statements.StepPositionStatementProvider;
import swiss.sib.swissprot.sapfhir.statements.StepRelatedStatementProvider;
import swiss.sib.swissprot.sapfhir.values.HandleGraphValueFactory;

/**
 * Generate the triples for the different possible values htat can be asked for.
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class PathHandleGraphTripleSource<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>
		implements TripleSource {

	private final HandleGraphValueFactory<P, S, N, E> vf;
	private final List<StatementProvider> statementProviders;

	/**
	 * A triple source for a certain sail
	 * 
	 * @param sail the handlegraph backed sail we extract data from
	 */
	public PathHandleGraphTripleSource(PathHandleGraphSail<P, S, N, E> sail) {
		this.vf = sail.getValueFactory();
		this.statementProviders = List.of(new StepPositionStatementProvider<>(sail),
				new NodeRelatedStatementProvider<>(sail), new StepRelatedStatementProvider<>(sail),
				new PathRelatedStatementProvider<>(sail));
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(Resource subject, IRI predicate, Value object,
			Resource... rsrcs) throws QueryEvaluationException {
		// We know that we don't have any blanknodes
		if (subject instanceof BNode || object instanceof BNode) {
			return new EmptyIteration<>();
		}
		var from = from(statementProviders.iterator());
		var subs = filter(from, sp -> sp.subjectMightReturnValues(subject));
		var preds = filter(subs, sp -> sp.predicateMightReturnValues(predicate));
		var obs = filter(preds, sp -> sp.objectMightReturnValues(object));

		var generateStatements = map(obs, ps -> statements(ps, subject, predicate, object));
		return new CloseableIterationFromAutoClosedIterator(flatMap(generateStatements));
	}

	private static AutoClosedIterator<Statement> statements(StatementProvider ps, Resource subject, IRI predicate,
			Value object) {
		return ps.getStatements(subject, predicate, object);
	}

	@Override
	public HandleGraphValueFactory<P, S, N, E> getValueFactory() {
		return vf;
	}

	/**
	 * Estimate how many statements match the BGP
	 * 
	 * @param subj the subject to estimate for
	 * @param pred the predicate to estimate for
	 * @param obj  the object to estimate for
	 * @return an estimate
	 */
	public double estimateCardinality(Resource subj, IRI pred, Value obj) {
		double estimate = 0;
		for (StatementProvider sp : statementProviders) {
			estimate = Math.max(estimate, sp.estimateCardinality(subj, pred, obj));
		}
		return estimate;
	}

	private static class CloseableIterationFromAutoClosedIterator implements CloseableIteration<Statement> {

		private final AutoClosedIterator<Statement> providedAsIter;
		private Statement last;

		public CloseableIterationFromAutoClosedIterator(AutoClosedIterator<Statement> providedAsIter) {
			this.providedAsIter = providedAsIter;
		}

		@Override
		public void close() throws QueryEvaluationException {
			providedAsIter.close();
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			return providedAsIter.hasNext();
		}

		@Override
		public Statement next() throws QueryEvaluationException {
			Statement next = providedAsIter.next();
			assert next != null : "null after" + last;
			last = next;
			return next;
		}

		@Override
		public void remove() throws QueryEvaluationException {
			providedAsIter.remove();
		}
	}
}
