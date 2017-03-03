/*
 * Copyright © 2016 Northwestern University LIST Lab, Libin Song and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sloth.filter;

import com.google.common.net.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.opendaylight.aaa.api.shiro.principal.ODLPrincipal;
import org.opendaylight.sloth.exception.ServiceUnavailableException;
import org.opendaylight.sloth.service.SlothServiceLocator;
import org.opendaylight.sloth.utils.MultiReadHttpServletRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sloth.permission.rev150105.CheckPermissionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sloth.permission.rev150105.CheckPermissionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sloth.permission.rev150105.CheckPermissionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sloth.permission.rev150105.HttpType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sloth.permission.rev150105.SlothPermissionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sloth.permission.rev150105.check.permission.input.PrincipalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sloth.permission.rev150105.check.permission.input.RequestBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SlothSecurityFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(SlothSecurityFilter.class);
    private final SlothPermissionService slothPermissionService;

    public SlothSecurityFilter() throws ServiceUnavailableException {
        slothPermissionService = SlothServiceLocator.getInstance().getPermissionService();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("SlothSecurityFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // TODO: need to check permission both before and after process
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            Subject subject = SecurityUtils.getSubject();
            ODLPrincipal odlPrincipal = (ODLPrincipal) subject.getPrincipal();
            HttpServletRequest multiReadHttpServletRequest =
                    httpServletRequest.getContentType() != null && httpServletRequest.getContentType().equals(MediaType.JSON_UTF_8.toString()) ?
                            new MultiReadHttpServletRequest(httpServletRequest) : httpServletRequest;
            CheckPermissionInput checkPermissionInput = httpRequestToPermissionInput(odlPrincipal, multiReadHttpServletRequest);
            final Future<RpcResult<CheckPermissionOutput>> rpcResultFuture = slothPermissionService.checkPermission(checkPermissionInput);
            try {
                RpcResult<CheckPermissionOutput> rpcResult = rpcResultFuture.get();
                if (rpcResult.isSuccessful()) {
                    LOG.info("SlothSecurityFilter, check permission successful");
                    if (rpcResult.getResult().getStatusCode() / 100 == 2) {
                        chain.doFilter(multiReadHttpServletRequest, response);
                    } else {
                        response.getWriter().write("status code: " + rpcResult.getResult().getStatusCode() +
                                ", response: " + rpcResult.getResult().getResponse());
                    }
                } else {
                    LOG.warn("SlothSecurityFilter, check permission unsuccessful");
                    response.getWriter().write("failed to check permission");
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                LOG.error("SlothSecurityFilter, check permission exception");
                response.getWriter().write("exception during check permission: " + e.toString());
            }
        } else {
            LOG.warn("not http request, no permission check");
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        LOG.info("SlothSecurityFilter destroyed");
    }

    private static CheckPermissionInput httpRequestToPermissionInput(ODLPrincipal odlPrincipal, HttpServletRequest request) {
        PrincipalBuilder principalBuilder = new PrincipalBuilder();
        RequestBuilder requestBuilder = new RequestBuilder();
        CheckPermissionInputBuilder inputBuilder = new CheckPermissionInputBuilder();

        LOG.info("user-id: " + odlPrincipal.getUserId() + ", user-name: " + odlPrincipal.getUserId() +
                ", domain: " + odlPrincipal.getDomain() + ", roles: " + String.join(", ", odlPrincipal.getRoles()));

        principalBuilder.setUserName(odlPrincipal.getUsername()).setUserId(odlPrincipal.getUserId())
                .setDomain(odlPrincipal.getDomain()).setRoles(new ArrayList<>(odlPrincipal.getRoles()));

        requestBuilder.setMethod(HttpType.valueOf(request.getMethod())).setRequestUrl(request.getRequestURI())
                .setQueryString(request.getQueryString());
        try {
            if (request.getContentType() != null && request.getContentType().equals(MediaType.JSON_UTF_8.toString())) {
                requestBuilder.setJsonBody(IOUtils.toString(request.getReader()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("failed to get json body from http servlet request");
        }
        return inputBuilder.setPrincipal(principalBuilder.build()).setRequest(requestBuilder.build()).build();
    }
}
