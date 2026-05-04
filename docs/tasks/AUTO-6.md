# AUTO-6 Manual deploy and network ingress fix

- Statusz: done
- Branch: `AUTO-6-manual-deploy-network-fix`
- PR: `PR #9`

## Cel

A ket hosztos Autoforge stack tenyleges production elinditasa (`private backend` + `public web/gateway`), majd a publikus eleres helyreallitasa OCI es host oldali tuzfal szabalyok javitasaval.

## Scope

- private deploy manualis vegrehajtasa jump hoston keresztul
- public deploy manualis vegrehajtasa
- health check validacio
- OCI security list ingress szabalyok javitasa (`80/tcp`, `443/tcp`)
- host szintu `iptables` szabalyok javitasa es persistalas
- Caddy TLS certificate ujraprobalasa es domain validacio

## Lepesnaplo (2026-05-03)

1. Private es public deploy env fajlok eloallitasa.
Leiras: Runtime `.env` es `.deploy.env` fajlok generalasa a szerverekre.
Parancs:
```bash
TOKEN="$(gh auth token)"
# /tmp/autoforge-private.env
# /tmp/autoforge-private.deploy.env
# /tmp/autoforge-public.env
# /tmp/autoforge-public.deploy.env
# GHCR_TOKEN=<redacted>
```

2. Private stack feltoltes es deploy.
Leiras: Compose fajl, deploy script, env fajlok feltoltese a private hostra SSH jump-pal, majd deploy script futtatasa.
Parancs:
```bash
ssh -i ${AUTOFORGE_SSH_KEY} -J ubuntu@144.24.176.5 ubuntu@10.42.0.91 "mkdir -p /opt/autoforge/private"
scp -i ${AUTOFORGE_SSH_KEY} -J ubuntu@144.24.176.5 infra/compose/docker-compose.private.yml ubuntu@10.42.0.91:/opt/autoforge/private/docker-compose.private.yml
scp -i ${AUTOFORGE_SSH_KEY} -J ubuntu@144.24.176.5 infra/deploy/private/deploy.sh ubuntu@10.42.0.91:/opt/autoforge/private/deploy.sh
scp -i ${AUTOFORGE_SSH_KEY} -J ubuntu@144.24.176.5 /tmp/autoforge-private.env ubuntu@10.42.0.91:/opt/autoforge/private/.env
scp -i ${AUTOFORGE_SSH_KEY} -J ubuntu@144.24.176.5 /tmp/autoforge-private.deploy.env ubuntu@10.42.0.91:/opt/autoforge/private/.deploy.env
ssh -i ${AUTOFORGE_SSH_KEY} -J ubuntu@144.24.176.5 ubuntu@10.42.0.91 "chmod +x /opt/autoforge/private/deploy.sh && APP_DIR=/opt/autoforge/private bash /opt/autoforge/private/deploy.sh"
```

3. Private backend health check.
Leiras: Backend container status es lokalis health endpoint ellenorzes.
Parancs:
```bash
ssh -i ${AUTOFORGE_SSH_KEY} -J ubuntu@144.24.176.5 ubuntu@10.42.0.91 "docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'"
ssh -i ${AUTOFORGE_SSH_KEY} -J ubuntu@144.24.176.5 ubuntu@10.42.0.91 "curl -fsS http://127.0.0.1:8080/actuator/health"
```

4. Public stack feltoltes es deploy.
Leiras: Public compose/Caddy/deploy fajlok es env feltoltese, majd deploy script futtatasa.
Parancs:
```bash
ssh -i ${AUTOFORGE_SSH_KEY} ubuntu@144.24.176.5 "mkdir -p /opt/autoforge/public"
scp -i ${AUTOFORGE_SSH_KEY} infra/compose/docker-compose.public.yml ubuntu@144.24.176.5:/opt/autoforge/public/docker-compose.public.yml
scp -i ${AUTOFORGE_SSH_KEY} infra/compose/Caddyfile ubuntu@144.24.176.5:/opt/autoforge/public/Caddyfile
scp -i ${AUTOFORGE_SSH_KEY} infra/deploy/public/deploy.sh ubuntu@144.24.176.5:/opt/autoforge/public/deploy.sh
scp -i ${AUTOFORGE_SSH_KEY} /tmp/autoforge-public.env ubuntu@144.24.176.5:/opt/autoforge/public/.env
scp -i ${AUTOFORGE_SSH_KEY} /tmp/autoforge-public.deploy.env ubuntu@144.24.176.5:/opt/autoforge/public/.deploy.env
ssh -i ${AUTOFORGE_SSH_KEY} ubuntu@144.24.176.5 "chmod +x /opt/autoforge/public/deploy.sh && APP_DIR=/opt/autoforge/public bash /opt/autoforge/public/deploy.sh"
```

