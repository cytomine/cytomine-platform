import * as utils from "./utils.js";
import {Cytomine} from "@";
import config from "./config.js";

describe("Cytomine", function() {
    before(function() {
        new Cytomine(config.host);
    });

    describe("Login/logout", function() {
        it("Login", async function() {
            await Cytomine.instance.login(config.username, config.password);
        });

        it("Logout", async function() {
            await Cytomine.instance.logout();
        });
    });

    describe("UI config", function() {
        let project;

        before(async function() {
            await utils.connect();
            project = await utils.getProject();
        });

        after(async function() {
            await utils.cleanData();
        });

        it("Global UI config", async function() {
            let config = await Cytomine.instance.fetchUIConfigCurrentUser();
            expect(config).to.be.an.instanceof(Object);
            for(let prop in config){
                expect(config[prop]).to.be.a("boolean");
            }
        });

        it("Project UI config", async function() {
            let config = await Cytomine.instance.fetchUIConfigCurrentUser(project.id);
            expect(config).to.be.an.instanceof(Object);
            for(let prop in config){
                expect(config[prop]).to.be.a("boolean");
            }
        });
    });

    describe("Signature", function() {
        before(async function() {
            await utils.connect();
        });

        it("Signature", async function() {
            let signature = await Cytomine.instance.fetchSignature();
            expect(signature).to.be.a("string");
        });
    });

});
