function (doc) {
    var org = doc.organization;

    if(doc.type && doc.type === 'Revision') {
        if(!doc.trash_info) {
            if(doc.attributes && doc.attributes.content_length) {
                if(org === 0) {
                    emit([org, doc.owner, doc._id], doc.attributes.content_length);
                }

                if (doc.links && doc.links._collaboration_roots) {
                    var roots = doc.links._collaboration_roots;
                    for (var i = 0; i < roots.length; i++) {
                        emit([org, roots[i], doc.owner, doc._id], doc.attributes.content_length);
                    }
                }
            }
        }
    }
}
