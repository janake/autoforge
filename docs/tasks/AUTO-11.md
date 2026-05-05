# AUTO-11: Private workspace block volume

Feladat leírása
A belső OCI szerverhez egy 100 GB-os Always Free keretbe illeszkedő block volume alapú filesystemet kell csatolni, amely tartós workspace-ként használható az OpenCode, backend és későbbi worker komponensek számára.

Statusz
- done

Branch
- `AUTO-11-private-workspace-volume`

PR
- pending

Acceptance criteria
- A belső szerverhez csatolva van egy 100 GB-os OCI Block Volume.
- A volume ext4 filesystemet kap.
- A filesystem `/mnt/autoforge-workspace` alá van mountolva.
- Az `/etc/fstab` UUID alapján tartalmazza a mountot, hogy reboot után is megmaradjon.
- A mount írható a deploy SSH user számára.
- A dokumentáció és az architektúra diagram tartalmazza az új storage elemet.

Munkalépések / tesztelés
1. Ellenőrizd a `prodet-new` compartment compute és storage állapotát.
2. Ellenőrizd az Always Free block storage kerethez számító boot/block volume méreteket.
3. Hozz létre egy 100 GB-os OCI Block Volume-ot a private instance Availability Domainjében.
4. Csatold a volume-ot paravirtualized attachmenttel a private instance-hez.
5. A private hoston formázd ext4-re, mountold `/mnt/autoforge-workspace` alá, és rögzítsd `/etc/fstab`-ban UUID alapján.
6. Ellenőrizd `mount -a`, `df -h`, `lsblk` és írási teszttel.

Dokumentumok és fájlok
- `docs/architecture.md`
- `docs/assets/autoforge-oci-architecture.svg`
- `docs/deployment.md`
- `docs/tasks/AUTO-11.md`
- `README.md`

Biztonsági megfontolások
- Gitbe nem kerülhet lokális SSH kulcs path, konkrét publikus IP, privát IP, OCID vagy személyes azonosító.
- Dokumentált parancsokban placeholdert kell használni.
- A mount UUID alapján történik, nem instabil `/dev/sdX` path alapján.
- Az OCI File Storage Service helyett Block Volume készült, mert az Always Free storage keret a Block Volume storage-ra vonatkozik.

Lepesnaplo
1. Ellenőriztem a compartmentet és az Availability Domaineket:
   ```bash
   oci iam compartment list --all --compartment-id-in-subtree true
   oci iam availability-domain list
   ```
2. Ellenőriztem a private és public instance adatait:
   ```bash
   oci compute instance list --compartment-id <compartment-ocid> --all
   oci compute instance list-vnics --instance-id <private-instance-ocid>
   ```
3. Ellenőriztem, hogy nincs meglévő block volume, és a két boot volume együtt kb. 94 GB:
   ```bash
   oci bv volume list --compartment-id <compartment-ocid> --all
   oci bv boot-volume list --compartment-id <compartment-ocid> --availability-domain <availability-domain> --all
   oci bv boot-volume get --boot-volume-id <boot-volume-ocid>
   ```
4. Létrehoztam a 100 GB-os block volume-ot:
   ```bash
   oci bv volume create \
     --compartment-id <compartment-ocid> \
     --availability-domain <availability-domain> \
     --display-name autoforge-private-workspace-100gb \
     --size-in-gbs 100 \
     --vpus-per-gb 0 \
     --freeform-tags '{"project":"autoforge","task":"AUTO-11","purpose":"private-workspace"}' \
     --wait-for-state AVAILABLE
   ```
5. Csatoltam a volume-ot a private instance-hez:
   ```bash
   oci compute volume-attachment attach \
     --type paravirtualized \
     --instance-id <private-instance-ocid> \
     --volume-id <workspace-volume-ocid> \
     --display-name autoforge-private-workspace-100gb-attachment \
     --wait-for-state ATTACHED
   ```
6. A private hoston ellenőriztem az új device-t:
   ```bash
   ssh -i ${AUTOFORGE_SSH_KEY} -J ubuntu@<public-host> ubuntu@<private-host> \
     "lsblk -o NAME,SIZE,TYPE,FSTYPE,MOUNTPOINTS,MODEL"
   ```
7. Formáztam, mountoltam, és fstab-ba írtam UUID alapján:
   ```bash
   ssh -i ${AUTOFORGE_SSH_KEY} -J ubuntu@<public-host> ubuntu@<private-host> \
     'sudo mkfs.ext4 -F -L autoforge-workspace /dev/sdb'

   ssh -i ${AUTOFORGE_SSH_KEY} -J ubuntu@<public-host> ubuntu@<private-host> \
     'UUID_VALUE=$(sudo blkid -s UUID -o value /dev/sdb); sudo mkdir -p /mnt/autoforge-workspace; printf "UUID=%s /mnt/autoforge-workspace ext4 defaults,nofail,_netdev 0 2\n" "$UUID_VALUE" | sudo tee -a /etc/fstab; sudo mount /mnt/autoforge-workspace; sudo chown ubuntu:ubuntu /mnt/autoforge-workspace'
   ```
8. Validáltam a mountot és az írhatóságot:
   ```bash
   ssh -i ${AUTOFORGE_SSH_KEY} -J ubuntu@<public-host> ubuntu@<private-host> \
     'sudo mount -a; touch /mnt/autoforge-workspace/.autoforge-write-test; rm /mnt/autoforge-workspace/.autoforge-write-test; df -h /mnt/autoforge-workspace; lsblk -o NAME,SIZE,TYPE,FSTYPE,LABEL,UUID,MOUNTPOINTS /dev/sdb'
   ```
9. Validáltam a dokumentációs változtatásokat:
   ```bash
   git diff --check
   python3 -c "import xml.etree.ElementTree as ET; ET.parse('docs/assets/autoforge-oci-architecture.svg')"
   rg -n "<sensitive-patterns>" README.md docs infra .github
   ```

Eredmény
- A 100 GB-os OCI Block Volume csatolva és mountolva van a private hoston.
- Mount point: `/mnt/autoforge-workspace`
- Elérhető méret: kb. 98 GB ext4 filesystem, kb. 93 GB szabad terület friss formázás után.
