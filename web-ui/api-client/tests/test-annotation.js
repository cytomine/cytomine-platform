import * as utils from "./utils.js";
import {Annotation, AnnotationType, AnnotationCollection, User} from "@";

describe("Annotation", function() {

    let location = "POLYGON ((10 10, 10 20, 20 20, 20 10, 15 10, 10 10), (16 16, 18 16, 18 18, 16 18, 16 16))";
    let project;
    let image;

    let annotation = null;
    let id = 0;

    before(async function() {
        this.timeout(10000);
        await utils.connect();
        ({id: project} = await utils.getProject()); // HACK required for "Save collection" test case
        let imageInstance = await utils.getImageInstance({project, review: true});
        await imageInstance.review();
        image = imageInstance.id;
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            annotation = new Annotation({location, image});
            annotation = await annotation.save();
            id = annotation.id;
            expect(annotation).to.be.an.instanceof(Annotation);
            expect(annotation.type).to.equal(AnnotationType.USER);
            expect(id).to.exist;
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedAnnotation = await Annotation.fetch(id);
            expect(fetchedAnnotation).to.be.an.instanceof(Annotation);
            expect(fetchedAnnotation).to.deep.equal(annotation);
        });

        it("Fetch with type", async function() {
            let fetchedAnnotation = await Annotation.fetch(id, AnnotationType.USER);
            expect(fetchedAnnotation).to.be.an.instanceof(Annotation);
            expect(fetchedAnnotation).to.deep.equal(annotation);
        });

        it("Fetch with instance method", async function() {
            let fetchedAnnotation = await new Annotation({id}).fetch();
            expect(fetchedAnnotation).to.be.an.instanceof(Annotation);
            expect(fetchedAnnotation).to.deep.equal(annotation);
        });

        it("Fetch with wrong ID", function() {
            expect(Annotation.fetch(0)).to.be.rejected;
        });
    });

    describe("Specific operations", function() {
        it("[Correction] Add", async function() {
            let currentUser = await User.fetchCurrent();
            let initialArea = annotation.area;
            let result = await Annotation.correctAnnotations(image, "POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))",
                false, false, [currentUser.id]);
            expect(result.id).to.equal(annotation.id);
            expect(result.area).to.be.above(initialArea);
        });

        it("[Correction] Remove", async function() {
            let currentUser = await User.fetchCurrent();
            let initialArea = annotation.area;
            let result = await Annotation.correctAnnotations(image, "POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))",
                false, true, [currentUser.id]);
            expect(result.id).to.equal(annotation.id);
            expect(result.area).to.be.below(initialArea);
        });

        it("Review", async function() {
            let reviewedAnnotation = await annotation.review();
            expect(reviewedAnnotation).to.be.instanceof(Annotation);
            expect(reviewedAnnotation.type).to.equal(AnnotationType.REVIEWED);
            expect(reviewedAnnotation.parentIdent).to.equal(annotation.id);
            await annotation.fetch();
            expect(annotation.reviewed).to.be.true;
        });

        it("Cancel review", async function() {
            await annotation.cancelReview();
            await annotation.fetch();
            expect(annotation.reviewed).to.be.false;
        });

        it("Simplify", async function() {
            await annotation.simplify(5, 10);
        });
    });

    describe("Update", function() {
        it("Update", async function() {
            let newLocation = "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))";
            annotation.location = newLocation;
            await annotation.update();
            expect(annotation).to.be.an.instanceof(Annotation);
            expect(annotation.centroid).to.deep.equal({x: 15, y: 15});
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await Annotation.delete(id);
        });

        it("Fetch a deleted element", function() {
            expect(Annotation.fetch(id)).to.be.rejected;
        });
    });

    // --------------------

    describe("AnnotationCollection", function() {
        let nb = 3;

        before(async function() {
            let annotationPromises = [];
            for(let i = 0; i < nb; i++) {
                annotationPromises.push(new Annotation({image, location}).save());
            }
            await Promise.all(annotationPromises);
        });

        after(async function() {
            let collection = await new AnnotationCollection({image}).fetch();
            let deletionPromises = [];
            for(let annot of collection) {
                deletionPromises.push(Annotation.delete(annot.id));
            }
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new AnnotationCollection({image}).fetch();
                expect(collection).to.be.an.instanceof(AnnotationCollection);
                expect(collection).to.have.lengthOf(nb);
            });

            it("Fetch (static method)", async function() {
                let collection = await AnnotationCollection.fetch({image});
                expect(collection).to.be.an.instanceof(AnnotationCollection);
                expect(collection).to.have.lengthOf(nb);
            });

            it("Fetch with several requests", async function() {
                let collection = await AnnotationCollection.fetch({image}, 1);
                expect(collection).to.be.an.instanceof(AnnotationCollection);
                expect(collection).to.have.lengthOf(nb);
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through the collection", async function() {
                let collection = await AnnotationCollection.fetch({image});
                for(let annot of collection) {
                    expect(annot).to.be.an.instanceof(Annotation);
                }
            });

            it("Add an item to the collection", function() {
                let collection = new AnnotationCollection({image});
                expect(collection).to.have.lengthOf(0);
                collection.push(new Annotation());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new AnnotationCollection({image});
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Save collection", function() {
            it("Save", async function() {
                let collection = new AnnotationCollection();
                collection.push(new Annotation({location, image, project}));
                collection.push(new Annotation({location, image, project}));
                await collection.save();
                expect(collection).to.have.lengthOf(2);
            });
        });

        describe("Search service", function() {
            it("Search", async function() {
                let collection = new AnnotationCollection({project});
                collection.project = project;
                collection.terms = [1, 2];
                collection.showTerm = true;
                await collection.fetch();
                expect(collection).to.have.lengthOf(0);
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new AnnotationCollection({project}, nbPerPage);
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new AnnotationCollection({project}, nbPerPage);
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new AnnotationCollection({project}, nbPerPage);
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
