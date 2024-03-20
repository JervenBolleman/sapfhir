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

import org.eclipse.rdf4j.model.IRI;

import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;

/**
 * Hiding a begin or end position in an IRI allowing for fast compares
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 */
public sealed interface StepPositionIRI<P extends PathHandle, S extends StepHandle> extends IRI
		permits StepBeginPositionIRI, StepEndPositionIRI {

	/**
	 * The default separator between the path part of the iri and the actual
	 * position value
	 */
	public static final String POSITION = "/position/";

	@Override
	public default String getNamespace() {
		return graph().getPathNameSpace(path()) + POSITION;
	}

	/**
	 * A position in use is always 0 or greater, however we use this field to mark
	 * that we don't know the position of the step and therefore it needs to
	 * recalculated.
	 * 
	 * -404 is used to look like 404 NOT FOUND from http to make it easier to spot.
	 */
	static final long UNSET_POSITION = -404;

	/**
	 * The path the step is on
	 * 
	 * @return the path
	 */
	public P path();

	/**
	 * The rank of the step along the path
	 * 
	 * @return the rank of the step
	 */
	public long rank();

	/**
	 * Graph that the step is in
	 * 
	 * @return the graph in which this step position is in
	 */
	public PathHandleGraphSail<P, S, ?, ?> graph();

	/**
	 * Positions may be cached for performance but this is not guaranteed
	 * 
	 * @param position to check if it is cached or not
	 * @return if true the position is cached
	 */
	default boolean hasCachedPosition(long position) {
		return position != UNSET_POSITION;
	};

	/**
	 * The end position of this step along the path in basepair space
	 * 
	 * @return the endposition
	 */
	public abstract long getEndPosition();

	/**
	 * The begin position of this step along the path in basepair space
	 * 
	 * @return the begin position
	 */
	public abstract long getBeginPosition();
}
