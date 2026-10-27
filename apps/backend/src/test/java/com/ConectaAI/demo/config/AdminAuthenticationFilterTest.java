package com.ConectaAI.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminAuthenticationFilterTest {

    @Test
    void allowsPublicRoutesWithoutAnAdminToken() throws Exception {
        AdminAuthenticationFilter filter = new AdminAuthenticationFilter("admin-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void deniesAnAdminRouteWithoutToken() throws Exception {
        AdminAuthenticationFilter filter = new AdminAuthenticationFilter("admin-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/config");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Acesso administrativo não autorizado");
    }

    @Test
    void allowsAnAdminRouteWithTheConfiguredToken() throws Exception {
        AdminAuthenticationFilter filter = new AdminAuthenticationFilter("admin-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/products");
        request.addHeader(AdminAuthenticationFilter.ADMIN_TOKEN_HEADER, "admin-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void failsClosedWhenNoAdministrativeTokenIsConfigured() throws Exception {
        AdminAuthenticationFilter filter = new AdminAuthenticationFilter(" ");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/handoffs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(503);
    }

    @Test
    void allowsCorsPreflightSoTheAuthenticatedBrowserRequestCanBeMade() throws Exception {
        AdminAuthenticationFilter filter = new AdminAuthenticationFilter("admin-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/admin/config");
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
