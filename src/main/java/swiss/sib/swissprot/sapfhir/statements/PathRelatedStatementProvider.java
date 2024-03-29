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
import static swiss.sib.swissprot.sapfhir.statements.StatementProvider.pathIriFromIri;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import io.github.jervenbolleman.handlegraph4j.EdgeHandle;
import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import io.github.jervenbolleman.handlegraph4j.iterators.AutoClosedIterator;
import swiss.sib.swissprot.handlegraph4jrdf.VG;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;
import swiss.sib.swissprot.sapfhir.values.PathIRI;

/**
 * Generate the statements associated with a paths
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P>  the type of PathHandle
 * @param <S>  the type of StepHandle
 * @param <E>  the type of EdgeHandle
 * @param <N>  the type of NodeHandle
 * @param sail the sail that this provides statements from
 */
public final record PathRelatedStatementProvider<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>(
		PathHandleGraphSail<P, S, N, E> sail) implements StatementProvider {

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
	public AutoClosedIterator<Statement> getStatements(Resource subject, IRI predicate, Value object) {
		if (subject instanceof BNode) {
			return empty();
		} else if (subject == null) {
			AutoClosedIterator<P> paths = sail.pathGraph().paths();
			var iris = map(paths, p -> this.getStatements(new PathIRI<P>(p, sail), predicate, object));
			return flatMap(iris);
		} else if (subject instanceof IRI) {
			PathIRI<P> pathIRI = pathIriFromIri((IRI) subject, sail);
			if (pathIRI == null) {
				return empty();
			} else if (RDF.TYPE.equals(predicate)) {
				return knownSubjectTypeStatements(pathIRI, object);
			} else if (RDFS.LABEL.equals(predicate)) {
				return knownSubjectLabelStatements(pathIRI, object);
			} else {
				return concat(knownSubjectTypeStatements(pathIRI, object),
						knownSubjectLabelStatements(pathIRI, object));
			}
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectTypeStatements(PathIRI<P> pathIRI, Value object) {
		if (object instanceof BNode || object instanceof Literal) {
			return empty();
		} else if (object == null || VG.Path.equals(object)) {
			Statement stat = new UnsafeStatement(pathIRI, RDF.TYPE, VG.Path);
			return of(stat);
		} else {
			return empty();
		}
	}

	private AutoClosedIterator<Statement> knownSubjectLabelStatements(PathIRI<P> pathIRI, Value object) {
		if (object instanceof BNode || object instanceof IRI) {
			return empty();
		}
		String nameOfPath = sail.pathGraph().nameOfPath(pathIRI.path());
		Literal label = sail.getValueFactory().createLiteral(nameOfPath);
		if (object == null || label.equals(object)) {
			Statement stat = new UnsafeStatement(pathIRI, RDFS.LABEL, label);
			return of(stat);
		} else {
			return empty();
		}
	}

	@Override
	public double estimatePredicateCardinality(IRI predicate) {
		if (RDFS.LABEL.equals(predicate)) {
			return sail.pathGraph().pathCount();
		} else if (RDF.TYPE.equals(predicate)) {
			return sail.pathGraph().pathCount();
		} else {
			return 0;
		}
	}
}
