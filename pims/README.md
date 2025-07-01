# cytomine-pims

Cytomine Python Image Management Server

## Run development server with Docker

    docker build -f docker/backend.dockerfile -t pims .
    docker run -p 5000:5000 pims

The server is running at http://127.0.0.1:5000 and API documentation is available 
at http://127.0.0.1:5000/docs

At this stage, it is hard to use Docker for development because hot-reload is not enabled and 
PIMS plugin system is not yet easily manageable in a Docker container for development. However, 
as PIMS requires a lot of low-level dependencies, developing using Docker would be a benefit.

## Run development server locally 

### Dependencies
First, dependencies must be installed
1. Dependencies in `docker/backend.dockerfile` must be installed first. For plugins, prerequisites 
   have to be 
   installed manually, especially for `before_vips` and `before_python`. See 
   `install_prerequisites.sh` in respective plugins.
2. `pip install -r requirements.txt`

### Cache
To run PIMS with cache, the cache must be configured in the settings. The cache uses an 
external Redis in-memory database. To launch a Redis instance using default values in PIMS 
settings, run: 
```bash
docker run -d --name pims-cache -p 6379:6379 redis
```
If the PIMS cache cannot be reached at PIMS startup, cache is automatically disabled.

### Task queue
Heavy computation like image imports are run in a task queue to prevent server flooding or 
blocking. The task queue uses Celery workers and RabbitMQ to communicate, but a fallback is 
possible when unavailable. 

