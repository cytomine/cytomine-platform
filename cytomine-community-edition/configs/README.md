# Configuration overrides folder
For each service, you can override any file within the container.
## File override
Under this `configs/` folder, just recreate the folder structure of the file from the root filesystem path and place a file there.
Example: override the `/etc/nginx/cors_params` of the `nginx` service.
```
configs
└─ nginx                   <-- service name
   └─ etc                  <-- /etc inside the container
      └─ nginx
         └─ cors_params    <-- file
```

## File templating
Additionally, environment variables can be interpolated inside those files if they end with `.sample`.
For instance, `/etc/nginx/nginx.conf.sample` of the `nginx` service   
```conf
...
    server {
        client_max_body_size 0;
        listen       80;
        server_name  $URLS_CORE;
...        
```
Will become `/etc/nginx/nginx.conf` with `$URLS_CORE` replaced by `cytomine.local`
```conf
...
    server {
        client_max_body_size 0;
        listen       80;
        server_name  cytomine.local
...        
```
# List of services and overrides examples
Here is a list of what can be defined in the `configs/` folder.
## core
Example:
```
configs
└─ core                    <-- service name
   └─ app                  <-- /app inside the container
      └─ logback.xml       <-- file
```
See here what can be overridden https://github.com/cytomine/Cytomine-core-spring/blob/main/README.md#override-configuration

## nginx
Example:
```
configs
└─ nginx                   <-- service name
   └─ etc                  <-- /etc inside the container
      └─ nginx
         └─ cors_params    <-- file
```
See here what can be overridden https://github.com/cytomine/Cytomine-nginx/tree/main/cm_configs_default

## pims
Example:
```
configs
└─ pims                    <-- service name
   └─ app                  <-- /app inside the container
      └─ logging.xml       <-- file
```
See here what can be overridden https://github.com/cytomine/pims

## web_ui
Example:
```
configs
└─ web_ui                              <-- service name
   └─ app                              <-- /app inside the container
      └─ configuration.json.sample     <-- file
```
See here what can be overridden https://github.com/cytomine/Cytomine-Web-UI
