// Load the Cloudant library.
var Cloudant = require('cloudant');

var compile = require('couchdb-compile');

// Compile _api view documents
compile(process.argv[2], function(err, compiled_view_doc) {
    if(err) {
        return console.log('Unable to compile view document: ' + err.message);
    }

    console.log(compiled_view_doc);

    var cloudant_user = process.env.CLOUDANT_USER;
    var cloudant_password = process.env.CLOUDANT_PASSWORD;

    // Upload compiled view document
    Cloudant({account:cloudant_user, password:cloudant_password}, function(err, cloudant) {
        if (err) {
            return console.log('Failed to initialize Cloudant: ' + err.message);
        }

        if(process.argv.length > 3) {
            var db = cloudant.db.use(process.argv[3]);
            db.insert(compiled_view_doc, function (err, body) {
                if (err) {
                    console.log('Unable to insert ' + compiled_view_doc._id + ': ' + body);
                }

                console.log('Inserted ' + compiled_view_doc._id);
            });
        } else {
            cloudant.db.list(function(err, allDbs) {
                if (err) {
                    return console.log('Failed to list databases: ' + err.message);
                }

                allDbs.foreach(function(db) {
                    db.insert(compiled_view_doc, function(err, body) {
                        if (err) {
                            console.log('Unable to insert ' + compiled_view_doc._id + ': ' + body);
                        } else {
                            console.log('Inserted ' + db + '/' + compiled_view_doc._id);
                        }

                    })
                })
            });
        }
    });
});
