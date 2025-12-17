# Documentation Hosting Guide

This guide explains how to host the awtea project documentation using various methods, from GitHub Pages to custom web servers.

## Table of Contents

1. [GitHub Pages (Recommended)](#github-pages-recommended)
2. [Custom Web Server](#custom-web-server)
3. [Docker-Based Hosting](#docker-based-hosting)
4. [Local Development Server](#local-development-server)
5. [CDN Deployment](#cdn-deployment)

## GitHub Pages (Recommended)

### Overview

GitHub Pages provides free hosting for static documentation directly from your repository. The documentation is automatically updated on every merge to `main`.

### Setup

#### 1. Enable GitHub Pages

1. Go to repository **Settings** → **Pages**
2. Under "Source", select **GitHub Actions**
3. Save the configuration

#### 2. Automatic Deployment

The `.github/workflows/deploy-docs.yml` workflow automatically:
- Triggers on pushes to `main` branch that affect documentation
- Generates API coverage reports using `./gradlew generateDocs`
- Copies all documentation from `docs/` directory
- Creates an index page with navigation
- Deploys to GitHub Pages

#### 3. Access Documentation

After the first deployment, documentation will be available at:
```
https://<username>.github.io/<repository>/
```

For the awtea project:
```
https://mdbell.github.io/awtea/
```

### Manual Trigger

You can manually trigger documentation deployment:
1. Go to **Actions** tab
2. Select **Deploy Documentation** workflow
3. Click **Run workflow** → **Run workflow**

### What Gets Published

- **API Coverage Report**: `coverage/report.html` - Generated API coverage analysis
- **Architecture Docs**: All markdown files in `docs/` (CI/CD, Font Rendering, etc.)
- **Diagrams**: Visual documentation from `docs/diagrams/`
- **Index Page**: Auto-generated landing page with navigation

## Custom Web Server

### Prerequisites

- Linux server (Ubuntu/Debian recommended)
- Nginx or Apache web server
- SSL certificate (Let's Encrypt recommended)
- Domain name (optional)

### Option 1: Nginx

#### Install Nginx

```bash
sudo apt update
sudo apt install nginx
```

#### Configure Site

Create configuration file `/etc/nginx/sites-available/awtea-docs`:

```nginx
server {
    listen 80;
    server_name docs.yourdomain.com;  # or use IP address
    
    root /var/www/awtea-docs;
    index index.html;
    
    # Enable gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    
    location / {
        try_files $uri $uri/ =404;
    }
    
    # Cache static assets
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

#### Enable Site

```bash
sudo ln -s /etc/nginx/sites-available/awtea-docs /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### Deploy Documentation

```bash
# Create deployment directory
sudo mkdir -p /var/www/awtea-docs

# Clone repository
cd /tmp
git clone https://github.com/mdbell/awtea.git
cd awtea

# Generate documentation
./gradlew generateDocs

# Copy to web root
sudo cp -r docs/* /var/www/awtea-docs/

# Set permissions
sudo chown -R www-data:www-data /var/www/awtea-docs
sudo chmod -R 755 /var/www/awtea-docs
```

#### Add SSL (Optional but Recommended)

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx

# Get certificate
sudo certbot --nginx -d docs.yourdomain.com

# Auto-renewal is configured automatically
```

### Option 2: Apache

#### Install Apache

```bash
sudo apt update
sudo apt install apache2
```

#### Configure Site

Create `/etc/apache2/sites-available/awtea-docs.conf`:

```apache
<VirtualHost *:80>
    ServerName docs.yourdomain.com
    DocumentRoot /var/www/awtea-docs
    
    <Directory /var/www/awtea-docs>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>
    
    # Enable compression
    <IfModule mod_deflate.c>
        AddOutputFilterByType DEFLATE text/html text/plain text/xml text/css text/javascript application/javascript
    </IfModule>
    
    ErrorLog ${APACHE_LOG_DIR}/awtea-docs-error.log
    CustomLog ${APACHE_LOG_DIR}/awtea-docs-access.log combined
</VirtualHost>
```

#### Enable Site

```bash
sudo a2ensite awtea-docs
sudo a2enmod rewrite deflate
sudo systemctl reload apache2
```

### Automated Updates

Create deployment script `/usr/local/bin/update-awtea-docs.sh`:

```bash
#!/bin/bash

# Configuration
REPO_DIR="/opt/awtea"
WEB_ROOT="/var/www/awtea-docs"
LOG_FILE="/var/log/awtea-docs-update.log"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "Starting documentation update..."

# Update repository
if [ -d "$REPO_DIR" ]; then
    cd "$REPO_DIR"
    git fetch origin
    git reset --hard origin/main
else
    git clone https://github.com/mdbell/awtea.git "$REPO_DIR"
    cd "$REPO_DIR"
fi

# Generate documentation
log "Generating documentation..."
./gradlew generateDocs --no-daemon

if [ $? -ne 0 ]; then
    log "ERROR: Documentation generation failed"
    exit 1
fi

# Deploy
log "Deploying documentation..."
sudo cp -r docs/* "$WEB_ROOT/"
sudo chown -R www-data:www-data "$WEB_ROOT"

log "Documentation updated successfully"
```

Make executable:

```bash
sudo chmod +x /usr/local/bin/update-awtea-docs.sh
```

#### Cron Job for Auto-Updates

Add to crontab (`sudo crontab -e`):

```cron
# Update documentation every 6 hours
0 */6 * * * /usr/local/bin/update-awtea-docs.sh
```

Or use a webhook for instant updates on git push (see GitHub webhook setup below).

## Docker-Based Hosting

### Simple Nginx Container

Create `Dockerfile`:

```dockerfile
FROM nginx:alpine

# Copy documentation
COPY docs /usr/share/nginx/html

# Add custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

Create `nginx.conf`:

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;
    
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    
    location / {
        try_files $uri $uri/ =404;
    }
}
```

Build and run:

```bash
# Generate docs first
./gradlew generateDocs

# Build image
docker build -t awtea-docs .

# Run container
docker run -d -p 8080:80 --name awtea-docs awtea-docs
```

Access at `http://localhost:8080`

### Docker Compose with Auto-Updates

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  docs:
    image: nginx:alpine
    container_name: awtea-docs
    restart: unless-stopped
    ports:
      - "8080:80"
    volumes:
      - ./docs:/usr/share/nginx/html:ro
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/"]
      interval: 30s
      timeout: 10s
      retries: 3
  
  updater:
    image: alpine/git
    container_name: awtea-docs-updater
    restart: unless-stopped
    volumes:
      - ./:/repo
      - /var/run/docker.sock:/var/run/docker.sock
    command: >
      sh -c "
        while true; do
          cd /repo &&
          git fetch origin &&
          git reset --hard origin/main &&
          ./gradlew generateDocs --no-daemon &&
          docker exec awtea-docs nginx -s reload
          sleep 3600
        done
      "
```

Start:

```bash
docker-compose up -d
```

## Local Development Server

### Python Simple HTTP Server

```bash
# Generate docs
./gradlew generateDocs

# Serve on port 8000
cd docs
python3 -m http.server 8000
```

Access at `http://localhost:8000`

### Node.js with Live Reload

Install `http-server`:

```bash
npm install -g http-server
```

Serve:

```bash
# Generate docs
./gradlew generateDocs

# Serve with live reload
cd docs
http-server -p 8000 -c-1
```

### Using Caddy (Simplest)

Caddy automatically handles HTTPS with Let's Encrypt.

Install:

```bash
sudo apt install caddy
```

Create `Caddyfile`:

```
docs.yourdomain.com {
    root * /var/www/awtea-docs
    file_server
    encode gzip
    
    header {
        X-Content-Type-Options nosniff
        X-Frame-Options DENY
        X-XSS-Protection "1; mode=block"
    }
}
```

Run:

```bash
sudo caddy run --config Caddyfile
```

Caddy automatically gets SSL certificates!

## CDN Deployment

### Cloudflare Pages

1. Sign up at [Cloudflare Pages](https://pages.cloudflare.com/)
2. Connect your GitHub repository
3. Configure build settings:
   - Build command: `./gradlew generateDocs`
   - Build output directory: `docs`
4. Deploy

### Netlify

1. Sign up at [Netlify](https://www.netlify.com/)
2. Connect repository
3. Build settings:
   - Build command: `./gradlew generateDocs --no-daemon`
   - Publish directory: `docs`
4. Deploy

Create `netlify.toml`:

```toml
[build]
  command = "./gradlew generateDocs --no-daemon"
  publish = "docs"

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200

[[headers]]
  for = "/*"
  [headers.values]
    X-Frame-Options = "DENY"
    X-XSS-Protection = "1; mode=block"
```

### Vercel

Similar to Netlify:

```json
{
  "buildCommand": "./gradlew generateDocs --no-daemon",
  "outputDirectory": "docs"
}
```

## GitHub Webhook for Instant Updates

### Setup Webhook Receiver

Install webhook listener on your server:

```bash
npm install -g webhook
```

Create `hooks.json`:

```json
[
  {
    "id": "awtea-docs",
    "execute-command": "/usr/local/bin/update-awtea-docs.sh",
    "command-working-directory": "/opt/awtea",
    "response-message": "Documentation update triggered",
    "trigger-rule": {
      "match": {
        "type": "payload-hash-sha1",
        "secret": "YOUR_SECRET_HERE",
        "parameter": {
          "source": "header",
          "name": "X-Hub-Signature"
        }
      }
    }
  }
]
```

Run webhook listener:

```bash
webhook -hooks hooks.json -verbose -port 9000
```

### Configure GitHub Webhook

1. Go to repository **Settings** → **Webhooks**
2. Add webhook:
   - Payload URL: `http://your-server:9000/hooks/awtea-docs`
   - Content type: `application/json`
   - Secret: Use the same secret from `hooks.json`
   - Events: Just the push event
3. Save

Now docs update automatically on every push to main!

## Monitoring

### Check Deployment Status

For GitHub Pages:
```bash
curl -I https://mdbell.github.io/awtea/
```

For custom server:
```bash
# Check if nginx is running
sudo systemctl status nginx

# Check logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### Health Check Script

Create `/usr/local/bin/check-docs.sh`:

```bash
#!/bin/bash

URL="http://localhost/coverage/report.html"
EXPECTED_CODE=200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$URL")

if [ "$STATUS" -eq "$EXPECTED_CODE" ]; then
    echo "✓ Documentation site is healthy"
    exit 0
else
    echo "✗ Documentation site returned HTTP $STATUS"
    exit 1
fi
```

Add to monitoring (e.g., Nagios, Zabbix) or cron for alerts.

## Backup

### Automated Backup Script

```bash
#!/bin/bash

BACKUP_DIR="/backup/awtea-docs"
WEB_ROOT="/var/www/awtea-docs"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"
tar czf "$BACKUP_DIR/docs_$DATE.tar.gz" -C "$WEB_ROOT" .

# Keep only last 7 backups
find "$BACKUP_DIR" -name "docs_*.tar.gz" -mtime +7 -delete

echo "Backup completed: docs_$DATE.tar.gz"
```

## Performance Optimization

### Enable Browser Caching

Add to nginx config:

```nginx
location ~* \.(html|css|js|jpg|png|gif|ico)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### Enable Compression

Nginx:
```nginx
gzip on;
gzip_vary on;
gzip_proxied any;
gzip_comp_level 6;
gzip_types text/plain text/css text/xml text/javascript application/json application/javascript application/xml+rss application/rss+xml font/truetype font/opentype application/vnd.ms-fontobject image/svg+xml;
```

### Use CDN

Point your domain to Cloudflare for free CDN and DDoS protection:
1. Add site to Cloudflare
2. Update nameservers
3. Enable "Always Use HTTPS"
4. Enable "Auto Minify" for HTML/CSS/JS

## Troubleshooting

### Documentation Not Updating

```bash
# Check workflow status
gh run list --workflow=deploy-docs.yml

# View logs
gh run view <run-id> --log

# Manual regeneration
./gradlew clean generateDocs
```

### 404 Errors

Check file permissions:
```bash
sudo chmod -R 755 /var/www/awtea-docs
sudo chown -R www-data:www-data /var/www/awtea-docs
```

### Build Failures

Check Gradle:
```bash
./gradlew generateDocs --stacktrace --info
```

## Security Best Practices

1. **Use HTTPS**: Always use SSL certificates (Let's Encrypt is free)
2. **Set Security Headers**: Add X-Frame-Options, X-XSS-Protection
3. **Disable Directory Listing**: Configure web server to prevent browsing
4. **Regular Updates**: Keep server and dependencies updated
5. **Firewall**: Only expose necessary ports (80, 443)
6. **Rate Limiting**: Prevent abuse with nginx rate limits

## Cost Comparison

| Method | Cost | Complexity | Auto-Update |
|--------|------|------------|-------------|
| GitHub Pages | Free | Low | Yes |
| DigitalOcean Droplet | $5/month | Medium | Manual/Webhook |
| AWS S3 + CloudFront | $1-5/month | Medium | Manual/CI |
| Netlify | Free (100GB) | Low | Yes |
| Vercel | Free | Low | Yes |
| Self-Hosted | Server cost | High | Manual/Webhook |

## Conclusion

**For awtea project, we recommend:**
1. **Primary**: GitHub Pages (free, automatic, integrated)
2. **Backup**: Netlify or Vercel (free tier, automatic deployment)
3. **Custom**: Nginx on VPS for full control

The GitHub Pages deployment is already configured and will work out of the box once you enable it in repository settings.

## References

- [GitHub Pages Documentation](https://docs.github.com/en/pages)
- [Nginx Documentation](https://nginx.org/en/docs/)
- [Caddy Documentation](https://caddyserver.com/docs/)
- [Let's Encrypt](https://letsencrypt.org/)
