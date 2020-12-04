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
package io.github.vgteam.handlegraph4j.sparql;

import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.PathGraph;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import io.github.vgteam.handlegraph4j.gfa1.GFA1Reader;
import io.github.vgteam.handlegraph4j.gfa1.line.PathLine.Step;
import io.github.vgteam.handlegraph4j.sparql.values.HandleGraphValueFactory;
import java.util.Arrays;
import java.util.Optional;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.sail.SailConnection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import sib.swiss.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import sib.swiss.swissprot.handlegraph4j.simple.SimpleNodeHandle;
import sib.swiss.swissprot.handlegraph4j.simple.SimplePathGraph;
import sib.swiss.swissprot.handlegraph4j.simple.SimplePathHandle;
import sib.swiss.swissprot.handlegraph4j.simple.SimpleStepHandle;
import sib.swiss.swissprot.handlegraph4j.simple.builders.SimplePathGraphFromGFA1Builder;
import sib.swiss.swissprot.handlegraph4jrdf.VG;

/**
 *
 * @author Jerven Bolleman <jerven.bolleman@sib.swiss>
 */
public class PathHandleGraphSailTest {

    private static final String EXAMPLE_BASE = "http://example.org/vg/";
    private static final String TEST_DATA = "H\tVN:Z:1.0\n"
            + "S\t1\tCAAATAAG\n"
            + "S\t2\tA\n"
            + "S\t3\tG\n"
            + "S\t4\tT\n"
            + "S\t5\tC\n"
            + "S\t6\tTTG\n"
            + "S\t7\tA\n"
            + "S\t8\tG\n"
            + "S\t9\tAAATTTTCTGGAGTTCTAT\n"
            + "S\t10\tA\n"
            + "S\t11\tT\n"
            + "S\t12\tATAT\n"
            + "S\t13\tA\n"
            + "S\t14\tT\n"
            + "S\t15\tCCAACTCTCTG\n"
            + "P\tx\t1+,3+,5+,6+,8+,9+,11+,12+,14+,15+,3+\t8M,1M,1M,3M,1M,19M,1M,4M,1M,11M\n"
            + "L\t1\t+\t2\t+\t0M\n"
            + "L\t1\t+\t3\t+\t0M\n"
            + "L\t2\t+\t4\t+\t0M\n"
            + "L\t2\t+\t5\t+\t0M\n"
            + "L\t3\t+\t4\t+\t0M\n"
            + "L\t3\t+\t5\t+\t0M\n"
            + "L\t4\t+\t6\t+\t0M\n"
            + "L\t5\t+\t6\t+\t0M\n"
            + "L\t6\t+\t7\t+\t0M\n"
            + "L\t6\t+\t8\t+\t0M\n"
            + "L\t7\t+\t9\t+\t0M\n"
            + "L\t8\t+\t9\t+\t0M\n"
            + "L\t9\t+\t10\t+\t0M\n"
            + "L\t9\t+\t11\t+\t0M\n"
            + "L\t10\t+\t12\t+\t0M\n"
            + "L\t11\t+\t12\t+\t0M\n"
            + "L\t12\t+\t13\t+\t0M\n"
            + "L\t12\t+\t14\t+\t0M\n"
            + "L\t13\t+\t15\t+\t0M\n"
            + "L\t14\t+\t15\t+\t0M\n"
            + "L\t15\t+\t3\t+\t0M";

    private final SimplePathGraph spg;

    public PathHandleGraphSailTest() {
        SimplePathGraphFromGFA1Builder simplePathGraphFromGFA1Builder = new SimplePathGraphFromGFA1Builder();
        simplePathGraphFromGFA1Builder.parse(new GFA1Reader(Arrays.asList(TEST_DATA.split("\n")).iterator()));
        spg = simplePathGraphFromGFA1Builder.build();
    }

