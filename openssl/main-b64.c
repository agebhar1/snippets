#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <openssl/evp.h>
#include <openssl/rand.h>

static const char magic[] = "Salted__";

// https://wiki.openssl.org/index.php/EVP_Symmetric_Encryption_and_Decryption
int main()
{
    char str[2] = {'q', '\0'};

    const EVP_CIPHER *cipher = NULL;
    EVP_CIPHER_CTX *ctx = NULL;
    BIO *out, *wbio;
    unsigned char key[EVP_MAX_KEY_LENGTH], iv[EVP_MAX_IV_LENGTH];
    unsigned char salt[PKCS5_SALT_LEN];

    out = BIO_new(BIO_s_file());
    BIO_set_fp(out, stdout, BIO_NOCLOSE);
    wbio = out;

    // BIO_write_filename(out, outf);
    BIO *b64 = BIO_new(BIO_f_base64());
    BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    wbio = BIO_push(b64, wbio);

    const EVP_MD *dgst = EVP_md5();

    ctx = EVP_CIPHER_CTX_new();
    cipher = EVP_aes_256_cbc();
    RAND_bytes(salt, sizeof salt);

    unsigned char *sptr = salt;
    EVP_BytesToKey(cipher, dgst, sptr, (unsigned char *)str, strlen(str), 1, key, iv);
    OPENSSL_cleanse(str, strlen(str));

    printf("salt=");
    for (int i = 0; i < (int)sizeof(salt); i++)
        printf("%02X", salt[i]);
    printf("\n");

    if (cipher->key_len > 0)
    {
        printf(" key=");
        for (int i = 0; i < cipher->key_len; i++)
            printf("%02X", key[i]);
        printf("\n");
    }
    if (cipher->iv_len > 0)
    {
        printf("  iv=");
        for (int i = 0; i < cipher->iv_len; i++)
            printf("%02X", iv[i]);
        printf("\n");
    }

    BIO_write(wbio, magic, sizeof magic - 1);
    BIO_write(wbio, (char *)salt, sizeof salt);

    BIO *benc = BIO_new(BIO_f_cipher());
    BIO_get_cipher_ctx(benc, &ctx);

    EVP_EncryptInit_ex(ctx, cipher, NULL, key, iv);
    wbio = BIO_push(benc, wbio);

    char plaintext[] = "The quick brown fox jumps over the lazy dog";
    BIO_write(wbio, (char *)plaintext, sizeof(plaintext));
    BIO_flush(wbio);

    BIO_free_all(out);
    BIO_free(benc);
    BIO_free(b64);

    return 0;
}