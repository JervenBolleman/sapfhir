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
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 */
public abstract class StepPositionIRI<P extends PathHandle, S extends StepHandle> implements IRI {

	private static final long serialVersionUID = 1;
	private static final int POSITION_NOT_SET = -404;
    public static final String POSITION = "/position/";
    protected final long position;
    protected final P path;
    protected final long rank;
    protected final PathHandleGraphSail<P, S, ?, ?> graph;

    protected StepPositionIRI(P path, long rank, PathHandleGraphSail<P, S, ?, ?> graph) {
        this.position = POSITION_NOT_SET;
        this.path = path;
        this.rank = rank;
        this.graph = graph;
    }

    protected StepPositionIRI(long position, P path, long rank, PathHandleGraphSail<P, S, ?, ?> graph) {
        this.position = position;
        this.path = path;
        this.rank = rank;
        this.graph = graph;
    }

    public P path() {
        return path;
    }

    public long rank() {
        return rank;
    }

    protected boolean hasCachedPosition() {
        return position != POSITION_NOT_SET;
    }

    public abstract long getEndPosition();

    public abstract long getBeginPosition();
}
