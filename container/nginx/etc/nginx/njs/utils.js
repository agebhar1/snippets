function njsInfo(r) {
    r.headersOut['content-type'] = 'text/javascript';
    r.return(200, JSON.stringify(njs))
}

export default {njsInfo}