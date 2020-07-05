package com.github.jobson.auth.jwt;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class MockRequestContext implements ContainerRequestContext {

    private final String method;
    private final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
    private SecurityContext ctx = null;

    public MockRequestContext(String method, Map<String, String> headers) {
        this.method = method;

        for (Map.Entry<String, String> e : headers.entrySet()) {
            final ArrayList<String> l = new ArrayList<>();
            l.add(e.getValue());
            this.headers.put(e.getKey(), l);
        }
    }

    @Override
    public Object getProperty(String s) {
        throw new RuntimeException("nyi");
    }

    @Override
    public Collection<String> getPropertyNames() {
        throw new RuntimeException("nyi");
    }

    @Override
    public void setProperty(String s, Object o) {
        throw new RuntimeException("nyi");
    }

    @Override
    public void removeProperty(String s) {
        throw new RuntimeException("nyi");
    }

    @Override
    public UriInfo getUriInfo() {
        throw new RuntimeException("nyi");
    }

    @Override
    public void setRequestUri(URI uri) {
        throw new RuntimeException("nyi");
    }

    @Override
    public void setRequestUri(URI uri, URI uri1) {
        throw new RuntimeException("nyi");
    }

    @Override
    public Request getRequest() {
        throw new RuntimeException("nyi");
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public void setMethod(String s) {
        throw new RuntimeException("nyi");
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return this.headers;
    }

    @Override
    public String getHeaderString(String s) {
        return this.headers.getFirst(s);
    }

    @Override
    public Date getDate() {
        throw new RuntimeException("nyi");
    }

    @Override
    public Locale getLanguage() {
        throw new RuntimeException("nyi");
    }

    @Override
    public int getLength() {
        throw new RuntimeException("nyi");
    }

    @Override
    public MediaType getMediaType() {
        throw new RuntimeException("nyi");
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        throw new RuntimeException("nyi");
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        throw new RuntimeException("nyi");
    }

    @Override
    public Map<String, Cookie> getCookies() {
        throw new RuntimeException("nyi");
    }

    @Override
    public boolean hasEntity() {
        throw new RuntimeException("nyi");
    }

    @Override
    public InputStream getEntityStream() {
        throw new RuntimeException("nyi");
    }

    @Override
    public void setEntityStream(InputStream inputStream) {

    }

    @Override
    public SecurityContext getSecurityContext() {
        return this.ctx;
    }

    @Override
    public void setSecurityContext(SecurityContext securityContext) {
        this.ctx = securityContext;
    }

    @Override
    public void abortWith(Response response) {
        throw new RuntimeException("nyi");
    }
}
