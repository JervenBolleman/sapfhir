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

import io.github.jervenbolleman.handlegraph4j.PathGraph;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;

/**
 * * An IRI hiding a path+position combination. May be equal to any IRI
 * representing the same path+position even if the other IRI is hiding the begin
 * of a step instead of the end
 * 
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P>      the type of PathHandle
 * @param <S>      the type of StepHandle
 * @param path     path the step is on
 * @param rank     of the step
 * @param graph    the graph the step is in
 * @param position where the step ends along the path
 */
public record StepEndPositionIRI<P extends PathHandle, S extends StepHandle>(P path, long rank,
		PathHandleGraphSail<P, S, ?, ?> graph, long position) implements StepPositionIRI<P, S> {

	private static final long serialVersionUID = 1;

	/**
	 * A step end position where we need to calculate the position as it is not
	 * known at object creation time
	 * 
	 * @param path  path the step is on
	 * @param rank  of the step
	 * @param graph the graph the step is in
	 */
	public StepEndPositionIRI(P path, long rank, PathHandleGraphSail<P, S, ?, ?> graph) {
		this(path, rank, graph, UNSET_POSITION);
	}

	@Override
	public String getLocalName() {
		return Long.toString(getEndPosition());
	}

	@Override
	public String stringValue() {
		return getNamespace() + '/' + getLocalName();
	}

	@Override
	public String toString() {
		return stringValue();
	}

	@Override
	public int hashCode() {
		return stringValue().hashCode();
	}

	private boolean equalsBeginStep(Object obj) {
		StepBeginPositionIRI<?, ?> other = (StepBeginPositionIRI<?, ?>) obj;
		if (!Objects.equals(this.path(), other.path())) {
			return false;
		}
		if (!Objects.equals(this.graph, other.graph())) {
			return false;
		}
		if (this.rank == other.rank()) {
			return false;
		} else {
			StepBeginPositionIRI<P, S> other2 = (StepBeginPositionIRI<P, S>) obj;
			long ourposition = this.getEndPosition();
			long thatPosition = other2.getBeginPosition();
			return ourposition == thatPosition;

		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof StepBeginPositionIRI<?, ?>) {
			return equalsBeginStep(obj);
		} else if (obj instanceof StepEndPositionIRI<?, ?>) {
			return equalsEndStep(obj);
		} else if (obj instanceof IRI) {
			return stringValue().equals(((IRI) obj).stringValue());
		}
		return true;
	}

	private boolean equalsEndStep(Object obj) {
		StepEndPositionIRI<?, ?> other = (StepEndPositionIRI<?, ?>) obj;
		if (!Objects.equals(this.path(), other.path())) {
			return false;
		}
		if (!Objects.equals(this.graph, other.graph)) {
			return false;
		}
		if (this.rank == other.rank()) {
			return true;
		} else {
			StepEndPositionIRI<P, S> other2 = (StepEndPositionIRI<P, S>) obj;
			long ourposition = this.getEndPosition();
			long thatPosition = other2.getEndPosition();
			return ourposition == thatPosition;
		}
	}

	@Override
	public long getBeginPosition() {
		PathGraph<P, S, ?, ?> pg = graph.pathGraph();
		S s = pg.stepByRankAndPath(path(), rank());
		return graph.pathGraph().beginPositionOfStep(s);
	}

	@Override
	public long getEndPosition() {
		if (hasCachedPosition(position)) {
			return position;
		} else {
			PathGraph<P, S, ?, ?> pg = graph.pathGraph();
			S s = pg.stepByRankAndPath(path(), rank());
			return graph.pathGraph().endPositionOfStep(s);
		}
	}
}
