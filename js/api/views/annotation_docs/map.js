/* View for annotation documents by
 * [entity Id, user Id, annotation type]
 */
function(doc) {
    if(doc.user && doc.entity && doc.annotation_type) {
        emit(doc.entity, null);
        emit([doc.entity, doc.annotation_type], null);
        emit([doc.entity, doc.user, doc.annotation_type], null);
    }
}
