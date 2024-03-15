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
package swiss.sib.swissprot.sapfhir.sparql;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.impl.DefaultCollectionFactory;
import org.eclipse.rdf4j.collection.factory.mapdb.MapDb3CollectionFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;

import io.github.jervenbolleman.handlegraph4j.EdgeHandle;
import io.github.jervenbolleman.handlegraph4j.NodeHandle;
import io.github.jervenbolleman.handlegraph4j.PathGraph;
import io.github.jervenbolleman.handlegraph4j.PathHandle;
import io.github.jervenbolleman.handlegraph4j.StepHandle;
import swiss.sib.swissprot.sapfhir.values.HandleGraphValueFactory;

/**
 *
 * @author <a href="mailto:jerven.bolleman@sib.swiss">Jerven Bolleman</a>
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class PathHandleGraphSail<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>> extends AbstractSail {

    private static final String NODE_IRI_PART = "node/";
    private static final String PATH_IRI_PART = "path/";
    private static final String STEP_IRI_PART = "step/";
    private final PathGraph<P, S, N, E> pathGraph;
    private final String base;

    public PathHandleGraphSail(PathGraph<P, S, N, E> pathGraph, String base) {
        this.pathGraph = pathGraph;
        this.base = base;
    }

    @Override
    protected void shutDownInternal() throws SailException {
        
    }

    @Override
    protected PathHandleGraphTripleSailConnection<P, S, N, E> getConnectionInternal()
            throws SailException {
        return new PathHandleGraphTripleSailConnection<>(this);
    }

    @Override
    public boolean isWritable() throws SailException {
        return false;
    }

    @Override
    public HandleGraphValueFactory<P, S, N, E> getValueFactory() {
        return new HandleGraphValueFactory<P, S, N, E>(this);
    }

    public String getNodeNameSpace() {
        return base + NODE_IRI_PART;
    }

    public String getPathNameSpace(P path) {
        String nameOfPath = pathGraph.nameOfPath(path);
        if (mightBeHttpOrFtpIri(nameOfPath)) {
            return nameOfPath;
        } else {
            return base + PATH_IRI_PART + getPathName(path);
        }
    }

    public boolean hasPathNameSpace(String namespace) {
        if (namespace.startsWith(base + PATH_IRI_PART)) {
            String pathNamePart = namespace.substring(base.length()
                    + PATH_IRI_PART.length());
            return pathGraph.pathByName(pathNamePart) != null;
        } else if (mightBeHttpOrFtpIri(namespace)) {
            return pathGraph.pathByName(namespace) != null;
        } else {
            return false;
        }
    }

    public boolean matchesNodeIriPattern(String namespace) {
        String nodeIriStart = base + NODE_IRI_PART;
        boolean looksLikeNodeIRI = namespace.startsWith(nodeIriStart);
        if (looksLikeNodeIRI && namespace.length() > nodeIriStart.length()) {
            try {
                Long.parseLong(namespace.substring(nodeIriStart.length()));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public N nodeFromIriString(String possibleNodeIri) {
        String nodeIriStart = base + NODE_IRI_PART;
        boolean looksLikeNodeIRI = possibleNodeIri.startsWith(nodeIriStart);
        int nodeIdStart = nodeIriStart.length();
        if (looksLikeNodeIRI && possibleNodeIri.length() > nodeIdStart) {
            try {
                String nodeIdPart = possibleNodeIri.substring(nodeIdStart);
                long nodeId = Long.parseLong(nodeIdPart);
                return pathGraph.fromLong(nodeId);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public P pathFromIriString(String possiblePathIri) {
        if (possiblePathIri.startsWith(base + PATH_IRI_PART)) {
            return pathGraph.pathByName(possiblePathIri.substring(base.length() + PATH_IRI_PART.length()));
        } else if (mightBeHttpOrFtpIri(possiblePathIri)) {
            return pathGraph.pathByName(possiblePathIri);
        } else {
            return null;
        }
    }

    /**
     * Given an IRI string try to return the S object
     *
     * @param possibleStepIri
     * @return a step or null if not found.
     */
    public S stepFromIriString(String possibleStepIri) {

        S s = extractStepFromBasicPathPattern(possibleStepIri);
        if (s != null) {
            return s;
        }
        return extractStepFromKnownPathName(possibleStepIri);
    }

    private S extractStepFromKnownPathName(String namespace) {
        Pattern endsWithStepPattern = Pattern.compile(STEP_IRI_PART + "(\\d+)$");
        Matcher endPatternMatcher = endsWithStepPattern.matcher(namespace);
        try {
            if (mightBeHttpOrFtpIri(namespace) && endPatternMatcher.find(5)) {
                String rankGroup = endPatternMatcher.group(1);
                long rank = Long.parseLong(rankGroup);
                String pathName = namespace.substring(0, namespace.length()
                        - (rankGroup.length() + 6));
                P pathByName = pathGraph.pathByName(pathName);
                if (pathByName != null) {
                    return pathGraph.stepByRankAndPath(pathByName, rank);
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    private static boolean mightBeHttpOrFtpIri(String namespace) {
        return namespace.startsWith("https://")
                || namespace.startsWith("http://")
                || namespace.startsWith("ftp://");
    }

    private S extractStepFromBasicPathPattern(String namespace) {

        Pattern standardPathNamePattern = Pattern.compile('^' + base
                + PATH_IRI_PART + "(.+)/"
                + STEP_IRI_PART + "(\\d+)");
        Matcher matcher = standardPathNamePattern.matcher(namespace);
        try {
            if (matcher.matches()) {
                P path = pathGraph.pathByName(matcher.group(1));
                if (path != null) {
                    long rank = Long.parseLong(matcher.group(2));
                    return pathGraph.stepByRankAndPath(path, rank);
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    public String getPathName(P path) {
        return pathGraph.nameOfPath(path);
    }

    public PathGraph<P, S, N, E> pathGraph() {
        return pathGraph;
    }

    String getBase() {
        return base;
    }

	@Override
	public Supplier<CollectionFactory> getCollectionFactory() {
		return () -> new MapDb3CollectionFactory(getIterationCacheSyncThreshold(), new DefaultCollectionFactory());
	}

}