#### Celery and RabbitMQ
PIMS is pre-configured to run with the Cytomine RabbitMQ configuration (see [Cytomine-bootstrap for PIMS](https://github.com/Cytomine-ULiege/Cytomine-bootstrap/tree/pims)).
RabbitMQ broker can be launched without Cytomine, by changing username and default password for 
task queue in PIMS settings by `guest`/`guest`, and then run:
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 --hostname rabbitmq rabbitmq:3.9
```

Then, you have to run a Celery worker:
```bash 
CONFIG_FILE="/path/to/my/config.env" celery -A pims.tasks.worker worker -l info -Q 
pims-import-queue -c 1
```
where `-c` is the concurrency level; 1 is enough for development.
See below for environment variables.

#### Fallback task queue
If task queue is disabled in PIMS configuration of if RabbitMQ is unreachable, heavy 
computations are still done in an asynchronous way but task queue is not mo running in separate 
processes and there is no guarantee that all tasks will be able to fallback on this.

### Run
To run the development server, run:
```bash
CONFIG_FILE="/path/to/my/config.env" python -m pims.main
```
    
The server is running at http://127.0.0.1:5000 and API documentation is available 
at http://127.0.0.1:5000/docs

In order to test PIMS without Cytomine, you can import images locally using 
```bash
CONFIG_FILE=/path/to/config.env python pims/importer/import_local_images.py --path /my/folder
```

#### Environment variables
* `CONFIG_FILE`: path to a `.env` configuration file. Default to `pims-config.env` (but some required configuration 
  settings need to be filled)
* `LOG_CONFIG_FILE`: path to a `.yml` Python logging configuration file. Default to `logging.yml`
* `DEBUG`: When set, enable debug tools and use `logging-debug.yml` as default logging configuration if not other 
  log config file is specified.
  
Configuration settings can be also given as environment variables and override values from `CONFIG_FILE`.

## PIMS plugins
PIMS plugins are quite new and plugin API is subject to change. To add a new format plugin, the 
best is to adapt the existing [`pims-plugin-format-openslide`](https://github.com/Cytomine-ULiege/pims-plugin-format-openslide).

A plugin can add one or several formats.

1. Create a directory `pims-plugin-format-{name}` and copy/paste content from Openslide plugin
2. In `setup.py`, rename the `NAME` variable
3. Rename the source directory to `pims_plugin_format_{name}`
4. Adapt `pims_plugin_format_{name}/__version__.py` content
5. If needed, adapt dependencies prerequisites in `install-prerequisites.sh`
6. Implement new formats by defining classes named `XYZFormat` (ending thus with `Format`) and 
   extending `AbstractFormat`.
   
### Develop with plugins
   
As the core server `pims` is a dependency of every PIMS plugins, `pims` has to be installed in 
the plugin virtual environment. At this stage, `pims` and plugins will probably be developed at 
same time, and the easiest way is to
1. install `pims` in editable mode in the plugin Python virtual env. Activate the plugin 
   environment, and run `pip install -e /local/path/to/pims` (you may need to add other options 
   like `--extra-index-url`)
2. install the plugin in editable mode in PIMS Python virtual env. Activate the PIMS 
   environment (e.g. in another terminal) and run 
   `pip install -e /local/path/to/pims-plugin-format-{name}`
   
When you start PIMS, the logs should list your plugin.

### Docker image with plugins
The `pims/scripts` folder has a script to build Docker images of PIMS with or without some 
plugins. Adapt the `plugin-list.csv` to build an image with the plugins (and their versions) 
you want.

### Available plugins
One can find below the listing of the PIMS plugins that have already been implemented.

| Plugin name | Format(s) | URL  | Remarks  |
|---|---|---|---|
| Openslide  | BIF, MRXS, NDPi, Philips TIFF, SCN, SVS, VMS  | https://github.com/cytomine/pims-plugin-format-openslide/tree/main/pims_plugin_format_openslide  | Depends on VIPS and Openslide. |
| Example  | / |  https://github.com/cytomine/pims-plugin-format-example | This is just a example plugin to explain how to implement a PIMS plugin.  |
| WSI Dicom  | WSIDICOM  | https://github.com/cytomine/pims-plugin-format-dicom  | PIMS plugin based on the WSI Dicom format implemented [here](https://github.com/imi-bigpicture/wsidicom). Annotations management not implemented yet. |

### Docker image with plugins resolution order 
During the upload of an image, a check is made to ensure the existence of the image format in accordance with the plugins installed with the Cytomine instance (these pulgins are specified in the CSV file named `plugin-list.csv`). To ensure that the uploaded file is handle with the right format resolver, one must define the resolution order of the plugins such that the most conservative checker happens before the less conservative one. 

One will find a resolution order column in the `plugin-list.csv`: the values must be assigned either an integer value or left unset. This resolution order is used to arrange plugins in relation to PIMS's native formats, which are given a default order of zero (0).The plugin that has the smallest, or most negative, integer for its resolution order will be checked before any other. This means that if a plugin's resolution order is more negative than all others, its formats will be evaluated first, even ahead of PIMS's native formats. In situations where the resolution order isn't specified for a plugin, it automatically inherits the same order as the native formats in PIMS, which is 0. This means it will be checked alongside, but not before, the native formats.

### Run development server locally with plugins resolution order 

In development mode, one can now create a new `checkerResolution.csv` file (name and path of this file can be defined in `pims-dev-config.env`) in order to specify format checkers resolution order. 

The CSV file must apply the following convention: 
| name | resolution_order | 
|---|---|
|`pims_plugin_format_{name}`| `INT` or `empty`|  

* `pims_plugin_format_{name}` must be the string referring to the name of the plugin as specified in the source directory. 
* the term 'resolution order' must be assigned either an integer value or left unset. This resolution order is used to arrange plugins in relation to PIMS's native formats, which are given a default order of zero (0).
The plugin that has the smallest, or most negative, integer for its resolution order will be checked before any other. This means that if a plugin's resolution order is more negative than all others, its formats will be evaluated first, even ahead of PIMS's native formats.
In situations where the resolution order isn't specified for a plugin, it automatically inherits the same order as the native formats in PIMS, which is 0. This means it will be checked alongside, but not before, the native formats.

To create the necessary file, you can modify the plugin-list.csv file to include your desired resolution order. After making these changes, execute the following command within the Docker directory: `python plugins.py --plugin_csv_path=/pims-ce/scripts/plugin-list.csv --checkerResolution_file_path=/pims-ce/checkerResolution.csv --method=checker_resolution_file`
