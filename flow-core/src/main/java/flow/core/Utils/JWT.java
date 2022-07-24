package flow.core.Utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

public class JWT {

  // TODO use dot env
  private String secret = System.getenv("SECRET");
  private static final String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
  Encoder e = Base64.getUrlEncoder().withoutPadding();
  Decoder d = Base64.getUrlDecoder();
  private Gson g = new Gson();

  public String create(String environmentId)
    throws JSONException, InvalidKeyException, NoSuchAlgorithmException {
    JSONObject payload = new JSONObject();

    SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
    Date currentDate = new Date();
    date.format(currentDate);
    Calendar c = Calendar.getInstance();
    c.setTime(currentDate);
    c.add(Calendar.DATE, 1);

    payload.put("environmentId", environmentId);
    payload.put("exp", date.format(c.getTime())); // convert tomorrows date to SimpleDateFormat

    String signature = hmac(
      e.encodeToString(header.getBytes()) +
      "." +
      e.encodeToString(payload.toString().getBytes())
    );
    System.out.println("sig..." + signature);
    String jwtToken =
      e.encodeToString(header.getBytes()) +
      "." +
      e.encodeToString(payload.toString().getBytes()) +
      "." +
      signature;

    return jwtToken;
  }

  public String hmac(String payloadAndHeader)
    throws NoSuchAlgorithmException, InvalidKeyException, JSONException {
    byte[] hash = secret.getBytes();

    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(hash, "HmacSHA256");

    mac.init(secretKey);

    byte[] signedBytes = mac.doFinal(
      payloadAndHeader.getBytes(StandardCharsets.UTF_8)
    );
    return e.encodeToString(signedBytes);
  }

  public JWTPayloadType decodeJWT(String token)
    throws InvalidKeyException, NoSuchAlgorithmException, JSONException, ParseException {
    String[] parts = token.split("\\.");
    String payload = new String(d.decode(parts[1]));
    JWTPayloadType jwtPayload = g.fromJson(payload, JWTPayloadType.class);
    // Convert string back to date and compare to today
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    Date jwtExpiryDate = df.parse(jwtPayload.exp);
    Boolean expired = jwtExpiryDate.compareTo(new Date()) < 0; // convert tomorrows date to SimpleDateFormat

    String signature = parts[2];
    String validSignature = hmac(parts[0] + "." + parts[1]);
    System.out.println(signature);
    System.out.println(validSignature);
    if (expired) throw new ParseException("JWT is expired", 0);
    if (validSignature.equals(signature)) {
      return jwtPayload;
    } else {
      throw new ParseException(
        "Could not decode the JWT, invalid token provided",
        0
      );
    }
  }
}
