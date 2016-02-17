function (doc) {
    if (doc.type && doc.type === "Revision") {
        if (doc.attributes && doc.attributes.file_id && doc.attributes.previous) {
            emit([doc.owner, doc.attributes.file_id], [[doc._id], doc.attributes.previous.length]);

            if (doc.links && doc.links._collaboration_roots) {
                var roots = doc.links._collaboration_roots;
                for (var i = 0; i < roots.length; i++) {
                    emit([roots[i], doc.attributes.file_id], [[doc._id], doc.attributes.previous.length]);
                }
            }
        }
    }
}
