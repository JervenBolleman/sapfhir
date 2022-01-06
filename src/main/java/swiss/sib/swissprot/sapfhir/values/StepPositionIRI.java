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
package swiss.sib.swissprot.sapfhir.values;

import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import org.eclipse.rdf4j.model.IRI;
import swiss.sib.swissprot.sapfhir.sparql.PathHandleGraphSail;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
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
