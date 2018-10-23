import * as utils from "./utils.js";
import {SoftwareProject, SoftwareProjectCollection} from "@";

describe("SoftwareProject", function() {

    let software;
    let project;
    let softwareName;

    let softwareProject;
    let id = 0;

    before(async function() {
        await utils.connect(true);
        ({id: project} = await utils.getProject());
        ({id: software, name: softwareName} = await utils.getSoftware());
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            softwareProject = new SoftwareProject({software, project});
            softwareProject = await softwareProject.save();
            id = softwareProject.id;
            expect(softwareProject).to.be.an.instanceof(SoftwareProject);
            expect(softwareProject.name).to.equal(softwareName);
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedSoftwareProject = await SoftwareProject.fetch(id);
            expect(fetchedSoftwareProject).to.be.an.instanceof(SoftwareProject);
            expect(fetchedSoftwareProject).to.deep.equal(softwareProject);
        });

        it("Fetch with instance method", async function() {
            let fetchedSoftwareProject = await new SoftwareProject({id}).fetch();
            expect(fetchedSoftwareProject).to.be.an.instanceof(SoftwareProject);
            expect(fetchedSoftwareProject).to.deep.equal(softwareProject);
        });

        it("Fetch with wrong ID", function() {
            expect(SoftwareProject.fetch(0)).to.be.rejected;
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await SoftwareProject.delete(id);
        });

        it("Fetch deleted", function() {
            expect(SoftwareProject.fetch(id)).to.be.rejected;
        });
    });

    // --------------------

    describe("SoftwareProjectCollection", function() {

        let nbSoftwareProjects = 3;
        let softwareProjects;
        let totalNb = 0;

        before(async function() {
            async function createSoftwareAndSoftwareProject() {
                let tempSoft = await utils.getSoftware();
                let softwareProject = new SoftwareProject({project, software: tempSoft.id});
                await softwareProject.save();
                return softwareProject;
            }

            let softwareProjectPromises = [];
            for(let i = 0; i < nbSoftwareProjects; i++) {
                softwareProjectPromises.push(createSoftwareAndSoftwareProject());
            }
            softwareProjects = await Promise.all(softwareProjectPromises);
        });

        after(async function() {
            let deletionPromises = softwareProjects.map(softwareProject => SoftwareProject.delete(softwareProject.id));
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new SoftwareProjectCollection().fetchAll();
                expect(collection).to.be.an.instanceof(SoftwareProjectCollection);
                expect(collection).to.have.lengthOf.at.least(nbSoftwareProjects);
                totalNb = collection.length;
            });

            it("Fetch (static method)", async function() {
                let collection = await SoftwareProjectCollection.fetchAll();
                expect(collection).to.be.an.instanceof(SoftwareProjectCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it("Fetch with several requests", async function() {
                let collection = await SoftwareProjectCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
                expect(collection).to.be.an.instanceof(SoftwareProjectCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it("Fetch with project filter", async function() {
                let collection = await new SoftwareProjectCollection({filterKey: "project", filterValue: project}).fetchAll();
                expect(collection).to.be.an.instanceof(SoftwareProjectCollection);
                expect(collection).to.have.lengthOf(nbSoftwareProjects);
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await SoftwareProjectCollection.fetchAll();
                for(let softwareProject of collection) {
                    expect(softwareProject).to.be.an.instanceof(SoftwareProject);
                }
            });

            it("Add item to the collection", function() {
                let collection = new SoftwareProjectCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new SoftwareProject());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new SoftwareProjectCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new SoftwareProjectCollection({nbPerPage});
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new SoftwareProjectCollection({nbPerPage});
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new SoftwareProjectCollection({nbPerPage});
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
