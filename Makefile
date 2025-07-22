build:
	docker build -t cytomine/installer:latest -f cytomine-installer/Dockerfile cytomine-installer
	cd cytomine-community-edition \
	&& docker run -v $$(pwd):/install --user $$(id -u):$$(id -g) --rm -it cytomine/installer:latest deploy -s /install \
	&& docker compose build

start-dev:
	cd cytomine-community-edition \
	&& ./cytomine-dev set-profile $(filter-out $@,$(MAKECMDGOALS)) \
	&& ./cytomine-dev up

stop-dev:
	cd cytomine-community-edition \
	&& ./cytomine-dev down

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
	&& docker compose down --volumes --remove-orphans \
	&& $(RM) .env cytomine.yml docker-compose.override.yml \
	&& sudo $(RM) -rf data/ envs/

# Catch-all rule to prevent "No rule to make target ..." errors
%:
	@:
