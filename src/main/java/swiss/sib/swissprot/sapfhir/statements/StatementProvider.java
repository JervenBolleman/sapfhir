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

import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import swiss.sib.swissprot.sapfhir.values.NodeIRI;
import swiss.sib.swissprot.sapfhir.values.PathIRI;
import swiss.sib.swissprot.sapfhir.values.StepIRI;
import java.util.stream.Stream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Collection of code to test if a certain basic graph pattern can be turned
 * into a Stream of statements.
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
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
            return AutoClosedIterator.filter(stream, s -> object.equals(s.getObject()));
        }
    }
}
