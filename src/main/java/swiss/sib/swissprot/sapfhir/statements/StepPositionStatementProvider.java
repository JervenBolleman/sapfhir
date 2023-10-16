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
import swiss.sib.swissprot.sapfhir.values.HandleGraphValueFactory;
import swiss.sib.swissprot.sapfhir.values.PathIRI;
import swiss.sib.swissprot.sapfhir.values.StepBeginPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepEndPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepPositionIRI;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
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
            var pg = sail.pathGraph();
            var paths = pg.paths();
            if (!paths.hasNext()) {
                return empty();
            }
            var flattened = flatMap(new PositionMaintainingStepIRIGenerator(paths, pg));
            var known = map(flattened, i -> knownSubject(i, predicate, object));
            return flatMap(known);
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
            var all = concat(typeStatement,
                    concat(knownSubjectReferenceStatements(object, stepSubject),
                    knownSubjectPositionStatements(object, stepSubject)));
            return all;
        } else {
            return empty();
        }
    }

    private AutoClosedIterator<Statement> knownSubjectTypeStatement(Value object,
            StepPositionIRI<P, S> subject) {
        if (object instanceof Literal || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        AutoClosedIterator<Statement> stream = concat(
                of(vf.createStatement(subject, RDF.TYPE, FALDO.Position)),
                of(vf.createStatement(subject, RDF.TYPE, FALDO.ExactPosition)));
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
            long position = ((StepBeginPositionIRI<?,?>) subject).getBeginPosition();
            var l = vf.createLiteral(position);
            stream = AutoClosedIterator.of(vf.createStatement(subject, FALDO.position, l));
        } else if (subject instanceof StepEndPositionIRI) {
            long position = ((StepEndPositionIRI<?,?>) subject).getEndPosition();
            var l = vf.createLiteral(position);
            stream = AutoClosedIterator.of(vf.createStatement(subject, FALDO.position, l));
        } else {
            return AutoClosedIterator.empty();
        }
        return filter(object, stream);
    }

    private StepPositionIRI<P, S> beginOrEndIriFromIri(IRI iri) {
        if (iri == null) {
            return null;
        } else if (iri instanceof StepBeginPositionIRI) {
            return (StepBeginPositionIRI<P,S>) iri;
        } else if (iri instanceof StepEndPositionIRI) {
            return (StepEndPositionIRI<P,S>) iri;
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

    private class PositionMaintainingStepIRIGenerator implements AutoClosedIterator<AutoClosedIterator<StepPositionIRI<P, S>>> {

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
        public AutoClosedIterator<StepPositionIRI<P, S>> next() {
            S s = steps.next();
            int seqln = pg.sequenceLengthOf(pg.nodeOfStep(s));

            var b = new StepBeginPositionIRI<>(path, rank, sail, beginPosition);
            long endPosition = beginPosition + seqln;
            var e = new StepEndPositionIRI<>(path, rank, sail, endPosition);
            beginPosition = endPosition + 1;
            rank++;
            return concat(of(b), of(e));
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
