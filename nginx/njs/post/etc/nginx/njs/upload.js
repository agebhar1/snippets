import crypto from 'crypto';
import fs from 'fs';

/**
 * @param {NginxHTTPRequest} r
 * */
function upload(r) {

  const hash = crypto
    .createHash('md5')
    .update(r.requestBuffer)
    .digest('base64url');

  fs.writeFileSync(`/mnt/api/v2/${hash}.write`, r.requestBuffer);
  fs.renameSync(`/mnt/api/v2/${hash}.write`, `/mnt/api/v2/${hash}`);

  r.headersOut['Content-Type'] = 'application/json'
  r.return(200, JSON.stringify({
    id: hash,
    data: `/api/v2/${hash}`
  }))
}

export default upload