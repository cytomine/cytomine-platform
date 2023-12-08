#!/bin/sh

echo $0 "Databases check."

ls -al /checks

wait_for_db() {
    until runuser -l postgres -c 'pg_isready' 2>/dev/null; do
      >&2 echo "Postgres is unavailable - sleeping for 3 seconds"
      sleep 3
    done

    echo "Postgres is up - executing command"
    sh /checks/check-monitoring.sh
}

wait_for_db &
echo $0 "Check ended."

# executing next entrypoint
exec "$@"