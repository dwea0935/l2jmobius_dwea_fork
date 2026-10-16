#!/bin/bash
# Runs once, on first start of an empty l2db volume (mariadb docker-entrypoint-initdb.d).
# Replaces the GUI DatabaseInstaller: imports login tables, then game tables.
# NOTE: keep this file NON-executable. The mariadb entrypoint then *sources* it,
# which is what makes docker_process_sql available (and its `set -e` aborts on errors).

DB="${MARIADB_DATABASE:-l2jmobiusepilogue}"

for dir in /l2sql/login /l2sql/game; do
	echo "[l2-init] Importing ${dir} into ${DB}..."
	for f in "${dir}"/*.sql; do
		echo "[l2-init]   $(basename "$f")"
		docker_process_sql --database="${DB}" < "$f"
	done
done

echo "[l2-init] Done."
