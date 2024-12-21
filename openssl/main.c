#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#include <openssl/err.h>
#include <openssl/evp.h>
#include <openssl/rand.h>

static const char magic[] = "Salted__";

// https://wiki.openssl.org/index.php/EVP_Symmetric_Encryption_and_Decryption
int main(int argc, char **argv)
{
    struct stat statbuf;
    if (stat(argv[1], &statbuf) == -1)
    {
        perror("stat");
        return EXIT_FAILURE;
    }

    FILE *fp = fopen(argv[1], "rb");
    if (!fp)
    {
        perror("fopen");
        return EXIT_FAILURE;
    }

    unsigned char *plaintext = malloc(statbuf.st_size + 1);
    size_t ret = fread(plaintext, 1, statbuf.st_size, fp);
    if (ret != statbuf.st_size)
    {
        fprintf(stderr, "fread() failed %zu\n", ret);
        return EXIT_FAILURE;
    }
    plaintext[statbuf.st_size] = '\0';

    if (fclose(fp))
    {
        perror("fclose");
        return EXIT_FAILURE;
    }

    char str[2] = {'q', '\0'};

    const EVP_CIPHER *cipher = EVP_aes_256_cbc(); // 16 bytes padding length/size - blocksize;
    EVP_CIPHER_CTX *ctx = NULL;
    unsigned char key[EVP_MAX_KEY_LENGTH], iv[EVP_MAX_IV_LENGTH];
    unsigned char salt[PKCS5_SALT_LEN];

    const EVP_MD *dgst = EVP_sha256(); // EVP_md5();

    ctx = EVP_CIPHER_CTX_new();
    RAND_bytes(salt, sizeof salt);

    unsigned char *sptr = salt;
    EVP_BytesToKey(cipher, dgst, sptr, (unsigned char *)str, strlen(str), 1, key, iv);
    OPENSSL_cleanse(str, strlen(str));

    /*
      E N C R Y P T
    */
    if (1 != EVP_EncryptInit_ex(ctx, cipher, NULL, key, iv))
    {
        ERR_print_errors_fp(stderr);
        return EXIT_FAILURE;
    }

    size_t size = statbuf.st_size + EVP_CIPHER_CTX_block_size(ctx);
    size_t header_size = strlen(magic) + sizeof salt;
    unsigned char *encrypted = malloc(size + header_size);

    unsigned char *ptr = encrypted;
    memcpy(ptr, magic, strlen(magic));
    ptr += strlen(magic);

    printf("block size= %d\n", EVP_CIPHER_CTX_block_size(ctx));
    printf("      salt= ");
    for (int i = 0; i < (int)sizeof(salt); i++)
        printf("%02X", salt[i]);
    printf("\n");
    memcpy(ptr, salt, sizeof salt);
    ptr += sizeof salt;

    if (EVP_CIPHER_CTX_key_length(ctx) > 0)
    {
        printf("       key= ");
        for (int i = 0; i < EVP_CIPHER_CTX_key_length(ctx); i++)
            printf("%02X", key[i]);
        printf(" (%d)\n", EVP_CIPHER_CTX_key_length(ctx));
    }
    if (EVP_CIPHER_CTX_iv_length(ctx) > 0)
    {
        printf("        iv= ");
        for (int i = 0; i < EVP_CIPHER_CTX_iv_length(ctx); i++)
            printf("%02X", iv[i]);
        printf(" (%d)\n", EVP_CIPHER_CTX_iv_length(ctx));
    }

    int len;
    int encrypted_len;
    if (1 != EVP_EncryptUpdate(ctx, ptr, &len, plaintext, statbuf.st_size))
    {
        ERR_print_errors_fp(stderr);
        return EXIT_FAILURE;
    }
    encrypted_len = len;
    printf("update: %d\n", encrypted_len);

    free(plaintext);
    plaintext = NULL;

    if (1 != EVP_EncryptFinal_ex(ctx, ptr + encrypted_len, &len))
    {
        ERR_print_errors_fp(stderr);
        return EXIT_FAILURE;
    }
    encrypted_len += len;
    printf("final : %d (%ld)\n", encrypted_len, size);

    char *filename = (char *)malloc(strlen(argv[1]) + sizeof ".enc");
    strncpy(filename, argv[1], strlen(argv[1]) + 1);
    strcat(filename, ".enc");
    printf("write to: %s\n", filename);
    fp = fopen(filename, "wb");
    if (!fp)
    {
        perror("fopen");
        return EXIT_FAILURE;
    }
    ret = fwrite(encrypted, 1, header_size + encrypted_len, fp);
    if (ret != header_size + encrypted_len)
    {
        fprintf(stderr, "fwrite() failed %zu\n", ret);
        return EXIT_FAILURE;
    }
    if (fclose(fp))
    {
        perror("fclose");
        return EXIT_FAILURE;
    }
    free(filename);
    filename = NULL;

    printf("\n");
    BIO_dump_fp(stdout, (const char *)encrypted, header_size + encrypted_len);
    printf("\n");

    EVP_CIPHER_CTX_free(ctx);
    ctx = NULL;

    /*
      D E C R Y P T
    */
    ctx = EVP_CIPHER_CTX_new();
    if (1 != EVP_DecryptInit_ex(ctx, cipher, NULL, key, iv))
    {
        ERR_print_errors_fp(stderr);
        return EXIT_FAILURE;
    }

    unsigned char *decrypted = malloc(encrypted_len); // statbuf.st_size + 1 -> free: corruption

    int decrypted_len;
    if (1 != EVP_DecryptUpdate(ctx, decrypted, &len, ptr, encrypted_len))
    {
        ERR_print_errors_fp(stderr);
        return EXIT_FAILURE;
    }
    decrypted_len = len;
    // printf("update: %d\n", decrypted_len);

    if (1 != EVP_DecryptFinal_ex(ctx, decrypted + decrypted_len, &len))
    {
        ERR_print_errors_fp(stderr);
        return EXIT_FAILURE;
    }
    decrypted_len += len;
    decrypted[decrypted_len] = '\0';
    printf("final : %d (%ld)\n", decrypted_len, strlen((const char *) decrypted));
    printf("decrypted :\n%s\n", decrypted);

    EVP_CIPHER_CTX_free(ctx);

    free(decrypted);
    free(encrypted);

    return 0;
}