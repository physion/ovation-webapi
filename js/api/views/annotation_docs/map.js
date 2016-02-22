/* View for annotation documents by
 * [entity Id, user Id, annotation type]
 */
function(doc) {
    if(doc.user && doc.entity && doc.annotation_type) {
        emit([doc.user, doc.entity], null);
        emit([doc.user, doc.entity, doc.annotation_type], null);
        emit([doc.user, doc.entity, doc.user, doc.annotation_type], null);

        if (doc.links && doc.links._collaboration_roots) {
            var roots = doc.links._collaboration_roots;
            for (var i = 0; i < roots.length; i++) {
                emit([roots[i], doc.entity], null);
                emit([roots[i], doc.entity, doc.annotation_type], null);
                emit([roots[i], doc.entity, doc.user, doc.annotation_type], null);
            }
        }
    }
}