    /**
     * Test of shutDownInternal method, of class PathHandleGraphSail.
     */
    @Test
    public void testShutDownInternal() {
        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);
        instance.shutDownInternal();
    }

    /**
     * Test of getConnectionInternal method, of class PathHandleGraphSail.
     */
    @Test
    public void testGetConnectionInternal() {
        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);

        try ( SailConnection result = instance.getConnectionInternal()) {
            assertTrue(result instanceof PathHandleGraphTripleSailConnection);
            assertFalse(result.isActive());
        }
    }

    /**
     * Test of isWritable method, of class PathHandleGraphSail.
     */
    @Test
    public void testIsWritable() {
        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);
        assertFalse(instance.isWritable());
    }

    /**
     * Test of getValueFactory method, of class PathHandleGraphSail.
     */
    @Test
    public void testGetValueFactory() {
        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);
        ValueFactory result = instance.getValueFactory();
        assertTrue(result instanceof HandleGraphValueFactory);
    }

    /**
     * Test of getNodeNameSpace method, of class PathHandleGraphSail.
     */
    @Test
    public void testGetNodeNameSpace() {
        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);
        String expResult = EXAMPLE_BASE + "node/";
        String result = instance.getNodeNameSpace();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPathNameSpace method, of class PathHandleGraphSail.
     */
    @Test
    public void testGetPathNameSpace() {
        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);
        String expResult = EXAMPLE_BASE + "path/x";
        PathHandle path = new SimplePathHandle(0);
        String result = instance.getPathNameSpace(path);
        assertEquals(expResult, result);
    }

    /**
     * Test of hasPathNameSpace method, of class PathHandleGraphSail.
     */
    @Test
    public void testHasPathNameSpace() {
        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);
        String expResult = EXAMPLE_BASE + "path/x";
        assertTrue(instance.hasPathNameSpace(expResult));
        assertFalse(instance.hasPathNameSpace(EXAMPLE_BASE + "path/y"));
    }

    /**
     * Test of matchesNodeIriPattern method, of class PathHandleGraphSail.
     */
    @Test
    public void testMatchesNodeIriPattern() {
        String nodeIri = EXAMPLE_BASE + "node/1";
        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);
        assertTrue(instance.matchesNodeIriPattern(nodeIri));
    }

    /**
     * Test of stepFromIriString method, of class PathHandleGraphSail.
     */
    @Test
    public void testStepFromString() {
        String expResult = EXAMPLE_BASE + "path/x";
        String possibleStepIri = expResult + "/step/0";
        PathHandleGraphSail<SimplePathHandle, SimpleStepHandle, SimpleNodeHandle, SimpleEdgeHandle> instance = new PathHandleGraphSail<>(spg, EXAMPLE_BASE);

        assertTrue(instance.hasPathNameSpace(expResult));
        SimplePathHandle path = new SimplePathHandle(0);
        SimpleStepHandle result = instance.stepFromIriString(possibleStepIri);
        SimpleNodeHandle node = spg.getNodeHandle(result);
        assertEquals(new SimpleStepHandle(path.id(), node.id(), 0l), result);
    }

//    /**
//     * Test of extractPath method, of class PathHandleGraphSail.
//     */
//    @Test
//    public void testExtractPath() {
//        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);
//        String expResult = EXAMPLE_BASE + "path/x";
//        String possibleStepIri = expResult + "/step/0";
//        PathHandle result = instance.extractPath(possibleStepIri);
//        assertEquals(expResult, instance.getPathNameSpace(result));
//    }
    /**
     * Test of getPathName method, of class PathHandleGraphSail.
     */
    @Test
    public void testGetPathName() {
        PathHandleGraphSail instance = new PathHandleGraphSail(spg, EXAMPLE_BASE);
        PathHandle path = new SimplePathHandle(0);
        String expResult = "x";
        String result = instance.getPathName(path);
        assertEquals(expResult, result);
    }

    /**
     * Test of pathGraph method, of class PathHandleGraphSail.
     */
    @Test
    public void testPathGraph() {

        SailRepository instance = new SailRepository(new PathHandleGraphSail(spg, EXAMPLE_BASE));

        try ( RepositoryConnection connection = instance.getConnection()) {
            TupleQuery prepareTupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, "PREFIX vg:<" + VG.NAMESPACE + "> SELECT ?node WHERE {?node a vg:Node}");
            try ( TupleQueryResult evaluate = prepareTupleQuery.evaluate()) {
                for (int i = 0; i < 15; i++) {
                    assertTrue(evaluate.hasNext(), "at i:" + i);
                    BindingSet next = evaluate.next();
                    assertNotNull(next);
                }
            }
        }
    }
}
