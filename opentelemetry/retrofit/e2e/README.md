# Run

> [!IMPORTANT]
> [AppArmor User Namespace Restrictions vs. Chromium Developer Builds](https://chromium.googlesource.com/chromium/src/+/main/docs/security/apparmor-userns-restrictions.md)


> [!NOTE]
> Container is running Alpine Linux by default. To run Ubuntu instead, do
> ```bash
> export CONTAINER_IMAGE=ubuntu
> ```

```bash
npm install
bash <(yq '.jobs.e2e.steps[] | select(.id == "docker-compose-up") | .run' < ../../../.github/workflows/retrofit.yml)
docker compose logs middleware
# middleware-1  | listening on /tmp/middleware.sock
# middleware-1  | connected: client(fd: 4, pid: 17) connections: 1
# middleware-1  | connected: client(fd: 5, pid: 19) connections: 2
npm run test -- --run
docker compose down --volumes
```
