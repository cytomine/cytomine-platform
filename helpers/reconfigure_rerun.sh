docker exec dev-iam-1 /bin/sh /opt/keycloak/bin/kc.sh export --users same_file --file /opt/keycloak/data/import/kc_config_updated.json --realm cytomine \
&& \
docker cp dev-iam-1:/opt/keycloak/data/import/kc_config_updated.json ./docker \
&& \
docker exec dev-iam-1 /bin/sh /opt/keycloak/bin/kc.sh import --dir /opt/keycloak/data/import --override true \
&& \
docker exec dev-iam-1 /bin/sh rm /opt/keycloak/data/import/kc_config.json \
&& \
docker exec dev-iam-1 /bin/sh /opt/keycloak/bin/kc.sh start-dev --import-realm