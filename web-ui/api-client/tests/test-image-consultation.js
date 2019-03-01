import * as utils from "./utils.js";
import {ProjectConnection, ImageConsultation, ImageConsultationCollection, User} from "@";

describe("ImageConsultation", function() {
    let project;
    let image;
    let projectConnection;

    let imageConsultation = null;

    before(async function() {
        await utils.connect();
        ({id: project} = await utils.getProject());
        ({id: projectConnection} = await new ProjectConnection({project}).save());
        ({id: image} = await utils.getImageInstance({project}));
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            imageConsultation = new ImageConsultation({image});
            await imageConsultation.save();
            expect(imageConsultation).to.be.an.instanceof(ImageConsultation);
            expect(imageConsultation.id).to.be.above(0);
        });
    });

    describe("Fetch", function() {
        it("Fetch", function() {
            expect(imageConsultation.fetch.bind(imageConsultation)).to.throw();
        });
    });

    describe("Update", function() {
        it("Update", function() {
            expect(imageConsultation.update.bind(imageConsultation)).to.throw();
        });
    });

    describe("Delete", function() {
        it("Delete", function() {
            expect(imageConsultation.delete.bind(imageConsultation)).to.throw();
        });
    });


    // --------------------

    describe("ImageConsultationCollection", function() {

        let nbConsultations = 3;
        let totalNb = 0;
        let user;

        before(async function() {
            ({id: user} = await User.fetchCurrent());

            let consultationsPromise = [];
            for(let i = 0; i < nbConsultations; i++) {
                consultationsPromise.push(new ImageConsultation({image}).save());
            }
            consultationsPromise = await Promise.all(consultationsPromise);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new ImageConsultationCollection({user, project}).fetchAll();
                expect(collection).to.be.an.instanceof(ImageConsultationCollection);
                expect(collection).to.have.lengthOf.at.least(nbConsultations);
                totalNb = collection.length;
            });

            it("Fetch (static method)", async function() {
                let collection = await ImageConsultationCollection.fetchAll({user, project});
                expect(collection).to.be.an.instanceof(ImageConsultationCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it.skip("Fetch with several requests", async function() {
                let collection = await ImageConsultationCollection.fetchAll({user, project, nbPerPage: Math.ceil(totalNb/3)});
                expect(collection).to.be.an.instanceof(ImageConsultationCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it.skip("Fetch from project connection", async function() { // erratic core behaviour
                let collection = await new ImageConsultationCollection({projectConnection}).fetchAll();
                expect(collection).to.be.an.instanceof(ImageConsultationCollection);
                expect(collection).to.have.lengthOf.at.least(nbConsultations);
            });

            it("Fetch resume", async function() {
                let collection = await new ImageConsultationCollection({resume: true, user, project}).fetchAll();
                expect(collection).to.be.an.instanceof(ImageConsultationCollection);
                expect(collection).to.have.lengthOf.at.least(1);
            });

            it("Fetch with incorrect parameters", async function() {
                let collection = new ImageConsultationCollection();
                expect(collection.fetchAll()).to.be.rejected;
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await ImageConsultationCollection.fetchAll({user, project});
                for(let connection of collection) {
                    expect(connection).to.be.an.instanceof(ImageConsultation);
                }
            });

            it("Add item to the collection", function() {
                let collection = new ImageConsultationCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new ImageConsultation());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new ImageConsultationCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });

            it("Download URL", async function() {
                let collection = new ImageConsultationCollection({user, project});
                expect(collection.downloadURL).to.be.a("string");
            });
        });
        
        
        describe.skip("Pagination", function() { // incorrect values returned for size and totalPages
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new ImageConsultationCollection({user, project, nbPerPage});
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new ImageConsultationCollection({user, project, nbPerPage});
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new ImageConsultationCollection({user, project, nbPerPage});
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
