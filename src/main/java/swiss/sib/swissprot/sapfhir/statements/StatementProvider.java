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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import swiss.sib.swissprot.sapfhir.values.NodeIRI;
import swiss.sib.swissprot.sapfhir.values.PathIRI;
import swiss.sib.swissprot.sapfhir.values.StepIRI;

/**
 * Collection of code to test if a certain basic graph pattern can be turned
 * into a Stream of statements.
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 */
public sealed interface StatementProvider permits StepRelatedStatementProvider, NodeRelatedStatementProvider,
		StepPositionStatementProvider, PathRelatedStatementProvider {

	/**
	 * Used to select a statement provider
	 * 
	 * @param iri an predicate
	 * @return true if this predicate is used in the RDF presentation of the java
	 *         backing object.
	 */
	boolean predicateMightReturnValues(IRI iri);

	/**
	 * Used to select a statement provider
	 * 
	 * @param val an object
	 * @return true if this object type is used in the RDF presentation of the
	 *         backing java object.
	 */
	boolean objectMightReturnValues(Value val);

	/**
	 * Used to select a statement provider
	 * 
	 * @param sub a subject
	 * @return true if this subject could be used in the RDF presentation of the
	 *         backing object.
	 */
	default boolean subjectMightReturnValues(Resource sub) {
		return sub == null || sub instanceof IRI;
	}

	/**
	 * Return the RDF statements that match the given BGP.
	 * 
	 * @param subject   the subject might be null
	 * @param predicate the predicate might be null
	 * @param object    the object might be null
	 * @return a stream like iterator.
	 */
	AutoClosedIterator<Statement> getStatements(Resource subject, IRI predicate, Value object);

	/**
	 * normalize an IRI to a PathIRI if possible else null
	 * 
	 * @param <P>     a kind of Path implementation
	 * @param subject an IRI that could represent a Path
	 * @param sail    the backing sail in which the path should be found in.
	 * @return an PathIri object or null
	 */
	static <P extends PathHandle> PathIRI<P> pathIriFromIri(IRI subject, PathHandleGraphSail<P, ?, ?, ?> sail) {
		if (subject instanceof PathIRI pi) {
			return pi;
		} else if (subject != null) {
			P path = sail.pathFromIriString(subject.stringValue());
			if (path != null) {
				return new PathIRI<>(path, sail);
			}
		}
		return null;
	}

	/**
	 * Normalize an IRI to be a step IRI if possible else null
	 * 
	 * @param <P>     a kind of Path implementation
	 * @param <S>     the kind step
	 * @param subject an IRI that could represent an step
	 * @param sail    the backing sail in which the path should be found in.
	 * @return an normalized StepIRI or null
	 */
	static <P extends PathHandle, S extends StepHandle> StepIRI<P> stepIriFromIri(IRI subject,
			PathHandleGraphSail<P, S, ?, ?> sail) {
		if (subject instanceof StepIRI si) {
			return si;
		} else if (subject != null) {
			S step = sail.stepFromIriString(subject.stringValue());
			if (step != null) {
				return new StepIRI<>(sail.pathGraph().pathOfStep(step), sail.pathGraph().rankOfStep(step), sail);
			}
		}
		return null;
	}

	/**
	 * Fetch the NodeHandle behind an IRI
	 * 
	 * @param <N>  the type of NodeHanlde
	 * @param iri  that could be hiding an NodeHandle
	 * @param sail the backing sail in which the path should be found in.
	 * @return a NodeIRI or null
	 */
	static <N extends NodeHandle> NodeIRI<N> nodeIriFromIRI(IRI iri, PathHandleGraphSail<?, ?, N, ?> sail) {
		if (iri == null) {
			return null;
		} else if (iri instanceof NodeIRI ni) {
			return ni;
		} else if (sail.matchesNodeIriPattern(iri.stringValue())) {
			try {
				long nodeid = Long.parseLong(iri.getLocalName());
				return new NodeIRI<>(nodeid, sail);
			} catch (NumberFormatException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Filter a stream of statements to not have statements with a certain object
	 * 
	 * @param object remove object from the stream
	 * @param stream the stream to filter
	 * @return the stream if object is null else the statements where the object
	 *         equals the input object.
	 */
	static AutoClosedIterator<Statement> filter(Value object, AutoClosedIterator<Statement> stream) {
		if (object == null) {
			return stream;
		} else {
			return AutoClosedIterator.filter(stream, s -> StatementProvider.objectEquals(object, s));
		}
	}

	private static boolean objectEquals(Value object, Statement stat) {
		return stat.getObject().equals(object);
	}

	/**
	 * Estimate how many statements match the BGP pattern
	 * 
	 * @param subj      to estimate for this subject
	 * @param predicate to estimate for this predicate
	 * @param obj       to estimate for this object
	 * @return an estimate
	 */
	public default double estimateCardinality(Resource subj, IRI predicate, Value obj) {
		double predEst = estimatePredicateCardinality(predicate);
		double subEst = estimateSubjectCardinality(subj);
		double objEst = estimateObjectCardinality(obj);
		return Math.min(Math.min(predEst, subEst), objEst);
	}

	/**
	 * Estimate how often this predicate is in the graph
	 * 
	 * @param predicate to estimate for
	 * @return an estimate
	 */
	public double estimatePredicateCardinality(IRI predicate);

	/**
	 * Estimate how often this subject is in the graph
	 * 
	 * @param subj to estimate for
	 * @return an estimate
	 */
	public default double estimateSubjectCardinality(Resource subj) {
		if (subj == null) {
			return Double.MAX_VALUE / 4;
		} else if (subjectMightReturnValues(subj)) {
			return 10;
		} else {
			return 0;
		}
	}

	/**
	 * Estimate how often this object will be in the graph
	 * 
	 * @param obj to estimate for
	 * @return an estimate
	 */
	public default double estimateObjectCardinality(Value obj) {
		if (obj == null) {
			return Double.MAX_VALUE / 2;
		} else if (objectMightReturnValues(obj)) {
			return 10;
		} else {
			return 0;
		}
	}
}
