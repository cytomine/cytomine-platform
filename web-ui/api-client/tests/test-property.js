import * as utils from "./utils.js";
import {Property, PropertyCollection, ImageInstance, User} from "@";

describe("Property", function() {

    let annotation = null;
    let key = "TEST_JS_KEY";
    let value = utils.randomString();

    let property = null;
    let id = 0;

    before(async function() {
        await utils.connect();
        annotation = await utils.getAnnotation();
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            property = new Property({key, value}, annotation);
            await property.save();
            expect(property).to.be.an.instanceof(Property);
            id = property.id;
            expect(id).to.exist;
            expect(property.value).to.equal(value);
        });

        it("Create without providing associated object", async function() {
            let propertyWithoutObject = new Property({key, value});
            expect(propertyWithoutObject.save()).to.be.rejected;
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedProperty = await Property.fetch(id, annotation);
            expect(fetchedProperty).to.be.an.instanceof(Property);
            expect(fetchedProperty.domainIdent).to.equal(annotation.id);
            expect(fetchedProperty.value).to.equal(value);
        });

        it("Fetch with instance method", async function() {
            let fetchedProperty = await new Property({id}, annotation).fetch();
            expect(fetchedProperty).to.be.an.instanceof(Property);
            expect(fetchedProperty.domainIdent).to.equal(annotation.id);
            expect(fetchedProperty.value).to.equal(value);
        });

        it("Fetch without providing associated object", function() {
            expect(Property.fetch({id})).to.be.rejected;
        });
    });

    describe("Update", function() {
        it("Update", async function() {
            let newValue = utils.randomString();
            property.value = newValue;
            property = await property.update();
            expect(property).to.be.an.instanceof(Property);
            expect(property.value).to.equal(newValue);
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await Property.delete(id, annotation);
        });

        it("Fetch deleted", function() {
            expect(Property.fetch(annotation)).to.be.rejected;
        });
    });

    // --------------------

    describe("PropertyCollection", function() {
        let nbPropertiesAnnot = 3;
        let nbPropertiesImage = 1;
        let properties;

        let image;

        before(async function() {
            image = await ImageInstance.fetch(annotation.image);

            let propertiesPromises = [];
            for(let i = 0; i < nbPropertiesAnnot; i++) {
                propertiesPromises.push(new Property({key, value: utils.randomString()}, annotation).save());
            }
            for(let i = 0; i < nbPropertiesImage; i++) {
                propertiesPromises.push(new Property({key, value: utils.randomString()}, image).save());
            }
            properties = await Promise.all(propertiesPromises);
        });

        after(async function() {
            let deletionPromises = properties.map(property => Property.delete(property.id, annotation));
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new PropertyCollection(annotation).fetch();
                expect(collection).to.be.an.instanceof(PropertyCollection);
                expect(collection).to.have.lengthOf(nbPropertiesAnnot);
            });

            it("Fetch (static method)", async function() {
                let collection = await PropertyCollection.fetch(image);
                expect(collection).to.be.an.instanceof(PropertyCollection);
                expect(collection).to.have.lengthOf(nbPropertiesImage);
            });

            it("Fetch with several requests", async function() {
                let collection = await PropertyCollection.fetch(annotation, 1);
                expect(collection).to.be.an.instanceof(PropertyCollection);
                expect(collection).to.have.lengthOf(nbPropertiesAnnot);
            });

            it("Fetch without associated object", async function() {
                expect(PropertyCollection.fetch()).to.be.rejected;
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await PropertyCollection.fetch(annotation);
                for(let property of collection) {
                    expect(property).to.be.an.instanceof(Property);
                }
            });

            it("Add item to the collection", function() {
                let collection = new PropertyCollection(annotation);
                expect(collection).to.have.lengthOf(0);
                collection.push(new Property({}, annotation));
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new PropertyCollection(annotation);
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Specific operations", function() {
            it("Fetch keys of annotation properties", async function() {
                let keys = await PropertyCollection.fetchKeysAnnotationProperties(null, image.id);
                expect(keys).to.be.an.instanceof(Array);
                expect(keys).to.have.lengthOf(1);
                expect(keys[0]).to.equal(key);
            });

            it("Fetch keys of image properties", async function() {
                let keys = await PropertyCollection.fetchKeysImageProperties(image.project);
                expect(keys).to.be.an.instanceof(Array);
                expect(keys).to.have.lengthOf(1);
                expect(keys[0]).to.equal(key);
            });

            it("Fetch the properties positions and values", async function() {
                let currentUser = await User.fetchCurrent();
                let props = await PropertyCollection.fetchPropertiesValuesAndPositions(currentUser.id, image.id, key);
                let listId = props.map(item => item.idAnnotation);
                let listValues = props.map(item => item.value);
                listId.forEach(id => {
                    expect(id).to.equal(annotation.id);
                });
                properties.forEach(prop => {
                    if(prop.domainIdent == annotation.id && prop.domainClassName == annotation.class) {
                        expect(listValues).to.include(prop.value);
                    }
                });
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new PropertyCollection(annotation, nbPerPage);
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new PropertyCollection(annotation, nbPerPage);
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new PropertyCollection(annotation, nbPerPage);
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
