# Backend Image Deployment Contract

## Scope

This repository builds and publishes the backend image and owns the shipped backend deploy helper. It does not own the EC2 Compose stack, Nginx upstream, or MySQL container.

On a successful `main` push, GitHub Actions publishes an image and invokes the installed deploy helper at `/usr/local/libexec/readle-backend/deploy-backend`. The backend deploy helper input must be:

```text
ghcr.io/<owner>/int2-readle-team02-be@sha256:<digest>
<40-character-git-sha>
```

Convenience tags such as `:main` or commit-SHA tags may exist for discovery only. They must not be deployment inputs. The deployment layer must use the immutable `@sha256` digest ref and pass the matching expected 40-character Git SHA as the revision.

The image namespace follows `github.repository_owner`. Before the repository transfer it is `realdev-int2`; after the transfer it is `programmers-intern-program`. Update the EC2 image repository prefix and digest ref after the first successful image publish in the new namespace.

## EC2 runtime contract

- EC2 pulls a prebuilt image; it must not run Gradle, `docker build`, or `git pull` for backend deployment.
- The backend runs with `SPRING_PROFILES_ACTIVE=prod` and the ignored production datasource/S3 environment values.
- Backend slots join both `readle-public` (for the Nginx upstream) and `readle-private` (for MySQL); neither backend nor MySQL publishes a host port. Nginx joins `readle-public` only, avoiding multiple default routes in its network namespace.
- The deployment layer starts a temporary candidate, waits for `GET /api/actuator/health/readiness`, then performs the ADR-008 Nginx switch.
- The active backend is a state-driven slot and may be either `readle-backend-blue` or `readle-backend-green`.
- MySQL is a singleton and is not recreated during application deployment.

## Monitoring contract

Monitoring rollout은 docs repo의 `../../docs/INFRA_POLICY.md`와 ADR-012를 기준으로 한다.

Deploy helper는 edge cutover 성공 뒤에만 active backend scrape target을 publish한다. Target file은 monitoring stack이 소유하는 Prometheus `file_sd_configs` input이며, `/var/lib/readle/monitoring/prometheus/file_sd/backend-active.json`에 `readle-private`의 active blue/green backend hostname으로 atomic update한다.

Monitoring은 application deployment를 gate하지 않는다.

- Prometheus reload/watch 실패는 성공한 backend deployment를 실패 처리하거나 rollback하지 않는다.
- Candidate cutover 실패는 candidate scrape target을 publish하지 않는다.
- Rollback은 Nginx가 서빙하는 active slot으로 scrape target을 되돌린다.
- Verification은 active slot state, rendered Nginx upstream, file-SD target을 비교한다.

Backend metrics endpoint는 `/api/actuator/prometheus`를 유지한다. 이 endpoint는 application boundary에서 Basic username `readle-monitor`와 root-only password file의 scoped scrape credential을 요구한다. Prometheus는 `readle-private`에서 active backend hostname으로만 scrape하며, public Nginx server block에는 `ops/monitoring/nginx/prometheus-metrics-deny.conf.template`의 exact location을 include해 외부 요청을 `403`으로 차단한다. Basic Auth는 이 Nginx 차단을 대체하지 않는 추가 방어선이다.

`readle-monitoring verify-host`는 Nginx rendered config와 local edge request의 `403`을 함께 확인한다. 배포 후에는 외부 네트워크에서도 다음을 확인한다.

```bash
curl -o /dev/null -sS -w '%{http_code}\n' https://<service-host>/api/actuator/prometheus
# 403 expected
```

Grafana Nginx host wiring is an explicit prerequisite: install the exact `/grafana` redirect,
`^~ /grafana/` proxy, `/grafana/login` rate-limit location, and a matching
`readle_grafana_login` `limit_req_zone` from `ops/monitoring/nginx/`. `readle-monitoring verify-host`
rejects hosts where those fragments are absent; it does not mutate the running Nginx container
automatically.

## Required first-deploy checks

The GHCR package remains **private**. EC2 authenticates with a dedicated GitHub account credential that has only `read:packages` and package read access. Store `GHCR_USERNAME` and `GHCR_PULL_TOKEN` only in the EC2 deployment environment; never commit them to this repository.

Run these on EC2 before adopting GHCR delivery:

```bash
uname -m # x86_64 expected
sudo podman version
printf '%s' "$GHCR_PULL_TOKEN" | sudo podman login ghcr.io -u "$GHCR_USERNAME" --password-stdin
sudo podman pull ghcr.io/<owner>/int2-readle-team02-be@sha256:<digest>
```

The workflow publishes `linux/amd64`. If EC2 is not `x86_64`, stop and add the required platform deliberately. If the pull cannot work because of EC2 outbound access or GitHub package policy, stop and choose a separate artifact-transfer design.

## First-time host bootstrap

Install the shipped helper once on the EC2 host before enabling CI deployment:

```bash
sudo install -d -o root -g root -m 0755 /usr/local/libexec/readle-backend
sudo install -o root -g root -m 0755 ops/backend/deploy-backend.sh /usr/local/libexec/readle-backend/deploy-backend
```

After bootstrap, normal deployment is only the GitHub Actions image publish and CI invocation of `/usr/local/libexec/readle-backend/deploy-backend` with the immutable image digest and matching Git revision. Normal deployment must not run `git pull`, Gradle, or a host-side image build.

## Rollback

Authenticate with the same EC2-only read credential, deploy a prior successful immutable digest ref with its matching 40-character Git revision, then use the same candidate/Nginx validation path.
