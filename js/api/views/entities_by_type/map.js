/*View mapping function to collate entities by type*/

function(doc) {
    var org = doc.organization;

    if (doc.type) {
        if(doc.owner) {
            emit([org, doc.owner, doc.type], null);
            if(doc.type === "Project") {
                emit([org, doc._id, doc.type], null);
            }
        }

        if(doc.user) {
            emit([org, doc.user, doc.type], null);
        }

        if(doc.user_id) {
            emit([org, doc.user_id, doc.type], null);
        }

        if (doc.links && doc.links._collaboration_roots) {
            var roots = doc.links._collaboration_roots;
            for (var i = 0; i < roots.length; i++) {
                emit([org, roots[i], doc.type], null);
            }
        }
    }
}
