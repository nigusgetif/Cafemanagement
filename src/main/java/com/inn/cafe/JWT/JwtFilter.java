package com.inn.cafe.JWT;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtill jwtUtill;
    @Autowired
    private CustomerUserDetailsService customerUserDetailsService;
    Claims claims;
    private String userName = null;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
         if (httpServletRequest.getServletPath().matches("/user/login|/user/forgotPassword|/user/signUp")) {
              filterChain.doFilter(httpServletRequest,httpServletResponse);
           }else {
             String authorizatioonHeader = httpServletRequest.getHeader("Authorization");
             String token = null;
             if (authorizatioonHeader != null && authorizatioonHeader.startsWith("Bearer ")){
                 token = authorizatioonHeader.substring(7);
                 userName =jwtUtill.extractUsername(token);
                 claims = jwtUtill.extractAllClaims(token);
             }
             if (userName != null && SecurityContextHolder.getContext().getAuthentication()==null){
                 UserDetails userDetails = customerUserDetailsService.loadUserByUsername(userName);
                 if (jwtUtill.validateToken(token,userDetails)){
                     UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                             new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
                     usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
                     SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                 }

             }
             filterChain.doFilter(httpServletRequest,httpServletResponse);
         }

        }

    public boolean isAdmin(){
        return "admin".equalsIgnoreCase((String) claims.get("role"));

   }
    public boolean isUser(){
        return "user".equalsIgnoreCase((String) claims.get("role"));

    }
    public String getCurrentUser(){
        return userName;
    }


}
