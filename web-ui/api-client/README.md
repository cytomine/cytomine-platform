# Cytomine Javascript Client

When using our software, we kindly ask you to cite our website URL and related publications in all your work (publications, studies, oral presentations,...). In particular, we recommend to cite (Marée et al., Bioinformatics 2016) paper, and to use our logo when appropriate. See our license files for additional details.

- URL: http://www.cytomine.org/
- Logo: [Available here](https://doc.cytomine.org/img/logo_cyto_org.png)
- Scientific paper: Raphaël Marée, Loïc Rollus, Benjamin Stévens, Renaud Hoyoux, Gilles Louppe, Rémy Vandaele, Jean-Michel Begon, Philipp Kainz, Pierre Geurts and Louis Wehenkel. Collaborative analysis of multi-gigapixel imaging data using Cytomine, Bioinformatics, DOI: 10.1093/bioinformatics/btw013, 2016. http://dx.doi.org/10.1093/bioinformatics/btw013

## Presentation
Cytomine-js-client is an opensource javascript client allowing to communicate with the REST API of a Cytomine instance. For more information about Cytomine, go to https://www.cytomine.org.
See installation instructions here : https://doc.cytomine.org/dev-guide/clients/javascript/installation

## How to use the client
The javascript client can be used in one of the following ways:
* As an NPM module for a browser application (install with `npm install cytomine-client`);
* With a direct `<script>` include.

In any case, the client should be used in a browser environment, since it relies on cookies for authentication.

#### Sample code
```javascript
import {Cytomine, User, ProjectCollection} from "cytomine-client"; // required only if used as an NPM module

// Initialize connection (replace CYTOMINE_URL by an appropriate value, e.g. "http://demo.cytomine.coop")
let cytomine = new Cytomine(CYTOMINE_URL);

// Login into Cytomine
await cytomine.login("username", "password"); // or await Cytomine.instance.login("username", "password");

// Fetch the connected user, and log "Hello " followed by its username in the console
let user = await User.fetchCurrent();
console.log("Hello " + user.username);

// Fetch the projects available to the current user and log their names in the console
let projects = await ProjectCollection.fetchAll();
console.log("You have access to these projects:");
for(let project of projects) {
    console.log(project.name);
}
```

## Development

#### Installation
First checkout the repository, then install the dependencies with

    npm install

 #### Test execution

Prior to the first test execution, update the [tests configuration file](tests/config.js) with appropriate values (**beware that the tests will create data on the configured Cytomine instance!**). To avoid committing the configuration values, it is advised to run `git update-index --skip-worktree tests/config.js`.

Then, to execute the tests, run

    npm run test

Note that the execution of the tests requires Mozilla Firefox browser. Moreover, the Cytomine instance used for the tests must allow CORS.

#### Build for production with minification
    npm run build
Generates a minified file located in dist folder, which can be included with `<script>`

#### Create a tarball
    npm pack
The tarball can then be installed in another npm project with

    npm install PATH_TO_TARBALL
