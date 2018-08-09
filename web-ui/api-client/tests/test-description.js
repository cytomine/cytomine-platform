import * as utils from "./utils.js";
import {Description} from "@";

describe("Description", function() {

    let annotation = null;
    let data = utils.randomString();

    let description = null;

    before(async function() {
        this.timeout(10000);
        await utils.connect();
        annotation = await utils.getAnnotation();
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            description = new Description({data}, annotation);
            await description.save();
            expect(description).to.be.an.instanceof(Description);
            expect(description.id).to.be.above(0);
            expect(description.data).to.equal(data);
        });

        it("Create without providing associated object", async function() {
            function fcn() {
                new Description({data});
            }
            expect(fcn).to.throw();
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedDescription = await Description.fetch(annotation);
            expect(fetchedDescription).to.be.an.instanceof(Description);
            expect(fetchedDescription.domainIdent).to.equal(annotation.id);
            expect(fetchedDescription.data).to.equal(data);
        });

        it("Fetch with instance method", async function() {
            let fetchedDescription = await new Description({}, annotation).fetch();
            expect(fetchedDescription).to.be.an.instanceof(Description);
            expect(fetchedDescription.domainIdent).to.equal(annotation.id);
            expect(fetchedDescription.data).to.equal(data);
        });

        it("Fetch without providing associated object", function() {
            expect(Description.fetch({})).to.be.rejected;
        });
    });

    describe("Update", function() {
        it("Update", async function() {
            let newData = utils.randomString();
            description.data = newData;
            description = await description.update();
            expect(description).to.be.an.instanceof(Description);
            expect(description.data).to.equal(newData);
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await Description.delete(annotation);
        });

        it("Fetch deleted", function() {
            expect(Description.fetch(annotation)).to.be.rejected;
        });
    });

});
