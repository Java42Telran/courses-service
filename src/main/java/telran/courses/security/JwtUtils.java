package telran.courses.security;

import java.util.Date;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Component
public class JwtUtils {
	static Logger LOG = LoggerFactory.getLogger(JwtUtils.class);
	@Value("${app.exiration.minutes: 60}")
	long expPeriodMin;
	@Value("${app.jwt.secret}")
	String secret;
	/**
	 * 
	 * @param username
	 * @return jwt
	 */
	public String create(String username) {
		Date currentDate = new Date();
		return Jwts.builder()
		.setExpiration(getExpDate(currentDate))
		.setIssuedAt(currentDate)
		.setSubject(username)
		.signWith(SignatureAlgorithm.HS512, secret)
		.compact();
	}
	/**
	 * 
	 * @param jwt
	 * @return username
	 */
	String validate(String jwt) {
		try {
			return Jwts.parser().setSigningKey(secret).parseClaimsJws(jwt).getBody().getSubject();
		} catch (ExpiredJwtException e) {
			LOG.warn("JWT expired; expiration period {} minutes", expPeriodMin);
		} catch (UnsupportedJwtException e) {
			LOG.error("signing algorithm unsupported");
		} catch (MalformedJwtException e) {
			LOG.error("malformed JWT exception");
		} catch (SignatureException e) {
			LOG.error("Corrupted JWT");
		} catch (IllegalArgumentException e) {
			LOG.error("empty token");
		}
		return null;
	}
	private Date getExpDate(Date currentDate) {
		
		return new Date(currentDate.getTime() + expPeriodMin * 60000);
	}
	

}
