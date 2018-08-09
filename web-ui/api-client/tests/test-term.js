import * as utils from "./utils.js";
import {Term, TermCollection} from "@";

describe("Term", function() {

    let ontology;
    let name = utils.randomString();
    let color = "#ffffff";

    let term = null;
    let id = 0;

    before(async function() {
        await utils.connect(true);
        ({id: ontology} = await utils.getOntology());
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            term = new Term({name, ontology, color});
            term = await term.save();
            id = term.id;
            expect(term).to.be.an.instanceof(Term);
            expect(term.name).to.equal(name);
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedTerm = await Term.fetch(id);
            expect(fetchedTerm).to.be.an.instanceof(Term);
            expect(fetchedTerm.name).to.equal(name);
        });

        it("Fetch with instance method", async function() {
            let fetchedTerm = await new Term({id}).fetch();
            expect(fetchedTerm).to.be.an.instanceof(Term);
            expect(fetchedTerm.name).to.equal(name);
        });

        it("Fetch with wrong ID", function() {
            expect(Term.fetch(0)).to.be.rejected;
        });
    });

    describe("Update", function() {
        it("Update", async function() {
            let newName = utils.randomString();
            term.name = newName;
            term = await term.update();
            expect(term).to.be.an.instanceof(Term);
            expect(term.name).to.equal(newName);
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await Term.delete(id);
        });

        it("Fetch deleted", function() {
            expect(Term.fetch(id)).to.be.rejected;
        });
    });

    // --------------------

    describe("TermCollection", function() {

        let nbTerms = 3;
        let terms;
        let totalNb = 0;

        let project;

        before(async function() {
            ({id: project} = await utils.getProject({ontology}));

            let termPromises = [];
            for(let i = 0; i < nbTerms; i++) {
                termPromises.push(new Term({name: utils.randomString(), ontology, color}).save());
            }
            terms = await Promise.all(termPromises);
        });

        after(async function() {
            let deletionPromises = terms.map(term => Term.delete(term.id));
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new TermCollection().fetch();
                expect(collection).to.be.an.instanceof(TermCollection);
                expect(collection).to.have.lengthOf.at.least(nbTerms);
                totalNb = collection.length;
            });

            it("Fetch (static method)", async function() {
                let collection = await TermCollection.fetch();
                expect(collection).to.be.an.instanceof(TermCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it("Fetch with several requests", async function() {
                let collection = await TermCollection.fetch(Math.ceil(totalNb/3));
                expect(collection).to.be.an.instanceof(TermCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await TermCollection.fetch();
                for(let term of collection) {
                    expect(term).to.be.an.instanceof(Term);
                }
            });

            it("Add item to the collection", function() {
                let collection = new TermCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new Term());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new TermCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Filtering", function() {
            it("Filter on project", async function() {
                let collection = new TermCollection();
                collection.setFilter("project", project);
                await collection.fetch();
                expect(collection).to.have.lengthOf.at.least(nbTerms);
            });

            it("Filter on ontology", async function() {
                let collection = new TermCollection(0, "ontology", ontology);
                await collection.fetch();
                expect(collection).to.have.lengthOf.at.least(nbTerms);
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new TermCollection(nbPerPage);
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new TermCollection(nbPerPage);
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new TermCollection(nbPerPage);
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
