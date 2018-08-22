import * as utils from "./utils.js";
import {AnnotationTerm, AnnotationTermCollection} from "@";

describe("AnnotationTerm", function() {

    let userannotation;
    let term;

    let annotationTerm = null;

    before(async function() {
        await utils.connect();
        ({id: userannotation} = await utils.getAnnotation());
        ({id: term} = await utils.getTerm());
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            annotationTerm = new AnnotationTerm({userannotation, term});
            annotationTerm = await annotationTerm.save();
            expect(annotationTerm).to.be.an.instanceof(AnnotationTerm);
        });

        it("Create and clear previous", async function() {
            annotationTerm = new AnnotationTerm({userannotation, term});
            annotationTerm = await annotationTerm.saveAndClearPrevious();
            expect(annotationTerm).to.be.an.instanceof(AnnotationTerm);
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedAnnotationTerm = await AnnotationTerm.fetch(userannotation, term);
            expect(fetchedAnnotationTerm).to.be.an.instanceof(AnnotationTerm);
            expect(fetchedAnnotationTerm).to.deep.equal(annotationTerm);
        });

        it("Fetch with instance method", async function() {
            let fetchedAnnotationTerm = await new AnnotationTerm({userannotation, term}).fetch();
            expect(fetchedAnnotationTerm).to.be.an.instanceof(AnnotationTerm);
            expect(fetchedAnnotationTerm).to.deep.equal(annotationTerm);
        });

        it("Fetch with wrong ID", function() {
            expect(AnnotationTerm.fetch(0)).to.be.rejected;
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await AnnotationTerm.delete(userannotation, term);
        });

        it("Fetch deleted", function() {
            expect(AnnotationTerm.fetch(userannotation, term)).to.be.rejected;
        });
    });

    // --------------------

    describe("AnnotationTermCollection", function() {

        let nbAnnotationTerms = 3;
        let annotationTerms;

        before(async function() {
            async function createTermAndAnnotTerm() {
                let tempTerm = await utils.getTerm();
                let annotTerm = new AnnotationTerm({userannotation, term: tempTerm.id});
                await annotTerm.save();
                return annotTerm;
            }

            let annotationTermPromises = [];
            for(let i = 0; i < nbAnnotationTerms; i++) {
                annotationTermPromises.push(createTermAndAnnotTerm());
            }
            annotationTerms = await Promise.all(annotationTermPromises);
        });

        after(async function() {
            let deletionPromises = annotationTerms.map(at => AnnotationTerm.delete(at.userannotation, at.term));
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new AnnotationTermCollection(0, "annotation", userannotation).fetch();
                expect(collection).to.be.an.instanceof(AnnotationTermCollection);
                expect(collection).to.have.lengthOf(nbAnnotationTerms);
            });

            it("Fetch (static method)", async function() {
                let collection = await AnnotationTermCollection.fetchWithFilter("annotation", userannotation);
                expect(collection).to.be.an.instanceof(AnnotationTermCollection);
                expect(collection).to.have.lengthOf(nbAnnotationTerms);
            });

            it("Fetch with several requests", async function() {
                let collection = await AnnotationTermCollection.fetchWithFilter("annotation", userannotation, 1);
                expect(collection).to.be.an.instanceof(AnnotationTermCollection);
                expect(collection).to.have.lengthOf(nbAnnotationTerms);
            });

            it("Fetch without filter", async function() {
                let collection = new AnnotationTermCollection();
                expect(collection.fetch()).to.be.rejected;
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await AnnotationTermCollection.fetchWithFilter("annotation", userannotation);
                for(let annotationTerm of collection) {
                    expect(annotationTerm).to.be.an.instanceof(AnnotationTerm);
                }
            });

            it("Add item to the collection", function() {
                let collection = new AnnotationTermCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new AnnotationTerm());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new AnnotationTermCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new AnnotationTermCollection(nbPerPage, "annotation", userannotation);
                await collection.fetchPage(1);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new AnnotationTermCollection(nbPerPage, "annotation", userannotation);
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new AnnotationTermCollection(nbPerPage, "annotation", userannotation);
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
