/*View mapping function to collate entities by type*/

function(doc) {
    if (doc.type) {
        if(doc.owner) {
            emit([doc.owner, doc.type], null);
            if(doc.type === "Project") {
                emit([doc._id, doc.type], null);
            }
        }

        if(doc.user) {
            emit([doc.user, doc.type], null);
        }

        if(doc.user_id) {
            emit([doc.user_id, doc.type], null);
        }

        if (doc.links && doc.links._collaboration_roots) {
            var roots = doc.links._collaboration_roots;
            for (var i = 0; i < roots.length; i++) {
                emit([roots[i], doc.type], null);
            }
        }
    }
}
