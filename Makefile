build:
	cd cytomine-community-edition \
	&& docker pull cytomine/installer:latest \
	&& docker run -v $$(pwd):/install --user $$(id -u):$$(id -g) --rm -it cytomine/installer:latest deploy -s /install \
	&& docker compose -f docker-compose.dev.yml build

start:
	cd cytomine-community-edition \
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
