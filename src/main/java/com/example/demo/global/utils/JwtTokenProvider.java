package com.example.demo.global.utils;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

	@Value("${token.access-expired-time}")
	private long ACCESS_EXPIRED_TIME;

	@Value("${token.refresh-expired-time}")
	private long REFRESH_EXPIRED_TIME;

	@Value("${token.secret}")
	private String SECRET;

	//Token 생성
	public String createJwtAccessToken(String userId, String uri, List<String> roles) {
		Claims claims = Jwts.claims().setSubject(userId);
		claims.put("roles", roles);

		return Jwts.builder()
				.addClaims(claims)
				.setExpiration(
						new Date(System.currentTimeMillis() + ACCESS_EXPIRED_TIME)
				)
				.setIssuedAt(new Date())
				.signWith(SignatureAlgorithm.HS512, SECRET)
				.setIssuer(uri)
				.compact();
	}

	//Token에 담긴 Claim값을 반환하는 로직.
	private Claims getClaimsFromJwtToken(String token) {
		try {
			return Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody();
		}catch(ExpiredJwtException e) {   //Token이 만료된 경우 Exception이 발생한다.
			return e.getClaims();
		}
	}

	public Date getExpiredTime(String token) {
		return getClaimsFromJwtToken(token).getExpiration();
	}

	public List<String> getRoles(String token) {
		return (List<String>) getClaimsFromJwtToken(token).get("roles");
	}

	//Token 유효성 검사.
	public boolean validateJwtToken(String token) {
		try {
			Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token);
			return true;
		} catch (SignatureException e) {
			log.error("Invalid JWT signature: {}", e.getMessage());
			return false;
		} catch (MalformedJwtException e) {
			log.error("Invalid JWT token: {}", e.getMessage());
			return false;
		} catch (ExpiredJwtException e) {
			log.error("JWT token is expired: {}", e.getMessage());
			return false;
		} catch (UnsupportedJwtException e) {
			log.error("JWT token is unsupported: {}", e.getMessage());
			return false;
		} catch (IllegalArgumentException e) {
			log.error("JWT claims string is empty: {}", e.getMessage());
			return false;
		}
	}

	public String createJwtRefreshToken() {
		Claims claims = Jwts.claims();
		claims.put("value", UUID.randomUUID());

		return Jwts.builder()
				.addClaims(claims)
				.setExpiration(
						new Date(System.currentTimeMillis() + REFRESH_EXPIRED_TIME)
				)
				.setIssuedAt(new Date())
				.signWith(SignatureAlgorithm.HS512, SECRET)
				.compact();
	}

	public String getUserId(String token) {
		return getClaimsFromJwtToken(token).getSubject();
	}

	public boolean equalRefreshTokenId(String refreshTokenId, String refreshToken) {
		String compareToken = this.getRefreshTokenId(refreshToken);
		return refreshTokenId.equals(compareToken);
	}

	public String getRefreshTokenId(String token) {
		return getClaimsFromJwtToken(token).get("value").toString();
	}

}
