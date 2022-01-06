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
package swiss.sib.swissprot.sapfhir.sparql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailConnection;
import org.junit.jupiter.api.Test;

import io.github.vgteam.handlegraph4j.gfa1.GFA1Reader;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleEdgeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleNodeHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathGraph;
import swiss.sib.swissprot.handlegraph4j.simple.SimplePathHandle;
import swiss.sib.swissprot.handlegraph4j.simple.SimpleStepHandle;
import swiss.sib.swissprot.handlegraph4j.simple.builders.SimplePathGraphFromGFA1Builder;
import swiss.sib.swissprot.handlegraph4jrdf.FALDO;
import swiss.sib.swissprot.handlegraph4jrdf.VG;
import swiss.sib.swissprot.sapfhir.values.HandleGraphValueFactory;
import swiss.sib.swissprot.sapfhir.values.StepBeginPositionIRI;
import swiss.sib.swissprot.sapfhir.values.StepIRI;

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
		var instance = getPathHandleGraphSail();
		instance.shutDownInternal();
	}

	/**
	 * Test of getConnectionInternal method, of class PathHandleGraphSail.
	 */
	@Test
	public void testGetConnectionInternal() {
		var instance = getPathHandleGraphSail();

		try (SailConnection result = instance.getConnectionInternal()) {
			assertTrue(result instanceof PathHandleGraphTripleSailConnection);
			assertFalse(result.isActive());
		}
	}

	/**
	 * Test of isWritable method, of class PathHandleGraphSail.
	 */
	@Test
	public void testIsWritable() {
		var instance = getPathHandleGraphSail();
		assertFalse(instance.isWritable());
	}

	/**
	 * Test of getValueFactory method, of class PathHandleGraphSail.
	 */
	@Test
	public void testGetValueFactory() {
		var instance = getPathHandleGraphSail();
		ValueFactory result = instance.getValueFactory();
		assertTrue(result instanceof HandleGraphValueFactory);
	}

	/**
	 * Test of getNodeNameSpace method, of class PathHandleGraphSail.
	 */
	@Test
	public void testGetNodeNameSpace() {
		var instance = getPathHandleGraphSail();
		String expResult = EXAMPLE_BASE + "node/";
		String result = instance.getNodeNameSpace();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getPathNameSpace method, of class PathHandleGraphSail.
	 */
	@Test
	public void testGetPathNameSpace() {
		var instance = getPathHandleGraphSail();
		String expResult = EXAMPLE_BASE + "path/x";
		SimplePathHandle path = new SimplePathHandle(1);
		String result = instance.getPathNameSpace(path);
		assertEquals(expResult, result);
	}

	/**
	 * Test of hasPathNameSpace method, of class PathHandleGraphSail.
	 */
	@Test
	public void testHasPathNameSpace() {
		var instance = getPathHandleGraphSail();
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
		var instance = getPathHandleGraphSail();
		assertTrue(instance.matchesNodeIriPattern(nodeIri));
	}

	/**
	 * Test of stepFromIriString method, of class PathHandleGraphSail.
	 */
	@Test
	public void testStepFromString() {
		String expResult = EXAMPLE_BASE + "path/x";
		String possibleStepIri = expResult + "/step/0";
		var instance = getPathHandleGraphSail();

		assertTrue(instance.hasPathNameSpace(expResult));
		SimplePathHandle path = new SimplePathHandle(1);
		SimpleStepHandle result = instance.stepFromIriString(possibleStepIri);
		SimpleNodeHandle node = spg.nodeOfStep(result);
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
		var phgs = getPathHandleGraphSail();
		SimplePathHandle path = new SimplePathHandle(1);
		String expResult = "x";
		String result = phgs.getPathName(path);
		assertEquals(expResult, result);
	}

	/**
	 * Test of pathGraph method, of class PathHandleGraphSail.
	 */
	@Test
	public void testCountNodeQuery() {

		SailRepository instance = getSailRepository();
		Consumer<TupleQueryResult> test = r -> {
			assertTrue(r.hasNext());
			BindingSet next = r.next();
			assertNotNull(next);
			Literal value = (Literal) next.getBinding("nodes").getValue();
			assertEquals(spg.nodeCount(), value.longValue());
			assertFalse(r.hasNext());
		};
		String nodes = "SELECT (COUNT(?node) AS ?nodes) WHERE {?node a vg:Node}";
		try (RepositoryConnection connection = instance.getConnection()) {
			evaluate(nodes, connection, test);
		}
	}

	@Test
	public void testAll() {
		SailRepository instance = getSailRepository();

		Consumer<TupleQueryResult> test = (r) -> {
			while (r.hasNext()) {
				BindingSet next = r.next();
				assertNotNull(next);
				IRI node = (IRI) next.getBinding("s").getValue();
				assertNotNull(node);
				IRI binding = (IRI) next.getBinding("p").getValue();
				assertNotNull(binding);
			}
			assertFalse(r.hasNext());
		};

		String all = "SELECT ?s ?p ?o \n" + "WHERE {?s ?p ?o .\n" + "} ";
		try (RepositoryConnection connection = instance.getConnection()) {
			evaluate(all, connection, test);
		}
		Consumer<TupleQueryResult> test2 = (r) -> {
			assertTrue(r.hasNext());
			BindingSet next = r.next();
			assertNotNull(next);
			Literal node = (Literal) next.getBinding("c").getValue();
			assertNotNull(node);
			assertFalse(r.hasNext());
		};
		String countall = "SELECT (COUNT(?s) AS ?c)\n" + "WHERE {?s ?p ?o .\n" + "} ";
		try (RepositoryConnection connection = instance.getConnection()) {
			evaluate(countall, connection, test2);
		}
	}

	@Test
	public void testNodeOrderQuery() {
		SailRepository instance = getSailRepository();

		Consumer<TupleQueryResult> test = (r) -> {
			for (int i = 0; i < spg.nodeCount(); i++) {
				assertTrue(r.hasNext(), "at i:" + i);
				BindingSet next = r.next();
				assertNotNull(next);
				IRI node = (IRI) next.getBinding("node").getValue();
				assertNotNull(node);
				Literal binding = (Literal) next.getBinding("id").getValue();
				assertNotNull(binding);
				assertEquals(i + 1, binding.intValue());
			}
			assertFalse(r.hasNext());
		};

		String select_node_WHERE_node_a_vgNode = "SELECT ?node ?id \n" + "WHERE {?node a vg:Node .\n"
				+ "BIND(xsd:int(SUBSTR(STR(?node),28)) AS ?id)\n" + "} ORDER BY ?id";
		try (RepositoryConnection connection = instance.getConnection()) {
			evaluate(select_node_WHERE_node_a_vgNode, connection, test);
		}
	}

	public void evaluate(String query, RepositoryConnection conn, Consumer<TupleQueryResult> evaluate) {
		TupleQuery prepareTupleQuery = prepareQuery(conn, query);
		try (TupleQueryResult result = prepareTupleQuery.evaluate()) {
			evaluate.accept(result);
		}
	}

	@Test
	public void testStepQueries() {
		SailRepository instance = getSailRepository();
		String allSteps = "SELECT ?step WHERE {?step a vg:Step}";
		String allStepsHaveARank = "SELECT ?step WHERE {?step a vg:Step ; vg:rank ?rank}";

		Consumer<TupleQueryResult> test = r -> {
			for (int i = 0; i < 11; i++) {
				assertTrue(r.hasNext(), "at i:" + i);
				BindingSet next = r.next();
				assertNotNull(next);
				Value step = next.getValue("step");
				assertNotNull(step);
				assertTrue(step instanceof StepIRI);
				StepIRI stepIri = (StepIRI) step;
				assertEquals(stepIri.rank(), i);
			}
			assertFalse(r.hasNext());
		};
		try (RepositoryConnection connection = instance.getConnection()) {

			evaluate(allSteps, connection, test);

			evaluate(allStepsHaveARank, connection, test);

		}
	}

	@Test
	public void testQueryReorderingValueQueries() {
		var pghs = getPathHandleGraphSail();
		String prefixes = "PREFIX faldo:<" + FALDO.NAMESPACE + "> " + "PREFIX vg:<" + VG.NAMESPACE + "> ";

		String allReorder = prefixes + "SELECT ?value WHERE {?node rdf:value ?value ; a vg:Node }";

		PathHandleGraphTripleSailConnection<SimplePathHandle, SimpleStepHandle, SimpleNodeHandle, SimpleEdgeHandle> connectionInternal = pghs
				.getConnectionInternal();
		try (var connection = connectionInternal) {
			PathHandleGraphTripleSource<SimplePathHandle, SimpleStepHandle, SimpleNodeHandle, SimpleEdgeHandle> tripleSource = connection
					.tripleSource();

			ParsedQuery toReorder = new SPARQLParser().parseQuery(allReorder, EXAMPLE_BASE);
			TupleExpr optimized = connection.optimize(tripleSource, connection.evalutationStrategy(tripleSource),
					toReorder.getTupleExpr(), new EmptyBindingSet());

			// TODO: actually test the optimizer did the right thing.
			assertNotEquals(toReorder, optimized);
		}
	}

	@Test
	public void testAllQueries() {
		SailRepository instance = getSailRepository();
		String all = "SELECT ?s ?p ?o WHERE {?s ?p ?o}";
		AtomicInteger in = new AtomicInteger();
		Consumer<TupleQueryResult> test = r -> {
			while (r.hasNext()) {
				BindingSet next = r.next();
				assertNotNull(next);
				var value = next.getBinding("p").getValue();
				assertTrue(value instanceof IRI);
				in.incrementAndGet();
			}
		};
		try (RepositoryConnection connection = instance.getConnection()) {

			evaluate(all, connection, test);
		}

		String countAll = "SELECT (COUNT(?s) AS ?c) WHERE {?s ?p ?o}";

		Consumer<TupleQueryResult> test2 = r -> {
			assertTrue(r.hasNext());
			BindingSet next = r.next();
			assertNotNull(next);
			var value = next.getBinding("c").getValue();
			assertTrue(value instanceof Literal);
			var l = (Literal) value;
			assertEquals(in.get(), l.intValue());
			assertFalse(r.hasNext());
		};
		try (RepositoryConnection connection = instance.getConnection()) {

			evaluate(countAll, connection, test2);
		}
	}

	@Test
	public void testStepBeginQueries() {
		SailRepository instance = getSailRepository();
		Consumer<TupleQueryResult> test = r -> {
			for (int i = 0; i < 11; i++) {
				assertTrue(r.hasNext(), "at i:" + i);
				BindingSet next = r.next();
				Value begin = next.getBinding("begin").getValue();
				assertTrue(begin instanceof StepBeginPositionIRI);

				assertNotNull(next);
			}
			assertFalse(r.hasNext());
		};

		Consumer<TupleQueryResult> test2 = r -> {
			for (int i = 0; i < 11; i++) {
				assertTrue(r.hasNext(), "at i:" + i);
				BindingSet next = r.next();
				Value begin = next.getBinding("begin").getValue();
				assertTrue(begin instanceof Literal);
				long pos = ((Literal) begin).longValue();
				assertTrue(pos >= 0);
				assertNotNull(next);
			}
			assertFalse(r.hasNext());
		};
		String beginIris = "SELECT ?begin WHERE {?step a vg:Step ; faldo:begin ?begin}";
		String beginPositions = "SELECT ?begin WHERE {?step a vg:Step ; faldo:begin ?pos . ?pos faldo:position ?begin}";
		try (RepositoryConnection connection = instance.getConnection()) {

			evaluate(beginIris, connection, test);
			evaluate(beginPositions, connection, test2);
		}
	}

	@Test
	public void testPathQueries() {
		SailRepository instance = getSailRepository();
		String paths = "SELECT ?path WHERE {?path a vg:Path}";
		String pathLabels = "SELECT ?path ?label WHERE {?path a vg:Path ; rdfs:label ?label}";
		Consumer<TupleQueryResult> test = r -> {
			for (int i = 0; i < 1; i++) {
				assertTrue(r.hasNext(), "at i:" + i);
				assertTrue(r.hasNext(), "at i:" + i);
				BindingSet next = r.next();
				assertNotNull(next);
			}
			assertFalse(r.hasNext());
		};
		try (RepositoryConnection connection = instance.getConnection()) {

			evaluate(paths, connection, test);
			evaluate(pathLabels, connection, test);
		}
	}

	private SailRepository getSailRepository() {
		var pghs = getPathHandleGraphSail();
		SailRepository instance = new SailRepository(pghs);
		return instance;
	}

	private PathHandleGraphSail<SimplePathHandle, SimpleStepHandle, SimpleNodeHandle, SimpleEdgeHandle> getPathHandleGraphSail() {
		var pghs = new PathHandleGraphSail<>(spg, EXAMPLE_BASE);
		return pghs;
	}

	private static TupleQuery prepareQuery(final RepositoryConnection connection, String query)
			throws MalformedQueryException, RepositoryException {
		return connection.prepareTupleQuery(QueryLanguage.SPARQL,
				"PREFIX faldo:<" + FALDO.NAMESPACE + "> " + "PREFIX vg:<" + VG.NAMESPACE + "> " + query);
	}

	@Test
	public void testNodeSequenceQuery() {
		SailRepository instance = getSailRepository();

		Consumer<TupleQueryResult> test = (r) -> {
			for (int i = 0; i < spg.nodeCount(); i++) {
				assertTrue(r.hasNext(), "at i:" + i);
				BindingSet next = r.next();
				assertNotNull(next);
				IRI node = (IRI) next.getBinding("node").getValue();
				assertNotNull(node);
				Literal binding = (Literal) next.getBinding("sequence").getValue();
				assertNotNull(binding);
				assertTrue(binding.stringValue().length() >= 1);
			}
			assertFalse(r.hasNext());
		};

		String select_node_WHERE_node_a_vgNode = "SELECT ?node ?sequence \n" + "WHERE {?node a vg:Node .\n"
				+ "?node rdf:value ?sequence .\n" + "}";
		try (RepositoryConnection connection = instance.getConnection()) {
			evaluate(select_node_WHERE_node_a_vgNode, connection, test);
		}
	}
}
