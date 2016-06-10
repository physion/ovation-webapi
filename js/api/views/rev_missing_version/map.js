
function(doc) {
    if (doc.type && doc.type === "Revision") {
        if (doc.links && doc.links._collaboration_roots) {
            var roots = doc.links._collaboration_roots;
            for (var i = 0; i < roots.length; i++) {
                if (doc.owner && doc.attributes && !doc.attributes.version && doc.attributes["created-at"]) {
                    emit([roots[i], doc.owner, doc.attributes["created-at"]], null);
                }
            }
        }
    }
}
