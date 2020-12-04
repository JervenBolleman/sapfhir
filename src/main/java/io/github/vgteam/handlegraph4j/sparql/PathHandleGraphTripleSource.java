/*
 * Copyright (C) 2020 SIB Swiss Institute of Bioinformatics.
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
package io.github.vgteam.handlegraph4j.sparql;

import io.github.vgteam.handlegraph4j.EdgeHandle;
import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import io.github.vgteam.handlegraph4j.sequences.Sequence;
import io.github.vgteam.handlegraph4j.sequences.SequenceType;
import io.github.vgteam.handlegraph4j.sparql.values.HandleGraphValueFactory;
import io.github.vgteam.handlegraph4j.sparql.values.NodeIRI;
import io.github.vgteam.handlegraph4j.sparql.values.StepBeginPositionIRI;
import io.github.vgteam.handlegraph4j.sparql.values.StepEndPositionIRI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import sib.swiss.swissprot.handlegraph4jrdf.FALDO;
import sib.swiss.swissprot.handlegraph4jrdf.VG;

/**
 *
 * @author jbollema
 */
public class PathHandleGraphTripleSource<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>> implements TripleSource {

    private final HandleGraphValueFactory<P, S, N, E> vf;
    private final PathHandleGraphSail<P, S, N, E> sail;
    private final List<StatementProvider> statementProviders;

    public PathHandleGraphTripleSource(PathHandleGraphSail<P, S, N, E> sail) {
        this.sail = sail;
        this.vf = new HandleGraphValueFactory<>(sail);
        this.statementProviders = List.of(new NodeSubjectStatementProvider<>(sail, vf));
    }

