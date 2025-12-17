package io.github.luminion.autoconfigure.servlet.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;
import java.util.function.Function;

/**
 *
 * @author luminion
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {
    private final Function<String,String> func;

    public XssRequestWrapper(HttpServletRequest request, Function<String,String> func) {
        super(request);
        this.func = func;
    }

    @Override
    public String getParameter(String name) {
        String val = super.getParameter(name);
        return sanitize(val);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] vals = super.getParameterValues(name);
        if (vals == null) return null;
        String[] res = new String[vals.length];
        for (int i = 0; i < vals.length; i++) res[i] = sanitize(vals[i]);
        return res;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> src = super.getParameterMap();
        Map<String, String[]> dst = new LinkedHashMap<>(src.size());
        src.forEach((k, v) -> {
            String[] nv = new String[v.length];
            for (int i = 0; i < v.length; i++) nv[i] = sanitize(v[i]);
            dst.put(k, nv);
        });
        return dst;
    }

    @Override
    public String getHeader(String name) {
        return sanitize(super.getHeader(name));
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> list = Collections.list(super.getHeaders(name));
        List<String> sanitized = new ArrayList<>(list.size());
        for (String v : list) sanitized.add(sanitize(v));
        return Collections.enumeration(sanitized);
    }

    @Override
    public String getQueryString() {
        return sanitize(super.getQueryString());
    }

    private String sanitize(String s) {
        if (s == null) return null;
        return func.apply(s);
    }
}