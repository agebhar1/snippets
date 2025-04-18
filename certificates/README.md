## Certificate Chain

### Root CA

```
$ openssl req -x509 -newkey rsa:4096 -keyout root_ca.key -out root_ca.crt -days 3650 -subj "/C=DE/O=Andreas Gebhardt/CN=Root CA" -addext "basicConstraints=critical,CA:true" -addext "keyUsage=cRLSign,keyCertSign" -passout 'env:ROOT_CA_PASSWD'
$ openssl x509 -noout -text -in root_ca.crt
```

### Intermediate CA (1)

```
$ openssl req -newkey rsa:4096 -keyout intermediate_ca.key -out intermediate_ca.csr -subj "/C=DE/O=Andreas Gebhardt/CN=Intermediate CA1/" -addext "basicConstraints=critical,CA:true,pathlen:0" -addext "keyUsage=digitalSignature,keyCertSign,cRLSign" -addext "extendedKeyUsage=clientAuth,serverAuth" -passout 'env:INTERMEDIATE_CA_PASSWD'
$ openssl req -noout -text -verify -in intermediate_ca.csr
```

```
$ openssl x509 -req -in intermediate_ca.csr -CAkey root_ca.key -CA root_ca.crt -copy_extensions copy -ext basicConstraints,keyUsage,extendedKeyUsage -days 365 -CAcreateserial -out intermediate_ca.crt -passin 'env:ROOT_CA_PASSWD'
$ openssl x509 -noout -text -in intermediate_ca.crt
$ openssl verify -CAfile root_ca.crt root_ca.crt intermediate_ca.crt
```

### Server Certificate

```
$ openssl req -newkey rsa:4096 -nodes -keyout server.key -out server.csr -subj "/C=DE/O=Andreas Gebhardt/CN=server.localhost/" -addext "subjectAltName=DNS:server.localhost,DNS:localhost,IP:127.0.0.1" -addext "keyUsage=digitalSignature" -addext "extendedKeyUsage=clientAuth,serverAuth"
$ openssl req -noout -text -verify -in server.csr
```

```
$ openssl x509 -req -in server.csr -CAkey intermediate_ca.key -CA intermediate_ca.crt -copy_extensions copy -ext subjectAltName,keyUsage,extendedKeyUsage -days 73 -CAcreateserial -out server.crt -passin 'env:INTERMEDIATE_CA_PASSWD'
$ openssl x509 -noout -text -in server.crt
$ openssl verify -verbose -CAfile root_ca.crt -untrusted intermediate_ca.crt root_ca.crt intermediate_ca.crt server.crt
```

### Usage

#### NGINX

```
$ cp server.key etc/nginx/ssl/
$ cat server.crt intermediate_ca.crt >> etc/nginx/ssl/server+intermediate_ca.crt
$ curl --silent https://ssl-config.mozilla.org/ffdhe2048.txt > etc/nginx/ssl/dhparam
$ # openssl dhparam -out etc/nginx/ssl/dhparam 4096
```

```
$ openssl s_client -showcerts -servername server.localhost -connect server.localhost:8443 -CAfile root_ca.crt
$ # openssl s_client -showcerts -servername server.localhost -connect server.localhost:8443 -CAfile root_ca.crt | grep '^Verify return code: 0 (ok)$'
$ curl --cacert root_ca.crt --silent https://server.localhost:8443
```

```
# -A
#    Add an existing certificate to a certificate database. The certificate database should already exist; if one is 
#    not present, this command option will initialize one by default.
# 
# -i input_file
# -n nickname
# -t trustargs
#    p - Valid peer
#    P - Trusted peer (implies p)
#    c - Valid CA
#    C - Trusted CA (implies c)
#    T - trusted CA for client authentication (ssl server only)
$ certutil -d sql:$HOME/.pki/nssdb -A -t "C,," -n root_ca.crt -i root_ca.crt
```

# Links

* https://github.com/puppeteer/puppeteer/issues/1319
* https://openssl-ca.readthedocs.io/en/latest/index.html
* https://stackoverflow.com/questions/25750890/nginx-install-intermediate-certificate/25769829#25769829
* https://gist.github.com/soarez/9688998
* https://github.com/openssl/openssl/issues/10458
* https://github.com/openssl/openssl/discussions/26144
* https://docs.openssl.org/master/man5/x509v3_config/
* https://docs.openssl.org/master/man1/openssl-passphrase-options/
