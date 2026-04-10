#!/bin/bash
# Clear Books Database Backup Script
# Run daily at 2:00 AM via cron: 0 2 * * * /path/to/backup.sh

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"

# Load environment variables
if [ -f "$PROJECT_DIR/.env" ]; then
    set -a
    source "$PROJECT_DIR/.env"
    set +a
fi

# Configuration
DB_NAME="${MYSQL_DB:-accounting}"
DB_USER="${MYSQL_USER:-accounting}"
DB_PASS="${MYSQL_PASSWORD:-}"
DB_HOST="${MYSQL_HOST:-localhost}"
DB_PORT="${MYSQL_PORT:-3306}"
BACKUP_DIR="$PROJECT_DIR/backups"
LOGS_DIR="$PROJECT_DIR/logs"
KEEP_DAYS=7
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="backup_${DB_NAME}_${DATE}.sql"

# Create directories
mkdir -p "$BACKUP_DIR"
mkdir -p "$LOGS_DIR"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOGS_DIR/backup.log"
}

log "Starting database backup..."
log "Database: $DB_NAME@$DB_HOST:$DB_PORT"

# Check if password is set
if [ -z "$DB_PASS" ]; then
    log "ERROR: MYSQL_PASSWORD not set in .env"
    exit 1
fi

# Check if mysqldump exists
if ! command -v mysqldump &> /dev/null; then
    log "ERROR: mysqldump command not found"
    log "Please install MySQL client: apt-get install mysql-client æˆ?yum install mysql"
    exit 1
fi

# Backup database
log "Dumping database..."

# Use MYSQL_PWD environment variable to avoid password in command line
export MYSQL_PWD="$DB_PASS"

mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
    --single-transaction \
    --routines \
    --triggers \
    "$DB_NAME" > "$BACKUP_DIR/$BACKUP_FILE" 2>> "$LOGS_DIR/backup.log"

DUMP_RESULT=$?
unset MYSQL_PWD

if [ $DUMP_RESULT -eq 0 ]; then
    # Get file size
    FILE_SIZE=$(du -h "$BACKUP_DIR/$BACKUP_FILE" | cut -f1)
    
    # Compress backup
    gzip "$BACKUP_DIR/$BACKUP_FILE"
    COMPRESSED_SIZE=$(du -h "$BACKUP_DIR/${BACKUP_FILE}.gz" | cut -f1)
    
    log "Backup successful: ${BACKUP_FILE}.gz"
    log "Original size: $FILE_SIZE, Compressed: $COMPRESSED_SIZE"
    
    # Delete old backups (keep last 7 days)
    DELETED_COUNT=$(find "$BACKUP_DIR" -name "backup_${DB_NAME}_*.sql.gz" -mtime +$KEEP_DAYS | wc -l)
    find "$BACKUP_DIR" -name "backup_${DB_NAME}_*.sql.gz" -mtime +$KEEP_DAYS -delete
    
    if [ "$DELETED_COUNT" -gt 0 ]; then
        log "Cleaned up $DELETED_COUNT old backup(s)"
    fi
    
    # List current backups
    BACKUP_COUNT=$(ls -1 "$BACKUP_DIR"/backup_${DB_NAME}_*.sql.gz 2>/dev/null | wc -l)
    log "Total backups: $BACKUP_COUNT"
else
    log "ERROR: Backup failed!"
    rm -f "$BACKUP_DIR/$BACKUP_FILE"
    exit 1
fi

log "Backup process completed"
