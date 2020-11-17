/*
 * Copyright (C) 2020 SIB Swiss Institute of Bioinformatics.
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

module Sapfhir {
//    requires rdf4j.sail.api;
    
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires rdf4j.query;
//    requires rdf4j.queryalgebra.model;
    requires rdf4j.queryalgebra.evaluation;
    requires rdf4j.model;
    requires rdf4j.util;
    requires org.slf4j;
    
    requires rdf4j.http.client;
    requires rdf4j.http.protocol;
//    requires rdf4j.queryresultio.api;
    requires rdf4j.queryresultio.binary;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.commons.codec;
    requires org.apache.commons.logging;
    requires mapdb;
    requires com.google.common;
    requires failureaccess;
    requires listenablefuture;
    requires jsr305;
    requires org.checkerframework.checker.qual;
    requires com.google.errorprone.annotations;
    requires j2objc.annotations;
    requires rdf4j.queryparser.serql;
    requires rdf4j.queryrender;
    requires rdf4j.queryresultio.sparqljson;
    requires rdf4j.queryresultio.text;
    requires opencsv;
    requires commons.beanutils;
    requires commons.collections;
    requires org.apache.commons.collections4;
    requires rdf4j.repository.contextaware;
    requires rdf4j.repository.event;
    requires rdf4j.repository.http;
    requires rdf4j.repository.manager;
    requires rdf4j.rio.binary;
    requires rdf4j.rio.datatypes;
    requires rdf4j.rio.jsonld;
//    requires org.apache.httpcomponents.httpclient.cache;
    requires org.apache.commons.io;
    requires rdf4j.rio.languages;
    requires rdf4j.rio.n3;
    requires rdf4j.rio.nquads;
    requires rdf4j.rio.ntriples;
    requires rdf4j.rio.rdfjson;
    requires rdf4j.rio.rdfxml;
    requires rdf4j.rio.trig;
    requires rdf4j.rio.trix;
    requires rdf4j.rio.turtle;
    requires rdf4j.sparqlbuilder;
    requires rdf4j.queryalgebra.geosparql;
    requires spatial4j;
    requires org.locationtech.jts;
    requires rdf4j.sail.base;
    requires rdf4j.sail.federation;
    requires rdf4j.sail.inferencer;
    requires rdf4j.sail.model;
    requires rdf4j.sail.elasticsearch;
//    requires rdf4j.sail.lucene.api;
    requires rdf4j.sail.lucene;
    requires lucene.core;
    requires lucene.queries;
    requires lucene.highlighter;
    requires lucene.join;
    requires lucene.memory;
//    requires lucene.analyzers.common;
    requires lucene.queryparser;
//    requires lucene.sandbox;
    requires lucene.spatial.extras;
    requires lucene.spatial3d;
    requires s2.geometry.library.java;
    requires rdf4j.sail.solr;
    requires solr.solrj;
    requires commons.math3;
    requires org.apache.httpcomponents.httpmime;
    requires zookeeper;
    requires stax2.api;
    requires woodstox.core.asl;
    requires noggit;
    requires rdf4j.sail.memory;
    requires rdf4j.sail.nativerdf;
    requires rdf4j.sail.spin;
//    requires rdf4j.lucene.spin;
//    requires rdf4j.spin;
    requires org.apache.commons.text;
    requires rdf4j.shacl;
    requires org.apache.commons.lang3;
    requires rdf4j.repository.dataset;
    requires rdf4j.repository.sail;
//    requires rdf4j.repository.api;
    requires rdf4j.rio.api;
    requires java.xml.bind;
    requires java.activation;
    requires jsonld.java;
//    requires httpclient.osgi;
    requires org.apache.httpcomponents.httpclient.fluent;
//    requires httpcore.osgi;
    requires org.apache.httpcomponents.httpcore.nio;
    requires rdf4j.repository.sparql;
//    requires rdf4j.queryparser.api;
    requires rdf4j.queryparser.sparql;
    requires rdf4j.queryresultio.sparqlxml;
    requires jextract;
    requires jdk.incubator.foreign;
}
