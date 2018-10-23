import * as utils from "./utils.js";
import {UserGroup, UserGroupCollection} from "@";

describe("UserGroup", function() {

    let group;
    let user;

    let userGroup;

    before(async function() {
        await utils.connect(true);
        ({id: user} = await utils.getUser());
        ({id: group} = await utils.getGroup());
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            userGroup = new UserGroup({group, user});
            userGroup = await userGroup.save();
            expect(userGroup).to.be.an.instanceof(UserGroup);
            expect(userGroup.id).to.exist;
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedUserGroup = await UserGroup.fetch(user, group);
            expect(fetchedUserGroup).to.be.an.instanceof(UserGroup);
            expect(fetchedUserGroup).to.deep.equal(userGroup);
        });

        it("Fetch with instance method", async function() {
            let fetchedUserGroup = await new UserGroup({user, group}).fetch();
            expect(fetchedUserGroup).to.be.an.instanceof(UserGroup);
            expect(fetchedUserGroup).to.deep.equal(userGroup);
        });

        it("Fetch with wrong ID", function() {
            expect(UserGroup.fetch(0)).to.be.rejected;
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await UserGroup.delete(user, group);
        });

        it("Fetch deleted", function() {
            expect(UserGroup.fetch(user, group)).to.be.rejected;
        });
    });

    // --------------------

    describe("UserGroupCollection", function() {

        let nbUserGroups = 3;
        let userGroups;

        before(async function() {
            async function createGroupAndUserGroup() {
                let tempGroup = await utils.getGroup();
                let userGroup = new UserGroup({user, group: tempGroup.id});
                await userGroup.save();
                return userGroup;
            }

            let userGroupPromises = [];
            for(let i = 0; i < nbUserGroups; i++) {
                userGroupPromises.push(createGroupAndUserGroup());
            }
            userGroups = await Promise.all(userGroupPromises);
        });

        after(async function() {
            let deletionPromises = userGroups.map(userGroup => UserGroup.delete(userGroup.user, userGroup.group));
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new UserGroupCollection({filterKey: "user", filterValue: user}).fetchAll();
                expect(collection).to.be.an.instanceof(UserGroupCollection);
                expect(collection).to.have.lengthOf(nbUserGroups);
            });

            it("Fetch (static method)", async function() {
                let collection = await UserGroupCollection.fetchAll({filterKey: "user", filterValue: user});
                expect(collection).to.be.an.instanceof(UserGroupCollection);
                expect(collection).to.have.lengthOf(nbUserGroups);
            });

            it("Fetch with several requests", async function() {
                let collection = await UserGroupCollection.fetchAll({nbPerPage: 1,
                    filterKey: "user", filterValue: user});
                expect(collection).to.be.an.instanceof(UserGroupCollection);
                expect(collection).to.have.lengthOf(nbUserGroups);
            });

            it("Fetch without filter", async function() {
                let collection = new UserGroupCollection();
                expect(collection.fetchAll()).to.be.rejected;
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await UserGroupCollection.fetchAll({filterKey: "user", filterValue: user});
                for(let userGroup of collection) {
                    expect(userGroup).to.be.an.instanceof(UserGroup);
                }
            });

            it("Add item to the collection", function() {
                let collection = new UserGroupCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new UserGroup());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new UserGroupCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new UserGroupCollection({nbPerPage, filterKey: "user", filterValue: user});
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new UserGroupCollection({nbPerPage, filterKey: "user", filterValue: user});
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new UserGroupCollection({nbPerPage, filterKey: "user", filterValue: user});
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