5. Hibaazonositas: domain timeout.
Leiras: Caddy log jelezte, hogy ACME challenge timeout miatt nincs cert; kulso `curl` is timeoutolt.
Parancs:
```bash
curl -m 20 https://oci.prodet.org
curl -m 20 https://api.oci.prodet.org/actuator/health
ssh -i ${AUTOFORGE_SSH_KEY} ubuntu@144.24.176.5 "docker logs --tail 80 autoforge-public-gateway-1"
```

6. OCI security list ingress javitas.
Leiras: subnet security listbe bekerult a `80/tcp` es `443/tcp` internet felol.
Parancs:
```bash
oci network security-list update \
  --security-list-id ocid1.securitylist.oc1.eu-frankfurt-1.aaaaaaaatfftpaem4d7e4kuqywmtfvnxbt62hokmfzmzwvu7n5cmz47czroq \
  --ingress-security-rules file:///tmp/prodet-new-securitylist-ingress.json \
  --force
```

7. Hibaazonositas: host szintu iptables blokk.
Leiras: public host INPUT lancban csak `22/tcp` volt engedett, `80/443` REJECT-re futott.
Parancs:
```bash
ssh -i ${AUTOFORGE_SSH_KEY} ubuntu@144.24.176.5 "sudo iptables -S | head -n 20"
```

8. Host tuzfal fix es persistalas.
Leiras: `/etc/iptables/rules.v4` modositasa, `80/443` engedelyezese, majd `iptables-restore` es `netfilter-persistent save`.
Parancs:
```bash
ssh -i ${AUTOFORGE_SSH_KEY} ubuntu@144.24.176.5 "sudo sed -i '/--dport 22 -j ACCEPT/a -A INPUT -p tcp -m state --state NEW -m tcp --dport 80 -j ACCEPT\\n-A INPUT -p tcp -m state --state NEW -m tcp --dport 443 -j ACCEPT' /etc/iptables/rules.v4 && sudo iptables-restore < /etc/iptables/rules.v4 && sudo netfilter-persistent save"
```

9. Docker halozati lanc helyreallitas.
Leiras: `iptables-restore` utan a Docker chain-eket daemon restarttal vissza kellett epiteni.
Parancs:
```bash
ssh -i ${AUTOFORGE_SSH_KEY} ubuntu@144.24.176.5 "sudo systemctl restart docker && cd /opt/autoforge/public && docker compose --env-file .env -f docker-compose.public.yml up -d --remove-orphans"
```

10. Vegso validacio.
Leiras: HTTP->HTTPS redirect, web tartalom, API health, Caddy cert success.
Parancs:
```bash
curl -I http://oci.prodet.org
curl https://oci.prodet.org | head -n 5
curl https://api.oci.prodet.org/actuator/health
ssh -i ${AUTOFORGE_SSH_KEY} ubuntu@144.24.176.5 "docker logs --tail 50 autoforge-public-gateway-1"
```

## Eredmeny

- Private backend: fut (`ghcr.io/<registry-owner>/autoforge/backend:main`), health: `UP`
- Public web + gateway: fut (`ghcr.io/<registry-owner>/autoforge/web:main` + `caddy:2.10-alpine`)
- `oci.prodet.org`: elerheto HTTPS-en
- `api.oci.prodet.org/actuator/health`: `{"status":"UP",...}`
- Caddy Let's Encrypt cert sikeresen kiadva mindket domainre
