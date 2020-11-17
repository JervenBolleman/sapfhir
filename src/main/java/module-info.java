/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

module RsHandlegrapSparql {
    requires rdf4j.http.client;
    requires rdf4j.model;
    requires rdf4j.query;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires rdf4j.queryalgebra.model;
    requires rdf4j.queryparser.api;
    requires rdf4j.queryparser.sparql;
    requires rdf4j.queryresultio.api;
    requires rdf4j.queryresultio.sparqlxml;
    requires rdf4j.repository.api;
    requires rdf4j.repository.sparql;
    requires rdf4j.rio.api;
    requires jsonld.java;
    requires httpclient.osgi;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient.fluent;
    requires httpcore.osgi;
    requires org.apache.httpcomponents.httpcore.nio;
    requires org.apache.httpcomponents.httpclient.cache;
    requires org.apache.commons.io;
    requires rdf4j.util;
    requires rdf4j.queryalgebra.evaluation;
    requires org.slf4j;
    requires rdf4j.repository.dataset;
    requires rdf4j.repository.sail;
    requires rdf4j.sail.api;
    requires org.apache.httpcomponents.httpmime;
    requires java.xml.bind;
    requires java.activation;
    requires jextract;
    requires jdk.incubator.foreign;
}
