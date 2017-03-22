/*View mapping function to get documents by ID and authorization ID*/

function(doc) {
    var org = doc.organization;

    if(doc.owner) {
        emit([org, doc.owner, doc._id], null);
        if(doc.type === "Project") {
            emit([org, doc._id, doc._id], null);
        }
    }

    //Annotation docs
    if(doc.user) {
        emit([org, doc.user, doc._id], null);
    }

    //Link docs
    if(doc.user_id) {
        emit([org, doc.user_id, doc._id], null);
    }

    if (doc.links && doc.links._collaboration_roots) {
        var roots = doc.links._collaboration_roots;
        for (var i = 0; i < roots.length; i++) {
            emit([org, roots[i], doc._id], null);
        }
    }
}