    @Override
    public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subject, IRI predicate, Value object, Resource... rsrcs) throws QueryEvaluationException {
        //We know that we don't have any blanknodes
        if (subject instanceof BNode || object instanceof BNode) {
            return new EmptyIteration<>();
        }
        Stream<Statement> stream = statementProviders.stream()
                .filter((sp) -> sp.predicateMightReturnValues(predicate) || sp.objectMightReturnValues(object))
                .flatMap(p -> p.getStatements(subject, predicate, object));

        return new CloseableIteration<Statement, QueryEvaluationException>() {
            Iterator<Statement> providedAsIter = stream.iterator();

            @Override
            public void close() throws QueryEvaluationException {
                stream.close();
            }

            @Override
            public boolean hasNext() throws QueryEvaluationException {
                return providedAsIter.hasNext();
            }

            @Override
            public Statement next() throws QueryEvaluationException {
                return providedAsIter.next();
            }

            @Override
            public void remove() throws QueryEvaluationException {

            }
        };

    }

    @Override
    public ValueFactory getValueFactory() {
        return vf;
    }

    private interface StatementProvider {

        boolean predicateMightReturnValues(IRI iri);

        boolean objectMightReturnValues(Value val);

        Stream<Statement> getStatements(Resource subject, IRI predicate, Value object);
    }

    private static class NodeSubjectStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>> implements StatementProvider {

        private final PathHandleGraphSail<P, S, N, E> sail;
        private final HandleGraphValueFactory<P, S, N, E> vf;

        public NodeSubjectStatementProvider(PathHandleGraphSail<P, S, N, E> sail, HandleGraphValueFactory<P, S, N, E> vf) {
            this.sail = sail;
            this.vf = vf;
        }

        private static final Set<IRI> linkPredicates = Set.of(VG.linksForwardToForward, VG.linksForwardToReverse, VG.linksReverseToForward,
                VG.linksReverseToReverse, VG.links);
        private static final Set<IRI> nodeRelatedPredicates = Set.of(VG.linksForwardToForward, VG.linksForwardToReverse, VG.linksReverseToForward,
                VG.linksReverseToReverse, VG.links, RDF.VALUE);

        @Override
        public boolean predicateMightReturnValues(IRI iri) {
            return nodeRelatedPredicates.contains(iri);
        }

        @Override
        public boolean objectMightReturnValues(Value val) {
            if (VG.Node.equals(val)) {
                return true;
            } else if (val instanceof Literal) {
                Literal lit = (Literal) val;
                return (XSD.STRING.equals(lit.getDatatype()));
            }
            return false;
        }

        @Override
        public Stream<Statement> getStatements(Resource subject, IRI predicate, Value object) {
            if (subject instanceof BNode) {
                return Stream.empty();
            }
            NodeIRI nodeSubject = iriMatchesNodeIri((IRI) subject);
            if (nodeSubject != null) {
                return generateTriplesForKnownNode(nodeSubject, predicate, object);
            } else if (subject == null && object == null) {
                return generateTriplesForAllNodes(predicate, object);
            } else if (object instanceof Literal && (predicate == null || RDF.VALUE.equals(predicate))) {
                return getNodeTriplesForKnownSequence(object, RDF.VALUE);
            } else if (object instanceof IRI) {
                NodeIRI<N> nodeObject = iriMatchesNodeIri((IRI) object);
                if (nodeObject != null) {
                    sail.pathGraph().followEdgesToWardsTheRight(nodeObject.node());
                } else if (VG.Node.equals(object)) {
                    Stream<Statement> map = sail.pathGraph().nodes()
                            .map(n -> new NodeIRI<>(n.id(), sail))
                            .map(n -> vf.createStatement(n, RDF.TYPE, VG.Node));
                    return map;
                }
                //TODO: generate incoming links

            }
            return Stream.empty();
        }

        private Stream<Statement> generateTriplesForAllNodes(IRI predicate, Value object) {
            //All nodes
            Stream<Statement> nodes = sail.pathGraph().nodes()
                    .flatMap(n -> nodeToTriples(n, predicate, object));
            if (predicate == null || linkPredicates.contains(predicate)) {
                Stream<E> edges = sail.pathGraph().edges();
                Stream<Statement> edgeStatements = edgesToStatements(predicate, edges);
                return Stream.concat(nodes, edgeStatements);
            }
            return nodes;
        }

        private Stream<Statement> generateTriplesForKnownNode(NodeIRI<N> nodeSubject, IRI predicate, Value object) {
            N node = nodeSubject.node();
            Stream<Statement> typeValue = nodeToTriples(node, predicate, object);

            if ((predicate == null || linkPredicates.contains(predicate) && (object instanceof IRI) || object == null)) {
                NodeIRI nodeObject = iriMatchesNodeIri((IRI) object);
                Stream<Statement> linksForNode = linksForNode(node, predicate, nodeObject);
                return Stream.concat(typeValue, linksForNode);
            }
            return typeValue;
        }

        private NodeIRI<N> iriMatchesNodeIri(IRI iri) {
            if (iri == null) {
                return null;
            } else if (iri instanceof NodeIRI) {
                return (NodeIRI<N>) iri;
            } else if (sail.matchesNodeIriPattern(iri.stringValue())) {
                try {
                    long nodeid = Long.parseLong(iri.getLocalName());
                    return new NodeIRI<N>(nodeid, sail);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }

        private Stream<Statement> getNodeTriplesForKnownSequence(Value object, IRI predicate) {
            Literal lit = (Literal) object;
            if ((lit.getDatatype() == null || lit.getDatatype() == XSD.STRING) && lit.getLanguage().isEmpty()) {
                String label = lit.getLabel();
                if (Sequence.stringCanBeDNASequence(label)) {
                    return sail.pathGraph().nodesWithSequence(SequenceType.fromByteArray(label.getBytes(StandardCharsets.US_ASCII)))
                            .flatMap(n -> nodeToTriples(n, predicate, object));
                }
            }
            return Stream.empty();
        }

        private Stream<Statement> nodeToTriples(N node, IRI predicate, Value object) {
            Statement[] statements = new Statement[2];
            NodeIRI<N> nodeSubject = new NodeIRI<>(sail.pathGraph().asLong(node), sail);
            if ((RDF.TYPE.equals(predicate) || predicate == null) && object == null || VG.Node.equals(object)) {
                statements[0] = nodeTypeStatement(nodeSubject);
            }
            if ((RDF.VALUE.equals(predicate) || predicate == null)) {
                Literal sequence = vf.createSequenceLiteral(node, sail.pathGraph());
                if (object == null || sequence.equals(object)) {
                    Statement nodeValueStatement = vf.createStatement(nodeSubject, RDF.VALUE, sequence);
                    statements[1] = nodeValueStatement;
                }
            }
            return Arrays.stream(statements).filter(Objects::nonNull);
        }

        private Statement nodeTypeStatement(NodeIRI nodeSubject) {
            return vf.createStatement(nodeSubject, RDF.TYPE, VG.Node);
        }

        private Stream<Statement> linksForNode(N node, IRI predicate, NodeIRI object) {
            Stream<E> asStream = sail.pathGraph().followEdgesToWardsTheLeft(node);
            if (object != null) {
                asStream.filter(e -> sail.pathGraph().asLong(e.right()) == object.id());
            }
            return edgesToStatements(predicate, asStream);
        }

        private Stream<Statement> edgesToStatements(IRI predicate, Stream<E> asStream) {
            if (VG.linksForwardToForward.equals(predicate)) {
                return asStream.map(this::forwardToForward);
            } else if (VG.linksForwardToReverse.equals(predicate)) {
                return asStream.map(this::forwardToReverse);
            } else if (VG.linksReverseToReverse.equals(predicate)) {
                return asStream.map(this::reverseToReverse);
            } else if (VG.linksReverseToForward.equals(predicate)) {
                return asStream.map(this::reverseToForward);
            } else if (VG.links.equals(predicate)) {
                return asStream.map(this::links);
            } else {
                return asStream.flatMap(e -> Stream.of(forwardToForward(e),
                        forwardToReverse(e),
                        reverseToReverse(e),
                        reverseToForward(e), links(e)));
            }
        }

        private Statement links(E edge) {
            return vf.createStatement(new NodeIRI(sail.pathGraph().asLong(edge.left()), sail), VG.links, new NodeIRI(sail.pathGraph().asLong(edge.right()), sail));
        }

        private Statement forwardToForward(E edge) {
            if (!sail.pathGraph().isReverseNodeHandle(edge.left()) && !sail.pathGraph().isReverseNodeHandle(edge.right())) {
                return vf.createStatement(new NodeIRI(sail.pathGraph().asLong(edge.left()), sail), VG.linksForwardToForward, new NodeIRI(sail.pathGraph().asLong(edge.right()), sail));
            } else {
                return null;
            }
        }

        private Statement forwardToReverse(E edge) {
            if (!sail.pathGraph().isReverseNodeHandle(edge.left()) && sail.pathGraph().isReverseNodeHandle(edge.right())) {
                return vf.createStatement(new NodeIRI(sail.pathGraph().asLong(edge.left()), sail), VG.linksForwardToReverse, new NodeIRI(sail.pathGraph().asLong(edge.right()), sail));
            } else {
                return null;
            }
        }

        private Statement reverseToReverse(E edge) {
            if (sail.pathGraph().isReverseNodeHandle(edge.left()) && sail.pathGraph().isReverseNodeHandle(edge.right())) {
                return vf.createStatement(new NodeIRI(sail.pathGraph().asLong(edge.left()), sail), VG.linksReverseToReverse, new NodeIRI(sail.pathGraph().asLong(edge.right()), sail));
            } else {
                return null;
            }
        }

        private Statement reverseToForward(E edge) {
            if (sail.pathGraph().isReverseNodeHandle(edge.left()) && !sail.pathGraph().isReverseNodeHandle(edge.right())) {
                return vf.createStatement(new NodeIRI(sail.pathGraph().asLong(edge.left()), sail), VG.linksReverseToForward, new NodeIRI(sail.pathGraph().asLong(edge.right()), sail));
            } else {
                return null;
            }
        }
    }

    private static class StepSubjectStatementProvider implements StatementProvider {

        private static final Pattern matchesEndOfStepIri = Pattern.compile("/step/(\\d+)$");
        private static final Set<IRI> stepAssociatedTypes = Set.of(FALDO.Region, FALDO.ExactPosition, FALDO.Position, VG.Step);
        private static final Set<IRI> stepAssociatedPredicates = Set.of(VG.rank, VG.position, VG.path, VG.node, VG.reverseOfNode, FALDO.begin, FALDO.end,
                FALDO.reference, FALDO.position);

        @Override
        public boolean predicateMightReturnValues(IRI iri) {
            return stepAssociatedPredicates.contains(iri);
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
            return false;
        }

        @Override
        public Stream<Statement> getStatements(Resource subject, IRI predicate, Value object) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

}
