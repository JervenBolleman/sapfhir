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
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.pathIriFromIri;
import io.github.vgteam.handlegraph4j.EdgeHandle;
import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.filter;
import swiss.sib.swissprot.sapfhir.values.HandleGraphValueFactory;
import swiss.sib.swissprot.sapfhir.values.PathIRI;
import java.util.stream.Stream;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import swiss.sib.swissprot.handlegraph4jrdf.VG;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class PathRelatedStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>> implements StatementProvider {

    private final HandleGraphValueFactory<P, S, N, E> vf;
    private final PathHandleGraphSail<P, S, N, E> sail;

    public PathRelatedStatementProvider(PathHandleGraphSail<P, S, N, E> sail,
            HandleGraphValueFactory<P, S, N, E> vf) {
        this.vf = vf;
        this.sail = sail;
    }

    @Override
    public boolean predicateMightReturnValues(IRI iri) {
        return iri == null || RDF.TYPE.equals(iri) || RDFS.LABEL.equals(iri);
    }

    @Override
    public boolean objectMightReturnValues(Value val) {

        return val == null || VG.Path.equals(val) || isString(val);
    }

    private static boolean isString(Value val) {
        return val instanceof Literal;
    }

    @Override
    public Stream<Statement> getStatements(Resource subject,
            IRI predicate,
            Value object) {
        if (subject instanceof BNode) {
            return Stream.empty();
        } else if (subject == null) {
            return sail.pathGraph().paths().map(p -> new PathIRI<P>(p, sail))
                    .flatMap(p -> this.getStatements(p, predicate, object));
        } else if (subject instanceof IRI) {
            PathIRI<P> pathIRI = pathIriFromIri((IRI) subject, sail);
            if (pathIRI == null) {
                return Stream.empty();
            } else if (RDF.TYPE.equals(predicate)) {
                return knownSubjectTypeStatements(pathIRI, object);
            } else if (RDFS.LABEL.equals(predicate)) {
                return knownSubjectLabelStatements(pathIRI, object);
            } else {
                return Stream.concat(knownSubjectTypeStatements(pathIRI, object),
                        knownSubjectLabelStatements(pathIRI, object));
            }
        } else {
            return Stream.empty();
        }
    }

    private Stream<Statement> knownSubjectTypeStatements(PathIRI<P> pathIRI,
            Value object) {
        if (object instanceof BNode || object instanceof Literal) {
            return Stream.empty();
        }
        Stream<Statement> stream = Stream.of(vf.createStatement(pathIRI, RDF.TYPE, VG.Path));
        return filter(object, stream);
    }

    

    private Stream<Statement> knownSubjectLabelStatements(PathIRI<P> pathIRI,
            Value object) {
        if (object instanceof BNode || object instanceof IRI) {
            return Stream.empty();
        }
        String nameOfPath = sail.pathGraph().nameOfPath(pathIRI.path());
        Literal label = vf.createLiteral(nameOfPath);
        Stream<Statement> stream = Stream.of(vf.createStatement(pathIRI, RDFS.LABEL, label));
        return filter(object, stream);
    }

    
}
