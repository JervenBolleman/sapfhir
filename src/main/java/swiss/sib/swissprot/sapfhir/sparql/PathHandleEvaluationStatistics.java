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

import java.io.IOException;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

import io.github.jervenbolleman.handlegraph4j.EdgeHandle;
import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;

/**
 * Specialized statistics for query planning
 * 
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
class PathHandleEvaluationStatistics<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>
		extends EvaluationStatistics {

	private final PathHandleGraphTripleSource<P, S, N, E> ts;

	/**
	 * 
	 * @param ts the triple source from which the statistics are calculated
	 */
	PathHandleEvaluationStatistics(PathHandleGraphTripleSource<P, S, N, E> ts) {
		this.ts = ts;
	}

	@Override
	protected PathHandleCardinalityCalculator createCardinalityCalculator() {
		return new PathHandleCardinalityCalculator();
	}

	protected class PathHandleCardinalityCalculator extends CardinalityCalculator {

		@Override
		protected double getCardinality(StatementPattern sp) {
			try {
				Value subj = getConstantValue(sp.getSubjectVar());
				if (!(subj instanceof Resource)) {
					// can happen when a previous optimizer has inlined a comparison operator.
					// this can cause, for example, the subject variable to be equated to a literal
					// value.
					// See SES-970
					subj = null;
				}
				Value pred = getConstantValue(sp.getPredicateVar());
				if (!(pred instanceof IRI)) {
					// can happen when a previous optimizer has inlined a comparison operator. See
					// SES-970
					pred = null;
				}
				Value obj = getConstantValue(sp.getObjectVar());
				Value context = getConstantValue(sp.getContextVar());
				if (!(context instanceof Resource)) {
					// can happen when a previous optimizer has inlined a comparison operator. See
					// SES-970
					context = null;
				}
				return cardinality((Resource) subj, (IRI) pred, obj, (Resource) context);
			} catch (IOException e) {
				return super.getCardinality(sp);
			}
		}

		private double cardinality(Resource subj, IRI pred, Value obj, Resource context) throws IOException {
			if (subj instanceof BNode || obj instanceof BNode || context != null || RDF4J.NIL.equals(context)) {
				return 0;
			}
			double estimate = Double.MAX_VALUE;
			estimate = Math.min(estimate, ts.estimateCardinality(subj, pred, obj));
			return estimate;
		}

		protected Value getConstantValue(Var var) {
			return (var != null) ? var.getValue() : null;
		}
	}

}
