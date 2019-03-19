import * as utils from "./utils.js";
import {AnnotationComment, AnnotationCommentCollection} from "@";

describe("AnnotationComment", function() {

    let annotation = null;
    let receivers = null;
    let text = utils.randomString();

    let annotationComment = null;
    let id = 0;

    before(async function() {
        await utils.connect();
        let user = await utils.getUser();
        receivers = [user.id];
        annotation = await utils.getAnnotation();
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            annotationComment = new AnnotationComment({comment: text, receivers}, annotation);
            await annotationComment.save();
            expect(annotationComment).to.be.an.instanceof(AnnotationComment);
            id = annotationComment.id;
            expect(id).to.exist;
            expect(annotationComment.comment).to.equal(text);
        });

        it("Create without providing annotation", async function() {
            let annotationCommentWithoutObject = new AnnotationComment({comment: text});
            expect(annotationCommentWithoutObject.save()).to.be.rejected;
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedAnnotationComment = await AnnotationComment.fetch(id, annotation);
            expect(fetchedAnnotationComment).to.be.an.instanceof(AnnotationComment);
            expect(fetchedAnnotationComment.domainIdent).to.equal(annotation.id);
            expect(fetchedAnnotationComment.comment).to.equal(text);
        });

        it("Fetch with instance method", async function() {
            let fetchedAnnotationComment = await new AnnotationComment({id}, annotation).fetch();
            expect(fetchedAnnotationComment).to.be.an.instanceof(AnnotationComment);
            expect(fetchedAnnotationComment.domainIdent).to.equal(annotation.id);
            expect(fetchedAnnotationComment.comment).to.equal(text);
        });

        it("Fetch without providing associated object", function() {
            expect(AnnotationComment.fetch({id})).to.be.rejected;
        });
    });

    describe("Update", function() {
        it("Update", function() {
            expect(annotationComment.update.bind(annotationComment)).to.throw();
        });
    });

    describe("Delete", function() {
        it("Delete", function() {
            expect(annotationComment.delete.bind(annotationComment)).to.throw();
        });
    });

    // --------------------

    describe("AnnotationCommentCollection", function() {
        let nbComments = 3;

        before(async function() {
            let commentsPromises = [];
            for(let i = 0; i < nbComments - 1; i++) {
                commentsPromises.push(new AnnotationComment({comment: utils.randomString(), receivers}, annotation).save());
            }
            await Promise.all(commentsPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new AnnotationCommentCollection({annotation}).fetchAll();
                expect(collection).to.be.an.instanceof(AnnotationCommentCollection);
                expect(collection).to.have.lengthOf(nbComments);
            });

            it("Fetch (static method)", async function() {
                let collection = await AnnotationCommentCollection.fetchAll({annotation});
                expect(collection).to.be.an.instanceof(AnnotationCommentCollection);
                expect(collection).to.have.lengthOf(nbComments);
            });

            it("Fetch with several requests", async function() {
                let collection = await AnnotationCommentCollection.fetchAll({nbPerPage: 1, annotation});
                expect(collection).to.be.an.instanceof(AnnotationCommentCollection);
                expect(collection).to.have.lengthOf(nbComments);
            });

            it("Fetch without associated object", async function() {
                expect(AnnotationCommentCollection.fetchAll()).to.be.rejected;
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await AnnotationCommentCollection.fetchAll({annotation});
                for(let annotationComment of collection) {
                    expect(annotationComment).to.be.an.instanceof(AnnotationComment);
                }
            });

            it("Add item to the collection", function() {
                let collection = new AnnotationCommentCollection({annotation});
                expect(collection).to.have.lengthOf(0);
                collection.push(new AnnotationComment({}, annotation));
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new AnnotationCommentCollection(annotation);
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new AnnotationCommentCollection({nbPerPage, annotation});
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new AnnotationCommentCollection({nbPerPage, annotation});
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new AnnotationCommentCollection({nbPerPage, annotation});
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
