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

import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.concat;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.of;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.filter;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.stepIriFromIri;

import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import io.github.vgteam.handlegraph4j.EdgeHandle;
import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
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
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class StepRelatedStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>> implements StatementProvider {

    private final HandleGraphValueFactory<P, S, N, E> vf;
    private final PathHandleGraphSail<P, S, N, E> sail;

    public StepRelatedStatementProvider(PathHandleGraphSail<P, S, N, E> sail, HandleGraphValueFactory<P, S, N, E> vf) {
        this.vf = vf;
        this.sail = sail;
    }
//    private static final Pattern matchesEndOfStepIri = Pattern.compile("/step/(\\d+)$");
    private static final Set<IRI> stepAssociatedTypes = Set.of(FALDO.Region,
            VG.Step);
    private static final Set<IRI> stepAssociatedPredicates = Set.of(
            RDF.TYPE,
            VG.rank,
            VG.path,
            VG.node,
            VG.reverseOfNode,
            FALDO.begin,
            FALDO.end);

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
                //TODO: Test if it can be a StepBegin Or StepEnd by pattern
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

    private AutoClosedIterator<Statement> knownSubject(IRI subject, IRI predicate, Value object) {
        StepIRI<P> stepSubject = stepIriFromIri((IRI) subject, sail);
        //If null it is not a Step IRI and therefore can't match the values here.
        if (stepSubject == null) {
            return AutoClosedIterator.empty();
        } else if (RDF.TYPE.equals(predicate)) {
            return knownSubjectTypeStatement(object, stepSubject);
        } else if (VG.rank.equals(predicate)) {
            return knownSubjectRankStatements(object, stepSubject);
        } else if (VG.node.equals(predicate)) {
            return knownSubjectNodeStatements(object, stepSubject);
        } else if (VG.reverseOfNode.equals(predicate)) {
            return knownSubjectReverseNodeStatements(object, stepSubject);
        } else if (VG.path.equals(predicate)) {
            return knownSubjectPathStatements(object, stepSubject);
        } else if (FALDO.begin.equals(predicate)) {
            return knownSubjectBeginStatements(object, stepSubject);
        } else if (FALDO.end.equals(predicate)) {
            return knownSubjectEndStatements(object, stepSubject);
        } else if (predicate == null) {
            var typeStatement = knownSubjectTypeStatement(object, stepSubject);
            var rankStatements = knownSubjectRankStatements(object, stepSubject);
            var nodeStatements = knownSubjectNodeStatements(object, stepSubject);
            var pathStatements = knownSubjectPathStatements(object, stepSubject);
            var reverseNodeStatements = knownSubjectReverseNodeStatements(object, stepSubject);
            var of = concat(concat(concat(typeStatement,
                    rankStatements),
                    pathStatements),
                    concat(nodeStatements,
                    reverseNodeStatements));
            return of;
        } else {
            return AutoClosedIterator.empty();
        }
    }

    private AutoClosedIterator<Statement> knownSubjectTypeStatement(Value object, StepIRI<P> subject) {
        if (object instanceof Literal || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        if (VG.Step.equals(object)) {
        	return of(vf.createStatement(subject, RDF.TYPE, VG.Step));
        } else if (FALDO.Region.equals(object) ) {
        	return of(vf.createStatement(subject, RDF.TYPE, FALDO.Region));
        } else {
	        AutoClosedIterator<Statement> stream = concat(
	                of(vf.createStatement(subject, RDF.TYPE, VG.Step)),
	                of(vf.createStatement(subject, RDF.TYPE, FALDO.Region)));
	        return filter(object, stream);
        }
    }

    private AutoClosedIterator<Statement> knownSubjectRankStatements(Value object, StepIRI<P> stepSubject) {
        if (object instanceof IRI || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        AutoClosedIterator<Statement> stream = AutoClosedIterator.of(vf.createStatement(stepSubject, VG.rank, vf.createLiteral(stepSubject.rank())));
        return filter(object, stream);
    }

    private AutoClosedIterator<Statement> knownSubjectNodeStatements(Value object, StepIRI<P> stepSubject) {
        if (object instanceof Literal || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        S step = sail.pathGraph().stepByRankAndPath(stepSubject.path(), stepSubject.rank());
        N node = sail.pathGraph().nodeOfStep(step);
        if (sail.pathGraph().isReverseNodeHandle(node)) {
            return AutoClosedIterator.empty();
        }
        NodeIRI<N> nodeIRI = new NodeIRI<>(node.id(), sail);
        AutoClosedIterator<Statement> stream = AutoClosedIterator.of(vf.createStatement(stepSubject, VG.node, nodeIRI));
        return filter(object, stream);
    }

    private AutoClosedIterator<Statement> knownSubjectReverseNodeStatements(Value object, StepIRI<P> stepSubject) {
        if (object instanceof Literal || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        S step = sail.pathGraph().stepByRankAndPath(stepSubject.path(), stepSubject.rank());
        N node = sail.pathGraph().nodeOfStep(step);
        if (!sail.pathGraph().isReverseNodeHandle(node)) {
            return AutoClosedIterator.empty();
        }
        NodeIRI<N> nodeIRI = new NodeIRI<>(node.id(), sail);
        AutoClosedIterator<Statement> stream = AutoClosedIterator.of(vf.createStatement(stepSubject, VG.reverseOfNode, nodeIRI));
        return filter(object, stream);
    }

    private AutoClosedIterator<Statement> knownSubjectPathStatements(Value object, StepIRI<P> stepSubject) {
        if (object instanceof Literal || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        PathIRI<P> path = new PathIRI<>(stepSubject.path(), sail);
        AutoClosedIterator<Statement> stream = AutoClosedIterator.of(vf.createStatement(stepSubject, VG.path, path));
        return filter(object, stream);
    }

    private AutoClosedIterator<Statement> knownSubjectEndStatements(Value object, StepIRI<P> stepSubject) {
        if (object instanceof Literal || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        long rank = stepSubject.rank();
        P path = stepSubject.path();
        var beginIRI = new StepEndPositionIRI<>(path, rank, sail);
        var stream = AutoClosedIterator.of(vf.createStatement(stepSubject, FALDO.begin, beginIRI));
        return filter(object, stream);
    }

    private AutoClosedIterator<Statement> knownSubjectBeginStatements(Value object, StepIRI<P> stepSubject) {
        if (object instanceof Literal || object instanceof BNode) {
            return AutoClosedIterator.empty();
        }
        long rank = stepSubject.rank();
        P path = stepSubject.path();
        var beginIRI = new StepBeginPositionIRI<>(path, rank, sail);
        var stream = AutoClosedIterator.of(vf.createStatement(stepSubject, FALDO.begin, beginIRI));
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
