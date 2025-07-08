build:
	find . -mindepth 2 -maxdepth 2 -type f -iname 'Dockerfile' | while read dockerfile; do \
		dir=$$(dirname $$dockerfile); \
		image=$$(basename $$dir | tr '[:upper:]' '[:lower:]'); \
		docker build -t cytomine/$$image:latest $$dir; \
	done

	docker build --target ci -t cytomine/iam:latest -f iam/Dockerfile iam
	docker build -t cytomine/web-ui:latest -f web-ui/docker/Dockerfile web-ui
	docker build -t cytomine/pims:latest -f pims/docker/backend.dockerfile pims

start:
	cd cytomine-community-edition \
	&& docker pull cytomine/installer:latest \
	&& docker run -v $$(pwd):/install --user $$(id -u):$$(id -g) --rm -it cytomine/installer:latest deploy -s /install \
	&& docker compose up -d

stop:
	docker compose -f cytomine-community-edition/docker-compose.yml stop

down:
	docker compose -f cytomine-community-edition/docker-compose.yml down

clean:
	cd cytomine-community-edition \
	&& docker compose -f docker-compose.yml down --volumes --remove-orphans \
	&& $(RM) .env cytomine.yml docker-compose.override.yml \
	&& sudo $(RM) -rf data/ envs/
