package com.veo.backend.service.impl;

import com.veo.backend.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
    private static final String SECRET_KEY = "your-256-bit-secret-your-256-bit-secret";


    @Override
    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities()
                .iterator().next().getAuthority();

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + 86400000)
                )
                .signWith(
                        Keys.hmacShaKeyFor(SECRET_KEY.getBytes()),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token)
                .equals(userDetails.getUsername()) && !isExpired(token);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(
                        Keys.hmacShaKeyFor(SECRET_KEY.getBytes())
                )
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }
}
