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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.BindingAssignerOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.CompareOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ConjunctiveConstraintSplitterOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ConstantOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.DisjunctiveConstraintOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.IterativeEvaluationOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ProjectionRemovalOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryJoinOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryModelNormalizerOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.RegexAsStringFunctionOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.SameTermFilterOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;

import io.github.jervenbolleman.handlegraph4j.EdgeHandle;
import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import swiss.sib.swissprot.handlegraph4jrdf.FALDO;
import swiss.sib.swissprot.handlegraph4jrdf.VG;

/**
 * A SailConnection knowing about the specifics in a handlegraph and optimizing
 * for those use cases.
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class PathHandleGraphTripleSailConnection<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>
		extends AbstractSailConnection {

	private final PathHandleGraphSail<P, S, N, E> phg;
	private final SPARQLServiceResolver fd;

	/**
	 * A sail connection for a certain handlegraph
	 * 
	 * @param phg the handle graph that we generate triples for etc.
	 */
	public PathHandleGraphTripleSailConnection(PathHandleGraphSail<P, S, N, E> phg) {
		super(phg);
		this.phg = phg;
		this.fd = new SPARQLServiceResolver();
	}

	@Override
	protected void closeInternal() throws SailException {
		// Does nothing
	}

	@Override
	protected CloseableIteration<? extends BindingSet> evaluateInternal(TupleExpr tupleExpr, Dataset dataset,
			BindingSet bindings, boolean includeInferred) throws SailException {
		try {

			var tripleSource = tripleSource();
			var strategy = evalutationStrategy(tripleSource);
			tupleExpr = optimize(tripleSource, strategy, tupleExpr, bindings);
			return strategy.precompile(tupleExpr).evaluate(bindings);
		} catch (QueryEvaluationException e) {
			throw new SailException(e);
		}
	}

	EvaluationStrategy evalutationStrategy(PathHandleGraphTripleSource<P, S, N, E> tripleSource) {
		return new DefaultEvaluationStrategy(tripleSource, fd);
	}

	PathHandleGraphTripleSource<P, S, N, E> tripleSource() {
		var tripleSource = new PathHandleGraphTripleSource<>(phg);
		return tripleSource;
	}

	TupleExpr optimize(PathHandleGraphTripleSource<P, S, N, E> tripleSource, EvaluationStrategy strategy,
			TupleExpr tupleExpr, BindingSet bindings) {
		var evStats = new PathHandleEvaluationStatistics<>(tripleSource);
		var queryOptimizer = new PathHandleQueryOptimizerPipeline<>(strategy, tripleSource, evStats);
		strategy.setOptimizerPipeline(queryOptimizer);
		var optimizedTupleExpr = strategy.optimize(tupleExpr, evStats, bindings);
		return optimizedTupleExpr;
	}

	@Override
	protected CloseableIteration<? extends Resource> getContextIDsInternal() throws SailException {
		return new EmptyIteration<>();
	}

	@Override
	protected CloseableIteration<? extends Statement> getStatementsInternal(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	@Override
	protected long sizeInternal(Resource... contexts) throws SailException {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	@Override
	protected void startTransactionInternal() throws SailException {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	@Override
	protected void commitInternal() throws SailException {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	@Override
	protected void rollbackInternal() throws SailException {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	@Override
	protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	@Override
	protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
																		// Tools | Templates.
	}

	@Override
	protected CloseableIteration<? extends Namespace> getNamespacesInternal() throws SailException {
		Iterator<SimpleNamespace> ns = List.of(new SimpleNamespace(FALDO.PREFIX, FALDO.NAMESPACE),
				new SimpleNamespace(VG.PREFIX, VG.NAMESPACE), new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE))
				.iterator();
		return new CloseableIteratorIteration<>(ns);
	}

	@Override
	protected String getNamespaceInternal(String prefix) throws SailException {
		switch (prefix) {
		case FALDO.PREFIX:
			return FALDO.NAMESPACE;
		case VG.PREFIX:
			return VG.NAMESPACE;
		default:
			return null;
		}
	}

	@Override
	protected void setNamespaceInternal(String prefix, String name) throws SailException {
		throw new UnsupportedOperationException("Read only.");
	}

	@Override
	protected void removeNamespaceInternal(String prefix) throws SailException {
		throw new UnsupportedOperationException("Read only.");
	}

	@Override
	protected void clearNamespacesInternal() throws SailException {
		throw new UnsupportedOperationException("Read only.");
	}

	private static class PathHandleQueryOptimizerPipeline<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>
			implements QueryOptimizerPipeline {

		private final PathHandleGraphTripleSource<P, S, N, E> ts;
		private final EvaluationStrategy strategy;
		private final PathHandleEvaluationStatistics<P, S, N, E> ev;

		public PathHandleQueryOptimizerPipeline(EvaluationStrategy strategy, PathHandleGraphTripleSource<P, S, N, E> ts,
				PathHandleEvaluationStatistics<P, S, N, E> ev) {
			this.ts = ts;
			this.strategy = strategy;
			this.ev = ev;
		}

		@Override
		public Iterable<QueryOptimizer> getOptimizers() {
			return Arrays.asList(new KnownValuesToConstantsPointerOptimizer(), new BindingAssignerOptimizer(),
					new ConstantOptimizer(strategy), new RegexAsStringFunctionOptimizer(ts.getValueFactory()),
					new CompareOptimizer(), new ConjunctiveConstraintSplitterOptimizer(),
					new DisjunctiveConstraintOptimizer(), new SameTermFilterOptimizer(),
					new QueryModelNormalizerOptimizer(), new QueryJoinOptimizer(ev), new IterativeEvaluationOptimizer(),
//                    new FilterOptimizer(),
					new ProjectionRemovalOptimizer());
		}

	}

	private static final Map<IRI, IRI> knowns = new HashMap<>();
	static {

		addIrisFromVocabClass(FALDO.class);
		addIrisFromVocabClass(VG.class);
		addIrisFromVocabClass(RDF.class);
		addIrisFromVocabClass(RDFS.class);
		addIrisFromVocabClass(OWL.class);
	}

	private static void addIrisFromVocabClass(Class<?> clazz) {
		for (Field f : clazz.getDeclaredFields()) {
			if (f.canAccess(null)) {
				Object object;
				try {
					object = f.get(null);
					if (object instanceof IRI i) {
						knowns.put(i, i);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// Do nothing this is not critical, it is only for performance
				}
			}
		}
	}

	private static class KnownValuesToConstantsPointerOptimizer implements QueryOptimizer {

		@Override
		public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
			QueryModelVisitor<Exception> v = new AbstractQueryModelVisitor<>() {

				@Override
				public void meet(Var node) throws Exception {
					if (node.isConstant() && node.getValue() instanceof IRI) {
						IRI iri = knowns.get((IRI) node.getValue());
						if (iri != null) {
							String name = node.getName();

							boolean anon = node.isAnonymous();

							Var replacement = new Var(name, iri, anon);
							if (node.isCardinalitySet()) {
								double car = node.getCardinality();
								replacement.setCardinality(car);
							}
							double estimate = node.getCostEstimate();
							replacement.setCostEstimate(estimate);
							node.getParentNode().replaceChildNode(node, replacement);
							node = replacement;
						}

					}
					super.meet(node);
				}

			};
			try {
				tupleExpr.visit(v);
			} catch (Exception e) {
				throw new QueryEvaluationException(e);
			}
		}

	}

}
