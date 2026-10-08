# website_trac_nghiem

## Chon moi truong chay

Ung dung co ba Spring profile: `local`, `docker` va `deploy`. Neu khong khai
bao `SPRING_PROFILES_ACTIVE`, profile `local` se duoc su dung.

### Local

Yeu cau JDK 17, MySQL o cong `3306` va database `web_tn_v1`.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:DB_PASSWORD = 'mat-khau-mysql-cua-ban'
.\mvnw.cmd spring-boot:run
```

### Docker Compose

Chay ca ung dung va MySQL:

```powershell
docker compose up --build
```

Hoac chi chay MySQL bang Docker, sau do chay ung dung tu IDE/terminal voi
profile `docker`:

```powershell
docker compose up -d mysql
$env:SPRING_PROFILES_ACTIVE = 'docker'
.\mvnw.cmd spring-boot:run
```

MySQL Docker duoc mo tai `localhost:3307`. Mat khau mac dinh la `root`; co the
ghi de bang bien `MYSQL_ROOT_PASSWORD` truoc khi chay Compose.

### Deploy

Dat `SPRING_PROFILES_ACTIVE=deploy` cung cac bien `MYSQLHOST`, `MYSQLPORT`,
`MYSQLDATABASE`, `MYSQLUSER` va `MYSQLPASSWORD` tren he thong trien khai.

Ung dung mo tai http://localhost:8095/ khi chay local hoac Docker Compose.
