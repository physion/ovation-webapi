/*View mapping function to collate entities by type*/

function(doc) {
    if (doc.links && doc.links._collaboration_roots) {
        var roots = doc.links._collaboration_roots;
        for (var i = 0; i < roots.length; i++) {
            emit(roots[i], null);
        }
    }
}
