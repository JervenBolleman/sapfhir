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
public interface StatementProvider {

    boolean predicateMightReturnValues(IRI iri);

    boolean objectMightReturnValues(Value val);

    default boolean subjectMightReturnValues(Resource sub) {
        return sub == null || sub instanceof IRI;
    }

    AutoClosedIterator<Statement> getStatements(Resource subject, IRI predicate, Value object);

    static <P extends PathHandle> PathIRI<P> pathIriFromIri(IRI subject, PathHandleGraphSail<P, ?, ?, ?> sail) {
        if (subject instanceof PathIRI) {
            return (PathIRI<P>) subject;
        } else if (subject != null) {
            P path = sail.pathFromIriString(subject.stringValue());
            if (path != null) {
                return new PathIRI<>(path, sail);
            }
        }
        return null;
    }

    static <P extends PathHandle, S extends StepHandle> StepIRI<P> stepIriFromIri(IRI subject, PathHandleGraphSail<P, S, ?, ?> sail) {
        if (subject instanceof StepIRI) {
            return (StepIRI<P>) subject;
        } else if (subject != null) {
            S step = sail.stepFromIriString(subject.stringValue());
            if (step != null) {
                return new StepIRI<>(sail.pathGraph().pathOfStep(step), sail.pathGraph().rankOfStep(step), sail);
            }
        }
        return null;
    }

    static <N extends NodeHandle> NodeIRI<N> nodeIriFromIRI(IRI iri, PathHandleGraphSail<?, ?, N, ?> sail) {
        if (iri == null) {
            return null;
        } else if (iri instanceof NodeIRI) {
            return (NodeIRI<N>) iri;
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

    public default double estimateCardinality(Resource subj, IRI predicate, Value obj) {
        double predEst = estimatePredicateCardinality(predicate);
        double subEst = estimateSubjectCardinality(subj);
        double objEst = estimateObjectCardinality(obj);
        return Math.min(Math.min(predEst, subEst), objEst);
    }

    public double estimatePredicateCardinality(IRI predicate);

    public default double estimateSubjectCardinality(Resource subj) {
        if (subj == null) {
            return Double.MAX_VALUE / 4;
        } else if (subjectMightReturnValues(subj)) {
            return 10;
        } else {
            return 0;
        }
    }

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
