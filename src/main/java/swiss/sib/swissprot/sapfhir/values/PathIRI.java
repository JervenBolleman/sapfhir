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
package swiss.sib.swissprot.sapfhir.values;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import io.github.jervenbolleman.handlegraph4j.PathHandle;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;

/**
 * Hide a path and graph object in an IRI allows efficient computation of equals
 * etc.
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P>   the type of PathHandle
 * @param path  the path hiding in this IRI
 * @param graph the graph the path is in
 */
public record PathIRI<P extends PathHandle>(P path, PathHandleGraphSail<P, ?, ?, ?> graph) implements IRI {
	private static final long serialVersionUID = 1;

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Override
	public String getNamespace() {
		return VF.createIRI(graph.getPathNameSpace(path)).getNamespace();
	}

	@Override
	public String getLocalName() {
		return VF.createIRI(graph.getPathNameSpace(path)).getLocalName();
	}

	@Override
	public String stringValue() {
		return graph.getPathNameSpace(path);
	}

	@Override
	public String toString() {
		return stringValue();
	}

	@Override
	public int hashCode() {
		return stringValue().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof PathIRI<?>) {
			final PathIRI<?> other = (PathIRI<?>) obj;
			if (!this.path.equals(other.path)) {
				return false;
			}
			if (!Objects.equals(this.graph, other.graph)) {
				return false;
			}
			return true;
		} else if (obj instanceof IRI) {
			return stringValue().equals(((IRI) obj).stringValue());
		}
		return true;
	}
}
