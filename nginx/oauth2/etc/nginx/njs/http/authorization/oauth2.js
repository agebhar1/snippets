/**
 * @param {String} data
 * @returns {String}
 */
const base64urlDecode = (data) => Buffer.from(data, 'base64url').toString()

/**
 * @param {String} data
 * @returns {Object}
 */
function jwt(data) {
  const parts = data.split('.').slice(0, 2)
    .map(base64urlDecode)
    .map(JSON.parse)

  return {
    headers: parts[0],
    payload: parts[1],
  };
}

/**
 * @param {NginxHTTPRequest} r
 * */
function userInfoFromAccessToken(r) {
  const payload = jwt(r.variables['access_token']).payload;

  r.headersOut['Content-Type'] = 'application/json';
  return r.return(200, JSON.stringify({
    // https://www.iana.org/assignments/jwt/jwt.xhtml
    sub: payload.sub,
    preferred_username: payload.preferred_username,
    email: payload.email
  }));
}

export default {
  userInfoFromAccessToken
}