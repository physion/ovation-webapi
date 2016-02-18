/*View mapping function to get documents by ID and authorization ID*/

function(doc) {
    if(doc.owner) {
        emit([doc.owner, doc._id], null);
        if(doc.type === "Project") {
            emit([doc._id, doc._id], null);
        }
    }

    if(doc.user) {
        emit([doc.user, doc._id], null);
    }

    if(doc.user_id) {
        emit([doc.user_id, doc._id], null);
    }

    if (doc.links && doc.links._collaboration_roots) {
        var roots = doc.links._collaboration_roots;
        for (var i = 0; i < roots.length; i++) {
            emit([roots[i], doc._id], null);
        }
    }
}
