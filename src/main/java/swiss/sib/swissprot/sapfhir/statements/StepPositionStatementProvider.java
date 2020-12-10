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

import io.github.vgteam.handlegraph4j.EdgeHandle;
import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.PathGraph;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import io.github.vgteam.handlegraph4j.sequences.AutoClosedIterator;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.filter;
import swiss.sib.swissprot.sapfhir.values.HandleGraphValueFactory;
import swiss.sib.swissprot.sapfhir.values.PathIRI;
import swiss.sib.swissprot.sapfhir.values.StepBeginPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepEndPositionIRI;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import swiss.sib.swissprot.handlegraph4jrdf.FALDO;
import swiss.sib.swissprot.handlegraph4jrdf.VG;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.pathIriFromIri;
import swiss.sib.swissprot.sapfhir.values.StepPositionIRI;
import static swiss.sib.swissprot.sapfhir.values.StepPositionIRI.POSITION;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class StepPositionStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>> implements StatementProvider {

    private final HandleGraphValueFactory<P, S, N, E> vf;
    private final PathHandleGraphSail<P, S, N, E> sail;

    public StepPositionStatementProvider(PathHandleGraphSail<P, S, N, E> sail, HandleGraphValueFactory<P, S, N, E> vf) {
        this.vf = vf;
        this.sail = sail;
    }
    private static final Set<IRI> types = Set.of(FALDO.Position,
            FALDO.ExactPosition);
    private static final Set<IRI> predicates = Set.of(
            RDF.TYPE,
            FALDO.position,
            FALDO.reference);

    @Override
    public boolean predicateMightReturnValues(IRI iri) {
        return iri == null || predicates.contains(iri);
    }

    @Override
    public boolean objectMightReturnValues(Value val) {
        if (val == null) {
            return true;
        } else if (val instanceof Literal) {
            Literal lit = (Literal) val;
            return (XMLDatatypeUtil.isNumericDatatype(lit.getDatatype()));
        } else if (val instanceof PathIRI) {
            return true;
        } else if (val instanceof IRI) {
            return types.contains((IRI) val)
                    || pathIriFromIri((IRI) val, sail) != null;
        }
        return false;
    }

    @Override
    public AutoClosedIterator<Statement> getStatements(Resource subject,
            IRI predicate,
            Value object) {
        if (subject == null && (object == null || !(object instanceof BNode))) {
            AutoClosedIterator<S> steps = sail.pathGraph().steps();
            AutoClosedIterator<AutoClosedIterator<StepPositionIRI>> map = AutoClosedIterator.map(steps, s -> {
                P path = sail.pathGraph().pathOfStep(s);
                long rank = sail.pathGraph().rankOfStep(s);
                var b = new StepBeginPositionIRI<>(path, rank, sail);
                var e = new StepEndPositionIRI<>(path, rank, sail);
                AutoClosedIterator<StepPositionIRI> of = AutoClosedIterator.<StepPositionIRI>of(b, e);
                return of;
            });
            AutoClosedIterator<StepPositionIRI> flattened = AutoClosedIterator.flatMap(map);
            var known = AutoClosedIterator.map(flattened, i -> knownSubject(i, predicate, object));
            return AutoClosedIterator.flatMap(known);
        } else if (subject instanceof IRI) {
            return knownSubject((IRI) subject, predicate, object);
        } else {
            return AutoClosedIterator.empty();
        }
    }

    private AutoClosedIterator<Statement> knownSubject(IRI subject,
            IRI predicate,
            Value object) {
        StepPositionIRI<P, S> stepSubject = beginOrEndIriFromIri((IRI) subject);
        //If null it is not a Step IRI and therefore can't match the values here.
        if (stepSubject == null) {
            return AutoClosedIterator.empty();
        } else if (RDF.TYPE.equals(predicate)) {
            return knownSubjectTypeStatement(object, stepSubject);
        } else if (FALDO.reference.equals(predicate)) {
            return knownSubjectReferenceStatements(object, stepSubject);
        } else if (FALDO.position.equals(predicate)) {
            return knownSubjectPositionStatements(object, stepSubject);
        } else if (predicate == null) {
            var typeStatement = knownSubjectTypeStatement(object, stepSubject);
            AutoClosedIterator<AutoClosedIterator<Statement>> of = AutoClosedIterator.of(typeStatement,
                    knownSubjectReferenceStatements(object, stepSubject),
                    knownSubjectPositionStatements(object, stepSubject));
            return AutoClosedIterator.flatMap(of);
        } else {
            return AutoClosedIterator.empty();
        }
    }

    private AutoClosedIterator<Statement> knownSubjectTypeStatement(Value object,
            StepPositionIRI subject) {
        if (object instanceof Literal || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        AutoClosedIterator<Statement> stream = AutoClosedIterator.of(
                vf.createStatement(subject, RDF.TYPE, FALDO.Position),
                vf.createStatement(subject, RDF.TYPE, FALDO.ExactPosition));
        if (object == null) {
            return stream;
        } else {
            return AutoClosedIterator.filter(stream, s -> object.equals(s.getObject()));
        }
    }

    private AutoClosedIterator<Statement> knownSubjectReferenceStatements(Value object,
            StepPositionIRI<P, S> subject) {
        if (object instanceof Literal || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        PathIRI<P> pathIRI = new PathIRI<>(subject.path(), sail);
        Statement stat = vf.createStatement(subject, VG.path, pathIRI);
        var stream = AutoClosedIterator.of(stat);
        return filter(object, stream);
    }

    private AutoClosedIterator<Statement> knownSubjectPositionStatements(Value object,
            StepPositionIRI<P, S> subject) {
        if (object instanceof IRI || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        AutoClosedIterator<Statement> stream;
        if (subject instanceof StepBeginPositionIRI) {
            S step = stepFromIri(subject);
            long position = sail.pathGraph().beginPositionOfStep(step);
            var l = vf.createLiteral(position);
            stream = AutoClosedIterator.of(vf.createStatement(subject, FALDO.position, l));
        } else if (subject instanceof StepEndPositionIRI) {
            S step = stepFromIri(subject);
            long position = sail.pathGraph().endPositionOfStep(step);
            var l = vf.createLiteral(position);
            stream = AutoClosedIterator.of(vf.createStatement(subject, FALDO.position, l));
        } else {
            return AutoClosedIterator.empty();
        }
        return filter(object, stream);
    }

    private S stepFromIri(StepPositionIRI<P, S> subjet) {
        PathGraph<P, S, N, E> pathGraph = sail.pathGraph();
        S step = pathGraph.stepByRankAndPath(subjet.path(), subjet.rank());
        return step;
    }

    private StepPositionIRI<P, S> beginOrEndIriFromIri(IRI iri) {
        if (iri == null) {
            return null;
        } else if (iri instanceof StepBeginPositionIRI) {
            return (StepBeginPositionIRI) iri;
        } else if (iri instanceof StepEndPositionIRI) {
            return (StepEndPositionIRI) iri;
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

}
