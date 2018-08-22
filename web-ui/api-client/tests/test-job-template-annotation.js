import * as utils from "./utils.js";
import {Job, JobTemplateAnnotation, JobTemplateAnnotationCollection} from "@";

describe("JobTemplateAnnotation", function() {

    let jobTemplate;
    let annotationIdent;
    let annotation; // alias to simplify tests on collection

    let jobTemplateAnnotation = null;
    let id = 0;
    let job = null;

    before(async function() {
        await utils.connect();
        let project;
        ({id: annotationIdent, project} = await utils.getAnnotation());
        annotation = annotationIdent;
        ({id: jobTemplate} = await utils.getJobTemplate({project}));
    });

    after(async function() {
        if(job != null) {
            await Job.delete(job.id);
        }
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            jobTemplateAnnotation = new JobTemplateAnnotation({jobTemplate, annotationIdent});
            job = await jobTemplateAnnotation.save();
            id = jobTemplateAnnotation.id;
            expect(id).to.exist;
            expect(job).to.be.an.instanceof(Job);
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedJobTemplateAnnotation = await JobTemplateAnnotation.fetch(id);
            expect(fetchedJobTemplateAnnotation).to.be.an.instanceof(JobTemplateAnnotation);
            expect(fetchedJobTemplateAnnotation.getPublicProperties()).to.deep.equal(jobTemplateAnnotation.getPublicProperties());
        });

        it("Fetch with instance method", async function() {
            let fetchedJobTemplateAnnotation = await new JobTemplateAnnotation({id}).fetch();
            expect(fetchedJobTemplateAnnotation).to.be.an.instanceof(JobTemplateAnnotation);
            expect(fetchedJobTemplateAnnotation.getPublicProperties()).to.deep.equal(jobTemplateAnnotation.getPublicProperties());
        });

        it("Fetch with wrong ID", function() {
            expect(JobTemplateAnnotation.fetch(0)).to.be.rejected;
        });
    });

    describe("Update", function() {
        it("Update", async function() {
            expect(jobTemplateAnnotation.update.bind(jobTemplateAnnotation, {})).to.throw();
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await JobTemplateAnnotation.delete(id);
        });

        it("Fetch deleted", function() {
            expect(JobTemplateAnnotation.fetch(id)).to.be.rejected;
        });
    });

    // --------------------

    describe("JobTemplateAnnotationCollection", function() {

        let nbJobTemplateAnnotations = 5;
        let jobTemplateAnnotations = [];
        let jobs;

        before(async function() {
            let jobTemplateAnnotationPromises = [];
            for(let i = 0; i < nbJobTemplateAnnotations; i++) {
                let jobTemplateAnnot = new JobTemplateAnnotation({jobTemplate, annotationIdent});
                jobTemplateAnnotations.push(jobTemplateAnnot);
                jobTemplateAnnotationPromises.push(jobTemplateAnnot.save());
            }
            jobs = await Promise.all(jobTemplateAnnotationPromises);
        });

        after(async function() {
            let deletionPromises = jobTemplateAnnotations.map(jobTemplateAnnotation =>
                JobTemplateAnnotation.delete(jobTemplateAnnotation.id));
            let jobDeletionPromises = jobs.map(job => Job.delete(job.id));
            deletionPromises.push(...jobDeletionPromises);
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new JobTemplateAnnotationCollection({annotation}).fetch();
                expect(collection).to.be.an.instanceof(JobTemplateAnnotationCollection);
                expect(collection).to.have.lengthOf(nbJobTemplateAnnotations);
            });

            it("Fetch (static method)", async function() {
                let collection = await JobTemplateAnnotationCollection.fetch({annotation});
                expect(collection).to.be.an.instanceof(JobTemplateAnnotationCollection);
                expect(collection).to.have.lengthOf(nbJobTemplateAnnotations);
            });

            it("Fetch with several requests", async function() {
                let collection = await JobTemplateAnnotationCollection.fetch({annotation}, 1);
                expect(collection).to.be.an.instanceof(JobTemplateAnnotationCollection);
                expect(collection).to.have.lengthOf(nbJobTemplateAnnotations);
            });

            it("Fetch with project and software filters", async function() {
                let collection = await new JobTemplateAnnotationCollection({jobtemplate: jobTemplate, annotation}).fetch();
                expect(collection).to.be.an.instanceof(JobTemplateAnnotationCollection);
                expect(collection).to.have.lengthOf(nbJobTemplateAnnotations);
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await JobTemplateAnnotationCollection.fetch({jobtemplate: jobTemplate});
                for(let jobTemplateAnnotation of collection) {
                    expect(jobTemplateAnnotation).to.be.an.instanceof(JobTemplateAnnotation);
                }
            });

            it("Add item to the collection", function() {
                let collection = new JobTemplateAnnotationCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new JobTemplateAnnotation());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new JobTemplateAnnotationCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new JobTemplateAnnotationCollection({annotation}, nbPerPage);
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new JobTemplateAnnotationCollection({annotation}, nbPerPage);
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new JobTemplateAnnotationCollection({annotation}, nbPerPage);
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
