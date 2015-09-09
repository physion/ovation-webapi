// Load the Cloudant library.
var Cloudant = require('cloudant');
var compile = require('couchdb-compile');


function update_view(cloudant, db_name, compiled_view_doc) {
    var db = cloudant.db.use(db_name);

    db.get(compiled_view_doc._id, function (err, body) {
        if (err) {
            db.insert(compiled_view_doc, function (err, body) {
                if (err) {
                    console.log('Unable to insert ' + compiled_view_doc._id + ': ' + err.message);
                }

                console.log('Inserted ' + db_name + '/' + compiled_view_doc._id);
            });
        } else {
            compiled_view_doc._rev = body._rev;
            db.insert(compiled_view_doc, function (err, body) {
                if (err) {
                    console.log('Unable to insert ' + compiled_view_doc._id + ': ' + err.message);
                }

                console.log('Inserted ' + db_name + '/' + compiled_view_doc._id);
            });
        }
    });
}


// Compile _api view documents
compile(process.argv[2], function(err, compiled_view_doc) {
    if(err) {
        return console.log('Unable to compile view document: ' + err.message);
    }

    var cloudant_user = process.env.CLOUDANT_USER;
    var cloudant_password = process.env.CLOUDANT_PASSWORD;

    // Upload compiled view document
    Cloudant({account:cloudant_user, password:cloudant_password}, function(err, cloudant) {
        if (err) {
            return console.log('Failed to initialize Cloudant: ' + err.message);
        }

        if(process.argv.length > 3) {
            update_view(cloudant, process.argv[3], compiled_view_doc);
        } else {
            cloudant.db.list(function(err, allDbs) {
                if (err) {
                    return console.log('Failed to list databases: ' + err.message);
                }

                allDbs.forEach(function (db) {
                    if (db.substring(0, 3) == 'db-') {
                        update_view(cloudant, db, compiled_view_doc);
                    }
                })
            });
        }
    });
});
