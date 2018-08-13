import * as utils from "./utils.js";
import {User, UserCollection} from "@";

describe("User", function() {

    let name = utils.randomString();
    let email = name + "@cytomine.coop";

    let project;

    let user = null;
    let id = 0;

    before(async function() {
        this.timeout(10000);
        await utils.connect(true);
        project = await utils.getProject();
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            user = new User({username: name, password: name, firstname: name, lastname: name, email});
            user = await user.save();
            id = user.id;
            expect(id).to.exist;
            expect(user.username).to.equal(name);
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedUser = await User.fetch(id);
            expect(fetchedUser).to.be.an.instanceof(User);
            expect(fetchedUser.username).to.equal(user.username);
        });

        it("Fetch with instance method", async function() {
            let fetchedUser = await new User({id}).fetch();
            expect(fetchedUser).to.be.an.instanceof(User);
            expect(fetchedUser.username).to.equal(user.username);
        });

        it("Fetch with wrong ID", function() {
            expect(User.fetch(0)).to.be.rejected;
        });
    });

    describe("Specific operations", function() {
        it("Fetch current user", async function() {
            let currentUser = await User.fetchCurrent();
            expect(currentUser).to.be.an.instanceof(User);
        });

        it("Fetch number of annotations user", async function() {
            let result = await user.fetchNbAnnotations(false);
            expect(result).to.be.finite;
            result = await user.fetchNbAnnotations(true);
            expect(result).to.be.finite;
        });

        it.skip("Fetch keys", async function() { // Bug in backend
            let keys = await user.fetchKeys();
            expect(keys.publicKey).to.exist;
            expect(keys.privateKey).to.exist;
        });

        it("Regenerate keys", async function() {
            await user.regenerateKeys();
            // TODO: once bug in backend preventing from fetching keys is solved, check the values of the keys
        });

        it("Fetch friends", async function() {
            let friends = await user.fetchFriends();
            expect(friends).to.be.instanceof(UserCollection);
        });

        it("Fetch activity resume", async function() {
            let activity = await user.fetchResumeActivity(project.id);
            expect(activity.firstConnection).to.be.null;
            expect(activity.lastConnection).to.be.null;
            expect(activity.totalAnnotations).to.equal(0);
            expect(activity.totalConnections).to.equal(0);
        });
    });

    describe("Update", function() {
        it("Update", async function() {
            let newName = utils.randomString();
            user.username = newName;
            user = await user.update();
            expect(user).to.be.an.instanceof(User);
            expect(user.username).to.equal(newName);
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await User.delete(id);
        });

        it("Fetch deleted", function() {
            expect(User.fetch(id)).to.be.rejected;
        });
    });

    // --------------------

    describe("UserCollection", function() {

        let nbUsers = 3;
        let users;
        let totalNb = 0;

        before(async function() {
            let userPromises = [];
            for(let i = 0; i < nbUsers; i++) {
                let name = utils.randomString();
                let email = name + "@cytomine.coop";
                userPromises.push(new User({username: name, password: name, firstname: name, lastname: name, email}).save());
            }
            users = await Promise.all(userPromises);
        });

        after(async function() {
            let deletionPromises = users.map(user => User.delete(user.id));
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new UserCollection().fetch();
                expect(collection).to.be.an.instanceof(UserCollection);
                expect(collection).to.have.lengthOf.at.least(nbUsers);
                totalNb = collection.length;
            });

            it("Fetch (static method)", async function() {
                let collection = await UserCollection.fetch();
                expect(collection).to.be.an.instanceof(UserCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it("Fetch with several requests", async function() {
                let collection = await UserCollection.fetch(Math.ceil(totalNb/3));
                expect(collection).to.be.an.instanceof(UserCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await UserCollection.fetch();
                for(let user of collection) {
                    expect(user).to.be.an.instanceof(User);
                }
            });

            it("Add item to the collection", function() {
                let collection = new UserCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new User());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new UserCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Filtering", function() {
            it("Filter on project", async function() {
                await UserCollection.fetchWithFilter("project", project.id);
            });

            it("Filter on ontology", async function() {
                await new UserCollection(10, "ontology", project.ontology).fetch();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new UserCollection(nbPerPage);
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new UserCollection(nbPerPage);
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new UserCollection(nbPerPage);
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
