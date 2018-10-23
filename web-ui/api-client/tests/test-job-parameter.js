import * as utils from "./utils.js";
import {JobParameter, JobParameterCollection} from "@";

describe("JobParameter", function() {

    let job;
    let software;
    let softwareParameter;
    let value = utils.randomString();

    let jobParameter = null;
    let id = 0;

    before(async function() {
        await utils.connect(true);
        ({id: softwareParameter, software} = await utils.getSoftwareParameter());
        ({id: job} = await utils.getJob({software}));
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            jobParameter = new JobParameter({job, softwareParameter, value});
            jobParameter = await jobParameter.save();
            id = jobParameter.id;
            expect(jobParameter).to.be.an.instanceof(JobParameter);
            expect(jobParameter.value).to.equal(value);
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedJobParameter = await JobParameter.fetch(id);
            expect(fetchedJobParameter).to.be.an.instanceof(JobParameter);
            expect(fetchedJobParameter).to.deep.equal(jobParameter);
        });

        it("Fetch with instance method", async function() {
            let fetchedJobParameter = await new JobParameter({id}).fetch();
            expect(fetchedJobParameter).to.be.an.instanceof(JobParameter);
            expect(fetchedJobParameter).to.deep.equal(jobParameter);
        });

        it("Fetch with wrong ID", function() {
            expect(JobParameter.fetch(0)).to.be.rejected;
        });
    });

    describe("Update", function() {
        it("Update", async function() {
            let newValue = utils.randomString();
            jobParameter.value = newValue;
            jobParameter = await jobParameter.update();
            expect(jobParameter).to.be.an.instanceof(JobParameter);
            expect(jobParameter.value).to.equal(newValue);
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await JobParameter.delete(id);
        });

        it("Fetch deleted", function() {
            expect(JobParameter.fetch(id)).to.be.rejected;
        });
    });

    // --------------------

    describe("JobParameterCollection", function() {

        let nbJobParameters = 3;
        let jobParameters;
        let totalNb = 0;

        before(async function() {
            let softwareParameters = [softwareParameter];
            for(let i = 0; i < nbJobParameters - 1; i++) {
                let {id: idSoftParam} = await utils.getSoftwareParameter({software});
                softwareParameters.push(idSoftParam);
            }

            let jobParameterPromises = softwareParameters.map(softwareParameter =>
                new JobParameter({softwareParameter, job, value}).save());
            jobParameters = await Promise.all(jobParameterPromises);
        });

        after(async function() {
            let deletionPromises = jobParameters.map(jobParameter => JobParameter.delete(jobParameter.id));
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new JobParameterCollection().fetchAll();
                expect(collection).to.be.an.instanceof(JobParameterCollection);
                expect(collection).to.have.lengthOf.at.least(nbJobParameters);
                totalNb = collection.length;
            });

            it("Fetch (static method)", async function() {
                let collection = await JobParameterCollection.fetchAll();
                expect(collection).to.be.an.instanceof(JobParameterCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it("Fetch with several requests", async function() {
                let collection = await JobParameterCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
                expect(collection).to.be.an.instanceof(JobParameterCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });
        });

        describe("Filtering", function() {
            it("Filter on job (static method)", async function() {
                let collection = await JobParameterCollection.fetchAll({filterKey: "job", filterValue: job});
                expect(collection).to.have.lengthOf(nbJobParameters);
            });

            it("Filter on job (instance method)", async function() {
                let collection = new JobParameterCollection(0);
                collection.setFilter("job", job);
                await collection.fetchAll();
                expect(collection).to.have.lengthOf(nbJobParameters);
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await JobParameterCollection.fetchAll();
                for(let jobParameter of collection) {
                    expect(jobParameter).to.be.an.instanceof(JobParameter);
                }
            });

            it("Add item to the collection", function() {
                let collection = new JobParameterCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new JobParameter());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new JobParameterCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new JobParameterCollection({nbPerPage});
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new JobParameterCollection({nbPerPage});
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new JobParameterCollection({nbPerPage});
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
