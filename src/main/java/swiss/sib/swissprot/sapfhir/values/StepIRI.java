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

import io.github.jervenbolleman.handlegraph4j.PathHandle;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P> the type of PathHandle
 */
public class StepIRI<P extends PathHandle> implements IRI {

	private static final long serialVersionUID = 1;
    private final P path;
    private final long rank;
    private final PathHandleGraphSail<P, ?, ?, ?> graph;

    public StepIRI(P pathId, long rank, PathHandleGraphSail<P, ?, ?, ?> graph) {
        this.path = pathId;
        this.rank = rank;
        this.graph = graph;
    }

    @Override
    public String getNamespace() {
        return graph.getPathNameSpace(path) + "/step/";
    }

    @Override
    public String getLocalName() {
        return Long.toString(rank);
    }

    @Override
    public String stringValue() {
        return getNamespace() + '/' + getLocalName();
    }

    public long rank() {
        return rank;
    }

    public P path() {
        return path;
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
        if (obj instanceof StepIRI<?>) {
            final StepIRI<?> other = (StepIRI<?>) obj;
            if (this.rank != other.rank()) {
                return false;
            }
            if (!Objects.equals(this.path, other.path())) {
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
